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
unit tests, a complete x86_64 development APK and smoke tests on Android 13, 14, 15 and 16. The minimum SDK is 33 and target/compile SDK is 36; no maximum SDK blocks newer Android versions.
The smoke suite uses an emulator without a signed-in account and checks the platform URL detector,
the Extras screen in English/Russian and light/dark themes, real switch taps and
account preference isolation, Ghost preset scope, silent-send mode selection and
story privacy prompt choices (including Back and exactly-once cancellation).
Navigation checks wait for the actual initial and nested transitions to finish.
A failure saves its screenshot and current stack state, rather than relaxing the
layout or transition assertions.
The suite includes JVM policy/file IO checks, seven credential-loader checks, and
native Android smoke checks. Archive fixtures verify persistence, duplicate
handling, limits, migration, search, deletion and account/backend isolation. A
local native-cache fixture verifies edit/delete capture, copied files surviving
cache unlink, proactive download completion, shared-file reuse and clearing
pending work. It supplies a synthetic capture owner in an unregistered emulator,
without initializing a login or native user ID; networking is disabled on its
test backend. A separate public Opus fixture exercises MediaCodec, resampling and
Vosk. AndroidKeyStore encryption is checked using synthetic bytes and a temporary
key. These are separate from authenticated Telegram-server acceptance and from
physical-device GPU/FPS measurements. The attestation records actual results.

For a local smoke run, build the standalone debug APK and its AndroidTest APK with
`-Pandroid.injected.build.abi=x86_64`, then run `scripts/run-android-smoke.py` with
`--serial emulator-PORT`. The script refuses physical devices, and the test refuses
signed-in accounts. Screenshots and the test log go to `build/banana-smoke`.

The checks workflow does not publish an APK. `release.yml` waits for the checks, uses the existing private signing key, verifies certificate continuity and 16 KiB ELF alignment, then publishes a prerelease with its APK and R8 mapping. The required signing secrets are `BANANAGRAM_RELEASE_KEYSTORE_BASE64`, `BANANAGRAM_RELEASE_STORE_PASSWORD`, `BANANAGRAM_RELEASE_KEY_ALIAS` and `BANANAGRAM_RELEASE_KEY_PASSWORD`. Telegram API credentials are separate from APK signing
keys, Firebase configuration and store distribution credentials. The upstream
sample signing key must be replaced before distributing a BananaGram release.

## Optional test-server capability probe

`BananaTestServerProbe` is excluded from the normal smoke class list. It requires
`banana_test_server=YES`, a debug emulator, an empty account slot and a native
test-backend check before every RPC. It uses a single reserved test phone, never
initializes a user account, never sends messages, and resets its local auth key
and backend afterwards. Do not use real phone numbers or OTPs in this probe.

The local 2026-10-26 probe reached `auth.sendCode`, but its deterministic
`auth.signIn` was rejected with `PHONE_CODE_INVALID`. This is an external
capability result, not successful Ghost/history protocol acceptance. A dedicated
working test-server account is still needed for those integration checks.


The deterministic emulator suite never depends on a third-party paste being
available. An optional `RemoteFilterNetworkProbe` accepts
`-e banana_filter_source_probe YES -e banana_public_filter_source <raw-HTTPS-URL>`
with `-e class org.telegram.banana.RemoteFilterNetworkProbe`. It requires a fresh
emulator with no signed-in accounts, fetches only an allowlisted public JSON
source and never applies its contents. Do not use a real account for this probe.
