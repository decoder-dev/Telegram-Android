package org.telegram.messenger.banana;

import android.util.SparseIntArray;

import org.telegram.ui.ActionBar.Theme;

/**
 * Palettes for BananaGram Cream, Graphite, and Glass.
 * Cream and Graphite follow iOS {@code makeBananaGramTheme}: warm paper by day,
 * warm charcoal by night. Classic White and Classic Black keep the stock assets.
 * Glass is a static frosted palette (cool mist by day, slate by night) plus
 * rounder surfaces. It does not start a blur loop or an animator: panels reuse
 * the draw pass already on screen, and the extra shapes are plain paints.
 */
public final class BananaThemes {
    public static final String CREAM = "Banana Cream";
    public static final String GRAPHITE = "Banana Graphite";
    public static final String GLASS = "Banana Glass";
    public static final String GLASS_NIGHT = "Banana Glass Night";
    public static final String CLASSIC_WHITE = "Classic White";
    public static final String CLASSIC_BLACK = "Classic Black";

    /** Chat-header panels: Glassgram's rounder capsule, otherwise the stock 18dp. */
    public static final int GLASS_PANEL_RADIUS_DP = 26;
    /** Composer island and dialog cards. */
    public static final int GLASS_ISLAND_RADIUS_DP = 26;
    /** Send button capsule, wider than it is tall, as on iOS. */
    public static final float GLASS_SEND_CAPSULE_DP = 46f;

    private BananaThemes() {}

    /** True only while Banana Glass or Banana Glass Night is the active theme. */
    public static boolean glassChrome() {
        Theme.ThemeInfo theme = Theme.getActiveTheme();
        if (theme == null || theme.name == null) {
            return false;
        }
        return GLASS.equals(theme.name) || GLASS_NIGHT.equals(theme.name);
    }

    public static void apply(Theme.ThemeInfo theme, SparseIntArray colors) {
        if (theme == null || colors == null || theme.name == null) {
            return;
        }
        if (CREAM.equals(theme.name)) {
            applyCream(colors);
        } else if (GRAPHITE.equals(theme.name)) {
            applyGraphite(colors);
        } else if (GLASS.equals(theme.name)) {
            applyGlass(colors, false);
        } else if (GLASS_NIGHT.equals(theme.name)) {
            applyGlass(colors, true);
        }
    }

    /**
     * Extras screens read a few keys through their own provider. Unknown keys
     * stay on the active theme, which is already this palette after apply().
     */
    public static int extrasColor(int key, boolean night) {
        if (key == Theme.key_windowBackgroundWhite || key == Theme.key_actionBarDefault) {
            return night ? 0xFF171E28 : 0xFFE7EEF5;
        }
        if (key == Theme.key_windowBackgroundGray) {
            return night ? 0xFF121820 : 0xFFDCE6F0;
        }
        if (key == Theme.key_dialogBackground) {
            return night ? 0xFF1E2733 : 0xFFF7FAFD;
        }
        if (key == Theme.key_windowBackgroundWhiteBlackText || key == Theme.key_profile_title) {
            return night ? 0xFFE8EEF5 : 0xFF1C2430;
        }
        if (key == Theme.key_windowBackgroundWhiteGrayText2 || key == Theme.key_windowBackgroundWhiteGrayText4 ||
                key == Theme.key_windowBackgroundWhiteHintText) {
            return night ? 0xFF9AA6B5 : 0xFF596574;
        }
        if (key == Theme.key_windowBackgroundWhiteBlueHeader || key == Theme.key_chat_messagePanelSend ||
                key == Theme.key_switchTrackChecked || key == Theme.key_checkbox ||
                key == Theme.key_featuredStickers_addButton) {
            return night ? 0xFF8EBBFF : 0xFF1764C0;
        }
        if (key == Theme.key_chat_outBubble) {
            return night ? 0xFF1C3D66 : 0xFFE4F0FF;
        }
        if (key == Theme.key_chat_inBubble) {
            return night ? 0xFF1E2733 : 0xFFFCFDFE;
        }
        return Theme.getColor(key);
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

    private static void applyGlass(SparseIntArray colors, boolean night) {
        final int background;
        final int surface;
        final int card;
        final int raised;
        final int foreground;
        final int secondary;
        final int accent;
        final int onAccent;
        final int separator;
        final int outgoing;
        final int outgoing2;
        final int pinned;
        final int wallpaper2;
        final int wallpaper3;
        final int story2;
        if (night) {
            background = 0xFF121820;
            surface = 0xFF171E28;
            card = 0xFF1E2733;
            raised = 0xFF243140;
            foreground = 0xFFE8EEF5;
            secondary = 0xFF9AA6B5;
            accent = 0xFF8EBBFF;
            onAccent = 0xFF0E141C;
            separator = 0xFF2A3544;
            outgoing = 0xFF1C3D66;
            outgoing2 = 0xFF163454;
            pinned = 0xFF243044;
            wallpaper2 = 0xFF152033;
            wallpaper3 = 0xFF101820;
            story2 = 0xFFB7D4FF;
        } else {
            background = 0xFFDCE6F0;
            surface = 0xFFE7EEF5;
            card = 0xFFF7FAFD;
            raised = 0xFFFCFDFE;
            foreground = 0xFF1C2430;
            secondary = 0xFF596574;
            accent = 0xFF1764C0;
            onAccent = 0xFFFFFFFF;
            separator = 0xFFD5DEE8;
            outgoing = 0xFFE4F0FF;
            outgoing2 = 0xFFD4E7FF;
            pinned = 0xFFF0F6FC;
            wallpaper2 = 0xFFF3F7FB;
            wallpaper3 = 0xFFE4EDF6;
            story2 = 0xFF5BA3FF;
        }
        paint(colors, background, surface, raised, foreground, secondary, accent, onAccent, separator, outgoing, pinned, night);
        put(colors, Theme.key_dialogBackground, card);
        put(colors, Theme.key_actionBarDefaultSubmenuBackground, card);
        put(colors, Theme.key_chat_topPanelBackground, card);
        put(colors, Theme.key_chat_messagePanelBackground, card);
        put(colors, Theme.key_chat_messagePanelText, foreground);
        put(colors, Theme.key_chat_messagePanelHint, secondary);
        put(colors, Theme.key_chat_messagePanelIcons, secondary);
        put(colors, Theme.key_chat_messagePanelSend, accent);
        put(colors, Theme.key_chat_messagePanelVoiceBackground, night ? raised : card);
        put(colors, Theme.key_glass_defaultIcon, secondary);
        put(colors, Theme.key_glass_defaultText, foreground);
        put(colors, Theme.key_glass_targetMainTabs, card);
        put(colors, Theme.key_glass_targetMainTopPanel, card);
        put(colors, Theme.key_glass_tabSelected, accent);
        put(colors, Theme.key_glass_tabSelectedText, onAccent);
        put(colors, Theme.key_glass_tabUnselected, secondary);
        put(colors, Theme.key_actionBarTabLine, accent);
        put(colors, Theme.key_inappPlayerBackground, card);
        put(colors, Theme.key_chat_serviceBackground, night ? 0xCC1E2733 : 0xB31C2430);
        put(colors, Theme.key_chat_serviceText, 0xFFFFFFFF);
        put(colors, Theme.key_chat_inBubbleSelected, night ? 0xFF2A3848 : 0xFFE7F1FA);
        put(colors, Theme.key_chat_outBubbleSelected, night ? 0xFF24507F : 0xFFD0E4FF);
        put(colors, Theme.key_chat_inBubbleShadow, night ? 0x66000000 : 0x140E141C);
        put(colors, Theme.key_chat_outBubbleShadow, night ? 0x66000000 : 0x143D8BFF);
        put(colors, Theme.key_chat_wallpaper_gradient_to2, wallpaper2);
        put(colors, Theme.key_chat_wallpaper_gradient_to3, wallpaper3);
        put(colors, Theme.key_chat_outBubbleGradient2, outgoing2);
        put(colors, Theme.key_stories_circle1, accent);
        put(colors, Theme.key_stories_circle2, story2);
        put(colors, Theme.key_stories_circle_dialog1, accent);
        put(colors, Theme.key_stories_circle_dialog2, story2);
        put(colors, Theme.key_chats_onlineCircle, accent);
        put(colors, Theme.key_profile_title, foreground);
        put(colors, Theme.key_profile_actionIcon, onAccent);
        put(colors, Theme.key_listSelector, night ? 0x228EBBFF : 0x143D8BFF);
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
    }

    private static void put(SparseIntArray colors, int key, int color) {
        colors.put(key, color);
    }
}
