#!/usr/bin/env python3
"""Extrae particiones y volúmenes lógicos desde el volcado completo de la eMMC.

Solo lee el respaldo. Cada partición se comprueba contra el SHA-256 calculado en el
dispositivo (device-part-hashes.txt) y cada imagen lógica queda registrada en manifest.json.

Uso: extract_firmware.py <carpeta-respaldo> <carpeta-salida> [--slot a]
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path

SECTOR = 512
CHUNK = 1 << 20
LP_GEOMETRY_MAGIC = 0x616C4467
LP_HEADER_MAGIC = 0x414C5030
LP_TARGET_LINEAR = 0

# Particiones físicas que la ROM puede reutilizar. Las únicas del equipo (factory, param,
# cri_data, rsv, tee...) no se extraen a propósito: nunca entran en una imagen de la ROM.
PHYSICAL = {
    "mmcblk0p20": "boot_a",
    "mmcblk0p22": "init_boot_a",
    "mmcblk0p5": "vendor_boot_a",
    "mmcblk0p12": "dtbo_a",
    "mmcblk0p25": "vbmeta_a",
    "mmcblk0p27": "vbmeta_system_a",
    "mmcblk0p16": "odm_ext_a",
    "mmcblk0p18": "oem_a",
    "mmcblk0p29": "super",
}


def copy_range(src, dst_path, offset, length, digest=None):
    src.seek(offset)
    remaining = length
    with open(dst_path, "wb") as dst:
        while remaining:
            data = src.read(min(CHUNK, remaining))
            if not data:
                raise SystemExit(f"Volcado truncado al escribir {dst_path}")
            dst.write(data)
            if digest:
                digest.update(data)
            remaining -= len(data)


def sha256_file(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        while data := handle.read(CHUNK):
            digest.update(data)
    return digest.hexdigest()


def read_table(blob, base, descriptor):
    offset, count, size = descriptor
    return [blob[base + offset + i * size : base + offset + (i + 1) * size] for i in range(count)]


def parse_super(path):
    with open(path, "rb") as handle:
        handle.seek(4096)
        geometry = handle.read(4096)
        magic, _, _, metadata_max, slots, block_size = struct.unpack_from("<II32sIII", geometry)
        if magic != LP_GEOMETRY_MAGIC:
            raise SystemExit("super sin geometría LP válida")
        handle.seek(4096 * 3)
        blob = handle.read(metadata_max)

    magic, major, minor, header_size = struct.unpack_from("<IHHI", blob)
    if magic != LP_HEADER_MAGIC:
        raise SystemExit("super sin cabecera LP válida")
    tables_size = struct.unpack_from("<I", blob, 44)[0]
    tables_sha = blob[48:80]
    if hashlib.sha256(blob[header_size : header_size + tables_size]).digest() != tables_sha:
        raise SystemExit("Checksum de las tablas LP incorrecto")

    partitions = read_table(blob, header_size, struct.unpack_from("<III", blob, 80))
    extents = read_table(blob, header_size, struct.unpack_from("<III", blob, 92))
    groups = read_table(blob, header_size, struct.unpack_from("<III", blob, 104))
    group_info = []
    for raw in groups:
        name = raw[:36].rstrip(b"\0").decode()
        _, maximum = struct.unpack_from("<IQ", raw, 36)
        group_info.append({"name": name, "maximum_size": maximum})

    logical = []
    for raw in partitions:
        name = raw[:36].rstrip(b"\0").decode()
        attrs, first, count, group = struct.unpack_from("<IIII", raw, 36)
        parts = []
        for ext in extents[first : first + count]:
            sectors, target_type, target_data, source = struct.unpack("<QIQI", ext)
            parts.append((sectors, target_type, target_data, source))
        logical.append({"name": name, "attributes": attrs, "group": group, "extents": parts})
    return {
        "version": f"{major}.{minor}",
        "metadata_max_size": metadata_max,
        "metadata_slots": slots,
        "block_size": block_size,
        "groups": group_info,
        "partitions": logical,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("backup", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--slot", default="a", choices="ab")
    args = parser.parse_args()

    physical_dir = args.output / "physical"
    logical_dir = args.output / "logical"
    physical_dir.mkdir(parents=True, exist_ok=True)
    logical_dir.mkdir(parents=True, exist_ok=True)

    table = {}
    for line in (args.backup / "device-part-hashes.txt").read_text().splitlines():
        fields = line.split()
        if len(fields) == 4:
            table[fields[0]] = (int(fields[1]), int(fields[2]), fields[3])

    manifest = {"physical": {}, "logical": {}}
    with open(args.backup / "mmcblk0-full.img", "rb") as dump:
        for dev, name in PHYSICAL.items():
            start, sectors, expected = table[dev]
            digest = hashlib.sha256()
            target = physical_dir / f"{name}.img"
            copy_range(dump, target, start * SECTOR, sectors * SECTOR, digest)
            if digest.hexdigest() != expected:
                raise SystemExit(f"{name}: el hash no coincide con el del dispositivo")
            manifest["physical"][name] = {"size": sectors * SECTOR, "sha256": expected}
            print(f"OK {name} ({sectors * SECTOR} bytes), hash igual al del dispositivo")

    metadata = parse_super(physical_dir / "super.img")
    suffix = f"_{args.slot}"
    with open(physical_dir / "super.img", "rb") as super_img:
        for part in metadata["partitions"]:
            if not part["extents"]:
                continue
            if not part["name"].endswith(suffix):
                continue
            target = logical_dir / f"{part['name'][: -len(suffix)]}.img"
            with open(target, "wb") as dst:
                for sectors, target_type, data, _ in part["extents"]:
                    if target_type != LP_TARGET_LINEAR:
                        dst.truncate(dst.tell() + sectors * SECTOR)
                        dst.seek(0, 2)
                        continue
                    super_img.seek(data * SECTOR)
                    remaining = sectors * SECTOR
                    while remaining:
                        chunk = super_img.read(min(CHUNK, remaining))
                        dst.write(chunk)
                        remaining -= len(chunk)
            manifest["logical"][target.stem] = {
                "size": target.stat().st_size,
                "sha256": sha256_file(target),
            }
            print(f"OK {target.stem} ({target.stat().st_size} bytes)")

    manifest["super"] = metadata
    (args.output / "manifest.json").write_text(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
