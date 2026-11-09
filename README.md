# 🍌 BananaGram for Android

**An unofficial Telegram client with more control over your conversations.**

BananaGram brings together our [iOS client](https://github.com/decoder-dev/Telegram-iOS), Telegram for Android and features from AyuGram: configurable Ghost Mode, message history, filters and extra sending tools.

[Android source](https://github.com/decoder-dev/Telegram-Android) · [iOS source](https://github.com/decoder-dev/Telegram-iOS) · [Issues](https://github.com/decoder-dev/Telegram-Android/issues) · [Releases](https://github.com/decoder-dev/Telegram-Android/releases)

> **Published candidate: [13.2.1-rc.15](https://github.com/decoder-dev/Telegram-Android/releases/tag/v13.2.1-rc.15).** The release includes a signed APK and checksum/signature information. `main` can contain newer fixes that are not yet in that APK. Candidates remain under testing; see [Android checks](https://github.com/decoder-dev/Telegram-Android/actions/workflows/android.yml) for each commit's results.

## Feature scope

The following features are present in the source and are being brought into a consistent Android experience:

- **Extras hub** — BananaGram iOS ForkExtras catalog on Android (Ninja, Ghost, Privacy, chat list, tabs, messages, media, profile), with account-scoped preferences.
- **Ghost controls** — online status, typing and upload activity, message and story read receipts, silent sending, read-on-interact and automatic return offline.
- **Message history** — local records of edits and deletions, saved attachments, search and archive cleanup.
- **Message filters** — shared and per-chat rules, exceptions, regular expressions, sample previews and reviewed rule imports.
- **Sending tools** — optional tracking-link cleanup, forwarding controls and scheduled sending.
- **Playback** — audio and video speed controls up to **4×**.
- **Interface** — English and Russian Extras screens with light and dark themes.

### Display refresh rate

Extras → Values and marks → Appearance → Display refresh rate offers System, Maximum available and the rates reported by the display. This is an app-wide window preference, reapplied when the main activity resumes; it does not alter system settings. Android can override it for device policy, power saving or thermal limits. Display refresh rate and the application's rendered FPS are different: choosing 120 Hz does not remove rendering stalls.

## Development setup

The toolchain is **JDK 17**, **Android SDK / build tools 36**, **NDK 27.2.12479018** and the included **Gradle 8.13** wrapper.

```sh
git clone https://github.com/decoder-dev/Telegram-Android.git BananaGram
cd BananaGram
```

Initialize the public native dependencies listed in [.gitmodules](.gitmodules) with `git submodule update --init --recursive`. The AyuGram message-history compatibility helpers are included in this source tree under GPL-2.0; their source is documented in [port sources](docs/port-sources.md).

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

The Android checks workflow compiles Java sources, runs unit tests and builds APKs for UI smoke tests on Android 13–16 emulators. The manual release workflow builds and publishes the signed APK; by default it runs acceptance checks or verifies a successful run for the same commit. See the [update-flow plan](docs/update-flow.md) for the in-app update design.

## Contributing

Bug reports and focused pull requests are welcome. Include the device, Android version, reproduction steps and relevant logs with tokens and private conversation data removed.

The [language strategy](docs/language-strategy.md) preserves upstream Java, uses Kotlin for new Android screens after the feature audit, and requires measurements before introducing Rust.

Use the [changelog](CHANGELOG.md), [import record](docs/ayugram-upstream-import.json), [iOS feature inventory](docs/phone-port-inventory.json) and [AyuGram inventory](docs/ayugram-port-inventory.json) to follow porting progress.

## Русский

**BananaGram — неофициальный клиент Telegram с дополнительными настройками общения.**

Переносим возможности нашего iOS-клиента и AyuGram: гибкий Ghost Mode, историю правок и удалений, фильтры сообщений, инструменты отправки и управление скоростью воспроизведения.

**Опубликованный кандидат: [13.2.1-rc.15](https://github.com/decoder-dev/Telegram-Android/releases/tag/v13.2.1-rc.15).** В релизе доступны подписанный APK, контрольная сумма и сведения о подписи. В `main` могут быть более свежие исправления, ещё не вошедшие в APK. Результаты проверок каждого коммита — в [GitHub Actions](https://github.com/decoder-dev/Telegram-Android/actions/workflows/android.yml).

Частота экрана выбирается в Extras → Значения и метки → Внешний вид → Частота обновления экрана. Настройка общая для приложения: системная, максимальная или доступная частота. Android может ограничивать запрос из-за энергосбережения, температуры или политики устройства. Выбор 120 Гц сам по себе не устраняет пропуски кадров.

## Credits and license

BananaGram is maintained by [decoder-dev](https://github.com/decoder-dev) and is not affiliated with Telegram.

This project builds on the work of:

- [Telegram for Android](https://github.com/DrKLO/Telegram) — the upstream client.
- [BananaGram for iOS](https://github.com/decoder-dev/Telegram-iOS) — our phone feature reference.
- [AyuGram / Radolyn](https://github.com/AyuGram/AyuGram4A) and [exteraGram](https://github.com/exteraSquad/exteraGram) — imported Android features and their foundations.
- [Telegraher](https://github.com/nikitasius/Telegraher), [Cherrygram](https://github.com/arsLan4k1390/Cherrygram), [Nagram](https://github.com/NextAlone/Nagram) and [Telegram FOSS](https://github.com/Telegram-FOSS-Team/Telegram-FOSS) — contributions acknowledged by the imported sources.
- [Nekogram](https://github.com/Nekogram/Nekogram) and [Forkgram](https://github.com/forkgram/TelegramAndroid) — adapted Android fixes.

Original copyright notices and source credits are retained. See [LICENSE](LICENSE), [source notes](docs/port-sources.md) and the licenses of individual components.
