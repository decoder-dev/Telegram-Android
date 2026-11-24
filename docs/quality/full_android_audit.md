# BananaGram Android: full audit journal

Started 2026-11-18 on `main` at `bff623df3`. The journal is kept up to date as fixes land.

## Method and limits

- Seven read-only reviews covered: storage/security, Ghost mode, filters, proxy/network, media/JNI,
  recent UI commits, and build/manifest/CI. Every finding below was re-read against its callers
  before it was patched. Findings that did not survive that check are not listed.
- No JDK, Android SDK or NDK is installed on the audit machine, and the owner chose CI as the
  build gate. Nothing below was compiled or run locally. **Build result** and **Runtime
  verification** therefore refer to the GitHub Actions run for the pushed commits: Java
  compilation, JVM unit tests, the x86_64 debug APK, and the unsigned smoke suite on API 33–36.
- Not available: physical devices, authenticated Telegram accounts, ASan/HWASan, Perfetto or
  frame-timing captures, and a signed release build. No claim below rests on any of these.
- `python -m unittest discover -s scripts -p 'test_*.py'`: 15 tests, OK (1 skipped) at the baseline.

## Coverage matrix

| Area | Status |
|---|---|
| Message filters (FilterStore, MessageFilters, RegexEngine, IsolatedRegexService, RemoteFilterSource, AyuFilter) | Static review completed; bugs confirmed; fixed (CI pending) |
| Ghost mode, BananaHooks, silent sending, tracking links | Static review completed; bugs confirmed; one fixed, others open |
| History DB, saved attachments, session backup, archive lock, account slots | Static review completed; bugs confirmed; two fixed, others open |
| Auto MTProxy, proxy rotation, VLESS parsing | Static review completed; bugs confirmed; two fixed, others open |
| tgnet native (fork changes) | Static review completed; bug confirmed; fixed (CI builds native only for the APK job) |
| Media conversion, FFmpeg, Vosk, video download service | Static review completed; one fixed, others open |
| Fork native/JNI (AV1 gate, twidget glue, evdns bounds, 10-account switch) | Static review completed; clean |
| Widgets, glass, chat row factories, forum topic fix, ten accounts | Static review completed; P3 items open |
| Manifests, FileProvider, receivers, PendingIntents, R8 keeps, workflows, scripts | Static review completed; P3 items open |
| Upstream Telegram core (MessagesController, MessagesStorage, FileLoader, ConnectionsManager beyond fork hooks) | Not inspected beyond the fork's integration points |
| Upstream native libraries (FFmpeg, libvpx, dav1d, WebRTC, tlottie, td) | Not inspected: pinned upstream submodules |
| Runtime on API 33–36 | CI smoke suite only (unsigned emulator) |
| Release build, R8 output, signing continuity | R8 covered: CI runs `minifyAfatStandaloneWithR8` on every push/PR (BUG-019 fixed 2026-11-24); signing continuity still needs the owner's keys |

CI run 43539172965 (failed on all four APIs) was a real regression in
`WidgetFrameNode`, fixed by `f57d75a10` and `bff623df3`. Run 43543353276 passed. It was not flaky.

## Fixed

### BUG-001: regex filters could freeze notifications and the UI (ReDoS)
- Severity: P1 · Subsystem: message filters
- Files: `banana/BoundedMatch.java` (removed), `banana/MessageFilters.java` (`shouldHideNotification`),
  `com/radolyn/ayugram/AyuFilter.java`; callers `NotificationsController.java:1432`,
  `ChatActivity.java` `getItemViewType`, `DialogCell.java:3836`, `DownloadController.java:878`
- Observed: `BoundedMatch` enforced its deadline in `CharSequence.charAt`. Android's libcore
  `Matcher` converts its input to a String for ICU, so the deadline never fired on a device. A rule
  like `(a|aa)+$` against about 40 characters would stall the shared notifications queue. Through
  `AyuFilter`, it would also freeze the UI thread (ANR), and `AyuFilter`'s class lock could make the
  UI thread wait on that stalled queue.
- Expected: a runaway expression is stopped and skipped.
- Reproduction: add the AyuGram filter `(a|aa)+$`, enable filters, and receive or open a channel
  message made of 40×"a" + "b".
- Root cause: in-process `java.util.regex` cannot be interrupted on Android. The JVM test passed
  only because OpenJDK's matcher does read through `charAt`.
- Fix: `IsolatedMatch` runs every synchronous match in `IsolatedRegexService`, which is killed
  after 250 ms.
  - Background callers wait up to 1.5 s and fail open.
  - The UI thread gets an asynchronous verdict, then the views refresh.
  - Expressions that time out or fail to compile are quarantined until the filters change.
  - `AyuFilter` no longer blocks while holding its lock.
- Regression test: `IsolatedMatchTest`. An on-device ReDoS test belongs in androidTest and was not added.
- Commit: `f487d8188` · Status: fixed, CI pending

### BUG-002: notifications matched different text from the chat
- Severity: P2 · File: `banana/MessageFilters.java`
- Observed: the notification path matched only the message body. The chat path also adds
  `<button>` and `<type>` tags, so reversed or tag rules hid or kept the wrong notifications.
- Fix: both paths use the same `text()`. · Commit: `f487d8188` · Status: fixed, CI pending

### BUG-003: toggling global case-insensitivity left stale verdicts
- Severity: P2 · File: `banana/MessageFilters.java` `settingsChanged`
- Root cause: the cache key does not include the flags, and `settingsChanged` only re-notified
  listeners.
- Fix: `settingsChanged` now drops the cache, queue and quarantine. A new `refreshViews` covers the
  case that only needs to redraw. · Commit: `f487d8188`

### BUG-004: a transient worker failure disabled a rule for the rest of the session
- Severity: P3 · File: `banana/MessageFilters.java` `evaluate`
- Fix: UNAVAILABLE and BUSY results re-queue the batch with a backoff, up to three times. Only
  TIMEOUT and BAD_PATTERN quarantine a rule. · Commit: `f487d8188`

### BUG-005: the Filters screen switch could not turn filtering off
- Severity: P2 · File: `ui/BananaFiltersActivity.java`; `BananaHooks.regexFiltersEnabled` ORs two flags
- Fix: the switch also sets the Extras flag. · Commit: `f487d8188`

### BUG-006: tgnet dropped data sent while a proxy host name resolved
- Severity: P1 (connection stalls on every hostname proxy) · File: `jni/tgnet/ConnectionSocket.cpp` `isDisconnected`
- Observed: the first `req_pq` or proxy ping through a proxy given by host name was dropped, and
  the connection waited for its timeout.
- Root cause: `1d2394ccc` moved `socket()` until after the resolve, so `socketFd == -1` during it,
  and `Connection::sendData` treated that as disconnected.
- Fix: a pending resolve counts as connecting. The existing `adjustWriteOpAfterResolve` path then
  flushes the queued data.
- Regression test: none (native; no harness). · Commit: `84b114497` · Status: fixed, CI pending (APK build only)

### BUG-007: the archive lock survived logout and passed to the next account in the slot
- Severity: P1 · Files: `banana/BananaArchiveLock.java`, `UserConfig.clearConfig`
- Observed: account B signed into slot N inherited account A's archive password, cooldown and
  biometric key. B's archive and Saved Messages were blocked.
- Fix: `BananaArchiveLock.clearAccount` runs from `clearConfig`. · Commit: `383711ba5`

### BUG-008: session-backup consent survived logout
- Severity: P2 · File: `banana/BananaHooks.clearAccountLocalData`
- Observed: the next account in the slot had its session keys backed up without opting in.
- Fix: the consent key is removed on logout. · Commit: `383711ba5`

### BUG-009: forwards ignored "Send without sound"
- Severity: P2 · File: `SendMessagesHelper.sendMessage(ArrayList<MessageObject>, …)`
- Fix: that method applies `BananaConfig.shouldSendSilently`, which also covers the AyuGram
  forwarders. · Commit: `23776f9b3`

### BUG-010: Auto MTProxy did not resume after the network returned
- Severity: P2 · File: `banana/BananaAutomaticProxy.java` (state observer)
- Fix: refreshes deferred offline and probes cancelled by a drop restart on the next non-connected
  state. Refresh runs at most once a minute. · Commit: `7d25f3299`

### BUG-011: tg:// and t.me list entries skipped port and host checks
- Severity: P3 · File: `banana/BananaAutomaticProxy.parseList`
- Observed: port 70000 wrapped to 4464 in native code. · Commit: `7d25f3299`

### BUG-012: FFmpeg followed playlists in received files
- Severity: P2 (security) · File: `banana/MediaConversionPolicy.arguments`
- Observed: a received `.m3u8` "music" file converted to voice could fetch http segments, leaking
  the IP, or splice other cached files into the converted output.
- Fix: `-protocol_whitelist file` and a container `-format_whitelist` that excludes hls and concat.
- Regression test: `MediaConversionPolicyTest.untrustedInputCannotUsePlaylistsOrNetworkProtocols`
- Commit: `815378189`. Not verified on a device with the bundled FFmpeg build: if it rejects an
  input format that users rely on, add that demuxer to `INPUT_FORMATS`.

### BUG-013: the AyuGram settings observer leaked after an account switch
- Severity: P3 · File: `com/radolyn/ayugram/ui/preferences/AyuGramPreferencesActivity.java` · Commit: `320b191b8`

### BUG-035: filter edits near the size limit crashed the app
- Severity: P3 · File: `ui/BananaFiltersActivity.java`
- Observed: toggling, deleting or excluding a rule, or applying a merged import, could make
  `FilterRules.export` exceed 256 KB. The resulting `IllegalArgumentException` escaped a dialog
  listener and crashed the app.
- Fix: these edits go through `saveRules`, which reports the error. The import preview checks the
  merged size before offering Apply. · Commit: see git log

### BUG-036: recorded round videos never sent ("!" and Retry fails at once)
- Severity: P1 (core messaging feature broken on rc.17) · Subsystem: uploads / Telegram 13.2 round recorder
- Files: `AndroidUtilities.isInternalUri` (fork-hardened), `FileUploadOperation.startUploadRequest`,
  `InstantCameraView2` (`setOutputDirectory(files/cache)`)
- Observed (owner's screen recording, rc.17): a recorded round shows a black preview and a red
  "!", and Retry fails immediately.
- Root cause:
  - The fork tightened the app-private upload check to refuse everything under the data
    directory except `cacheDir` and `no_backup/ayu-attachments`.
  - Telegram 13.2's new round recorder writes into `files/cache/`, and the camera into
    `files/camera_tmp.mp4`.
  - `FileUploadOperation` checks the open descriptor and throws "trying to upload internal file",
    so the upload fails and the message is marked as failed.
  - Official Telegram only matches `/data/data/<pkg>`, which a canonical path never contains, so
    it never refused these.
- Fix: allow `files/cache/`, `camera_tmp.mp4` and the `*thumb.jpg` previews. Everything else stays
  refused (databases, `tgnet.dat`, prefs).
- Regression test: `PrivateUriTest.recordedRoundVideosAndCameraOutputCanBeUploaded` (emulator suite).
- Runtime verification: not run on a device. The diagnosis is from code, not from logs.

### Second batch (2026-11-18)
| ID | Sev | Fix |
|---|---|---|

## Liquid glass port from Cherrygram (2026-11-18)

Source: arsLan4k1390/Cherrygram at 8c66eb51f (GPL v2 or later, like this tree). Its glass engine
(`utils/glass`, `blur3`, the AGSL shaders, `GlassTabView`) is the same upstream Telegram code that this fork
already has; ours carries newer fixes (display-list release on rebuild, generation ids for wallpaper sources).
What Cherrygram adds on top, and what happened to each piece:

| Cherrygram | Here |
|---|---|
| Glare effects: Disable / Default / Stroke | Already ported as Glass highlights. Now Off and Outline keep an even outline ring, as in Cherrygram, instead of removing every rim (`GlassRim.withoutHighlights`, `GlassRimTest`). |
| Inner stroke sized into the rim slices (`GlassRimRenderer`) | Ported: a thick stroke's corners were cut where the nine-patch stretches. |
| Live chat header and composer preview in the glare settings | Ported as `BananaGlassPreviewView` at the top of Extras → Values → Liquid glass, with the highlight choice. Since 2026-11-19 the composer is the real `ChatActivityEnterView` in preview mode, as in Cherrygram's `ChatInputPreviewView`. |
| iOS-style message field (`CP_iOSMessageInputField`): attach and send buttons in their own glass bubbles | Ported 2026-11-19 as Split message field (`BananaConfig.glassComposer`, off by default). Only the field's hunks were taken from Cherrygram's `ChatActivityEnterView`, `ChatActivity`, `ChatInputViewsContainer`, `PeerStoriesView`, the two timeline views and the mic/camera icon; its Gemini, pre-send translation, haptics and send-as long press were left out. Differences: the layout is cached per field at creation (Cherrygram re-reads the setting on every call), the mic/camera animations are chosen per field rather than globally, and the attach icon is Telegram's, not the Solar pack's. |
| Main tabs without titles | Ported as Tab bar → Icons only in tab bar (`TAB_BAR_ICONS_ONLY`). |
| Main tab order, profile tab, open settings by swipe, force-open chats, folders in the bar | Not glass; this fork already has its own tab bar options. Not ported. |
| Split glass message field follow-ups (`PeerStoriesView` side controls guard + `InstantCameraView.setButtonsBackground`, `ChatActivity` `bottomOverlayStartButton` full-width) | Ported 2026-11-20. |
| Bottom chat folders blur capture (`DialogsActivity.iBlur3PositionFolders`, `invalidateBlurCaptures`, search field translation when folders are at bottom) | Ported 2026-11-20 for `bananaFoldersAtBottom()`. |
| Main tabs backdrop z-order & zero-alpha skip (`MainTabsActivity` `getPagerChildIndex` / `getAlphaInContentView`), null-safe `blur3_updateColors` & `GlassTabView.updateUserAvatar`, floating circular glass search button beside bottom tabs | Ported 2026-11-20; floating search button gated behind `BananaConfig.glassTabSearch` (off by default) and fixes Cherrygram's double-dp `-dp(10)` margin. |
| Smooth liquid glass reactions handoff & double-shadow removal (`ReactionsContainerLayout`, `CustomEmojiReactionsWindow`) | Ported 2026-11-20 (no duplicate shadow under glass delegate, 200 ms `glassHandoff` crossfade on close, `searchBox.setUseCustomBackground()`). |
| Centered adaptive glass chat header (`ActionBar`, `ActionBarMenu`, `ActionBarMenuItem`, `BackDrawable`, `SimpleActionBarMenuOverlay`, `ChatAvatarContainer`, `ChatActivity`, `ChannelAdminLogActivity`, `MediaActivity`, `ProfileActivity`, `ChatActivityTopPanelLayout`, status drawables, `BananaGlassPreviewView`) | Ported 2026-11-20 as `BananaConfig.glassCenteredHeader` (off by default, live-previewed in Extras → Values → Liquid glass), including 24 dp chat top-panel radius, compact centered status drawables, chevron back arrow across chat/profile/media/admin log, and action bar glass crossfade hooks. |
| Active tab stroke outline (`FilterTabsView`, `ViewPagerFixed`, `ScrollSlidingTextTabStrip`), capsule profile glass buttons (`ProfileActionsView`, 50 dp radius), and `ProfileActivity` `otherItem` `resourcesProvider` fix | Ported 2026-11-20 (`BananaConfig.glassTabStroke` and `BananaConfig.glassProfilePills`, both off by default). |
| Blurred message menu background (`WindowBlurHelper` / `MessageMenuReactionsGlass`) | A different message menu gated by Cherrygram's donation check (closed-source). Not ported. |
| Attach button always in the side controls when the iOS field is off; mute/unmute in the channel bar | Cherrygram layout preferences outside the glass field. Not ported. |

Verification: Earlier commits verified in CI (JVM tests, compile, API 33–36 screenshots); the 2026-11-20 port pass was verified statically (balanced syntax, imports, cross-class signatures, XML validation, Python `scripts/` tests) and awaits GitHub Actions CI compile/tests.

## Open (confirmed, not fixed in this pass)

| ID | Sev | Where | Issue | Why not fixed yet |
|---|---|---|---|---|
| BUG-014 | P2 | `BananaConfig.migrateAyuDuplicates` | Migration resets AyuGram's `useScheduledMessages`, but Banana scheduling needs the full ghost preset, which includes stories that AyuGram never had. Auto-scheduling stops after the upgrade. | Keeping the Ayu flag would leave it with no visible switch, and forcing story reads on adds privacy behaviour. Needs a product decision; the migration has already run on existing installs. |
| BUG-015 | P2 | `ChatActivity` OPTION_TTL → `markMessageAsRead2` / `SecretChatHelper` | With "Don't read messages" on, the explicit "TTL" action arms local self-destruct, but the read never reaches the server. | Needs a `force` path through core read methods; needs authenticated testing. |
| BUG-016 | P2 | `MessageHistory`, `UserConfig.clearConfig` | The deleted/edited-message archive and up to 512 MB of media per owner are never wiped on logout. | Product decision (keep or wipe on logout). |
| BUG-030 | P3 | `LaunchActivity` `tg://xiaomi` | Any link opens the system uninstall dialog on MIUI (AyuGram easter egg). | Removing a fork feature needs the owner's approval. |
| BUG-034 | P3 | `DialogCell` / `MessageFilters.notifyChanged` | Banana filters do not apply to chat-list previews, yet every batch reloads the dialogs. | Product decision. |

## Resolved 2026-11-24

| ID | Resolution |
|---|---|
| BUG-017 | LRU eviction: `HistoryDatabase.evictOldestStoredMedia` frees whole files (speculative retained copies first, then record-linked oldest-first, text and media descriptors kept), `SavedAttachments.availableBytes` sizes it, `MessageHistory.saveWithEviction` applies it before both copy paths. Covered by `HistoryEvictionTest` (instrumented) and `SavedAttachmentsTest.availableBytesTracksTheRemainingBudget` (JVM). |
| BUG-019 | CI now runs `:TMessagesProj_AppStandalone:minifyAfatStandaloneWithR8` on every push and pull request; minification needs no signing keys, only packaging does. |
| BUG-020 | Already fixed in `1e1c5f3`: unlock requires the authenticated `CryptoObject` cipher to actually work. The table row was stale. |
| BUG-037 | `BananaConfig.clampInt` capped `VOICE_CHANGER_PRESET` at 5 (CUSTOM), so the newly added Livesey preset (6) could not be saved; the bound now follows `VoicePresets.LIVESEY`. |
| BUG-038 | `run-android-smoke.py` capped the whole instrumentation at 600 s; the glass scenarios pushed the suite past eleven minutes on API 36 and the cap fired mid-scenario. The cap is 1200 s now, and `HistoryEvictionTest` joined the smoke class list (it was compiled but never run). |
| BUG-031 | Restored `android:enableOnBackInvokedCallback="true"` in the application manifest. `LaunchActivity` already carried upstream's full OnBackInvoked/OnBackAnimation registration — only the opt-in flag was missing, so predictive back stayed inert on API 33–35. Upstream master ships the same flag; other activities' `onBackPressed` overrides keep working through the framework default callback. forkgram #380 confirms this was user-visible pain. |
