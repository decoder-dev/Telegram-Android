#!/usr/bin/env python3
"""Reuse successful Android acceptance only for the exact release source commit."""
import argparse
import json
import re
import subprocess

REQUIRED_JOBS = {'Android compilation and policy tests'} | {
    f'Android {api} APK and UI checks' for api in (33, 34, 35, 36, 37)
}


def verify(run, jobs, repository, commit):
    if run.get('head_sha') != commit:
        raise ValueError('CI source commit differs from release commit')
    if run.get('head_repository', {}).get('full_name', '').lower() != repository.lower():
        raise ValueError('CI belongs to a different repository')
    if run.get('path', '').split('@', 1)[0] != '.github/workflows/android.yml':
        raise ValueError('Expected the Android acceptance workflow')
    if run.get('status') != 'completed' or run.get('conclusion') != 'success':
        raise ValueError('CI has not completed successfully')
    completed = {job['name'] for job in jobs if job.get('status') == 'completed' and job.get('conclusion') == 'success'}
    missing = REQUIRED_JOBS - completed
    if missing:
        raise ValueError('Required acceptance jobs are missing: ' + ', '.join(sorted(missing)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--repository', required=True)
    parser.add_argument('--commit', required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'[0-9]+', args.run) or not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', args.repository) or not re.fullmatch(r'[0-9a-f]{40}', args.commit):
        parser.error('Invalid run, repository or commit')
    def api(path):
        result = subprocess.run(['gh', 'api', path], text=True, capture_output=True)
        if result.returncode:
            raise ValueError('Cannot read GitHub acceptance metadata')
        return json.loads(result.stdout)
    base = f'repos/{args.repository}/actions/runs/{args.run}'
    try:
        run = api(base)
        jobs = api(base + '/jobs?per_page=100')['jobs']
        verify(run, jobs, args.repository, args.commit)
    except (ValueError, KeyError) as failure:
        raise SystemExit(str(failure))
    print(f'Android 13–17 acceptance verified: run {args.run}, commit {args.commit}')


if __name__ == '__main__':
    main()
