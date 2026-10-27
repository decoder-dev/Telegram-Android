package org.telegram.messenger.banana;

import android.util.SparseIntArray;

import org.telegram.ui.ActionBar.Theme;

/**
 * Palettes for BananaGram Cream and BananaGram Graphite.
 * Values follow iOS {@code makeBananaGramTheme} in MakePresentationTheme.swift
 * (decoder-dev/Telegram-iOS): warm paper by day, warm charcoal by night.
 * Classic White and Classic Black keep the stock Day and Night assets.
 * Scheduled auto-night stays off: {@link Theme#selectedAutoNightType} still defaults
 * to follow-system on API 29+ and to none otherwise ({@code Theme} around the
 * {@code selectedAutoNightType} preference read). Time-of-day switching is not enabled.
 */
public final class BananaThemes {
    public static final String CREAM = "Banana Cream";
    public static final String GRAPHITE = "Banana Graphite";
    public static final String CLASSIC_WHITE = "Classic White";
    public static final String CLASSIC_BLACK = "Classic Black";

    private BananaThemes() {}

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
    }

    private static void put(SparseIntArray colors, int key, int color) {
        colors.put(key, color);
    }
}
