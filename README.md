## BananaGram for Android

An unofficial Telegram client being ported from our [BananaGram iOS fork](https://github.com/decoder-dev/Telegram-iOS).
The port scope also includes [AyuGram](https://github.com/AyuGram) features.
Full feature parity is still in progress; there is no BananaGram Android release yet.

The first implementation block adds account-specific Extras, opt-in tracking-link
cleanup, Ghost Mode controls and playback speeds up to 4×. The next block adds
a Ghost preset, silent sending and an optional return offline after activity.
An opt-in local archive adds previous/deleted versions, downloaded attachments,
search and clear. Local capture is tested; live message updates and server-side
Ghost acceptance are still pending. See the
[complete iOS feature reference](docs/phone-features-source.md),
[iOS port inventory](docs/phone-port-inventory.json),
[AyuGram inventory](docs/ayugram-port-inventory.json), [source credits](docs/port-sources.md)
and [Android build checks](docs/android-build.md) for the current scope and limits.

**Русский:** переносим все функции телефонного BananaGram. Перенос ещё не завершён;
сборки для проверки не являются готовым релизом.

### Telegram upstream

[Telegram](https://telegram.org) is a messaging app with a focus on speed and security. It’s superfast, simple and free.
This fork is based on the official source code for [Telegram App for Android](https://play.google.com/store/apps/details?id=org.telegram.messenger).

## Creating your Telegram Application

We welcome all developers to use our API and source code to create applications on our platform.
There are several things we require from **all developers** for the moment.

1. [**Obtain your own api_id**](https://core.telegram.org/api/obtaining_api_id) for your application.
2. Please **do not** use the name Telegram for your app — or make sure your users understand that it is unofficial.
3. Kindly **do not** use our standard logo (white paper plane in a blue circle) as your app's logo.
3. Please study our [**security guidelines**](https://core.telegram.org/mtproto/security_guidelines) and take good care of your users' data and privacy.
4. Please remember to publish **your** code too in order to comply with the licences.

### API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTproto protocol manuals: https://core.telegram.org/mtproto

### Compilation Guide

**Note**: In order to support [reproducible builds](https://core.telegram.org/reproducible-builds), this repo contains dummy release.keystore,  google-services.json and filled variables inside BuildVars.java. Before publishing your own APKs please make sure to replace all these files with your own.

You will require Android Studio 2025.1.4, Android NDK 27.2.12479018 and Android SDK 36.

1. Clone the Telegram source code with its submodules:
   ```bash
   git clone --recursive --shallow-submodules https://github.com/DrKLO/Telegram.git Telegram
   ```
   In case you forgot the `--recursive` flag, change to the `Telegram` directory and run:
   ```bash
   git submodule init && git submodule update --init --recursive --depth=1
   ```
2. Copy your release.keystore into TMessagesProj/config
3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in gradle.properties to access your  release.keystore
4.  Go to https://console.firebase.google.com/, create two android apps with application IDs org.telegram.messenger and org.telegram.messenger.beta, turn on firebase messaging and download google-services.json, which should be copied to the same folder as TMessagesProj.
5. Open the project in the Studio (note that it should be opened, NOT imported).
6. For BananaGram, supply `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` through the build environment or the private local loader described in [Android build checks](docs/android-build.md). Do not put API credentials in `BuildVars.java`.
7. You are ready to compile Telegram.

### Localization

We moved all translations to https://translations.telegram.org/en/android/. Please use it.
