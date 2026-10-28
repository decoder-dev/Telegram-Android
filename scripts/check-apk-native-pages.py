#!/usr/bin/env python3
"""Reject incompatible ELF64 libraries, including extracted Python/FFmpeg DSOs."""
import argparse
import io
from pathlib import PurePosixPath
import struct
from zipfile import ZipFile

WEBP_OVERRIDES = {'libsharpyuv.so', 'libwebpdecoder.so', 'libwebp.so', 'libwebpdemux.so', 'libwebpmux.so'}


def check_elf(data, name):
    if len(data) < 64 or data[:6] != b'\x7fELF\x02\x01':
        raise ValueError(f'{name}: expected little-endian ELF64')
    offset = struct.unpack_from('<Q', data, 32)[0]
    size, count = struct.unpack_from('<HH', data, 54)
    if size < 56 or not count or offset + size * count > len(data):
        raise ValueError(f'{name}: invalid ELF program headers')
    loads = 0
    for index in range(count):
        header = offset + index * size
        if struct.unpack_from('<I', data, header)[0] != 1:
            continue
        file_offset, address = struct.unpack_from('<QQ', data, header + 8)
        alignment = struct.unpack_from('<Q', data, header + 48)[0]
        if alignment < 16384 or (address - file_offset) % 16384:
            raise ValueError(f'{name}: LOAD segment does not support 16 KiB pages')
        loads += 1
    if not loads:
        raise ValueError(f'{name}: no LOAD segments')


def check_apk(path):
    checked = nested = overrides = 0
    with ZipFile(path) as archive:
        files = set(archive.namelist())
        for name in sorted(files):
            if not name.endswith('.so') or not name.startswith(('lib/arm64-v8a/', 'lib/x86_64/')):
                continue
            data = archive.read(name)
            if PurePosixPath(name).name in {'libpython.zip.so', 'libffmpeg.zip.so'} and data[:4] == b'PK\x03\x04':
                with ZipFile(io.BytesIO(data)) as extracted:
                    for entry in extracted.namelist():
                        value = extracted.read(entry)
                        if value[:6] != b'\x7fELF\x02\x01':
                            continue  # Resources and archive symlink entries are not ELF.
                        nested += 1
                        try:
                            check_elf(value, name + '/' + entry)
                        except ValueError:
                            replacement = str(PurePosixPath(name).parent / PurePosixPath(entry).name)
                            # FFmpegNativeRuntime atomically installs exactly these packaged builds.
                            if PurePosixPath(name).name != 'libffmpeg.zip.so' or PurePosixPath(entry).parent != PurePosixPath('usr/lib') or PurePosixPath(entry).name not in WEBP_OVERRIDES or replacement not in files:
                                raise
                            check_elf(archive.read(replacement), replacement)
                            overrides += 1
                continue
            check_elf(data, name)
            checked += 1
    if not checked:
        raise ValueError('No 64-bit native libraries found')
    return checked, nested, overrides


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk')
    args = parser.parse_args()
    try:
        counts = check_apk(args.apk)
    except ValueError as error:
        raise SystemExit(str(error))
    print('16 KiB page alignment verified: %d packaged libraries, %d extracted ELF64 dependencies, %d WebP replacements' % counts)


if __name__ == '__main__':
    main()
