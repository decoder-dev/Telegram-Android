# Changelog

Notable BananaGram Android changes. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- BananaGram Extras hub mirrored from iOS ForkExtrasController: Ninja, Ghost, Privacy, Chat list, Tab bar, Messages, Media, Profile.
- Full boolean ForkExtrasSettings catalog in `BananaConfig` (account-scoped preferences) with EN/RU labels.
- Non-boolean ForkExtrasSettings in `BananaConfig` (ints, marks, backends) with Values hub UI and EN/RU labels; iOS-matching defaults.
- Behavioral wiring for high-impact toggles: hide ads, local Premium sync, confirm before call, hide mention/pinned notifications, hide stories row, collapse archive, exact counters, hide selected service messages, Ghost read-on-interact sync with AyuGram.
- Wired value prefs: deleted/edited marks in history and message labels, double-tap seek seconds in PhotoViewer, outgoing photo quality in `AndroidUtilities.getPhotoSize`.

### Planned adaptations

Researched upstream fixes to adapt into this client. Not shipped until adapted and verified; see [port-sources.md](docs/port-sources.md).

- **BananaGram:** [сохранение исключённых чатов](https://github.com/decoder-dev/Telegram-iOS/commit/5b83d2df95b8f3c52654d1e0daae5d97f3ecd5ea) без потери [закреплений](https://github.com/decoder-dev/Telegram-iOS/commit/a9e065064383e2331918a0a9f05a875f84ba52bc) и структуры форума.
- **BananaGram:** [защита от подозрительных отправителей](https://github.com/decoder-dev/Telegram-iOS/commit/007fb3f1acddbb9484544e0f9d91e9d850c8c47d) — задержка прочтений до подтверждения.
- **AyuGram ПК:** [исправления архива, ссылок и прочтений](https://github.com/AyuGram/AyuGramDesktop/commit/105b6c21a69703a5c103d1615e4fbba12b519c3b) ([архив](https://github.com/AyuGram/AyuGramDesktop/commit/d623608993885a3456fc4a118ebaa2030fd407c4)).
- **AyuGram ПК:** [исправления исчезающих медиа и таймеров](https://github.com/AyuGram/AyuGramDesktop/commit/d4b7546e77f93ac4c9bb53a3fcf7e9ffbbecf1b5).
- Remaining value-pref behaviors (sticker size/shape, glass levels, double-tap action, backends) and per-toggle UI/behavior parity still incremental.
