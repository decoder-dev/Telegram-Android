package org.telegram.messenger.banana;

import android.util.SparseIntArray;

import org.telegram.ui.ActionBar.Theme;

/** The four iOS palettes; glass chrome is controlled independently in Extras. */
public final class BananaThemes {
    public static final String CREAM = "Banana Cream";
    public static final String GRAPHITE = "Banana Graphite";
    public static final String CLASSIC_WHITE = "Classic White";
    public static final String CLASSIC_BLACK = "Classic Black";

    /** Chat-header panels: Glassgram's rounder capsule, otherwise the stock 18dp. */
    public static final int GLASS_PANEL_RADIUS_DP = 26;
    /** Composer island and dialog cards. */
    public static final int GLASS_ISLAND_RADIUS_DP = 26;
    /** Send button capsule, wider than it is tall, as on iOS. */
    public static final float GLASS_SEND_CAPSULE_DP = 46f;

    private BananaThemes() {}

    /** Glass geometry follows the account switch, independently of the palette. */
    public static boolean glassChrome() {
        return BananaHooks.glassBubbles(org.telegram.messenger.UserConfig.selectedAccount);
    }

    public static void apply(Theme.ThemeInfo theme, SparseIntArray colors) {
        if (theme == null || colors == null || theme.name == null) {
            return;
        }
        if (CREAM.equals(theme.name)) {
            applyCream(colors);
        } else if (GRAPHITE.equals(theme.name)) {
            applyGraphite(colors);
        }
    }

    private static void applyCream(SparseIntArray colors) {
        final int background = 0xFFF6F0E3;
        final int surface = 0xFFFFFAF2;
        final int raised = 0xFFFFFDF8;
        final int foreground = 0xFF3A3430;
        final int secondary = 0xFF6A655D;
        final int accent = 0xFF8A5F1E;
        final int onAccent = 0xFFFFFFFF;
        final int separator = 0xFFE6DDCC;
        final int outgoing = 0xFFFCF2DC;
        final int pinned = 0xFFFBF6EA;
        paint(colors, background, surface, raised, foreground, secondary, accent, onAccent, separator, outgoing, pinned, false);
    }

    private static void applyGraphite(SparseIntArray colors) {
        final int background = 0xFF1D1C1A;
        final int surface = 0xFF272522;
        final int raised = 0xFF312F2B;
        final int foreground = 0xFFEFEBE0;
        final int secondary = 0xFFB5B0A4;
        final int accent = 0xFFEBD58F;
        final int onAccent = 0xFF1B1A16;
        final int separator = 0xFF3A3733;
        final int outgoing = 0xFF5E5236;
        final int pinned = 0xFF2B2926;
        paint(colors, background, surface, raised, foreground, secondary, accent, onAccent, separator, outgoing, pinned, true);
    }

    private static void paint(SparseIntArray colors, int background, int surface, int raised, int foreground, int secondary, int accent, int onAccent, int separator, int outgoing, int pinned, boolean dark) {
        put(colors, Theme.key_windowBackgroundWhite, surface);
        put(colors, Theme.key_windowBackgroundGray, background);
        put(colors, Theme.key_windowBackgroundWhiteBlackText, foreground);
        put(colors, Theme.key_windowBackgroundWhiteGrayText, secondary);
        put(colors, Theme.key_windowBackgroundWhiteGrayText2, secondary);
        put(colors, Theme.key_windowBackgroundWhiteBlueText, accent);
        put(colors, Theme.key_windowBackgroundWhiteBlueText2, accent);
        put(colors, Theme.key_windowBackgroundWhiteHintText, secondary);
        put(colors, Theme.key_divider, separator);
        put(colors, Theme.key_listSelector, dark ? 0xFF36332E : 0xFFF1E8D6);
        put(colors, Theme.key_switchTrackChecked, accent);
        put(colors, Theme.key_actionBarDefault, surface);
        put(colors, Theme.key_actionBarDefaultIcon, accent);
        put(colors, Theme.key_actionBarDefaultTitle, foreground);
        put(colors, Theme.key_actionBarDefaultSubtitle, secondary);
        put(colors, Theme.key_actionBarDefaultSelector, dark ? 0xFF36332E : 0xFFF1E8D6);
        put(colors, Theme.key_chats_menuBackground, surface);
        put(colors, Theme.key_chats_name, foreground);
        put(colors, Theme.key_chats_nameMessage, foreground);
        put(colors, Theme.key_chats_nameMessage_threeLines, foreground);
        put(colors, Theme.key_chats_message, secondary);
        put(colors, Theme.key_chats_date, secondary);
        put(colors, Theme.key_chats_unreadCounter, accent);
        put(colors, Theme.key_chats_unreadCounterText, onAccent);
        put(colors, Theme.key_dialogBackground, surface);
        put(colors, Theme.key_dialogTextBlack, foreground);
        put(colors, Theme.key_chat_inBubble, raised);
        put(colors, Theme.key_chat_outBubble, outgoing);
        put(colors, Theme.key_chat_outBubbleGradient1, outgoing);
        put(colors, Theme.key_chat_outBubbleGradient2, dark ? 0xFF4A4029 : 0xFFF8EBCB);
        put(colors, Theme.key_chat_outBubbleGradient3, outgoing);
        put(colors, Theme.key_chat_outBubbleGradientAnimated, 0);
        put(colors, Theme.key_chat_messageTextIn, foreground);
        put(colors, Theme.key_chat_messageTextOut, dark ? foreground : 0xFF3A3430);
        put(colors, Theme.key_chat_inTimeText, secondary);
        put(colors, Theme.key_chat_outTimeText, dark ? secondary : 0xFF85591A);
        put(colors, Theme.key_chat_wallpaper, background);
        put(colors, Theme.key_chat_wallpaper_gradient_to1, dark ? 0xFF34312B : 0xFFF4E7C9);
        put(colors, Theme.key_chat_wallpaper_gradient_to2, dark ? 0xFF2A2824 : 0xFFFCF6E8);
        put(colors, Theme.key_chat_wallpaper_gradient_to3, dark ? 0xFF1D1C1A : 0xFFF0E2C2);
        put(colors, Theme.key_avatar_backgroundSaved, accent);
        put(colors, Theme.key_featuredStickers_addButton, accent);
        put(colors, Theme.key_profile_actionBackground, accent);
        put(colors, Theme.key_player_actionBarTitle, foreground);
        colors.put(Theme.key_chats_pinnedOverlay, pinned);

        // Complete the stock Day/Night surfaces. Otherwise the composer, sheets and
        // selection controls keep cold blue/grey colors while Extras uses warm colors.
        final int highlighted = dark ? 0xFF36332E : 0xFFF1E8D6;
        final int search = dark ? 0xFF302E2A : 0xFFF0E9DB;
        final int disabled = dark ? 0xFF77726A : 0xFFA39C8E;
        final int outgoingSecondary = dark ? 0xFFD5CBBA : 0xFF85591A;
        final int outgoingAccent = dark ? accent : 0xFF85591A;
        final int selectionOverlay = (accent & 0x00FFFFFF) | 0x18000000;
        put(colors, Theme.key_chat_inBubbleSelectedOverlay, selectionOverlay);
        put(colors, Theme.key_chat_outBubbleSelectedOverlay, selectionOverlay);
        put(colors, Theme.key_chat_outBubbleGradientSelectedOverlay, selectionOverlay);
        put(colors, Theme.key_chat_inBubbleSelected, Theme.blendOver(raised, selectionOverlay));
        put(colors, Theme.key_chat_outBubbleSelected, Theme.blendOver(outgoing, selectionOverlay));

        putAll(colors, surface,
                Theme.key_actionBarDefaultArchived, Theme.key_actionBarDefaultSubmenuBackground, Theme.key_actionBarActionModeDefault,
                Theme.key_chat_messagePanelBackground, Theme.key_chat_emojiPanelBackground, Theme.key_chat_topPanelBackground,
                Theme.key_chat_goDownButton, Theme.key_chats_menuTopBackground);
        putAll(colors, background,
                Theme.key_dialogBackgroundGray, Theme.key_graySection);
        putAll(colors, highlighted,
                Theme.key_settings_listSelector, Theme.key_actionBarDefaultArchivedSelector, Theme.key_actionBarActionModeDefaultSelector,
                Theme.key_chat_botKeyboardButtonBackgroundPressed);
        putAll(colors, search,
                Theme.key_dialogSearchBackground, Theme.key_chat_emojiSearchBackground);
        putAll(colors, foreground,
                Theme.key_actionBarDefaultArchivedTitle, Theme.key_actionBarDefaultSearch, Theme.key_actionBarDefaultSubmenuItem,
                Theme.key_actionBarActionModeDefaultIcon, Theme.key_dialogSearchText, Theme.key_chat_messagePanelText,
                Theme.key_chat_topPanelMessage, Theme.key_chat_emojiPanelTrendingTitle, Theme.key_chat_botKeyboardButtonText,
                Theme.key_player_actionBarTitle, Theme.key_chats_menuItemText);
        putAll(colors, secondary,
                Theme.key_windowBackgroundWhiteGrayText3, Theme.key_windowBackgroundWhiteGrayText4, Theme.key_windowBackgroundWhiteGrayText5,
                Theme.key_windowBackgroundWhiteGrayText6, Theme.key_windowBackgroundWhiteGrayText7, Theme.key_windowBackgroundWhiteGrayText8,
                Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteValueText, Theme.key_dialogTextGray,
                Theme.key_dialogTextGray2, Theme.key_dialogTextGray3, Theme.key_dialogTextGray4,
                Theme.key_dialogTextHint, Theme.key_dialogIcon, Theme.key_dialogSearchHint,
                Theme.key_dialogSearchIcon, Theme.key_emptyListPlaceholder, Theme.key_graySectionText,
                Theme.key_actionBarDefaultSearchPlaceholder, Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_chats_menuItemIcon,
                Theme.key_chats_message_threeLines, Theme.key_chats_date, Theme.key_chats_muteIcon,
                Theme.key_chats_pinnedIcon, Theme.key_chat_messagePanelHint, Theme.key_chat_messagePanelIcons,
                Theme.key_chat_messagePanelCancelInlineBot, Theme.key_chat_replyPanelClose, Theme.key_chat_topPanelClose,
                Theme.key_chat_emojiSearchIcon, Theme.key_chat_emojiPanelEmptyText, Theme.key_chat_emojiPanelIcon,
                Theme.key_chat_emojiBottomPanelIcon, Theme.key_chat_emojiPanelBackspace, Theme.key_chat_emojiPanelStickerSetName,
                Theme.key_chat_emojiPanelStickerSetNameIcon, Theme.key_chat_emojiPanelTrendingDescription,
                Theme.key_player_actionBarSubtitle, Theme.key_player_time, Theme.key_player_button);
        putAll(colors, accent,
                Theme.key_windowBackgroundWhiteBlueText3, Theme.key_windowBackgroundWhiteBlueText4, Theme.key_windowBackgroundWhiteBlueText5,
                Theme.key_windowBackgroundWhiteBlueText6, Theme.key_windowBackgroundWhiteBlueText7, Theme.key_windowBackgroundWhiteBlueIcon,
                Theme.key_windowBackgroundWhiteBlueHeader, Theme.key_windowBackgroundWhiteLinkText, Theme.key_windowBackgroundWhiteInputFieldActivated,
                Theme.key_windowBackgroundChecked, Theme.key_progressCircle, Theme.key_dialogTextBlue,
                Theme.key_dialogTextBlue2, Theme.key_dialogTextBlue4, Theme.key_dialogTextLink,
                Theme.key_dialogButton, Theme.key_dialogInputFieldActivated, Theme.key_dialogRadioBackgroundChecked,
                Theme.key_dialogCheckboxSquareBackground, Theme.key_dialogRoundCheckBox, Theme.key_dialogLineProgress,
                Theme.key_dialogFloatingButton, Theme.key_switchTrackBlueChecked, Theme.key_switch2TrackChecked,
                Theme.key_checkbox, Theme.key_checkboxSquareBackground, Theme.key_windowBackgroundWhiteBlueButton,
                Theme.key_radioBackgroundChecked, Theme.key_actionBarDefaultArchivedIcon, Theme.key_actionBarTabActiveText,
                Theme.key_actionBarTabLine, Theme.key_chats_actionBackground, Theme.key_featuredStickers_addButton2,
                Theme.key_chats_sentCheck, Theme.key_chats_sentReadCheck, Theme.key_chats_sentClock,
                Theme.key_avatar_background2Saved, Theme.key_chat_messagePanelSend, Theme.key_chat_messagePanelCursor,
                Theme.key_chat_replyPanelName, Theme.key_chat_replyPanelIcons, Theme.key_chat_topPanelTitle,
                Theme.key_chat_topPanelLine, Theme.key_chat_emojiPanelIconSelected, Theme.key_chat_emojiPanelStickerSetNameHighlight,
                Theme.key_chat_emojiPanelStickerPackSelectorLine, Theme.key_chat_goDownButtonCounterBackground, Theme.key_player_progress,
                Theme.key_player_buttonActive);
        putAll(colors, onAccent,
                Theme.key_windowBackgroundCheckText, Theme.key_chat_messagePanelVoicePressed, Theme.key_dialogCheckboxSquareCheck,
                Theme.key_dialogRoundCheckBoxCheck, Theme.key_dialogFloatingIcon, Theme.key_checkboxCheck,
                Theme.key_checkboxSquareCheck, Theme.key_switchTrackBlueThumbChecked, Theme.key_chats_actionIcon,
                Theme.key_chat_goDownButtonCounter, Theme.key_profile_actionIcon, Theme.key_featuredStickers_buttonText);
        putAll(colors, separator,
                Theme.key_windowBackgroundWhiteInputField, Theme.key_dialogInputField, Theme.key_dialogGrayLine,
                Theme.key_dialogLineProgressBackground, Theme.key_checkboxSquareUnchecked, Theme.key_radioBackground,
                Theme.key_dialogRadioBackground, Theme.key_dialogCheckboxSquareUnchecked, Theme.key_chat_emojiPanelShadowLine);
        putAll(colors, disabled,
                Theme.key_switchTrack, Theme.key_switchTrackBlue, Theme.key_switch2Track,
                Theme.key_checkboxDisabled, Theme.key_checkboxSquareDisabled, Theme.key_dialogCheckboxSquareDisabled);
        putAll(colors, raised,
                Theme.key_chat_botKeyboardButtonBackground, Theme.key_chat_recordedVoiceBackground);
        putAll(colors, outgoingSecondary,
                Theme.key_chat_outTimeText, Theme.key_chat_outTimeSelectedText, Theme.key_chat_outViews,
                Theme.key_chat_outViewsSelected, Theme.key_chat_outAudioPerformerText, Theme.key_chat_outAudioPerformerSelectedText,
                Theme.key_chat_outAudioDurationText, Theme.key_chat_outAudioDurationSelectedText);
        putAll(colors, outgoingAccent,
                Theme.key_chat_outSentCheck, Theme.key_chat_outSentCheckRead, Theme.key_chat_outSentCheckSelected,
                Theme.key_chat_outSentCheckReadSelected, Theme.key_chat_outReplyLine, Theme.key_chat_outReplyNameText,
                Theme.key_chat_outViaBotNameText, Theme.key_chat_outAudioTitleText, Theme.key_chat_outAudioProgress,
                Theme.key_chat_outAudioSelectedProgress, Theme.key_chat_outVoiceSeekbarFill, Theme.key_chat_outPreviewLine,
                Theme.key_chat_outSiteNameText, Theme.key_chat_messageLinkOut);
        putAll(colors, accent,
                Theme.key_chat_inReplyLine, Theme.key_chat_inReplyNameText, Theme.key_chat_inViaBotNameText,
                Theme.key_chat_inAudioTitleText, Theme.key_chat_inAudioProgress, Theme.key_chat_inAudioSelectedProgress,
                Theme.key_chat_inVoiceSeekbarFill, Theme.key_chat_inPreviewLine, Theme.key_chat_inSiteNameText,
                Theme.key_chat_messageLinkIn);
    }

    private static void putAll(SparseIntArray colors, int color, int... keys) {
        for (int key : keys) colors.put(key, color);
    }

    private static void put(SparseIntArray colors, int key, int color) {
        colors.put(key, color);
    }
}
