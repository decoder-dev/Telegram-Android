# BananaGram Android build credentials

BananaGram is an unofficial Telegram client. Its own Telegram application API
credentials come from the build environment; do not paste them into `BuildVars.java`.

The repository's Actions secrets are `TELEGRAM_API_ID` and `TELEGRAM_API_HASH`.
The Android checks workflow reads them only in the Gradle steps. Pull requests
from forks can compile debug code without access to these secrets. A build
without credentials is suitable for development checks, but cannot sign in.
Non-debug variants require a valid pair and fail when it is absent.

For local builds, export both environment variables or use a private JSON file
outside the checkout containing those two fields. On Linux/macOS, restrict that
file to its owner with `chmod 600`.

```sh
python3 scripts/build-android.py --credentials-file /absolute/private/path/android-credentials.json -- :TMessagesProj:compileDebugJavaWithJavac
```

Use JDK 17, Android SDK/build tools 36 and NDK 27.2.12479018. Initialize the
repository's pinned submodules recursively before building. Gradle options such
as `-Dorg.gradle.workers.max=2` go after `--`.

The current workflow checks the credential loader and Java compilation. It does
not publish an APK. Telegram API credentials are separate from APK signing
keys, Firebase configuration and store distribution credentials. The upstream
sample signing key must be replaced before distributing a BananaGram release.
