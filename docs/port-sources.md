# BananaGram Android sources

This client builds on the GPL-licensed Telegram Android source and preserves its
copyright notices and licenses. The port starts at Telegram 13.1.1 (7237), commit
`3de37c8cfb039452be7398909461da6288babf15`.

Phone features are ported from
[decoder-dev/Telegram-iOS](https://github.com/decoder-dev/Telegram-iOS), including
the tracking URL projection and Ghost Mode behavior. Android uses its own UI,
account preferences and message delivery paths; iOS-specific framework code is
not copied into the Android client.

Adapted fixes from other Android forks:

- [Nekogram: use the owning account for file path lookup](https://github.com/Nekogram/Nekogram/commit/1a9dda22f6abd4ac8da5e9b83fe6af6a440d5bc9).
- [Nekogram: avoid mono-forum database reads on UI paths](https://github.com/Nekogram/Nekogram/commit/a2fd001a9bae88072673cf8a6195c89251368839).

Other researched fork changes are candidates, not included features until they
are adapted and verified. Source credit is retained when a change is adopted.

BananaGram iOS Extras catalog (`ForkExtrasSettings`) is exposed on Android through
`BananaConfig` and the Extras hub in Settings. Boolean preferences are stored per
account; high-impact behaviors (ads, Premium unlock, call confirmation, mention/pinned
notification filters, stories/archive visibility, exact counters, Ghost↔AyuGram sync)
are wired. Non-boolean values (saturation, sticker size/shape, photo quality, seek,
double-tap action, glass levels, marks, backends) live under Values and Marks with
iOS defaults; deleted/edited marks, media seek and outgoing photo size are wired.
Remaining toggles and value behaviors keep preferences for incremental call-site work.

AyuGram research and planned adaptations:

- [AyuGram Desktop](https://github.com/AyuGram/AyuGramDesktop), reference commit
  `0f2a31bb420de98f331990432265b131b8d836b5` (GPL-3.0).
- [AyuGram4A](https://github.com/AyuGram/AyuGram4A), reference commit
  `7013145676d36d82ee13c02a89f72097b7490dcd` (GPL-2.0). Its older Android
  implementation is a behavior reference, not an alternative upstream baseline.
- [AyuGram documentation](https://github.com/AyuGram/AyuGramDocs) (MIT).

Ghost preset, silent-send modes and automatic return offline follow the behavior
documented by AyuGram (original project by Radolyn). Android implementations here
are new adaptations to the current Telegram base, not copies of the older fork.
Live-server acceptance of Ghost behavior is still pending.

[AyuGramX](https://github.com/AyuGram/AyuGramX) at
`5c81e822463a34ef8939bd99873f60789727a3fc` is also inventoried as a behavior
reference. Its TDLib architecture is not merged into this MTProto client.

The AyuGram inventory distinguishes implemented controls from pending scope.
No feature is claimed merely because its setting appears in the inventory.

The local text archive follows the behavior of BananaGram iOS
`MessageSavingBridge.swift` / `MessageSavingStore.swift` and AyuGram message
saving. Its Android database and capture hooks are new implementations. The
archive defaults off, is keyed by owning user and test/production backend, and
is stored outside Telegram cache and Android automatic backups. It stores
previous/deleted text and formatting, with a 32-version/5,000-record/32-MB text
budget, search and clear. Optional downloaded attachments use a separate
64-MiB/file and 512-MiB/account budget. Files are opened before Telegram unlinks
its cache and copied on a separate worker; immutable media is reused across
versions. Schema 1 migrates to schema 2 without dropping records.

Only the dedicated attachment subdirectory is added to FileProvider, and sharing
happens after an explicit Open action; sibling databases stay outside its roots.
Private archive cleanup never deletes source paths. Proactive downloads, TTL or
secret retention, inline retained messages and import/export remain pending.

The optional pre-story prompt follows AyuGram Ghost documentation. Its native
Android presenter and current-viewer integration are new adaptations. Ownership
is checked again after choosing; a cancelled prompt does not open a viewer.
Existing parent-story playback is paused through its native dialog state and
restored on dismissal. Authenticated story playback and server receipts still
require a working test-server account for acceptance.


Message filtering follows [AyuGram4A AyuFilter (Radolyn, 2023)](https://github.com/AyuGram/AyuGram4A/blob/7013145676d36d82ee13c02a89f72097b7490dcd/TMessagesProj/src/main/java/com/radolyn/ayugram/AyuFilter.java)
and the [AyuGram v2 interchange documentation](https://github.com/AyuGram/AyuGramDocs/blob/42f5f32c6b05aab4a69a5f8d0bf7652e1cb11a9b/android/filters/dev.md).
The Android 13.1.1 integration, scoped storage, immutable v2 model, native editor
and asynchronous evaluator are new adaptations. Original source notices remain
in the upstream files; no claim of AyuGram authorship or complete parity is made.

Java regex syntax, including lookaround and backreferences, is evaluated in a
private permissionless Android service rather than during UI drawing. A 250-ms
watchdog stops a pathological worker; malformed, unavailable and slow rules
leave messages visible. Android 10+ uses separate service instances per engine;
older Android versions share the private isolated service. App initialization
and configuration-change handling skip Telegram setup in that process,
including Android 16 service-name suffixes. Idle bindings are released after
30 seconds so inactive filters do not keep an extra process resident.
See [Android isolated service binding](https://developer.android.com/reference/android/content/Context#bindIsolatedService(android.content.Intent,int,java.lang.String,java.util.concurrent.Executor,android.content.ServiceConnection)).

The bounded cache includes exact text/button/type content, dialog/message or
album identity, owning user/backend and configuration generation. Rules default
off. Local v2 imports preview the resulting list and require an explicit Apply;
peer URLs are not opened. Rule/exclusion counts, UTF-8 payload sizes and JSON nesting depth are bounded
before the platform parser is called.
Filtered chat rows retain message positions at 1 px and exclude accessibility
content. Drag selection and copy/forward selection cannot include hidden rows;
a newly hidden selected message clears the current selection. Automatic download suppression, dialog preview masking, blocked-user
filtering are separate remaining parity work.
Authenticated chat scrolling and group rendering still need live-account QA.


Bluetooth call-device fixes adapt Forkgram changes by Eduard Tykhoniuk
([detection](https://github.com/forkgram/TelegramAndroid/commit/0dccc03b93c41033527a74ca773319ce0d87334e),
[stale output menu](https://github.com/forkgram/TelegramAndroid/commit/258e9b71bae598fb9f61fed79756b6166b8f18b9)).
The adaptation checks permissions through the service context so a foreground
Activity is unnecessary. Permission revocation falls back to call-capable audio
devices, including hearing aids and LE Audio on their supported Android versions;
an A2DP media route alone is not treated as a call headset. Radio-off and device
removal clear stale state/name, profile-proxy cleanup is unconditional, and a
queued SCO start cannot reconnect after the headset state was cleared. Device-name
lookups remain retryable after failures. Emulator checks do not replace real
headset, permission-revocation and live-call acceptance.


Remote rule imports support the documented `tg://ayu/filters/import/URL` syntax
and raw HTTPS sources on dpaste.com, gist.githubusercontent.com, pastebin.com
and nekobin.com. Every redirect is validated against the same allowlist; non-HTTPS,
credentials in URLs, unsupported ports and fragments are rejected. Source reads
run on a bounded worker pool, have a 20-second result deadline and can be cancelled
without waiting for network cleanup on the UI. Raw and decompressed bodies are
limited to 256 KiB and decoded as strict UTF-8. URLs and downloaded rule bodies
are not written to application logs, and peer links are never resolved or joined.

Both local and remote input use the same review-before-Apply path. Import does
not enable the master filter switch. Native EN/RU Blue/Night tests cover preview,
cancel, apply, export and rejection of an unsupported source; a separate opt-in
emulator probe verified an actual public Gist HTTPS JSON transfer without applying
it. Authenticated Telegram deep-link routing and a live external AyuGram list are
still acceptance work; the documentation's old dpaste sample was unavailable.


The editor preserves its draft expression and sample when Android rebuilds its
view (including theme changes). Each view owns a separate evaluator and ignores
callbacks from the previous view. Invalid syntax, a matching deadline and a
service-start failure have distinct feedback. The isolated service initializes
Android's regex runtime before exposing its Binder; the 250 ms expression budget
is unchanged. Native tests rebuild the editor before checking and saving a rule.
A cold GitHub emulator previously rejected a valid expression; these changes are
under renewed CI validation and must not be treated as confirmed live-account QA.


Quick filter creation follows AyuGram's documented selected-text action. The
native chat selection toolbar offers Add filter for ordinary message/caption
selection, respects Telegram's copy restrictions and excludes secret chats,
fact-checks and description text. The action clears selection and offers current
chat or shared scope. Selected text is quoted as a literal (including embedded
`\E` terminators), with the expanded expression checked against the normal size
limit. It opens an editable draft with the original text as a sample; no rule is
saved and no filter switch is enabled until the user explicitly saves. Leaving
the scope dialog or draft cancels without altering current rules.
