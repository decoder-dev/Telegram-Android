import unittest
from verify_android_ci import REQUIRED_JOBS, verify


class ReleaseCiTest(unittest.TestCase):
    def run_data(self):
        return dict(head_sha='a' * 40, head_repository=dict(full_name='decoder-dev/Telegram-Android'),
                    path='.github/workflows/android.yml', status='completed', conclusion='success')

    def jobs(self):
        return [dict(name=name, status='completed', conclusion='success') for name in REQUIRED_JOBS]

    def test_accepts_only_exact_source_with_all_android_versions(self):
        verify(self.run_data(), self.jobs(), 'decoder-dev/Telegram-Android', 'a' * 40)

    def test_rejects_another_source_or_repository(self):
        for changes in (dict(head_sha='b' * 40), dict(head_repository=dict(full_name='elsewhere/fork')),
                        dict(path='.github/workflows/release.yml')):
            run = self.run_data(); run.update(changes)
            with self.assertRaises(ValueError): verify(run, self.jobs(), 'decoder-dev/Telegram-Android', 'a' * 40)

    def test_diagnostic_only_and_skipped_tests_cannot_authorize_release(self):
        for jobs in ([], [dict(name='Read previous Android failure reports', status='completed', conclusion='success')],
                     [dict(job, conclusion='skipped') for job in self.jobs()]):
            with self.assertRaises(ValueError): verify(self.run_data(), jobs, 'decoder-dev/Telegram-Android', 'a' * 40)

    def test_failed_or_incomplete_matrix_never_authorizes_release(self):
        for changes in (dict(status='in_progress', conclusion=None), dict(conclusion='failure'), dict(conclusion='cancelled')):
            run = self.run_data(); run.update(changes)
            with self.assertRaises(ValueError): verify(run, self.jobs(), 'decoder-dev/Telegram-Android', 'a' * 40)
        with self.assertRaises(ValueError): verify(self.run_data(), self.jobs()[:-1], 'decoder-dev/Telegram-Android', 'a' * 40)
        without_latest = [job for job in self.jobs() if job['name'] != 'Android 36 APK and UI checks']
        with self.assertRaises(ValueError): verify(self.run_data(), without_latest, 'decoder-dev/Telegram-Android', 'a' * 40)
