# Security policy

## Reporting a vulnerability

Please report security issues in this repository privately through
[GitHub private vulnerability reporting](https://github.com/decoder-dev/Telegram-Android/security/advisories/new).

Do not open a public GitHub issue for a vulnerability, and do not send exploit
details in a pull request. Include what you can of the following:

- affected component and version or commit
- what an attacker can do
- steps or a proof of concept that demonstrates the issue

decoder-dev will acknowledge a report on the advisory when it is received and
will use that advisory for any follow-up. There is no separate security email
for this project.

## Supported code

Security reports should be about this fork (`decoder-dev/Telegram-Android`) and
the code it ships. Issues that exist only in upstream Telegram and are not
introduced or modified here are better reported to Telegram's own security
process.

## What GitHub security features are enabled

These are turned on for `decoder-dev/Telegram-Android`:

- Dependabot vulnerability alerts
- Dependabot security updates (automated security fixes)
- Secret scanning
- Secret scanning push protection
- Private vulnerability reporting

Code scanning is not configured. CodeQL is not added as a workflow. This tree
is a full Android client with large native dependencies, and a stock CodeQL
job would sit on top of an already long compile and is likely to time out or
fail the existing Android checks. Enabling code scanning still needs the
GitHub code-scanning setup in the repository Security tab if you want it;
that click is separate from the switches above. Default setup reports
`state: not-configured`.
