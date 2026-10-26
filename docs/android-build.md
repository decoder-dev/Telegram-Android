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

The current workflow checks the credential loader, Java compilation, policy
unit tests, a complete x86_64 development APK and smoke tests on Android 16.
The smoke suite uses an emulator without a signed-in account and checks the platform URL detector,
the Extras screen in English/Russian and light/dark themes, real switch taps and
account preference isolation, Ghost preset scope and silent-send mode selection.
The current suite has 18 JVM policy tests, 7 credential-loader tests and 8 Android
smoke tests. Archive tests use synthetic private databases and verify persistence, duplicate
handling, limits, search, deletion and account/backend isolation. They do not yet
claim live-message capture or live-server Ghost Mode acceptance.

For a local smoke run, build the standalone debug APK and its AndroidTest APK with
`-Pandroid.injected.build.abi=x86_64`, then run `scripts/run-android-smoke.py` with
`--serial emulator-PORT`. The script refuses physical devices, and the test refuses
signed-in accounts. Screenshots and the test log go to `build/banana-smoke`.

The workflow does not publish an APK. Telegram API credentials are separate from APK signing
keys, Firebase configuration and store distribution credentials. The upstream
sample signing key must be replaced before distributing a BananaGram release.
