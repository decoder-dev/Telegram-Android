#!/usr/bin/env python3
"""Check the actual R8 output used by live Extras theme bindings."""
import argparse
from pathlib import Path
import re

REQUIRED = {
    'HeaderCell': {'textView'},
    'TextCheckCell': {'textView', 'valueTextView', 'checkBox'},
    'TextSettingsCell': {'textView', 'valueTextView'},
    'TextDetailSettingsCell': {'textView', 'valueTextView'},
    'TextInfoPrivacyCell': {'textView'},
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mapping', type=Path)
    args = parser.parse_args()
    expected = {'org.telegram.ui.Cells.' + name: fields for name, fields in REQUIRED.items()}
    found = {name: {} for name in expected}
    current = None
    with args.mapping.open() as source:
        for line in source:
            header = re.fullmatch(r'(\S+) -> \S+:\s*', line)
            if header:
                current = header[1] if header[1] in expected else None
            elif current:
                field = re.fullmatch(r'\s+\S+ (\w+) -> (\w+)\s*', line)
                if field:
                    found[current][field[1]] = field[2]
    broken = [f'{name}.{field} -> {found[name].get(field, "missing")}'
              for name, fields in expected.items() for field in sorted(fields)
              if found[name].get(field) != field]
    if broken:
        raise SystemExit('Live theme reflection fields were renamed or removed:\n' + '\n'.join(broken))
    print('All nine live Extras theme reflection fields survive R8 unchanged')


if __name__ == '__main__':
    main()
