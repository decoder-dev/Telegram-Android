# Аттестация новых функций BananaGram Android

Проверка исходников охватывает все 176 настроек каталога iOS и 157 переключателей Android. Наличие вызова или переключателя не означает успешной проверки поведения на устройстве.

## Подтверждённые проверки

- XML ресурсов разбирается; у каждого переключателя есть английская и русская подпись.
- Каждый переключатель включён в каталог Extras; неизвестных пунктов в каталоге нет.
- Записанные результаты JVM: 67 тестов; ошибок 0, падений 0, пропусков 0.
- Результаты сборки и ограничения среды зафиксированы в `feature-attestation-validation.md`.

## Реальные ограничения

- Сессии Android: Keystore для локальных копий, пароль для экспорта; перенос настоящей сессии требует отдельной приёмки.
- Локальная расшифровка: Vosk RU/EN, без серверного запроса; выполнение MediaCodec/Vosk проверяется отдельным Android-сценарием.
- TikTok/Shorts: встроенный yt-dlp/FFmpeg; реальные сайты, отмена и сохранение в галерею требуют сетевой приёмки.
- Сохранение медиа: завершение загрузки связано с отдельным архивом; сценарий кеш/удаление/очистка включён в AndroidTest.
- Ghost, серверные удаления/правки и истории требуют приёмки на авторизованном тестовом аккаунте.
- GPU-стекло, читаемость и частота кадров требуют проверки на устройстве. JVM-тесты не измеряют FPS.

## Темы и стекло

Разрешены только Classic White, Classic Black, BananaGram Cream и BananaGram Graphite. Обычное применение, фоновые переходы, временный предпросмотр и импорт проверяют политику. Посторонний объект с разрешённым названием не проходит проверку принадлежности реестру. Палитры чатов не переопределяют эти темы. Старые Glass-палитры мигрируют в светлую/тёмную классику.

«Включить жидкое стекло» — отдельный переключатель Extras. Он управляет геометрией и GPU-преломлением в существующем blur3, заливкой и независимыми бликами пузырей. При ошибке RuntimeShader остаётся обычное размытие. Настройки энергосбережения сохраняют приоритет. Пузыри получают прозрачную заливку и контур; текстовые пузыри Android 13+ используют GPU-линзу над обоями; она принадлежит ячейке, не захватывает сам чат и не запускает собственный цикл кадров. GPU-приёмка ещё не выполнена.

## Каталог iOS: индивидуальные результаты

Статусы ниже — результаты проверки реализации в исходниках. `implemented_behavior_wired` обозначает подключение к коду, а не успешный сценарий на устройстве. Серверная и визуальная приёмка не присваиваются автоматически.

| Настройка | Статус исходников | Реализация / ограничение |
|---|---|---|
| `removePings` | `implemented_behavior_wired` | BananaConfig.Option.REMOVE_PINGS via BananaHooks call sites |
| `sendTelegramStatistics` | `implemented_behavior_wired` | BananaHooks.shouldDropAnalyticsRequest |
| `bananaIslandEnabled` | `implemented_behavior_wired` | BananaConfig.Option.BANANA_ISLAND_ENABLED via BananaHooks call sites |
| `bananaIslandShowsName` | `implemented_behavior_wired` | BananaConfig.Option.BANANA_ISLAND_SHOWS_NAME via BananaHooks call sites |
| `bananaIslandFollowsIcon` | `implemented_behavior_wired` | BananaConfig.Option.BANANA_ISLAND_FOLLOWS_ICON via BananaHooks call sites |
| `wideTabBar` | `implemented_behavior_wired` | BananaHooks.filterTabPaddingDp |
| `integratedTabSearch` | `implemented_behavior_wired` | BananaConfig.Option.INTEGRATED_TAB_SEARCH via BananaHooks call sites |
| `tabSearchOnLeft` | `implemented_behavior_wired` | BananaConfig.Option.TAB_SEARCH_ON_LEFT via BananaHooks call sites |
| `bottomChatFoldersEnabled` | `implemented_behavior_wired` | BananaConfig.Option.BOTTOM_CHAT_FOLDERS_ENABLED via BananaHooks call sites |
| `avatarGlowEnabled` | `implemented_behavior_wired` | BananaHooks.glowPaint |
| `snowEnabled` | `implemented_behavior_wired` | BananaConfig.Option.SNOW_ENABLED via BananaHooks call sites |
| `showChannelForwardCount` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_CHANNEL_FORWARD_COUNT via BananaHooks call sites |
| `reactionGlowEnabled` | `implemented_behavior_wired` | BananaHooks.glowPaint |
| `showContactsTab` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_CONTACTS_TAB via BananaHooks call sites |
| `ghostDontReadMessages` | `implemented_pending_full_integration` | Option.DONT_READ_MESSAGES |
| `ghostDontReadStories` | `implemented_pending_full_integration` | Option.DONT_READ_STORIES |
| `ghostDontSendOnline` | `implemented_pending_full_integration` | Option.HIDE_ONLINE |
| `ghostDontSendTyping` | `implemented_pending_full_integration` | Option.HIDE_TYPING, Option.HIDE_RECORDING, Option.HIDE_UPLOAD |
| `ghostGoOfflineAutomatically` | `implemented_pending_live_server_acceptance` | Option.OFFLINE_AFTER_ACTIVITY: own-online updates and ordinary send acknowledgements; debounced and bounded retries. |
| `ghostReadOnInteract` | `implemented_behavior_wired` | BananaConfig.Option.GHOST_READ_ON_INTERACT via BananaHooks call sites |
| `ghostAlertBeforeOpeningStory` | `implemented_prompt_and_policy_verified_pending_authenticated_story_flow` | Option.WARN_BEFORE_STORIES. Account-owned choice: enable Ghost, open normally or cancel; own-only stories and already-private views skip it. Native button/back/dismiss behavior and EN/RU Blue/Night layout verified, including fresh install with disabled system animations. |
| `instantPasscodeLock` | `implemented_behavior_wired` | BananaConfig.Option.INSTANT_PASSCODE_LOCK via BananaHooks call sites |
| `hideMentionNotifications` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_MENTION_NOTIFICATIONS via BananaHooks call sites |
| `hidePinnedNotifications` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_PINNED_NOTIFICATIONS via BananaHooks call sites |
| `sessionKeychainBackup` | `implemented_android_backup_pending_session_acceptance` | BananaSessionBackup: Android Keystore automatic copies; password-encrypted SAF export/import into an empty account slot; restore only before native startup. |
| `compactChatList` | `implemented_behavior_wired` | BananaHooks.dialogCellHeightDp |
| `compactMessagePreview` | `implemented_behavior_wired` | BananaConfig.Option.COMPACT_MESSAGE_PREVIEW via BananaHooks call sites |
| `compactFolderNames` | `implemented_behavior_wired` | BananaHooks.compactFolderLabel |
| `hideReactionsBar` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_REACTIONS_BAR via BananaHooks call sites |
| `useRecentEmojiInReactions` | `implemented_behavior_wired` | BananaConfig.Option.USE_RECENT_EMOJI_IN_REACTIONS via BananaHooks call sites |
| `showDC` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_DC via BananaHooks call sites |
| `showProfileId` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_PROFILE_ID via BananaHooks call sites |
| `accentColorSaturation` | `implemented_behavior_wired` | BananaHooks.applyAccentSaturation (Theme) |
| `confirmBeforeCall` | `implemented_behavior_wired` | BananaConfig.Option.CONFIRM_BEFORE_CALL via BananaHooks call sites |
| `sendWithReturnKey` | `implemented_behavior_wired` | BananaConfig.Option.SEND_WITH_RETURN_KEY via BananaHooks call sites |
| `forceBuiltInMic` | `implemented_behavior_wired` | BananaConfig.Option.FORCE_BUILT_IN_MIC via BananaHooks call sites |
| `keepAliveInBackground` | `implemented_behavior_wired` | BananaConfig.Option.KEEP_ALIVE_IN_BACKGROUND via BananaHooks call sites |
| `translationBackend` | `implemented_behavior_wired` | BananaHooks.preferAlternativeTranslation (TranslateController) |
| `transcriptionBackend` | `implemented_offline_engine_pending_device_acceptance` | BananaOfflineVoice: Vosk RU/EN model download, MediaCodec decode, mono 16 kHz resampling; local path intercepts Telegram transcription RPC; opt-in public-fixture Android test. |
| `scrollToNextChatDisabled` | `implemented_behavior_wired` | BananaConfig.Option.SCROLL_TO_NEXT_CHAT_DISABLED via BananaHooks call sites |
| `saveDeletedMessages` | `implemented_local_capture_verified_pending_live_server_acceptance` | Opt-in previous/deleted text and downloaded photo/document snapshots. Native cache replacement, unlink, shared-file reuse, media-only rows and pending-clear verified. No live server acceptance yet; inline anti-recall and import/export pending. |
| `saveMessagesHistory` | `implemented_local_capture_verified_pending_live_server_acceptance` | Opt-in previous/deleted text and downloaded photo/document snapshots. Native cache replacement, unlink, shared-file reuse, media-only rows and pending-clear verified. No live server acceptance yet; inline anti-recall and import/export pending. |
| `saveForBots` | `implemented_local_capture_verified_pending_live_server_acceptance` | Opt-in previous/deleted text and downloaded photo/document snapshots. Native cache replacement, unlink, shared-file reuse, media-only rows and pending-clear verified. No live server acceptance yet; inline anti-recall and import/export pending. |
| `saveMedia` | `implemented_proactive_retention_pending_device_acceptance` | Separate immutable media archive; 64 MiB/file and 512 MiB/account. FileLoader completion links pending copies. Secret/TTL media excluded. |
| `proactiveSaveMedia` | `implemented_proactive_retention_pending_device_acceptance` | Per-owner/backend proactive media index survives cache deletion without creating edit-history rows; clear invalidates pending copies. |
| `deletedMessageMark` | `implemented_wired` | BananaConfig + history/UI labels via AyuConfig marks |
| `editedMessageMark` | `implemented_wired` | BananaConfig + history/UI labels via AyuConfig marks |
| `ayuForward` | `implemented_behavior_wired` | BananaConfig.Option.AYU_FORWARD via BananaHooks call sites |
| `bypassDownloadRestrictions` | `implemented_behavior_wired` | BananaConfig.Option.BYPASS_DOWNLOAD_RESTRICTIONS via BananaHooks call sites |
| `localPremium` | `implemented_behavior_wired` | BananaConfig.Option.LOCAL_PREMIUM via BananaHooks call sites |
| `hideAds` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_ADS via BananaHooks call sites |
| `hideBlockedMessages` | `implemented_behavior_wired` | BananaHooks.shouldHideBlockedSender |
| `allowSecretScreenshots` | `implemented_behavior_wired` | BananaHooks.shouldSecureForSecret |
| `expireTtlButton` | `implemented_behavior_wired` | BananaConfig.Option.EXPIRE_TTL_BUTTON via BananaHooks call sites |
| `keepBannedChats` | `implemented_behavior_wired` | BananaConfig.Option.KEEP_BANNED_CHATS via BananaHooks call sites |
| `regexMessageFiltersEnabled` | `implemented_behavior_wired` | BananaConfig.Option.REGEX_MESSAGE_FILTERS_ENABLED via BananaHooks call sites |
| `regexMessageFiltersCaseInsensitive` | `implemented_behavior_wired` | BananaConfig.Option.REGEX_MESSAGE_FILTERS_CASE_INSENSITIVE via BananaHooks call sites |
| `regexMessageFilterPatterns` | `implemented_behavior_wired` | Per-dialog patterns in FilterStore and BananaFilterEditActivity. iOS stores one account string list; Android applies the same filter engine per chat. |
| `ghostScheduleMessages` | `implemented_behavior_wired` | BananaConfig.Option.GHOST_SCHEDULE_MESSAGES via BananaHooks call sites |
| `hideAllChats` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_ALL_CHATS via BananaHooks call sites |
| `rememberLastFolder` | `implemented_behavior_wired` | BananaConfig.Option.REMEMBER_LAST_FOLDER via BananaHooks call sites |
| `hideTabBar` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_TAB_BAR via BananaHooks call sites |
| `showMessageSeconds` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_MESSAGE_SECONDS via BananaHooks call sites |
| `stripTrackingParameters` | `implemented_pending_full_integration` | Option.CLEAN_TRACKING_LINKS |
| `wideChannelPosts` | `implemented_behavior_wired` | BananaConfig.Option.WIDE_CHANNEL_POSTS via BananaHooks call sites |
| `stickerSizePercent` | `implemented_behavior_wired` | BananaHooks.stickerSizeScale (MessageObject/ChatMessageCell) |
| `doubleTapToEdit` | `implemented_behavior_wired` | BananaConfig.Option.DOUBLE_TAP_TO_EDIT via BananaHooks call sites |
| `quickTranslateButton` | `implemented_behavior_wired` | BananaConfig.Option.QUICK_TRANSLATE_BUTTON via BananaHooks call sites |
| `saveToCloudMenu` | `implemented_behavior_wired` | BananaConfig.Option.SAVE_TO_CLOUD_MENU via BananaHooks call sites |
| `selectFromAuthor` | `implemented_behavior_wired` | BananaConfig.Option.SELECT_FROM_AUTHOR via BananaHooks call sites |
| `downloadSpeedBoost` | `implemented_behavior_wired` | BananaHooks.downloadQueueMaxActive/downloadUseBoostedChunks |
| `outgoingPhotoQuality` | `implemented_wired` | BananaConfig + AndroidUtilities.getPhotoSize |
| `streamerMode` | `implemented_behavior_wired` | BananaHooks.maybeMaskPhone/Username |
| `hideStickerStatus` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_STICKER_STATUS via BananaHooks call sites |
| `hideStickerReplyColors` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_STICKER_REPLY_COLORS via BananaHooks call sites |
| `chooseFolderAfterJoin` | `implemented_behavior_wired` | BananaHooks.maybePromptFolderAfterJoin |
| `recentChatsMenu` | `implemented_behavior_wired` | BananaConfig.Option.RECENT_CHATS_MENU via BananaHooks call sites |
| `messageBookmarks` | `implemented_behavior_wired` | BananaConfig.Option.MESSAGE_BOOKMARKS via BananaHooks call sites |
| `localGroupAlias` | `implemented_behavior_wired` | BananaConfig.Option.LOCAL_GROUP_ALIAS via BananaHooks call sites |
| `disableStoryCameraSwipe` | `implemented_behavior_wired` | BananaConfig.Option.DISABLE_STORY_CAMERA_SWIPE via BananaHooks call sites |
| `jumpToStartInChatMenu` | `implemented_behavior_wired` | BananaConfig.Option.JUMP_TO_START_IN_CHAT_MENU via BananaHooks call sites |
| `hideServiceMessages` | `implemented_behavior_wired` | BananaHooks.shouldHideServiceMessage |
| `confirmReactionsInUnjoinedChats` | `implemented_behavior_wired` | BananaConfig.Option.CONFIRM_REACTIONS_IN_UNJOINED_CHATS via BananaHooks call sites |
| `confirmTelegramLinks` | `implemented_behavior_wired` | BananaHooks.interceptOpenUrl |
| `confirmShareLinks` | `implemented_behavior_wired` | BananaHooks.interceptOpenUrl |
| `confirmChatPreviewEntry` | `implemented_behavior_wired` | BananaConfig.Option.CONFIRM_CHAT_PREVIEW_ENTRY via BananaHooks call sites |
| `disableGiftStarsRedirect` | `implemented_behavior_wired` | BananaConfig.Option.DISABLE_GIFT_STARS_REDIRECT via BananaHooks call sites |
| `visualLabels` | `implemented_behavior_wired` | BananaVisualLabels for the signed-in peer. Extras Profile picker. Drawn on own profile and drawer. Server scam and fake flags win. |
| `hideStickerReplyEmoji` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_STICKER_REPLY_EMOJI via BananaHooks call sites |
| `hideStickerReplyBackground` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_STICKER_REPLY_BACKGROUND via BananaHooks call sites |
| `hidePremiumReplyEmoji` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_PREMIUM_REPLY_EMOJI via BananaHooks call sites |
| `showChatCreationDate` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_CHAT_CREATION_DATE via BananaHooks call sites |
| `profileIdBotApiFormat` | `implemented_behavior_wired` | BananaHooks.formatProfileId |
| `showMutualContact` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_MUTUAL_CONTACT via BananaHooks call sites |
| `hidePremiumStatuses` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_PREMIUM_STATUSES via BananaHooks call sites |
| `hideVerification` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_VERIFICATION via BananaHooks call sites |
| `hideGiftButton` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_GIFT_BUTTON via BananaHooks call sites |
| `visualPhoneEnabled` | `implemented_behavior_wired` | BananaHooks.maybeMaskPhone |
| `visualPhoneNumber` | `implemented_behavior_wired` | BananaHooks.maybeMaskPhone / visualPhoneNumber |
| `ghostSendWithoutSound` | `implemented_pending_live_server_acceptance` | BananaConfig.SilentSending; NORMAL,IN_GHOST_MODE,ALWAYS. |
| `ghostSuggestForStories` | `implemented_prompt_and_policy_verified_pending_authenticated_story_flow` | Option.WARN_BEFORE_STORIES. Account-owned choice: enable Ghost, open normally or cancel; own-only stories and already-private views skip it. Native button/back/dismiss behavior and EN/RU Blue/Night layout verified, including fresh install with disabled system animations. |
| `hideStories` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_STORIES via BananaHooks call sites |
| `hideArchive` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_ARCHIVE via BananaHooks call sites |
| `allowProtectedScreenshots` | `implemented_behavior_wired` | BananaHooks.shouldSecureForProtected |
| `accelerateUpload` | `implemented_behavior_wired` | BananaHooks.uploadMaxConcurrent/uploadMaxKBytes |
| `infiniteRecentStickers` | `implemented_behavior_wired` | BananaHooks.recentStickersMaxCount |
| `onlyAddedStickers` | `implemented_behavior_wired` | BananaConfig.Option.ONLY_ADDED_STICKERS via BananaHooks call sites |
| `autoPauseMediaInBackground` | `implemented_behavior_wired` | BananaConfig.Option.AUTO_PAUSE_MEDIA_IN_BACKGROUND via BananaHooks call sites |
| `doubleTapSeekSeconds` | `implemented_wired` | BananaConfig + PhotoViewer double-tap seek |
| `showMessageId` | `implemented_behavior_wired` | BananaHooks.appendMessageId |
| `autoRevealSpoilers` | `implemented_behavior_wired` | BananaConfig.Option.AUTO_REVEAL_SPOILERS via BananaHooks call sites |
| `doubleTapAction` | `implemented_behavior_wired` | BananaHooks.doubleTapAction (ChatActivity) |
| `repeatMessageMenu` | `implemented_behavior_wired` | BananaConfig.Option.REPEAT_MESSAGE_MENU via BananaHooks call sites |
| `staticRoundVideoZoom` | `implemented_behavior_wired` | BananaConfig.Option.STATIC_ROUND_VIDEO_ZOOM via BananaHooks call sites |
| `stickerShape` | `implemented_behavior_wired` | BananaHooks.stickerRoundRadiusPx (ChatMessageCell) |
| `commaAfterMention` | `implemented_behavior_wired` | BananaHooks.mentionInsertSuffix |
| `disableColoredReplies` | `implemented_behavior_wired` | BananaConfig.Option.DISABLE_COLORED_REPLIES via BananaHooks call sites |
| `hideStickerChecks` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_STICKER_CHECKS via BananaHooks call sites |
| `mentionAvatars` | `implemented_behavior_wired` | BananaConfig.Option.MENTION_AVATARS via BananaHooks call sites |
| `relativeLastSeen` | `implemented_behavior_wired` | BananaHooks.formatUserStatusOverride |
| `disableCustomBackgrounds` | `implemented_behavior_wired` | BananaConfig.Option.DISABLE_CUSTOM_BACKGROUNDS via BananaHooks call sites |
| `channelBottomDiscuss` | `implemented_behavior_wired` | BananaConfig.Option.CHANNEL_BOTTOM_DISCUSS via BananaHooks call sites |
| `hideChannelBottomButton` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_CHANNEL_BOTTOM_BUTTON via BananaHooks call sites |
| `hideShareButton` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_SHARE_BUTTON via BananaHooks call sites |
| `glassBubbles` | `implemented_pending_gpu_acceptance` | Extras switch drives glass chrome/refraction, bubble tint and independent rims on any of four allowed palettes. Device GPU acceptance pending. |
| `glassBubbleTint` | `implemented_pending_gpu_acceptance` | BananaGlassStyle.fillColor; independent of edge strengths. |
| `glassBubbleRimTop` | `implemented_pending_gpu_acceptance` | MessageDrawable directional highlight; independent top/sides/bottom. |
| `glassBubbleRimSides` | `implemented_pending_gpu_acceptance` | MessageDrawable directional highlight; independent top/sides/bottom. |
| `glassBubbleRimBottom` | `implemented_pending_gpu_acceptance` | MessageDrawable directional highlight; independent top/sides/bottom. |
| `showDisappearedGifts` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_DISAPPEARED_GIFTS via BananaHooks call sites |
| `roundVideoStartsWithRear` | `implemented_behavior_wired` | BananaHooks.initialRoundVideoFrontface |
| `roundVideoRemembersCamera` | `implemented_behavior_wired` | BananaHooks.rememberRoundVideoFrontface |
| `chatListHidesConnectionStatus` | `implemented_behavior_wired` | BananaConfig.Option.CHAT_LIST_HIDES_CONNECTION_STATUS via BananaHooks call sites |
| `chatListHidesEmojiStatus` | `implemented_behavior_wired` | BananaConfig.Option.CHAT_LIST_HIDES_EMOJI_STATUS via BananaHooks call sites |
| `chatListCentersTitle` | `implemented_behavior_wired` | BananaConfig.Option.CHAT_LIST_CENTERS_TITLE via BananaHooks call sites |
| `chatListHidesSearch` | `implemented_behavior_wired` | BananaConfig.Option.CHAT_LIST_HIDES_SEARCH via BananaHooks call sites |
| `pollResultsBeforeVoting` | `implemented_behavior_wired` | BananaConfig.Option.POLL_RESULTS_BEFORE_VOTING via BananaHooks call sites |
| `exactLastSeen` | `implemented_behavior_wired` | BananaHooks.formatUserStatusOverride |
| `downloadTikTok` | `implemented_in_app_download_pending_network_acceptance` | BananaVideoDownloadService: yt-dlp/FFmpeg, foreground notification/cancel, bounded HTTPS input/size/time, save to gallery. |
| `downloadYouTubeShorts` | `implemented_in_app_download_pending_network_acceptance` | BananaVideoDownloadService: yt-dlp/FFmpeg, foreground notification/cancel, bounded HTTPS input/size/time, save to gallery. |
| `signDownloadedMedia` | `implemented_behavior_wired` | BananaHooks.maybeSignDownloadedFileName |
| `quoteRepliesToDeleted` | `implemented_behavior_wired` | BananaConfig.Option.QUOTE_REPLIES_TO_DELETED via BananaHooks call sites |
| `exactCounts` | `implemented_behavior_wired` | BananaConfig.Option.EXACT_COUNTS via BananaHooks call sites |
| `confirmVoiceSend` | `implemented_behavior_wired` | BananaConfig.Option.CONFIRM_VOICE_SEND via BananaHooks call sites |
| `hideTypingText` | `implemented_pending_full_integration` | Option.HIDE_TYPING |
| `hideTypingVoice` | `implemented_pending_full_integration` | Option.HIDE_RECORDING |
| `hideTypingFiles` | `implemented_pending_full_integration` | Option.HIDE_UPLOAD |
| `messageShot` | `implemented_behavior_wired` | BananaConfig.Option.MESSAGE_SHOT via BananaHooks call sites |
| `powerSaving` | `implemented_behavior_wired` | BananaConfig.Option.POWER_SAVING via BananaHooks call sites |
| `showsOfflineDot` | `implemented_behavior_wired` | BananaConfig.Option.SHOWS_OFFLINE_DOT via BananaHooks call sites |
| `hidePaidReactions` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_PAID_REACTIONS via BananaHooks call sites |
| `hideViaBot` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_VIA_BOT via BananaHooks call sites |
| `removeLinkPreviews` | `implemented_behavior_wired` | BananaHooks (message preview gate) |
| `hideBirthdayNotifications` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_BIRTHDAY_NOTIFICATIONS via BananaHooks call sites |
| `hideBotAutomation` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_BOT_AUTOMATION via BananaHooks call sites |
| `showPinnedWithBot` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_PINNED_WITH_BOT via BananaHooks call sites |
| `stopAfterVoice` | `implemented_behavior_wired` | BananaConfig.Option.STOP_AFTER_VOICE via BananaHooks call sites |
| `stopAfterRoundVideo` | `implemented_behavior_wired` | BananaConfig.Option.STOP_AFTER_ROUND_VIDEO via BananaHooks call sites |
| `hideGreetingSticker` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_GREETING_STICKER via BananaHooks call sites |
| `hashtagsPreferCurrentChat` | `implemented_behavior_wired` | BananaConfig.Option.HASHTAGS_PREFER_CURRENT_CHAT via BananaHooks call sites |
| `showOriginalForwardDate` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_ORIGINAL_FORWARD_DATE via BananaHooks call sites |
| `sendBannedGifsAsVideo` | `implemented_behavior_wired` | BananaConfig.Option.SEND_BANNED_GIFS_AS_VIDEO via BananaHooks call sites |
| `showSensitiveContent` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_SENSITIVE_CONTENT via BananaHooks call sites |
| `disableStoryCreation` | `implemented_behavior_wired` | BananaConfig.Option.DISABLE_STORY_CREATION via BananaHooks call sites |
| `hideChannelPostReactions` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_CHANNEL_POST_REACTIONS via BananaHooks call sites |
| `defaultSendFormatting` | `implemented_behavior_wired` | BananaHooks.applyDefaultSendFormatting (ChatActivityEnterView) |
| `avatarLongPressMention` | `implemented_behavior_wired` | BananaConfig.Option.AVATAR_LONG_PRESS_MENTION via BananaHooks call sites |
| `fixLinkPreviews` | `implemented_behavior_wired` | BananaHooks (message preview gate) |
| `saveStickerToPhotos` | `implemented_behavior_wired` | BananaConfig.Option.SAVE_STICKER_TO_PHOTOS via BananaHooks call sites |
| `showMediaInfo` | `implemented_behavior_wired` | BananaConfig.Option.SHOW_MEDIA_INFO via BananaHooks call sites |
| `inviteLinksBulkRevoke` | `implemented_behavior_wired` | BananaConfig.Option.INVITE_LINKS_BULK_REVOKE via BananaHooks call sites |
| `hideFolderUnreadCount` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_FOLDER_UNREAD_COUNT via BananaHooks call sites |
| `confirmUnpin` | `implemented_behavior_wired` | BananaConfig.Option.CONFIRM_UNPIN via BananaHooks call sites |
| `hidePrivateChatActivities` | `implemented_behavior_wired` | BananaConfig.Option.HIDE_PRIVATE_CHAT_ACTIVITIES via BananaHooks call sites |
| `stayAtLatestMessageAfterRefresh` | `implemented_behavior_wired` | BananaConfig.Option.STAY_AT_LATEST_MESSAGE_AFTER_REFRESH via BananaHooks call sites |
| `defaultVideoQuality` | `implemented_behavior_wired` | BananaHooks.defaultVideoQuality (VideoPlayer) |

Полный машинный отчёт с местами использования: `feature-attestation.json`.

Повторная проверка: `python3 scripts/audit-extras.py --output docs/feature-attestation.json --markdown docs/feature-attestation.md`.
