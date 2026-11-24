#!/usr/bin/env python3
"""Build with optional local credentials, without putting them in arguments or logs."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

# Transient dependency-download failures seen in CI (Maven Central rate limits etc.).
TRANSIENT_MARKERS = (
    'Received status code 429',
    'Received status code 502',
    'Received status code 503',
    'Could not resolve',
    'Read timed out',
    'Connection reset',
)
MAX_ATTEMPTS = 3
RETRY_DELAYS_SECONDS = (60, 120)
# Keep only the tail of the build log for marker detection; full log is streamed live.
LOG_TAIL_LIMIT = 256 * 1024


def run_gradle(command, cwd, env):
    """Run Gradle, stream output live, and return (returncode, log tail)."""
    process = subprocess.Popen(
        command,
        cwd=cwd,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    tail = bytearray()
    for chunk in iter(lambda: process.stdout.readline(), b''):
        sys.stdout.buffer.write(chunk)
        sys.stdout.buffer.flush()
        tail.extend(chunk)
        if len(tail) > LOG_TAIL_LIMIT:
            del tail[: len(tail) - LOG_TAIL_LIMIT]
    process.stdout.close()
    return process.wait(), tail.decode('utf-8', errors='replace')


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
    for attempt in range(1, MAX_ATTEMPTS + 1):
        returncode, log_tail = run_gradle(command, root, env)
        if returncode == 0:
            return 0
        transient = any(marker in log_tail for marker in TRANSIENT_MARKERS)
        if not transient or attempt == MAX_ATTEMPTS:
            return returncode
        delay = RETRY_DELAYS_SECONDS[attempt - 1]
        print(
            'build-android: transient dependency failure detected '
            f'(attempt {attempt}/{MAX_ATTEMPTS}), retrying in {delay}s',
            flush=True,
        )
        time.sleep(delay)
    return returncode


if __name__ == '__main__':
    raise SystemExit(main())
