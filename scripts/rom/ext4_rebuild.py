#!/usr/bin/env python3
"""Reconstruye una imagen ext4 de Android quitando rutas, sin montar nada.

Las imágenes de fábrica usan shared_blocks y no admiten escritura, así que se crea un ext4
nuevo copiando cada entrada con su modo, dueño y xattrs (SELinux y capabilities). Al terminar
se relee la imagen nueva y se compara contra la original: cualquier diferencia que no sea una
ruta quitada aborta el proceso.

Uso:
  ext4_rebuild.py list <imagen>
  ext4_rebuild.py build <imagen> <salida.img> [--remove RUTA]... [--replace RUTA=ARCHIVO]...
"""
import argparse
import hashlib
import json
import math
import os
import re
import subprocess
import tempfile
from pathlib import Path

BLOCK = 4096
FIXED_TIME = "20250108200438"
FIXED_UUID = "7c5a7a52-52a5-4b4b-9d55-1a3c8f6e0a10"
FIXED_SEED = "1f4b8a26-6d0e-4c32-8f7a-9b2e4d6c1a55"
FEATURES = (
    "^has_journal,^resize_inode,^metadata_csum,^64bit,^flex_bg,ext_attr,dir_index,filetype,"
    "extent,sparse_super,large_file,huge_file,uninit_bg,dir_nlink,extra_isize"
)
LS_LINE = re.compile(r"^/(\d+)/(\d+)/(\d+)/(\d+)/(.*)/(\d*)/$")
XATTR_LINE = re.compile(r"^\s*(\S+) \(\d+\)(?: = (.*))?$")


def tool(name):
    prefix = subprocess.check_output(["brew", "--prefix", "e2fsprogs"], text=True).strip()
    return f"{prefix}/sbin/{name}"


def quote(path):
    if '"' in path:
        raise SystemExit(f"Ruta no admitida: {path}")
    return f'"{path}"'


def debugfs(image, commands, write=False):
    """Ejecuta comandos en lote y devuelve la salida de cada uno."""
    if not commands:
        return [], ""
    with tempfile.NamedTemporaryFile("w", suffix=".cmd") as script:
        script.write("\n".join(commands) + "\n")
        script.flush()
        env = dict(os.environ, DEBUGFS_PAGER="__none__", E2FSPROGS_FAKE_TIME="1736366678", E2FSCK_TIME="1736366678")
        args = [tool("debugfs")] + (["-w"] if write else []) + ["-f", script.name, str(image)]
        result = subprocess.run(args, env=env, capture_output=True, errors="surrogateescape")
    sections = []
    for line in result.stdout.split("\n"):
        if line.startswith("debugfs: "):
            sections.append([])
        elif sections:
            sections[-1].append(line)
    if len(sections) != len(commands):
        raise SystemExit(f"debugfs falló: {result.stderr[-500:]}")
    return sections, result.stderr


def walk(image):
    entries, pending = {}, ["/"]
    root_section, _ = debugfs(image, ["ls -p /"])
    for line in root_section[0]:
        match = LS_LINE.match(line)
        if match and match[5] == ".":
            entries["/"] = (int(match[1]), int(match[2], 8), int(match[3]), int(match[4]), 0)
    while pending:
        sections, _ = debugfs(image, [f"ls -p {quote(d)}" for d in pending])
        next_dirs = []
        for parent, lines in zip(pending, sections):
            for line in lines:
                match = LS_LINE.match(line)
                if not match or match[5] in (".", ".."):
                    continue
                path = parent.rstrip("/") + "/" + match[5]
                mode = int(match[2], 8)
                if path == "/lost+found":
                    continue
                size = int(match[6]) if match[6] else 0
                entries[path] = (int(match[1]), mode, int(match[3]), int(match[4]), size)
                if mode & 0o170000 == 0o040000:
                    next_dirs.append(path)
        pending = next_dirs
    return entries


def read_xattrs(image, paths):
    """Devuelve {ruta: {nombre: bytes}} leyendo cada xattr a un archivo para no interpretar texto."""
    sections, _ = debugfs(image, [f"ea_list {quote(p)}" for p in paths])
    wanted = []
    for path, lines in zip(paths, sections):
        for line in lines:
            match = XATTR_LINE.match(line)
            if match and not line.startswith("Extended"):
                wanted.append((path, match[1]))
    result = {p: {} for p in paths}
    with tempfile.TemporaryDirectory() as tmp:
        commands = []
        for index, (path, name) in enumerate(wanted):
            commands.append(f"ea_get -f {tmp}/{index} {quote(path)} {name}")
        if commands:
            debugfs(image, commands)
        for index, (path, name) in enumerate(wanted):
            result[path][name] = Path(tmp, str(index)).read_bytes()
    return result


def read_symlinks(image, paths):
    sections, _ = debugfs(image, [f"stat {quote(p)}" for p in paths])
    targets = {}
    for path, lines in zip(paths, sections):
        text = "\n".join(lines)
        match = re.search(r'Fast link dest: "?(.*?)"?\n', text + "\n")
        if not match:
            raise SystemExit(f"Enlace largo sin soporte: {path}")
        targets[path] = match[1]
    return targets


def snapshot(image, blobs):
    """Describe el árbol completo. Los archivos se guardan por inodo para no depender de APFS."""
    entries = walk(image)
    paths = list(entries)
    xattrs = read_xattrs(image, paths)
    links = read_symlinks(image, [p for p in paths if entries[p][1] & 0o170000 == 0o120000])
    regular = {}
    for path in paths:
        if entries[path][1] & 0o170000 == 0o100000:
            regular.setdefault(entries[path][0], path)
    blobs.mkdir(parents=True, exist_ok=True)
    debugfs(image, [f"dump <{inode}> {blobs}/{inode}" for inode in regular])
    tree = {}
    for path in paths:
        inode, mode, uid, gid, size = entries[path]
        item = {"mode": mode, "uid": uid, "gid": gid, "xattrs": {k: v.hex() for k, v in xattrs[path].items()}}
        kind = mode & 0o170000
        if kind == 0o100000:
            blob = blobs / str(inode)
            item["sha256"] = hashlib.sha256(blob.read_bytes()).hexdigest()
            item["size"] = blob.stat().st_size
            item["inode"] = inode
        elif kind == 0o120000:
            item["target"] = links[path]
        elif kind != 0o040000:
            raise SystemExit(f"Tipo de archivo sin soporte: {path}")
        tree[path] = item
    return tree


def size_for(tree):
    blocks = sum(math.ceil(i.get("size", 0) / BLOCK) + 2 for i in tree.values())
    return (int(blocks * 1.06) + 4096) * BLOCK


def create_image(output, tree, blobs, label, extra):
    env = dict(os.environ, E2FSPROGS_FAKE_TIME="1736366678", E2FSCK_TIME="1736366678")
    size = size_for(tree)
    output.unlink(missing_ok=True)
    subprocess.run(
        [tool("mke2fs"), "-q", "-F", "-t", "ext4", "-b", str(BLOCK), "-I", "256", "-m", "0",
         "-N", str(len(tree) + 64), "-L", label, "-U", FIXED_UUID, "-O", FEATURES,
         "-E", f"hash_seed={FIXED_SEED},lazy_itable_init=0", str(output), str(size // BLOCK)],
        check=True, env=env,
    )
    commands, seen = [], {}
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        for path in sorted(tree, key=lambda p: (p.count("/"), p)):
            item = tree[path]
            kind = item["mode"] & 0o170000
            parent, name = os.path.split(path)
            if path != "/":
                commands.append(f"cd {quote(parent)}")
            if kind == 0o040000 and path != "/":
                commands.append(f"mkdir {quote(name)}")
            elif kind == 0o120000:
                commands.append(f"symlink {quote(name)} {quote(item['target'])}")
            elif kind == 0o100000:
                source = item.get("source") or f"{blobs}/{item['inode']}"
                if item.get("inode") in seen:
                    commands.append(f"ln {quote(seen[item['inode']])} {quote(name)}")
                    continue
                if item.get("inode"):
                    seen[item["inode"]] = path
                commands.append(f"write {quote(source)} {quote(name)}")
            commands.append(f"sif {quote(path)} mode 0{item['mode']:o}")
            commands.append(f"sif {quote(path)} uid {item['uid']}")
            commands.append(f"sif {quote(path)} gid {item['gid']}")
            for field in ("atime", "ctime", "mtime", "crtime"):
                commands.append(f"sif {quote(path)} {field} {FIXED_TIME}")
            for attr, value in item["xattrs"].items():
                digest = hashlib.sha256(value.encode()).hexdigest()
                holder = tmp / digest
                holder.write_bytes(bytes.fromhex(value))
                commands.append(f"ea_set -f {holder} {quote(path)} {attr}")
        _, errors = debugfs(output, commands, write=True)
    bad = [l for l in errors.splitlines() if l.strip() and "debugfs 1." not in l]
    if bad:
        raise SystemExit("Errores de debugfs:\n" + "\n".join(bad[:20]))
    check = subprocess.run([tool("e2fsck"), "-fn", str(output)], capture_output=True, text=True, env=env)
    if check.returncode != 0:
        raise SystemExit(f"e2fsck rechazó la imagen:\n{check.stdout[-1500:]}")


def compare(stock, new, removed, added):
    def strip(tree):
        return {p: {k: v for k, v in i.items() if k != "inode"} for p, i in tree.items()}

    stock, new = strip(stock), strip(new)
    differences = []
    for path in sorted(set(stock) | set(new)):
        gone = any(path == r or path.startswith(r + "/") for r in removed)
        if path in added:
            continue
        if gone:
            if path in new:
                differences.append(f"sigue presente: {path}")
        elif stock.get(path) != new.get(path):
            differences.append(f"distinto: {path}")
    return differences


def shrink(image):
    env = dict(os.environ, E2FSPROGS_FAKE_TIME="1736366678", E2FSCK_TIME="1736366678")
    subprocess.run([tool("e2fsck"), "-fy", str(image)], check=True, capture_output=True, env=env)
    subprocess.run([tool("resize2fs"), "-M", str(image)], check=True, capture_output=True, env=env)
    count = re.search(r"Block count:\s+(\d+)", subprocess.check_output(
        [tool("dumpe2fs"), "-h", str(image)], text=True, stderr=subprocess.DEVNULL, env=env))[1]
    os.truncate(image, int(count) * BLOCK)


def rebuild(image, output, remove=(), replace=None, label=""):
    """Crea output desde image, lo verifica y devuelve su descripción."""
    replace = replace or {}
    with tempfile.TemporaryDirectory(dir=image.parent) as tmp:
        blobs = Path(tmp, "blobs")
        tree = snapshot(image, blobs)
        for path in list(remove) + list(replace):
            if path not in tree:
                raise SystemExit(f"La ruta no existe en la imagen: {path}")
        kept = {
            p: dict(i) for p, i in tree.items()
            if not any(p == r or p.startswith(r + "/") for r in remove)
        }
        for path, source in replace.items():
            source = Path(source).resolve()
            kept[path].pop("inode", None)
            kept[path].update(source=str(source), size=source.stat().st_size)
        create_image(output, kept, blobs, label or image.stem, {})
        shrink(output)
        rebuilt = snapshot(output, Path(tmp, "verify"))
        differences = compare(tree, rebuilt, remove, set(replace))
        for path, source in replace.items():
            expected = hashlib.sha256(Path(source).read_bytes()).hexdigest()
            current = {k: v for k, v in rebuilt[path].items() if k not in ("inode", "sha256", "size")}
            original = {k: v for k, v in tree[path].items() if k not in ("inode", "sha256", "size")}
            if rebuilt[path]["sha256"] != expected or current != original:
                differences.append(f"reemplazo incorrecto: {path}")
        if differences:
            raise SystemExit("La imagen no coincide:\n" + "\n".join(differences[:40]))
    return {
        "entries": len(rebuilt),
        "size": output.stat().st_size,
        "sha256": hashlib.sha256(output.read_bytes()).hexdigest(),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    list_cmd = sub.add_parser("list")
    list_cmd.add_argument("image", type=Path)
    build = sub.add_parser("build")
    build.add_argument("image", type=Path)
    build.add_argument("output", type=Path)
    build.add_argument("--remove", action="append", default=[])
    build.add_argument("--replace", action="append", default=[])
    build.add_argument("--label", default="")
    args = parser.parse_args()

    if args.command == "list":
        with tempfile.TemporaryDirectory(dir=args.image.parent) as tmp:
            for path, item in sorted(snapshot(args.image, Path(tmp, "blobs")).items()):
                print(f"{item['mode']:o}\t{item.get('size', 0)}\t{path}")
        return
    replace = dict(item.split("=", 1) for item in args.replace)
    print(json.dumps(rebuild(args.image, args.output, args.remove, replace, args.label)))


if __name__ == "__main__":
    main()
