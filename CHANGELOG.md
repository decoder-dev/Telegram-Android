# Changelog

Notable BananaGram Android changes. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- The BananaGram Extras hub follows the iOS ForkExtrasController sections: Ninja, Ghost, Privacy, Chat list, Tab bar, Messages, Media, and Profile. Boolean settings are stored per account in `BananaConfig`, with English and Russian labels.
- Non-boolean ForkExtrasSettings (integers, marks, and backends) have a Values hub, English and Russian labels, and the same defaults as iOS.
- High-impact toggles change behavior: hide ads, local Premium, confirm before a call, hide mention and pinned notifications, hide the stories row, collapse the archive, exact counters, hide selected service messages, and Ghost read-on-interact synced with AyuGram.
- Value preferences are wired for deleted and edited marks in history and on message labels, double-tap seek seconds in the photo viewer, and outgoing photo quality in `AndroidUtilities.getPhotoSize`.
- Extras that were previously only switches now run at their call sites. While a chat is open, the Dynamic Island chip can show the chat name and follow the app icon. The message menu adds Message Shot, Save to Saved Messages, Repeat, Select from author, and Bookmark. The chat menu can jump to the start of the history. Invite links can be revoked in bulk. After joining a chat, the app can ask which folder to use. A group can keep a local alias. Scrolling from the bottom of one chat into the next chat can be turned off. Recent chats can be opened from the navigation drawer.
- Ghost and privacy options in this pass cover silent send, an Ayu forward gate, screenshot overrides for secret and protected chats, an Expire now action for disappearing messages, keeping banned chats in the list, hiding activity in private chats, and staying on the latest message after a refresh. The gift-stars redirect can be skipped. TikTok and YouTube Shorts links can open outside the app when a download extra is on, and saved gallery filenames can be prefixed with `BananaGram_`. Banana `SAVE_DELETED` and `SAVE_EDITS` feed the AyuGram history gates. Send with Return sends the draft on the return key. When Telegram statistics are off, analytics requests are dropped. Opening a Telegram link or a share link can ask for confirmation first. Folder names can be drawn in a compact form. Download and upload boosts stay inside the existing caps (one extra download slot, at most three concurrent uploads) and turn off when the power-saving Extra is on or the device is in power-save mode.
- Banana keep-alive does not wake the phone while the interface is open. In the background it schedules a single inexact alarm no sooner than 15 minutes and does not hold a partial wake lock.
- Auto MTProxy is a row in the proxy list. It fetches public MTProto lists and accepts `tg://proxy`, `t.me/proxy`, and `host:port:secret`. A proxy added by hand stays selected. The list refreshes on a 10-minute interval. A refresh is skipped when the app has been in the background for a minute, power save is on, or there is no network, and another refresh is not scheduled after that minute in the background. Each pass probes at most four candidates, 200 ms apart, and only while the screen is on.
- Classic proxy links stay available. `tg://proxy` and `t.me/proxy` still add SOCKS5, MTProto, and WEB proxies, and those three remain the live transports.
- A `vless://` link can be pasted and saved from the proxy list. This Android build does not embed Xray, so the saved link does not replace the active SOCKS5, MTProto, or WEB proxy.
- Archive and Saved Messages can be locked with a password. They stay hidden until Settings is tapped 10 times, then a PBKDF2-HMAC-SHA256 password or biometrics opens them. The session locks when the app leaves the foreground. Notifications for those chats show a generic title and body while a password is set. Existing archive toggles are left as they are, and archived chats stay muted.
- The theme picker lists Classic White, Classic Black, BananaGram Cream, and BananaGram Graphite. Cream and Graphite use the iOS palettes (warm paper by day, warm charcoal by night). Classic White and Classic Black keep the stock day and night assets. Scheduled time-of-day night mode is not turned on. The app still follows the system theme on Android 10 and later, and does not switch by clock time on older versions.
- The launcher icon, the round icon, the notification mark, and the in-app island use the BananaGram banana-and-plane artwork.
- `SECURITY.md` asks for vulnerability reports through GitHub private vulnerability reporting for this fork. Dependabot alerts, Dependabot security updates, secret scanning, and secret scanning push protection are enabled. Code scanning is not configured, and this tree does not add a CodeQL workflow.

### Planned adaptations

Researched upstream fixes to adapt into this client. Not shipped until adapted and verified; see [port-sources.md](docs/port-sources.md).

- **BananaGram:** [сохранение исключённых чатов](https://github.com/decoder-dev/Telegram-iOS/commit/5b83d2df95b8f3c52654d1e0daae5d97f3ecd5ea) без потери [закреплений](https://github.com/decoder-dev/Telegram-iOS/commit/a9e065064383e2331918a0a9f05a875f84ba52bc) и структуры форума.
- **BananaGram:** [защита от подозрительных отправителей](https://github.com/decoder-dev/Telegram-iOS/commit/007fb3f1acddbb9484544e0f9d91e9d850c8c47d) — задержка прочтений до подтверждения.
- **AyuGram ПК:** [исправления архива, ссылок и прочтений](https://github.com/AyuGram/AyuGramDesktop/commit/105b6c21a69703a5c103d1615e4fbba12b519c3b) ([архив](https://github.com/AyuGram/AyuGramDesktop/commit/d623608993885a3456fc4a118ebaa2030fd407c4)).
- **AyuGram ПК:** [исправления исчезающих медиа и таймеров](https://github.com/AyuGram/AyuGramDesktop/commit/d4b7546e77f93ac4c9bb53a3fcf7e9ffbbecf1b5).
- Remaining gaps are mostly Ghost/Ayu live-server acceptance verification and a few niche patterns (regex filter pattern editor, visual labels). ~154 Extras settings now have behavior wiring.
