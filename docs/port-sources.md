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

The AyuGram inventory records pending scope. No AyuGram implementation is claimed
merely because its setting appears in that inventory. Credits for adapted source
will identify the specific implementation and author.
