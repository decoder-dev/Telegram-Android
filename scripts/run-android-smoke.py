#!/usr/bin/env python3
"""Run the unregistered-client smoke suite on an explicitly selected emulator."""
import argparse
from pathlib import Path
import re
import shutil
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--serial', required=True)
parser.add_argument('--output-dir', type=Path, default=Path('build/banana-smoke'))
args = parser.parse_args()
if not re.fullmatch(r'emulator-\d+', args.serial):
    parser.error('Use a fresh Android emulator; this suite must not run on a personal phone')
adb = shutil.which('adb')
if not adb:
    parser.error('Add Android SDK platform-tools to PATH')
root = Path(__file__).resolve().parents[1]
args.output_dir.mkdir(parents=True, exist_ok=True)


def run(*command, timeout=180):
    result = subprocess.run([adb, '-s', args.serial, *command], cwd=root, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    if result.returncode:
        raise SystemExit(result.stdout)
    return result.stdout


build_dir = root / 'TMessagesProj_AppStandalone/build'
all_apks = sorted(build_dir.rglob('*.apk'))
app_apks = [path for path in all_apks
            if 'androidtest' not in path.name.lower()
            and 'androidtest' not in str(path.relative_to(build_dir)).lower()]
test_apks = [path for path in all_apks
             if 'androidtest' in path.name.lower()
             or 'androidtest' in str(path.relative_to(build_dir)).lower()]
if len(app_apks) != 1:
    candidates = ', '.join(str(path.relative_to(root)) for path in all_apks) or 'none found'
    raise SystemExit(f'Expected exactly one standalone app APK under {build_dir}; found: {candidates}')
if len(test_apks) != 1:
    candidates = ', '.join(str(path.relative_to(root)) for path in test_apks) or 'none found'
    raise SystemExit(f'Expected exactly one standalone instrumentation APK under {build_dir}; found: {candidates}')
run('install', '-t', '-r', str(app_apks[0]))
run('install', '-t', '-r', str(test_apks[0]))
run('shell', 'rm', '-rf', '/sdcard/Android/data/org.telegram.messenger.web/files/banana-smoke')
output = run('shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
             'org.telegram.banana.BananaSmokeTest,org.telegram.banana.HistoryDatabaseTest,org.telegram.banana.HistoryCaptureTest,org.telegram.banana.RegexEngineTest,org.telegram.banana.FilterRulesTest,org.telegram.banana.VoipAudioDevicesTest,org.telegram.banana.SelectionFilterMenuTest',
             'org.telegram.messenger.web.test/androidx.test.runner.AndroidJUnitRunner')
(args.output_dir / 'instrumentation.log').write_text(output)
# Preserve visual diagnostics even when another test in the same run fails.
pull = subprocess.run([adb, '-s', args.serial, 'pull',
                       '/sdcard/Android/data/org.telegram.messenger.web/files/banana-smoke',
                       str(args.output_dir / 'screens')], cwd=root, text=True,
                      stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
(args.output_dir / 'screenshots.log').write_text(pull.stdout)
passed = re.search(r'OK \((\d+) tests?\)', output)
if not passed or int(passed.group(1)) < 22 or 'FAILURES!!!' in output:
    print(output)
    raise SystemExit('Android smoke checks failed')
if pull.returncode:
    raise SystemExit('Tests passed but their screenshots could not be collected; see screenshots.log')
print('Android smoke checks passed: native player/history/filters, account isolation and headless Bluetooth setup')
