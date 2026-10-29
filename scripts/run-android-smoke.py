#!/usr/bin/env python3
"""Run the unregistered-client smoke suite on an explicitly selected emulator."""
import argparse
import io
import json
from pathlib import Path
import re
import shutil
import subprocess
import tarfile
import time

parser = argparse.ArgumentParser()
parser.add_argument('--serial', required=True)
parser.add_argument('--voice-probe', action='store_true', help='Download the public English model and decode a public Opus fixture')
parser.add_argument('--expected-api', type=int)
parser.add_argument('--expected-page-size', type=int)
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


def service_ready(name):
    try:
        return 'found' in run('shell', 'service', 'check', name, timeout=30)
    except (SystemExit, subprocess.TimeoutExpired):
        return False


def wait_for_system(timeout=180):
    # Boot can report completion while system_server is still restarting ("Can't find service:
    # activity"); every later step needs the activity, package and storage services.
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            booted = run('shell', 'getprop', 'sys.boot_completed', timeout=30).strip() == '1'
        except (SystemExit, subprocess.TimeoutExpired):
            booted = False
        if booted and service_ready('activity') and service_ready('package') and service_ready('mount'):
            return
        time.sleep(5)
    print(f'System services were still not ready after {timeout} seconds; continuing anyway')


wait_for_system()
device = {
    'api': int(run('shell', 'getprop', 'ro.build.version.sdk').strip()),
    'page_size': int(run('shell', 'getconf', 'PAGE_SIZE').strip()),
    'release': run('shell', 'getprop', 'ro.build.version.release').strip(),
}
(args.output_dir / 'device-info.json').write_text(json.dumps(device, indent=2) + '\n')
if args.expected_api is not None and device['api'] != args.expected_api:
    raise SystemExit(f'Wrong emulator API: {device}')
if args.expected_page_size is not None and device['page_size'] != args.expected_page_size:
    raise SystemExit(f'Wrong emulator page size: {device}')
print(f'Verified emulator: {device}')


build_dirs = [root / 'build']
build_dirs.extend(path / 'build' for path in root.iterdir()
                   if path.is_dir() and (path / 'build').is_dir())
all_apks = sorted({apk for build_dir in build_dirs if build_dir.is_dir()
                   for apk in build_dir.rglob('*.apk')})
build_roots = ', '.join(str(path.relative_to(root)) for path in build_dirs)
app_apks = [path for path in all_apks
            if 'androidtest' not in path.name.lower()
            and 'androidtest' not in str(path.relative_to(root)).lower()]
test_apks = [path for path in all_apks
             if 'androidtest' in path.name.lower()
             or 'androidtest' in str(path.relative_to(root)).lower()]
if len(app_apks) != 1:
    candidates = ', '.join(str(path.relative_to(root)) for path in all_apks) or 'none found'
    raise SystemExit(f'Expected exactly one standalone app APK under build dirs ({build_roots}); found: {candidates}')
if len(test_apks) != 1:
    candidates = ', '.join(str(path.relative_to(root)) for path in test_apks) or 'none found'
    raise SystemExit(f'Expected exactly one standalone instrumentation APK under build dirs ({build_roots}); found: {candidates}')
def install(apk):
    # Slow emulator images can drop the streamed install ("Failure calling service package:
    # Broken pipe") while system_server is still settling. Wait for the package service and retry.
    for attempt in range(1, 4):
        deadline = time.monotonic() + 120
        while not service_ready('package') and time.monotonic() < deadline:
            time.sleep(5)
        try:
            return run('install', '-t', '-r', str(apk))
        except (SystemExit, subprocess.TimeoutExpired) as error:
            installation.append(f'Attempt {attempt} for {apk.name} failed: {error}')
            if attempt == 3:
                raise
            time.sleep(10)


installation = []
try:
    for apk in (app_apks[0], test_apks[0]):
        installation.append(install(apk))
except (SystemExit, subprocess.TimeoutExpired) as error:
    installation.append(str(error))
    (args.output_dir / 'installation.log').write_text('\n'.join(installation))
    try:
        (args.output_dir / 'logcat.log').write_text(run('logcat', '-d', '-t', '4000', '-v', 'threadtime'))
    except (SystemExit, subprocess.TimeoutExpired) as diagnostic_error:
        (args.output_dir / 'logcat-error.log').write_text(str(diagnostic_error))
    raise SystemExit('APK installation failed before tests started; see installation.log and logcat.log')
(args.output_dir / 'installation.log').write_text('\n'.join(installation))
PACKAGE = 'org.telegram.messenger.web'
SCREENS = f'/sdcard/Android/data/{PACKAGE}/files/banana-smoke'


def shell_or_app(*command, timeout=60):
    # Newer images can deny the shell user access to an app's Android/data folder. The debug
    # APK is debuggable, so fall back to running the same command as the app itself.
    for prefix in ((), ('run-as', PACKAGE)):
        result = subprocess.run([adb, '-s', args.serial, 'shell', *prefix, *command], cwd=root,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
        if not result.returncode:
            return True, result.stdout
    return False, result.stdout


def clear_screens():
    cleared, output = shell_or_app('rm', '-rf', SCREENS)
    if not cleared:
        print(f'Could not remove old screenshots, continuing: {output.decode(errors="replace").strip()}')


def pull_screens():
    destination = args.output_dir / 'screens'
    pull = subprocess.run([adb, '-s', args.serial, 'pull', SCREENS, str(destination)], cwd=root, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
    if not pull.returncode:
        return True, pull.stdout
    archive = subprocess.run([adb, '-s', args.serial, 'exec-out', 'run-as', PACKAGE, 'tar', '-cf', '-',
                              '-C', SCREENS, '.'], cwd=root, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE, timeout=120)
    if archive.returncode or not archive.stdout:
        return False, pull.stdout + '\nrun-as fallback: ' + archive.stderr.decode(errors='replace')
    destination.mkdir(parents=True, exist_ok=True)
    with tarfile.open(fileobj=io.BytesIO(archive.stdout)) as screens:
        # Only plain PNG files written by the suite are expected; never trust archive paths.
        for member in screens.getmembers():
            name = Path(member.name).name
            if member.isfile() and name.endswith('.png'):
                (destination / name).write_bytes(screens.extractfile(member).read())
    return True, pull.stdout + '\nCollected screenshots through run-as\n'


clear_screens()
SUITE = ('org.telegram.banana.MediaConversionAndroidTest,org.telegram.banana.VideoEngineAndroidTest,'
         'org.telegram.banana.SessionBackupAndroidTest,org.telegram.banana.BananaSmokeTest,'
         'org.telegram.banana.HistoryDatabaseTest,org.telegram.banana.HistoryCaptureTest,'
         'org.telegram.banana.RegexEngineTest,org.telegram.banana.FilterRulesTest,'
         'org.telegram.banana.VoipAudioDevicesTest,org.telegram.banana.SelectionFilterMenuTest')


def run_suite():
    try:
        # A healthy run takes about two minutes; a hung UI wait must not burn the whole job.
        result = run('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', SUITE,
                     'org.telegram.messenger.web.test/androidx.test.runner.AndroidJUnitRunner', timeout=420)
        return result, False
    except SystemExit as error:
        return str(error), True
    except subprocess.TimeoutExpired as error:
        result = error.stdout or ''
        if isinstance(result, bytes):
            result = result.decode(errors='replace')
        return result + f'\nInstrumentation timed out: {error}\n', True


def suite_passed(result, failed):
    count = re.search(r'OK \((\d+) tests?\)', result)
    return not failed and count is not None and int(count.group(1)) >= 30 and 'FAILURES!!!' not in result


output, instrumentation_failed = run_suite()
if not suite_passed(output, instrumentation_failed):
    # Swiftshader emulators occasionally miss a synthesized tap or a viewer close. Keep the first
    # attempt's evidence, reset the app to the fresh unsigned state and run the suite once more.
    (args.output_dir / 'instrumentation-attempt1.log').write_text(output)
    (args.output_dir / 'logcat-attempt1.log').write_text(run('logcat', '-d', '-t', '4000', '-v', 'threadtime'))
    print('Android smoke suite failed on the first attempt; retrying once from a cleared app state')
    wait_for_system()
    try:
        installed = 'package:' in run('shell', 'pm', 'path', PACKAGE, timeout=60)
    except (SystemExit, subprocess.TimeoutExpired):
        installed = False
    if installed:
        try:
            run('shell', 'am', 'force-stop', PACKAGE)
            run('shell', 'pm', 'clear', PACKAGE)
        except (SystemExit, subprocess.TimeoutExpired):
            installed = False
            wait_for_system()
    if not installed:
        # A system_server restart during the first attempt can leave the app without its data
        # directory or unregistered; install both APKs again instead of failing on cleanup.
        for apk in (app_apks[0], test_apks[0]):
            installation.append(install(apk))
        (args.output_dir / 'installation.log').write_text('\n'.join(installation))
    clear_screens()
    output, instrumentation_failed = run_suite()
    output = '[retry: first attempt failed, see instrumentation-attempt1.log]\n' + output
(args.output_dir / 'instrumentation.log').write_text(output)
# Android's instrumentation runner may terminate its own process and return a short
# "Process crashed" result without forwarding the Java exception to stdout. Preserve
# recent system logs before attempting screenshot collection so CI failures are diagnosable.
logcat = run('logcat', '-d', '-t', '4000', '-v', 'threadtime')
(args.output_dir / 'logcat.log').write_text(logcat)
# Preserve visual diagnostics even when another test in the same run fails.
pulled, pull_log = pull_screens()
(args.output_dir / 'screenshots.log').write_text(pull_log)
if not suite_passed(output, instrumentation_failed):
    print(output)
    print('\n--- Recent Android logcat ---\n')
    print(logcat[-30000:])
    raise SystemExit('Android smoke checks failed')
if not pulled:
    raise SystemExit('Tests passed but their screenshots could not be collected; see screenshots.log')
print('Android smoke checks passed: media conversions, native player/history/filters, account isolation and headless Bluetooth setup')

if args.voice_probe:
    voice = run('shell', 'am', 'instrument', '-w', '-r', '-e', 'banana_voice_probe', 'YES',
                '-e', 'class', 'org.telegram.banana.OfflineVoiceTest',
                'org.telegram.messenger.web.test/androidx.test.runner.AndroidJUnitRunner', timeout=600)
    (args.output_dir / 'offline-voice.log').write_text(voice)
    (args.output_dir / 'offline-voice-logcat.log').write_text(run('logcat', '-d', '-t', '2000', '-v', 'threadtime'))
    if 'OK (1 test)' not in voice or 'FAILURES!!!' in voice:
        raise SystemExit(voice)
    print('Offline Opus decoding and Vosk recognition passed')
