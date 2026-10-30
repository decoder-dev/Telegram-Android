# Changelog

Notable BananaGram Android changes. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Changed

- BananaGram Extras follows the iOS layout: Ninja, Ghost, Privacy, Appearance, Chat list, Tab bar, Messages, Media, Profile, Network, with an icon per section. The new Appearance section holds the island, glass, glow, snow and chat look; Network holds keep-alive and the download and upload boosts. Long sections are split under headers, such as Display, Composer and links, Message menu, Reactions and stickers, Channels and gifts.
- The AyuGram screen no longer repeats BananaGram switches: ghost mode and its packets, read after send, scheduled sending, the push service, ad hiding, local Premium and the marks live in BananaGram Extras. A one-time migration carries any value set on the AyuGram screen into every account.

### Removed

- UpdaterBottomSheet2, an unused second copy of the updater sheet.

### Added

- Two BananaGram app icons, Graphite and Cream, with the banana mark on the theme backgrounds. Every app icon is unlocked; none needs Telegram Premium or is hidden when Premium features are blocked.
- The BananaGram hub picks a theme from live mini-chat previews of the four bundled themes, like the iOS theme picker, instead of a list of names.
- The updater checks for a new release once a day in the background (an inexact alarm that never wakes the device) and posts a notification once per version. Tapping it opens the updater sheet. The check follows the updater's "Check on launch" switch.

### Fixed

- Proxies given by host name connect faster. Their address lookup asked www.google.com for Google's DNS JSON, which now answers 404, so every lookup waited for that before falling back to system DNS. It asks dns.google directly and ignores CNAME answers, which are host names rather than addresses.
- Message history: one deleted message that failed to archive no longer drops the rest of its batch, which Telegram then deletes for good. A message whose formatting is over the archive limits is kept as plain text instead of being skipped.
- Video download: asking for a second download while one runs no longer risks Android's "did not call startForeground" crash, and closing the service at the moment a download ends no longer passes a cleared process id to yt-dlp.
- The avatar and reaction glow cache draws an unusually large glow directly instead of risking an exception while allocating its bitmap.
- A run of failed downloads, such as all of a deleted chat's files, shows one "Message not found" bulletin instead of one per file, and a download without a file no longer counts as the one that failed. Ported from Glassgram.
- Tapping the keep-alive notification opens its system channel settings, where it can be hidden; the service keeps running. It used to open the app with a mutable PendingIntent. Ported from Glassgram.
- With glass bubbles on, outgoing messages are glass too. Their gradient colours were read past the glass tint, so they stayed opaque while incoming bubbles showed the blurred wallpaper.
- AyuGram: copying a secret-chat attachment into the deleted-message archive closes both streams on failure and removes the partial file. Regex filters are thread-safe (the chat list, notifications and downloads read them from different threads) and skip a stored expression that no longer compiles instead of crashing. Delete permissions and one-shot read/schedule flags update atomically. AyuForward waits are bounded at 15 minutes, so a send or download that never reports back no longer keeps a thread and its observers alive forever.
- Opening a chat no longer scans the whole deleted-message archive on the main thread. The deleted-message, edit-history and reaction tables had no indexes, so every history page, the "edited" check and each reaction lookup grew with the archive. Database version 22 adds the indexes through an explicit migration; the destructive fallback would otherwise have wiped saved history on upgrade. Deleting a saved message also deletes its reactions instead of leaving them behind.
- The archive lock tolerates a corrupted failure counter instead of crashing on unlock, and the island label no longer splits an emoji when it shortens a long title.
- AyuSync closes the HTTP response it used to leak and backs off from 1.5 s to 5 minutes between retries instead of retrying every 1.5 s forever.
- Session backup export, import and restore log the failure type instead of failing silently.
- With "Don't read messages" on, the unread-reactions and unread-mentions buttons in a chat no longer come back after being cleared. Ghost mode skipped messages.readReactions and messages.readMentions, which only clear your own badges and are visible to nobody, so the server kept the counts. Listened voice and round messages stay unsent.
- BananaGram Cream draws switched-off controls at 3:1 contrast, the WCAG minimum; they were at 2.6:1. The gallery background in attachments and inline code in incoming messages no longer stay Telegram blue in Cream and Graphite. Both palettes live in BananaPalette, and a JVM test keeps every text and control pair at WCAG AA.
- Automatic proxy waits at least 20 s after a switch before a failure-driven switch, and only switches while the app is still connecting through the proxy. On slow mobile links it used to hop to a new proxy every 6 s and reconnect every account.
- Changing or removing the archive password now shows the lockout countdown after the fifth wrong attempt, like unlocking does, instead of "0 attempts left".
- The exteraGram appearance settings no longer show a dead "People Nearby" drawer switch labelled "Or choose a venue"; Telegram removed the feature.
- Saving self-destructing and auto-deleted messages (AyuGram "save deleted", on by default) no longer blocks the UI thread. For every expiring message the hook waited on a storage-queue read and then wrote the AyuGram database, and could copy media, on the main thread. It now runs on the storage queue, still before the message and its files are deleted.
- The AyuGram regex-filter verdict cache is cleared after 256 chats instead of growing for the whole session.
- Glass bubbles reuse their recorded backdrop while the bubble stays in place. Re-recording on every frame made the GPU recompute each bubble's blur and refraction whenever anything in the chat animated.
- Avatar and reaction glow are drawn from small cached bitmaps instead of a shadow-layer stroke that was blurred again on every draw while scrolling.
- Opening the message menu skips the edit-history database query for messages that were never edited.
- The auto-download check no longer builds a full MessageObject for every media message when regex filters are off. The AyuGram hook parsed text, entities and emoji on each check while scrolling and loading chats, only to have the disabled filter return false.
- The updater sheet no longer crashes when "Check on launch" or "Auto-download" is tapped. Upstream TextCell creates its switch only with needCheck, so these rows had no switch and setChecked hit a null Switch. TextCell now creates the switch when a text-and-check setter needs it.
- Strings defined in ayu.xml and extera.xml now reach the generated localization assets. The advanced preferences, updater and other exteraGram/AyuGram screens showed "LOC_ERR:" labels because the generator read only strings.xml.

- Banana ghost switches and local Premium apply only to the account they are set on. They used to write AyuGram's process-wide flags, so one account's ghost mode leaked into every other account. A one-time migration resets the leaked flags; accounts without Banana ghost send read receipts and online status again.
- With "Read on Interact" off, sending in a ghosted chat no longer marks it read. Switching "Don't send typing" off restores typing, recording and upload status. The double-tap action defaults to the quick reaction, as in Telegram, instead of Off. It alone decides double-tap behavior: react, reply, repeat and translate work on incoming messages while "Double tap to edit" is on, and only the reaction requires a reaction the chat allows.
- "Send as round" goes through Telegram's round-video path, the same one a camera recording uses, so the message gets a round preview while uploading and the right secret-chat attribute.
- The keep-alive service no longer crashes the app when Android 12+ refuses foreground promotion of a background start.
- "Clean tracking links" also cleans photo, video and file captions, not only text messages.
- Deleted and edited marks stay on the account they are set for. The time label and the edited-text animation now read the same mark, so the animation finds it when the Banana mark differs from AyuGram's.

### Changed

- Settings credit decoder-dev and link this repository. Visible AyuGram names are replaced; the storage folder keeps its name so existing downloads stay reachable.

- Enter the foreground before rejecting a video download, so a rejected request no longer crashes Android 9+. Message shots are shared through a configured FileProvider cache root. A cancelled voice-model download reports the abort once without a failure toast. Deleted/edited marks are truncated without splitting surrogate pairs. Automatic MTProxy list downloads run on a dedicated queue instead of the shared global queue. The tlrpc patch script no longer uses an invalid escape sequence.
- Ghost typing, recording and upload toggles each apply to their own activity kind, including activity requests built outside `sendTyping`; cancellations are never dropped. Compact folder names keep custom emoji offsets and surrogate pairs intact.
- Session export asks for the password twice. The island chip uses dp margins and an open chat shows a single chip. VLESS link errors come from string resources. Message shots older than an hour are pruned from the cache.
- The in-app updater registers one exported DownloadManager receiver, so a finished download reaches verification and the install prompt; a missed broadcast no longer blocks the update button until restart, and release dates are parsed as UTC. Archived history names the chat, seek labels are localized, mark fields stop at eight characters, and the filter limit has its own message.
- Limit selectable and applicable themes to the four BananaGram iOS palettes, including imported files, previews, background transitions and per-chat overrides. Migrate retired Glass palettes to Classic White/Black and retain glass as a separate Extras switch.
- Wire the glass switch to native refraction and chrome on every allowed palette. Text bubbles use cell-owned wallpaper-only GPU backdrops on Android 13+, release their RenderNodes on detach, respect drawable alpha, and fall back without a redraw loop. Tint and top/side/bottom highlights are independent. Shader initialization failures retain the ordinary blurred backdrop.
- Disable the nonfunctional Keychain-backup and local-transcription choices with an explanation; the latter no longer bypasses server transcription limits. Label TikTok/Shorts controls according to their implemented external-app behavior.
- Translate 118 remaining English Extras and theme labels in the Russian resources. All 157 Extras switches now have Russian labels.
- Reuse each dialog/reaction layout's glow paint and rebuild its shadow only when color or density changes, avoiding paint and shadow allocation on every draw while glow is enabled. Device frame-rate improvement has not been measured.
- Photo viewer initialization no longer adds a duplicate PiP item through an uninitialized menu. Restore the separate delete button; PiP stays in its existing submenu.
- Restore the current Telegram message serialization tail before local attachment metadata, including quick replies, effects, paid-message fields, summaries and rich messages. Read current-layer media and processing flags without interpreting inverted media as an older topic-start flag. Regression tests cover the `RichMessage` constructor `2001` cache failure and repeated photo viewer initialization.

### Validation

- Main Android Java sources and AndroidTest sources compile; all 50 JVM tests pass. The per-setting [feature attestation](docs/feature-attestation.md) covers 176 iOS settings, 157 Android switches, known implementation gaps, and unperformed GPU/device/authenticated-server acceptance.

## [13.1.1-rc.1] — 2026-10-27

This is a source-only release candidate pending a successful Android build and emulator smoke run. No APK is included in this release. The previous AyuGram proprietary-submodule dependency has been replaced with documented GPL-2.0 history helpers in the source tree.

### Fixed

- Auto MTProxy probes at most four candidates per pass, 200 ms apart, and only while the app is in front. A candidate that did not answer is not turned on. An enabled proxy that was added by hand stays selected. A list fetch closes its error stream, and a missing proxy address no longer crashes the refresh.
- Saving a photo, video, or file to the gallery marks the row pending only on Android 10 and later, writes with the output stream that exists on Android 7, and deletes the row if the copy fails. A decoder that never produces a frame stops its frame callback instead of invalidating forever. Hardware Back deletes the selection or the character before the caret, and leaves the screen when the caret is already at the start.
- Save-deleted and save-edits no longer treat a group or channel id as a user id, so a cached bot cannot turn retention off for that chat. A missing id in the auto-delete timer is skipped, and one failed history save does not abort the rest of the timer. Archive unlock checks that the host is a fragment activity before showing biometrics, and a failed password dialog does not leave the prompt stuck. The proxy list uses stable ids that cannot collide with the automatic-proxy and VLESS rows. The island chip removes its overlay only when the parent is a view group. A VLESS short id is validated as a whole value, and the error text survives a missing locale.

### Added

- The BananaGram Extras hub follows the iOS ForkExtrasController sections: Ninja, Ghost, Privacy, Chat list, Tab bar, Messages, Media, and Profile. Boolean settings are stored per account in `BananaConfig`, with English and Russian labels.
- Non-boolean ForkExtrasSettings (integers, marks, and backends) have a Values hub, English and Russian labels, and the same defaults as iOS.
- High-impact toggles change behavior: hide ads, local Premium, confirm before a call, hide mention and pinned notifications, hide the stories row, collapse the archive, exact counters, hide selected service messages, and Ghost read-on-interact synced with AyuGram.
- Value preferences are wired for deleted and edited marks in history and on message labels, double-tap seek seconds in the photo viewer, and outgoing photo quality in `AndroidUtilities.getPhotoSize`.
- Extras that were previously only switches now run at their call sites. While a chat is open, the Dynamic Island chip can show the chat name and follow the app icon. The message menu adds Message Shot, Save to Saved Messages, Repeat, Select from author, and Bookmark. The chat menu can jump to the start of the history. Invite links can be revoked in bulk. After joining a chat, the app can ask which folder to use. A group can keep a local alias. Scrolling from the bottom of one chat into the next chat can be turned off. Recent chats can be opened from the navigation drawer.
- Ghost and privacy options in this pass cover silent send, an Ayu forward gate, screenshot overrides for secret and protected chats, an Expire now action for disappearing messages, keeping banned chats in the list, hiding activity in private chats, and staying on the latest message after a refresh. The gift-stars redirect can be skipped. TikTok and YouTube Shorts links can open outside the app when a download extra is on, and saved gallery filenames can be prefixed with `BananaGram_`. Banana `SAVE_DELETED` and `SAVE_EDITS` feed the AyuGram history gates. When save-deleted is on, the local auto-delete timer keeps the message instead of dropping it, and self-destruct photos, expiring media, and view-once media are not wiped on this device. Send with Return sends the draft on the return key. When Telegram statistics are off, analytics requests are dropped. Opening a Telegram link or a share link can ask for confirmation first. Folder names can be drawn in a compact form. Download and upload boosts stay inside the existing caps (one extra download slot, at most three concurrent uploads) and turn off when the power-saving Extra is on or the device is in power-save mode.
- Banana keep-alive does not wake the phone while the interface is open. In the background it schedules a single inexact alarm no sooner than 15 minutes and does not hold a partial wake lock.
- Auto MTProxy is a row in the proxy list. It fetches public MTProto lists and accepts `tg://proxy`, `t.me/proxy`, and `host:port:secret`. A proxy added by hand stays selected. The list refreshes on a 10-minute interval. A refresh is skipped when the app has been in the background for a minute, power save is on, or there is no network, and another refresh is not scheduled after that minute in the background. Each pass probes at most four candidates, 200 ms apart, and only while the screen is on.
- Classic proxy links stay available. `tg://proxy` and `t.me/proxy` still add SOCKS5, MTProto, and WEB proxies, and those three remain the live transports.
- A `vless://` link can be pasted and saved from the proxy list. This Android build does not embed Xray, so the saved link does not replace the active SOCKS5, MTProto, or WEB proxy.
- Archive and Saved Messages can be locked with a password. They stay hidden until Settings is tapped 10 times, then a PBKDF2-HMAC-SHA256 password or biometrics opens them. Revealing the row does not unlock it: until the session is open, those rows stay title-only, with no peer names, previews, or badges. The session locks when the app leaves the foreground, and it also locks when the last Archive chat, Saved Messages chat, or archived profile leaves the stack. While Archive contents are on screen, the recents snapshot is kept blank. Setting a password mutes archived chats, keeps them in Archive, and clears their notifications. Notifications for those chats show a generic title and body while a password is set. Existing archive toggles are left as they are.
- The theme picker lists Classic White, Classic Black, BananaGram Cream, and BananaGram Graphite. Cream and Graphite use the iOS palettes (warm paper by day, warm charcoal by night). Classic White and Classic Black keep the stock day and night assets. Choosing one of these palettes turns a clock schedule off. The app still follows the system theme on Android 10 and later when no schedule is set, and does not switch by clock time on older versions.
- The theme picker also lists BananaGram Glass and BananaGram Glass Night. Glass is a static frosted look: mist by day, slate by night, rounder chat panels, a capsule send button, and lifted chat-list cards. It does not run a blur loop. Cream, Graphite, and the two classic themes are unchanged. The banana launcher icon is unchanged.
- The launcher icon, the round icon, the notification mark, and the in-app island use the BananaGram banana-and-plane artwork. The default activity icon is set explicitly so the banana mark wins over a library icon.
- The signed-in account can show a visual label that only you see: None, SCAM, FAKE, SUPPORT, or BOT. It is drawn on your profile and in the navigation drawer. It does not change the account on the server. If the server already marks the account scam or fake, that flag is shown instead.
- `SECURITY.md` asks for vulnerability reports through GitHub private vulnerability reporting for this fork. Dependabot alerts, Dependabot security updates, secret scanning, and secret scanning push protection are enabled. Code scanning is not configured, and this tree does not add a CodeQL workflow.
- A few client fixes landed with this polish. A video decoder that never produces a frame backs off and then stops, instead of spinning. When playback ends, the media session publishes a stopped state. Gallery saves are marked pending until the file is written, so Android 16 does not discard them. Proxy sockets open with the address family that DNS actually returned, IPv4 or IPv6. Spoiler thumbnails and hint flicker no longer invalidate the view on every frame. Story chats load by a positive chat id. Refreshing a profile photo asks for that photo instead of only the first 80. Media captions in a supergroup receive that chat's custom emoji. System force-dark is off, so Android does not recolor the chosen theme. On a device with a hardware keyboard, Back deletes one character in the focused text field instead of leaving the screen. The chat list does not relayout while a dialog or sheet is open.

### Planned adaptations

Researched upstream fixes to adapt into this client. Not shipped until adapted and verified; see [port-sources.md](docs/port-sources.md).

- **BananaGram:** [сохранение исключённых чатов](https://github.com/decoder-dev/Telegram-iOS/commit/5b83d2df95b8f3c52654d1e0daae5d97f3ecd5ea) без потери [закреплений](https://github.com/decoder-dev/Telegram-iOS/commit/a9e065064383e2331918a0a9f05a875f84ba52bc) и структуры форума.
- **BananaGram:** [защита от подозрительных отправителей](https://github.com/decoder-dev/Telegram-iOS/commit/007fb3f1acddbb9484544e0f9d91e9d850c8c47d) — задержка прочтений до подтверждения.
- **AyuGram ПК:** [исправления архива, ссылок и прочтений](https://github.com/AyuGram/AyuGramDesktop/commit/105b6c21a69703a5c103d1615e4fbba12b519c3b) ([архив](https://github.com/AyuGram/AyuGramDesktop/commit/d623608993885a3456fc4a118ebaa2030fd407c4)).
- Remaining gaps are mostly Ghost and Ayu flows that still need live-server acceptance. 156 of 176 Extras settings are marked `implemented_behavior_wired`, including per-chat regex filters and the display-only visual label. The AyuGram disappearing-media timer fix is in this tree when save-deleted is on.
