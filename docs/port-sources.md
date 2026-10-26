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
