#!/usr/bin/env python3
"""Check live theme reflection fields in DEX, using R8 only to resolve classes."""
import argparse
from pathlib import Path
import re
import struct
from zipfile import ZipFile

REQUIRED = {
    'HeaderCell': {'textView'},
    'TextCheckCell': {'textView', 'valueTextView', 'checkBox'},
    'TextSettingsCell': {'textView', 'valueTextView'},
    'TextDetailSettingsCell': {'textView', 'valueTextView'},
    'TextInfoPrivacyCell': {'textView'},
}


def declared_fields(data, wanted):
    if data[:4] != b'dex\n' or len(data) < 112:
        raise ValueError('Expected a DEX file')

    def u32(offset):
        return struct.unpack_from('<I', data, offset)[0]

    def uleb(offset):
        value = 0
        for shift in range(0, 35, 7):
            byte = data[offset]
            offset += 1
            value |= (byte & 127) << shift
            if not byte & 128:
                return value, offset
        raise ValueError('Invalid DEX ULEB128 value')

    strings = u32(60)
    types = u32(68)
    fields = u32(84)
    class_count, classes = u32(96), u32(100)
    names = {}

    def string(index):
        if index not in names:
            _, start = uleb(u32(strings + index * 4))
            names[index] = data[start:data.index(b'\0', start)].decode('utf-8', errors='replace')
        return names[index]

    result = {}
    for i in range(class_count):
        entry = classes + i * 32
        descriptor = string(u32(types + u32(entry) * 4))
        if descriptor not in wanted:
            continue
        result[descriptor] = set()
        offset = u32(entry + 24)
        if not offset:
            continue
        sizes = []
        for _ in range(4):
            size, offset = uleb(offset)
            sizes.append(size)
        for size in sizes[:2]:  # Static and instance fields each restart their index.
            index = 0
            for _ in range(size):
                delta, offset = uleb(offset)
                _, offset = uleb(offset)  # Access flags.
                index += delta
                result[descriptor].add(string(u32(fields + index * 8 + 4)))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mapping', type=Path)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--apk', type=Path)
    source.add_argument('--dex-dir', type=Path)
    args = parser.parse_args()
    expected = {'org.telegram.ui.Cells.' + name: fields for name, fields in REQUIRED.items()}
    classes = dict((name, name) for name in expected)
    with args.mapping.open() as source:
        for line in source:
            header = re.fullmatch(r'(\S+) -> (\S+):\s*', line)
            if header and header[1] in expected:
                classes[header[1]] = header[2]
    descriptors = {name: 'L' + mapped.replace('.', '/') + ';' for name, mapped in classes.items()}
    wanted = set(descriptors.values())
    found = {}
    if args.apk:
        with ZipFile(args.apk) as apk:
            for name in apk.namelist():
                if re.fullmatch(r'classes\d*\.dex', name):
                    found.update(declared_fields(apk.read(name), wanted))
    else:
        for file in args.dex_dir.rglob('*.dex'):
            found.update(declared_fields(file.read_bytes(), wanted))
    # R8 may omit identity field mappings. Their absence is not evidence of
    # removal: check the actual declared fields that getDeclaredField will see.
    broken = [f'{name}.{field} is absent from {classes[name]}'
              for name, fields in expected.items() for field in sorted(fields)
              if field not in found.get(descriptors[name], set())]
    if broken:
        raise SystemExit('Live theme reflection fields were renamed or removed:\n' + '\n'.join(broken))
    print('All nine live Extras theme reflection fields verified in the actual DEX')


if __name__ == '__main__':
    main()
