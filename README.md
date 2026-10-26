# 🍌 BananaGram for Android

**An unofficial Telegram client with more control over your conversations.**

BananaGram brings together our [iOS client](https://github.com/decoder-dev/Telegram-iOS), Telegram for Android and features from AyuGram: configurable Ghost Mode, message history, filters and extra sending tools.

[Android source](https://github.com/decoder-dev/Telegram-Android) · [iOS source](https://github.com/decoder-dev/Telegram-iOS) · [Issues](https://github.com/decoder-dev/Telegram-Android/issues) · [Releases](https://github.com/decoder-dev/Telegram-Android/releases)

> **Work in progress.** The Android port is undergoing integration after the AyuGram import. The current branch has not been built or tested after that import and is not a verified release. There is no published BananaGram Android release yet.

## Feature scope

The following features are present in the source and are being brought into a consistent Android experience:

- **Ghost controls** — online status, typing and upload activity, message and story read receipts, silent sending and automatic return offline.
- **Message history** — local records of edits and deletions, saved attachments, search and archive cleanup.
- **Message filters** — shared and per-chat rules, exceptions, regular expressions, sample previews and reviewed rule imports.
- **Sending tools** — optional tracking-link cleanup, forwarding controls and scheduled sending.
- **Playback** — audio and video speed controls up to **4×**.
- **Interface** — English and Russian Extras screens with light and dark themes.

## Development setup

The intended toolchain is **JDK 17**, **Android SDK / build tools 36**, **NDK 27.2.12479018** and the included **Gradle 8.13** wrapper. Some imported build settings and dependencies still need reconciliation.

```sh
git clone https://github.com/decoder-dev/Telegram-Android.git BananaGram
cd BananaGram
```

Review [.gitmodules](.gitmodules) before initializing dependencies. Imported AyuGram code references a separate proprietary component; access to that component or a compatible implementation is required for the code that depends on it. The dependency layout is part of the ongoing integration.

### Telegram API credentials

Register your own application at [my.telegram.org](https://my.telegram.org). The build reads `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` from the environment or a private JSON file outside the checkout.

With a private file containing those two fields:

```sh
chmod 600 /absolute/private/path/android-credentials.json
python3 scripts/build-android.py \
  --credentials-file /absolute/private/path/android-credentials.json \
  -- :TMessagesProj_AppStandalone:assembleAfatDebug
```

API credentials are separate from APK signing keys and Firebase configuration. Configure your own application identity and signing before publishing a release; keep credentials and private keys out of Git.

### Automated builds

The repository contains Android checks and an inherited release workflow. The inherited release workflow is disabled on GitHub pending manual setup. Earlier check results describe the pre-import code, not the current integration snapshot.

## Contributing

Bug reports and focused pull requests are welcome. Include the device, Android version, reproduction steps and relevant logs with tokens and private conversation data removed.

Use the [import record](docs/ayugram-upstream-import.json), [iOS feature inventory](docs/phone-port-inventory.json) and [AyuGram inventory](docs/ayugram-port-inventory.json) to follow porting progress.

## Русский

**BananaGram — неофициальный клиент Telegram с дополнительными настройками общения.**

Переносим возможности нашего iOS-клиента и AyuGram: гибкий Ghost Mode, историю правок и удалений, фильтры сообщений, инструменты отправки и управление скоростью воспроизведения.

Android-порт проходит интеграцию; готового релиза пока нет. Исходники, обсуждение ошибок и будущие сборки размещаются в этом репозитории.

## Credits and license

BananaGram is maintained by [decoder-dev](https://github.com/decoder-dev) and is not affiliated with Telegram.

This project builds on the work of:

- [Telegram for Android](https://github.com/DrKLO/Telegram) — the upstream client.
- [BananaGram for iOS](https://github.com/decoder-dev/Telegram-iOS) — our phone feature reference.
- [AyuGram / Radolyn](https://github.com/AyuGram/AyuGram4A) and [exteraGram](https://github.com/exteraSquad/exteraGram) — imported Android features and their foundations.
- [Telegraher](https://github.com/nikitasius/Telegraher), [Cherrygram](https://github.com/arsLan4k1390/Cherrygram), [Nagram](https://github.com/NextAlone/Nagram) and [Telegram FOSS](https://github.com/Telegram-FOSS-Team/Telegram-FOSS) — contributions acknowledged by the imported sources.
- [Nekogram](https://github.com/Nekogram/Nekogram) and [Forkgram](https://github.com/forkgram/TelegramAndroid) — adapted Android fixes.

Original copyright notices and source credits are retained. See [LICENSE](LICENSE), [source notes](docs/port-sources.md) and the licenses of individual components.
