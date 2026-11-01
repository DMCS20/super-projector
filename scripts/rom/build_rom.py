#!/usr/bin/env python3
"""Construye las imágenes de un perfil a partir de las de fábrica. No toca ningún dispositivo.

Uso: build_rom.py <perfil.json> <carpeta-stock> <carpeta-salida>
"""
import argparse
import hashlib
import json
import shutil
import tempfile
from pathlib import Path

from ext4_rebuild import debugfs, rebuild


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("profile", type=Path)
    parser.add_argument("stock", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    profile = json.loads(args.profile.read_text())
    args.output.mkdir(parents=True, exist_ok=True)
    stock_manifest = json.loads((args.stock / "manifest.json").read_text())
    report = {"profile": args.profile.name, "profile_sha256": hashlib.sha256(args.profile.read_bytes()).hexdigest(), "images": {}}

    for name, info in stock_manifest["logical"].items():
        source = args.stock / "logical" / f"{name}.img"
        target = args.output / f"{name}.img"
        spec = profile["images"].get(name)
        if not spec:
            shutil.copyfile(source, target)
            report["images"][name] = {"size": info["size"], "sha256": info["sha256"], "changed": False}
            print(f"{name}: igual que fábrica")
            continue
        with tempfile.TemporaryDirectory(dir=args.output) as tmp:
            replace = {}
            for path, edits in spec.get("patch", {}).items():
                holder = Path(tmp, f"{len(replace)}.patched")
                debugfs(source, [f'dump "{path}" {holder}'])
                text = holder.read_text()
                for edit in edits:
                    if text.count(edit["old"]) != 1:
                        raise SystemExit(f"{path}: el parche no aplica una sola vez ({edit['why']})")
                    text = text.replace(edit["old"], edit["new"])
                holder.write_text(text)
                replace[path] = holder
            result = rebuild(source, target, spec.get("remove", []), replace, name)
        report["images"][name] = {**result, "changed": True, "removed": spec.get("remove", []), "patched": list(replace)}
        print(f"{name}: {info['size']} -> {result['size']} bytes, verificada")

    total = sum(i["size"] for i in report["images"].values())
    limit = next(g["maximum_size"] for g in stock_manifest["super"]["groups"] if g["name"].endswith("_a"))
    report["total_size"], report["group_limit"] = total, limit
    if total > limit:
        raise SystemExit(f"No cabe en super: {total} > {limit}")
    (args.output / "build-report.json").write_text(json.dumps(report, indent=2))
    print(f"Total {total} de {limit} bytes disponibles")


if __name__ == "__main__":
    main()
