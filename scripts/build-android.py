#!/usr/bin/env python3
"""Build with optional local credentials, without putting them in arguments or logs."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument('--credentials-file', type=Path)
    parser.add_argument('tasks', nargs='*', default=[':TMessagesProj_AppStandalone:assembleAfatDebug'])
    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    env = dict(os.environ)
    if args.credentials_file:
        try:
            if os.name == 'posix' and args.credentials_file.stat().st_mode & 0o077:
                parser.error('The credential file must be private: chmod 600 <file>')
            credentials = json.loads(args.credentials_file.read_text())
        except (OSError, ValueError):
            parser.error('Cannot read credentials: supply a readable private JSON file')
        if not isinstance(credentials, dict):
            parser.error('The credential file must contain a JSON object')
        for key in ('TELEGRAM_API_ID', 'TELEGRAM_API_HASH'):
            value = str(credentials.get(key, ''))
            pattern = r'[1-9][0-9]{0,9}' if key.endswith('_ID') else r'[a-fA-F0-9]{32}'
            if not re.fullmatch(pattern, value) or (key.endswith('_ID') and int(value) > 2147483647):
                parser.error('Invalid or missing ' + key)
            env[key] = value
    command = [str(root / 'gradlew'), '--no-daemon', '--console=plain', *args.tasks]
    return subprocess.call(command, cwd=root, env=env)


if __name__ == '__main__':
    raise SystemExit(main())
