#!/usr/bin/env python3
"""Reject 64-bit native APK libraries incompatible with Android 16 KiB pages."""
import argparse
import struct
from zipfile import ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('apk')
args = parser.parse_args()
checked = 0
with ZipFile(args.apk) as archive:
    for name in archive.namelist():
        if not name.endswith('.so') or not name.startswith(('lib/arm64-v8a/', 'lib/x86_64/')):
            continue
        data = archive.read(name)
        if name.rsplit('/', 1)[-1] in {'libpython.zip.so', 'libffmpeg.zip.so'} and data[:4] == b'PK\x03\x04':
            continue  # Extractor runtime archives are packaged as JNI resources, not ELF.
        if data[:6] != b'\x7fELF\x02\x01':
            raise SystemExit(f'{name}: expected little-endian ELF64')
        offset = struct.unpack_from('<Q', data, 32)[0]
        size, count = struct.unpack_from('<HH', data, 54)
        if size < 56 or not count or offset + size * count > len(data):
            raise SystemExit(f'{name}: invalid ELF program headers')
        loads = 0
        for index in range(count):
            header = offset + index * size
            if struct.unpack_from('<I', data, header)[0] != 1:
                continue
            file_offset, address = struct.unpack_from('<QQ', data, header + 8)
            alignment = struct.unpack_from('<Q', data, header + 48)[0]
            if alignment < 16384 or (address - file_offset) % 16384:
                raise SystemExit(f'{name}: LOAD segment does not support 16 KiB pages')
            loads += 1
        if not loads:
            raise SystemExit(f'{name}: no LOAD segments')
        checked += 1
if not checked:
    raise SystemExit('No 64-bit native libraries found')
print(f'16 KiB page alignment verified for {checked} native libraries')
