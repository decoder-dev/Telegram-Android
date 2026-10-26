import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('build_android', Path(__file__).with_name('build-android.py'))
build_android = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build_android)


class CredentialsTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.file = Path(self.directory.name) / 'credentials.json'
        self.credentials = {'TELEGRAM_API_ID': 123456, 'TELEGRAM_API_HASH': 'a' * 32}

    def write(self, data):
        self.file.write_text(json.dumps(data))
        self.file.chmod(0o600)

    def reject(self):
        with patch.object(build_android.subprocess, 'call') as call, contextlib.redirect_stderr(io.StringIO()) as error:
            with self.assertRaises(SystemExit) as result:
                build_android.main(['--credentials-file', str(self.file)])
            self.assertEqual(result.exception.code, 2)
            call.assert_not_called()
            self.assertNotIn('a' * 32, error.getvalue())

    def test_values_go_only_to_child_environment(self):
        self.write(self.credentials)
        with patch.object(build_android.subprocess, 'call', return_value=17) as call:
            result = build_android.main(['--credentials-file', str(self.file), '--', '-Dorg.gradle.workers.max=2', ':TMessagesProj:compileDebugJavaWithJavac'])
        self.assertEqual(result, 17)
        command = call.call_args.args[0]
        env = call.call_args.kwargs['env']
        self.assertEqual(env['TELEGRAM_API_ID'], '123456')
        self.assertEqual(env['TELEGRAM_API_HASH'], 'a' * 32)
        self.assertNotIn('a' * 32, ' '.join(command))
        self.assertIn('-Dorg.gradle.workers.max=2', command)

    def test_environment_mode_and_default_task(self):
        with patch.dict(os.environ, {'TELEGRAM_API_ID': '123456', 'TELEGRAM_API_HASH': 'b' * 32}), patch.object(build_android.subprocess, 'call', return_value=0) as call:
            build_android.main([])
        self.assertEqual(call.call_args.kwargs['env']['TELEGRAM_API_HASH'], 'b' * 32)
        self.assertEqual(call.call_args.args[0][-1], ':TMessagesProj_AppStandalone:assembleAfatDebug')

    def test_rejects_missing_pair(self):
        self.write({'TELEGRAM_API_ID': 123456})
        self.reject()

    def test_rejects_invalid_or_overflowed_id(self):
        for value in (0, -1, 2147483648, '1; echo unsafe', True):
            with self.subTest(value=value):
                self.write(dict(self.credentials, TELEGRAM_API_ID=value))
                self.reject()

    def test_rejects_invalid_hash(self):
        self.write(dict(self.credentials, TELEGRAM_API_HASH='a' * 31 + '"'))
        self.reject()

    def test_rejects_invalid_json_without_echoing_contents(self):
        self.file.write_text('a' * 32)
        self.file.chmod(0o600)
        self.reject()
        self.write([])
        self.reject()

    @unittest.skipUnless(os.name == 'posix', 'POSIX file permissions')
    def test_rejects_public_file(self):
        self.write(self.credentials)
        self.file.chmod(0o644)
        self.reject()


if __name__ == '__main__':
    unittest.main()
