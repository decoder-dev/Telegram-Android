# Changelog

Notable BananaGram Android changes. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- BananaGram Extras hub mirrored from iOS ForkExtrasController: Ninja, Ghost, Privacy, Chat list, Tab bar, Messages, Media, Profile.
- Full boolean ForkExtrasSettings catalog in `BananaConfig` (account-scoped preferences) with EN/RU labels.
- Behavioral wiring for high-impact toggles: hide ads, local Premium sync, confirm before call, hide mention/pinned notifications, hide stories row, collapse archive, exact counters, hide selected service messages, Ghost read-on-interact sync with AyuGram.

### Planned adaptations

- BananaGram: [excluded chats / pins / forum](https://github.com/decoder-dev/Telegram-iOS/commit/5b83d2df95b8f3c52654d1e0daae5d97f3ecd5ea), [suspicious-sender reads](https://github.com/decoder-dev/Telegram-iOS/commit/007fb3f1acddbb9484544e0f9d91e9d850c8c47d).
- AyuGram Desktop: [archive/links/reads](https://github.com/AyuGram/AyuGramDesktop/commit/105b6c21a69703a5c103d1615e4fbba12b519c3b), [expiring media](https://github.com/AyuGram/AyuGramDesktop/commit/d4b7546e77f93ac4c9bb53a3fcf7e9ffbbecf1b5).
- Remaining non-boolean Extras (sticker size, double-tap action, glass levels, backends) and per-toggle UI/behavior parity still incremental.
