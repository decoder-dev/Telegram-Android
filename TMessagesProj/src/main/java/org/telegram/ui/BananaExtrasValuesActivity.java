package org.telegram.ui;

import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaConfig.IntValue;
import org.telegram.messenger.banana.BananaConfig.StringValue;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Cells.BananaValueCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BananaSettingsFragment;

import java.util.ArrayList;

/** Non-boolean BananaGram Extras (ints, marks, backends). */
public final class BananaExtrasValuesActivity extends BananaSettingsFragment {
    private static final int ID_ACCENT = 1;
    private static final int ID_STICKER_SIZE = 2;
    private static final int ID_PHOTO_QUALITY = 3;
    private static final int ID_SEEK = 4;
    private static final int ID_DOUBLE_TAP = 5;
    private static final int ID_STICKER_SHAPE = 6;
    private static final int ID_GLASS_TINT = 7;
    private static final int ID_GLASS_TOP = 8;
    private static final int ID_GLASS_SIDES = 9;
    private static final int ID_GLASS_BOTTOM = 10;
    private static final int ID_SEND_FORMAT = 11;
    private static final int ID_VIDEO_QUALITY = 12;
    private static final int ID_TRANSLATION = 13;
    private static final int ID_TRANSCRIPTION = 14;
    private static final int ID_DELETED_MARK = 15;
    private static final int ID_EDITED_MARK = 16;
    private static final int ID_VISUAL_PHONE = 17;
    private static final int ID_REFRESH_RATE = 18;
    private static final int ID_GLASS_GLARE = 19;
    private static final int ID_GLASS_PREVIEW = 23;
    private static final int ID_GLASS_COMPOSER = 24;
    private static final int ID_GLASS_CENTERED_HEADER = 25;
    private static final int ID_GLASS_TAB_SEARCH = 26;
    private static final int ID_GLASS_TAB_STROKE = 27;
    private static final int ID_GLASS_PROFILE_PILLS = 28;
    private static final int ID_GLASS_BACK_UNREAD_PILL = 29;
    private static final int ID_GLASS_PROFILE_TINT = 30;
    private static final int ID_GLASS_ACTIONBAR_CROSSFADE = 31;
    private org.telegram.ui.Components.BananaGlassPreviewView glassPreview;
    private static final int ID_TEXT_RULES = 20;
    private static final int ID_DEEPL_KEY = 21, ID_YANDEX_KEY = 22;
    private static final int ID_EQ_FIRST = 30; // 30..34, one per band
    private static final int ID_VOICE_PRESET = 40, ID_VOICE_PITCH = 41, ID_VOICE_REVERB = 42, ID_VOICE_ECHO = 43;
    private static final int[] EQ_TITLES = {R.string.BananaVal_Eq60, R.string.BananaVal_Eq230, R.string.BananaVal_Eq910, R.string.BananaVal_Eq3600, R.string.BananaVal_Eq14000};
    private static final int[] VOICE_PRESETS = {R.string.BananaVoicePresetAnonymous, R.string.BananaVoicePresetFemale, R.string.BananaVoicePresetMale,
            R.string.BananaVoicePresetChild, R.string.BananaVoicePresetRobot, R.string.BananaVoicePresetCustom};

    private static String decibels(int value) {
        return value == 0 ? "0 " + LocaleController.getString(R.string.BananaVal_Db)
                : (value > 0 ? "+" : "\u2212") + Math.abs(value) + " " + LocaleController.getString(R.string.BananaVal_Db);
    }

    private static String semitones(int value) {
        return value == 0 ? LocaleController.getString(R.string.BananaVal_PitchUnchanged)
                : (value > 0 ? "+" : "\u2212") + Math.abs(value) + " " + LocaleController.getString(R.string.BananaVal_Semitones);
    }

    private static String amount(int value) {
        return value == 0 ? LocaleController.getString(R.string.BananaVal_Off) : value + "%";
    }

    private void pickLabeled(int title, IntValue value, int[] codes, java.util.function.IntFunction<String> label) {
        CharSequence[] labels = new CharSequence[codes.length];
        for (int i = 0; i < codes.length; i++) labels[i] = label.apply(codes[i]);
        pickFrom(title, value, labels, codes);
    }

    public BananaExtrasValuesActivity(int account) {
        setCurrentAccount(account);
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return BananaTheme.forExtras();
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString(R.string.BananaValues);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesAppearance)));
        float refresh = org.telegram.messenger.MessagesController.getGlobalMainSettings().getFloat(
                org.telegram.messenger.banana.RefreshRatePolicy.KEY, -1f);
        items.add(cell(ID_REFRESH_RATE, R.string.BananaRefreshRate, refreshRateLabel(refresh)));
        items.add(cell(ID_ACCENT, R.string.BananaVal_AccentSaturation, percent(BananaConfig.accentColorSaturation(currentAccount))));
        items.add(cell(ID_STICKER_SIZE, R.string.BananaVal_StickerSize, percent(BananaConfig.stickerSizePercent(currentAccount))));
        items.add(cell(ID_STICKER_SHAPE, R.string.BananaVal_StickerShape, stickerShapeLabel(BananaConfig.stickerShape(currentAccount))));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesGlass)));
        if (glassPreview == null && getContext() != null) {
            // The chat's own theme, not the Extras palette: this is how the glass will look in chats.
            glassPreview = new org.telegram.ui.Components.BananaGlassPreviewView(getContext(), this, currentAccount, null);
        }
        if (glassPreview != null) {
            UItem preview = UItem.asCustom(ID_GLASS_PREVIEW, glassPreview);
            preview.intValue = AndroidUtilities.dp(org.telegram.ui.Components.BananaGlassPreviewView.HEIGHT_DP);
            items.add(preview);
        }
        items.add(cell(ID_GLASS_GLARE, R.string.BananaGlassHighlights, glassGlareLabel(BananaConfig.glassGlare())));
        items.add(BananaSwitchCell.Factory.of(ID_GLASS_COMPOSER, LocaleController.getString(R.string.BananaGlassComposer), BananaConfig.glassComposer()));
        items.add(BananaSwitchCell.Factory.of(ID_GLASS_CENTERED_HEADER, LocaleController.getString(R.string.BananaGlassCenteredHeader), BananaConfig.glassCenteredHeader()));
        if (BananaConfig.glassCenteredHeader()) {
            items.add(BananaSwitchCell.Factory.of(ID_GLASS_BACK_UNREAD_PILL, LocaleController.getString(R.string.BananaGlassBackUnreadPill), BananaConfig.glassBackUnreadPill()));
        }
        items.add(BananaSwitchCell.Factory.of(ID_GLASS_TAB_SEARCH, LocaleController.getString(R.string.BananaGlassTabSearch), BananaConfig.glassTabSearch()));
        items.add(BananaSwitchCell.Factory.of(ID_GLASS_TAB_STROKE, LocaleController.getString(R.string.BananaGlassTabStroke), BananaConfig.glassTabStroke()));
        items.add(BananaSwitchCell.Factory.of(ID_GLASS_PROFILE_PILLS, LocaleController.getString(R.string.BananaGlassProfilePills), BananaConfig.glassProfilePills()));
        items.add(BananaSwitchCell.Factory.of(ID_GLASS_PROFILE_TINT, LocaleController.getString(R.string.BananaGlassProfileTint), BananaConfig.glassProfileTint()));
        items.add(BananaSwitchCell.Factory.of(ID_GLASS_ACTIONBAR_CROSSFADE, LocaleController.getString(R.string.BananaGlassActionBarCrossfade), BananaConfig.glassActionBarCrossfade()));
        items.add(cell(ID_GLASS_TINT, R.string.BananaVal_GlassTint, percent(BananaConfig.glassBubbleTint(currentAccount))));
        items.add(cell(ID_GLASS_TOP, R.string.BananaVal_GlassRimTop, percent(BananaConfig.glassBubbleRimTop(currentAccount))));
        items.add(cell(ID_GLASS_SIDES, R.string.BananaVal_GlassRimSides, percent(BananaConfig.glassBubbleRimSides(currentAccount))));
        items.add(cell(ID_GLASS_BOTTOM, R.string.BananaVal_GlassRimBottom, percent(BananaConfig.glassBubbleRimBottom(currentAccount))));
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaValuesGlassInfo)));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesMedia)));
        items.add(cell(ID_PHOTO_QUALITY, R.string.BananaVal_OutgoingPhotoQuality, photoQualityLabel(BananaConfig.outgoingPhotoQuality(currentAccount))));
        items.add(cell(ID_SEEK, R.string.BananaVal_DoubleTapSeek, seekLabel(BananaConfig.doubleTapSeekSeconds(currentAccount))));
        items.add(cell(ID_VIDEO_QUALITY, R.string.BananaVal_DefaultVideoQuality, videoQualityLabel(BananaConfig.defaultVideoQuality(currentAccount))));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesMessages)));
        items.add(cell(ID_DOUBLE_TAP, R.string.BananaVal_DoubleTapAction, doubleTapLabel(BananaConfig.doubleTapAction(currentAccount))));
        items.add(cell(ID_SEND_FORMAT, R.string.BananaVal_DefaultSendFormatting, sendFormatLabel(BananaConfig.defaultSendFormatting(currentAccount))));
        items.add(cell(ID_DELETED_MARK, R.string.BananaVal_DeletedMessageMark, markDisplay(BananaConfig.deletedMessageMark(currentAccount))));
        items.add(cell(ID_EDITED_MARK, R.string.BananaVal_EditedMessageMark, markDisplay(BananaConfig.editedMessageMark(currentAccount))));
        items.add(cell(ID_TEXT_RULES, R.string.BananaVal_TextReplacementRules, LocaleController.formatString(R.string.BananaVal_RulesCount,
                org.telegram.messenger.banana.OutgoingTextRules.parseRules(BananaConfig.getString(currentAccount, StringValue.TEXT_REPLACEMENT_RULES)).size())));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesBackends)));
        items.add(cell(ID_TRANSLATION, R.string.BananaVal_TranslationBackend, translationLabel(BananaConfig.translationBackend(currentAccount))));
        int translator = BananaConfig.translationBackend(currentAccount);
        boolean fallback = BananaConfig.get(currentAccount, BananaConfig.Option.TRANSLATION_FALLBACK);
        if (translator == 2 || fallback) {
            items.add(cell(ID_DEEPL_KEY, R.string.BananaVal_DeeplKey, keyDisplay(BananaConfig.getString(currentAccount, StringValue.DEEPL_API_KEY))));
        }
        if (translator == 3 || fallback) {
            items.add(cell(ID_YANDEX_KEY, R.string.BananaVal_YandexKey, keyDisplay(BananaConfig.getString(currentAccount, StringValue.YANDEX_API_KEY))));
        }
        items.add(cell(ID_TRANSCRIPTION, R.string.BananaVal_TranscriptionBackend, transcriptionLabel(BananaConfig.transcriptionBackend(currentAccount))));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesProfile)));
        items.add(cell(ID_VISUAL_PHONE, R.string.BananaVal_VisualPhoneNumber, markDisplay(BananaConfig.visualPhoneNumber(currentAccount))));
        items.add(UItem.asShadow(null));

        if (BananaConfig.get(currentAccount, BananaConfig.Option.EQUALIZER_ENABLED)) {
            items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesEqualizer)));
            for (int band = 0; band < EQ_TITLES.length; band++) {
                items.add(cell(ID_EQ_FIRST + band, EQ_TITLES[band], decibels(BananaConfig.getInt(currentAccount, org.telegram.messenger.banana.BananaEqualizer.band(band)))));
            }
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaValuesEqualizerInfo)));
        }
        if (BananaConfig.get(currentAccount, BananaConfig.Option.VOICE_CHANGER_ENABLED)) {
            int preset = BananaConfig.getInt(currentAccount, IntValue.VOICE_CHANGER_PRESET);
            items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesVoiceChanger)));
            items.add(cell(ID_VOICE_PRESET, R.string.BananaVal_VoicePreset, LocaleController.getString(VOICE_PRESETS[preset])));
            if (preset == org.telegram.messenger.banana.VoicePresets.CUSTOM) {
                items.add(cell(ID_VOICE_PITCH, R.string.BananaVal_VoicePitch, semitones(BananaConfig.getInt(currentAccount, IntValue.VOICE_CHANGER_PITCH))));
                items.add(cell(ID_VOICE_REVERB, R.string.BananaVal_VoiceReverb, amount(BananaConfig.getInt(currentAccount, IntValue.VOICE_CHANGER_REVERB))));
                items.add(cell(ID_VOICE_ECHO, R.string.BananaVal_VoiceEcho, amount(BananaConfig.getInt(currentAccount, IntValue.VOICE_CHANGER_ECHO))));
            }
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaValuesVoiceChangerInfo)));
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaValuesInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_REFRESH_RATE:
                pickRefreshRate();
                break;
            case ID_GLASS_GLARE:
                pickGlassGlare();
                break;
            case ID_GLASS_COMPOSER:
                toggleGlassComposer();
                break;
            case ID_GLASS_CENTERED_HEADER:
                toggleGlassCenteredHeader();
                break;
            case ID_GLASS_TAB_SEARCH:
                toggleGlassChromeOption(() -> BananaConfig.setGlassTabSearch(!BananaConfig.glassTabSearch()), false);
                break;
            case ID_GLASS_TAB_STROKE:
                toggleGlassChromeOption(() -> BananaConfig.setGlassTabStroke(!BananaConfig.glassTabStroke()), false);
                break;
            case ID_GLASS_PROFILE_PILLS:
                toggleGlassChromeOption(() -> BananaConfig.setGlassProfilePills(!BananaConfig.glassProfilePills()), false);
                break;
            case ID_GLASS_BACK_UNREAD_PILL:
                toggleGlassChromeOption(() -> BananaConfig.setGlassBackUnreadPill(!BananaConfig.glassBackUnreadPill()), true);
                break;
            case ID_GLASS_PROFILE_TINT:
                toggleGlassChromeOption(() -> BananaConfig.setGlassProfileTint(!BananaConfig.glassProfileTint()), false);
                break;
            case ID_GLASS_ACTIONBAR_CROSSFADE:
                toggleGlassChromeOption(() -> BananaConfig.setGlassActionBarCrossfade(!BananaConfig.glassActionBarCrossfade()), false);
                break;
            case ID_ACCENT:
                pickPercent(R.string.BananaVal_AccentSaturation, IntValue.ACCENT_COLOR_SATURATION, 0, 100, 5);
                break;
            case ID_STICKER_SIZE:
                pickPercent(R.string.BananaVal_StickerSize, IntValue.STICKER_SIZE_PERCENT, 50, 150, 10);
                break;
            case ID_GLASS_TINT:
                pickPercent(R.string.BananaVal_GlassTint, IntValue.GLASS_BUBBLE_TINT, 0, 100, 5);
                break;
            case ID_GLASS_TOP:
                pickPercent(R.string.BananaVal_GlassRimTop, IntValue.GLASS_BUBBLE_RIM_TOP, 0, 100, 5);
                break;
            case ID_GLASS_SIDES:
                pickPercent(R.string.BananaVal_GlassRimSides, IntValue.GLASS_BUBBLE_RIM_SIDES, 0, 100, 5);
                break;
            case ID_GLASS_BOTTOM:
                pickPercent(R.string.BananaVal_GlassRimBottom, IntValue.GLASS_BUBBLE_RIM_BOTTOM, 0, 100, 5);
                break;
            case ID_PHOTO_QUALITY:
                pickFrom(R.string.BananaVal_OutgoingPhotoQuality, IntValue.OUTGOING_PHOTO_QUALITY,
                        new CharSequence[]{
                                LocaleController.getString(R.string.BananaVal_PhotoQualityDefault),
                                LocaleController.getString(R.string.BananaVal_PhotoQualityBetter),
                                LocaleController.getString(R.string.BananaVal_PhotoQualityMax)
                        }, new int[]{0, 1, 2});
                break;
            case ID_SEEK:
                pickFrom(R.string.BananaVal_DoubleTapSeek, IntValue.DOUBLE_TAP_SEEK_SECONDS,
                        new CharSequence[]{seekLabel(0), seekLabel(5), seekLabel(10), seekLabel(15), seekLabel(30)},
                        new int[]{0, 5, 10, 15, 30});
                break;
            case ID_DOUBLE_TAP:
                pickFrom(R.string.BananaVal_DoubleTapAction, IntValue.DOUBLE_TAP_ACTION,
                        new CharSequence[]{
                                LocaleController.getString(R.string.BananaVal_TapOff),
                                LocaleController.getString(R.string.BananaVal_TapReact),
                                LocaleController.getString(R.string.BananaVal_TapReply),
                                LocaleController.getString(R.string.BananaVal_TapRepeat),
                                LocaleController.getString(R.string.BananaVal_TapTranslate),
                                LocaleController.getString(R.string.BananaVal_TapEdit)
                        }, new int[]{0, 1, 2, 3, 4, 6});
                break;
            case ID_STICKER_SHAPE:
                pickFrom(R.string.BananaVal_StickerShape, IntValue.STICKER_SHAPE,
                        new CharSequence[]{
                                LocaleController.getString(R.string.BananaVal_ShapeStock),
                                LocaleController.getString(R.string.BananaVal_ShapeRounded),
                                LocaleController.getString(R.string.BananaVal_ShapeMoreRounded)
                        }, new int[]{0, 1, 2});
                break;
            case ID_SEND_FORMAT:
                pickFrom(R.string.BananaVal_DefaultSendFormatting, IntValue.DEFAULT_SEND_FORMATTING,
                        new CharSequence[]{
                                LocaleController.getString(R.string.BananaVal_FormatOff),
                                LocaleController.getString(R.string.BananaVal_FormatBold),
                                LocaleController.getString(R.string.BananaVal_FormatItalic),
                                LocaleController.getString(R.string.BananaVal_FormatUnderline),
                                LocaleController.getString(R.string.BananaVal_FormatStrike),
                                LocaleController.getString(R.string.BananaVal_FormatSpoiler)
                        }, new int[]{0, 1, 2, 3, 4, 5});
                break;
            case ID_VIDEO_QUALITY:
                pickFrom(R.string.BananaVal_DefaultVideoQuality, IntValue.DEFAULT_VIDEO_QUALITY,
                        new CharSequence[]{
                                LocaleController.getString(R.string.BananaVal_VideoAuto),
                                LocaleController.getString(R.string.BananaVal_VideoHighest),
                                LocaleController.getString(R.string.BananaVal_VideoLowest)
                        }, new int[]{0, 1, 2});
                break;
            case ID_TRANSLATION:
                pickFrom(R.string.BananaVal_TranslationBackend, IntValue.TRANSLATION_BACKEND,
                        new CharSequence[]{
                                LocaleController.getString(R.string.BananaVal_BackendDefault),
                                LocaleController.getString(R.string.BananaVal_BackendGoogle),
                                "DeepL",
                                LocaleController.getString(R.string.BananaVal_BackendYandex)
                        }, new int[]{0, 1, 2, 3});
                break;
            case ID_TRANSCRIPTION:
                pickFrom(R.string.BananaVal_TranscriptionBackend, IntValue.TRANSCRIPTION_BACKEND,
                        new CharSequence[]{LocaleController.getString(R.string.BananaVal_BackendDefault),
                                LocaleController.getString(R.string.BananaVoiceRussian), LocaleController.getString(R.string.BananaVoiceEnglish)}, new int[]{0, 1, 2});
                break;

            case ID_DELETED_MARK:
                editString(R.string.BananaVal_DeletedMessageMark, StringValue.DELETED_MESSAGE_MARK, "🧹");
                break;
            case ID_EDITED_MARK:
                editString(R.string.BananaVal_EditedMessageMark, StringValue.EDITED_MESSAGE_MARK, "");
                break;
            case ID_VISUAL_PHONE:
                editString(R.string.BananaVal_VisualPhoneNumber, StringValue.VISUAL_PHONE_NUMBER, "");
                break;
            case ID_EQ_FIRST:
            case ID_EQ_FIRST + 1:
            case ID_EQ_FIRST + 2:
            case ID_EQ_FIRST + 3:
            case ID_EQ_FIRST + 4: {
                int band = item.id - ID_EQ_FIRST;
                pickLabeled(EQ_TITLES[band], org.telegram.messenger.banana.BananaEqualizer.band(band),
                        org.telegram.messenger.banana.BananaEqualizer.GAIN_CHOICES, BananaExtrasValuesActivity::decibels);
                break;
            }
            case ID_VOICE_PRESET: {
                CharSequence[] names = new CharSequence[VOICE_PRESETS.length];
                int[] codes = new int[VOICE_PRESETS.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = LocaleController.getString(VOICE_PRESETS[i]);
                    codes[i] = i;
                }
                pickFrom(R.string.BananaVal_VoicePreset, IntValue.VOICE_CHANGER_PRESET, names, codes);
                break;
            }
            case ID_VOICE_PITCH:
                pickLabeled(R.string.BananaVal_VoicePitch, IntValue.VOICE_CHANGER_PITCH,
                        org.telegram.messenger.banana.VoicePresets.PITCH_CHOICES, BananaExtrasValuesActivity::semitones);
                break;
            case ID_VOICE_REVERB:
                pickLabeled(R.string.BananaVal_VoiceReverb, IntValue.VOICE_CHANGER_REVERB,
                        org.telegram.messenger.banana.VoicePresets.AMOUNT_CHOICES, BananaExtrasValuesActivity::amount);
                break;
            case ID_VOICE_ECHO:
                pickLabeled(R.string.BananaVal_VoiceEcho, IntValue.VOICE_CHANGER_ECHO,
                        org.telegram.messenger.banana.VoicePresets.AMOUNT_CHOICES, BananaExtrasValuesActivity::amount);
                break;
            case ID_DEEPL_KEY:
                editString(R.string.BananaVal_DeeplKey, StringValue.DEEPL_API_KEY, LocaleController.getString(R.string.BananaVal_ApiKey));
                break;
            case ID_YANDEX_KEY:
                editString(R.string.BananaVal_YandexKey, StringValue.YANDEX_API_KEY, LocaleController.getString(R.string.BananaVal_ApiKey));
                break;
            case ID_TEXT_RULES:
                editString(R.string.BananaVal_TextReplacementRules, StringValue.TEXT_REPLACEMENT_RULES,
                        LocaleController.getString(R.string.BananaVal_TextReplacementRulesHint));
                break;
            default:
                break;
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private UItem cell(int id, int title, CharSequence value) {
        return BananaValueCell.Factory.of(id, LocaleController.getString(title), value);
    }

    private String refreshRateLabel(float rate) {
        return rate < 0 ? LocaleController.getString(R.string.BananaRefreshMaximum)
                : rate == 0 ? LocaleController.getString(R.string.BananaRefreshSystem)
                : LocaleController.formatString(R.string.BananaRefreshHz, Math.round(rate));
    }

    private static String glassGlareLabel(BananaConfig.GlassGlare mode) {
        switch (mode) {
            case OFF: return LocaleController.getString(R.string.BananaGlassHighlightsOff);
            case OUTLINE: return LocaleController.getString(R.string.BananaGlassHighlightsOutline);
            default: return LocaleController.getString(R.string.BananaGlassHighlightsDefault);
        }
    }

    private void pickGlassGlare() {
        if (getParentActivity() == null) return;
        BananaConfig.GlassGlare[] modes = BananaConfig.GlassGlare.values();
        CharSequence[] labels = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) labels[i] = glassGlareLabel(modes[i]);
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaGlassHighlights))
                .setMessage(LocaleController.getString(R.string.BananaGlassHighlightsInfo))
                .setItems(labels, (dialog, which) -> {
                    if (which < 0 || which >= modes.length) return;
                    BananaConfig.setGlassGlare(modes[which]);
                    if (glassPreview != null) glassPreview.invalidate();
                    if (listView != null) listView.adapter.update(true);
                    if (getParentActivity() instanceof LaunchActivity) {
                        ((LaunchActivity) getParentActivity()).rebuildAllFragments(true);
                    }
                }).create());
    }

    /**
     * The field layout is fixed when a message field is built, so the preview is made again and open chats are
     * rebuilt to pick the new layout up at once.
     */
    private void toggleGlassComposer() {
        toggleGlassChromeOption(() -> BananaConfig.setGlassComposer(!BananaConfig.glassComposer()), true);
    }

    private void toggleGlassCenteredHeader() {
        toggleGlassChromeOption(() -> BananaConfig.setGlassCenteredHeader(!BananaConfig.glassCenteredHeader()), true);
    }

    private void toggleGlassChromeOption(Runnable toggle, boolean rebuildPreview) {
        toggle.run();
        if (rebuildPreview) glassPreview = null;
        if (listView != null) listView.adapter.update(true);
        if (getParentActivity() instanceof LaunchActivity) {
            ((LaunchActivity) getParentActivity()).rebuildAllFragments(true);
        }
    }

    private void pickRefreshRate() {
        if (getParentActivity() == null) return;
        ArrayList<Float> rates = new ArrayList<>();
        rates.add(0f); rates.add(-1f);
        android.view.Display display = getParentActivity().getWindowManager().getDefaultDisplay();
        if (display != null) {
            java.util.TreeSet<Float> supported = new java.util.TreeSet<>();
            for (float rate : display.getSupportedRefreshRates()) {
                if (Float.isFinite(rate) && rate > 0) supported.add(rate);
            }
            rates.addAll(supported);
        }
        CharSequence[] labels = new CharSequence[rates.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = refreshRateLabel(rates.get(i));
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaRefreshRate))
                .setItems(labels, (dialog, which) -> {
                    org.telegram.messenger.MessagesController.getGlobalMainSettings().edit()
                            .putFloat(org.telegram.messenger.banana.RefreshRatePolicy.KEY, rates.get(which)).apply();
                    if (getParentActivity() != null) AndroidUtilities.setPreferredMaxRefreshRate(getParentActivity().getWindow());
                    if (listView != null) listView.adapter.update(true);
                }).create());
    }

    private void pickFrom(int title, IntValue value, CharSequence[] labels, int[] codes) {
        if (getParentActivity() == null) {
            return;
        }
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(title))
                .setItems(labels, (dialog, which) -> {
                    if (which >= 0 && which < codes.length) {
                        BananaConfig.setInt(currentAccount, value, codes[which]);
                        listView.adapter.update(true);
                    }
                }).create());
    }

    private void pickPercent(int title, IntValue value, int min, int max, int step) {
        if (getParentActivity() == null) {
            return;
        }
        int count = ((max - min) / step) + 1;
        CharSequence[] labels = new CharSequence[count];
        int[] codes = new int[count];
        for (int i = 0; i < count; i++) {
            codes[i] = min + i * step;
            labels[i] = percent(codes[i]);
        }
        pickFrom(title, value, labels, codes);
    }

    private void editString(int title, StringValue value, String hint) {
        if (getParentActivity() == null) {
            return;
        }
        EditTextBoldCursor input = new EditTextBoldCursor(getParentActivity());
        input.setTextSize(1, 18);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        input.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, getResourceProvider()));
        input.setBackgroundDrawable(Theme.createEditTextDrawable(getParentActivity(), true));
        input.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        boolean multiline = value == StringValue.TEXT_REPLACEMENT_RULES;
        boolean secret = value == StringValue.DEEPL_API_KEY || value == StringValue.YANDEX_API_KEY;
        input.setMaxLines(multiline ? 8 : 1);
        input.setSingleLine(!multiline);
        if (secret) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(org.telegram.messenger.banana.TranslationPolicy.MAX_KEY)});
        } else if (multiline) {
            input.setMinLines(3);
            input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16384)});
        } else if (value == StringValue.VISUAL_PHONE_NUMBER) {
            input.setInputType(InputType.TYPE_CLASS_PHONE);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            // BananaConfig keeps eight characters; stop typing there instead of cutting on save.
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        }
        input.setHint(hint);
        input.setText(BananaConfig.getString(currentAccount, value));
        input.setSelection(input.length());

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        if (multiline) {
            android.widget.TextView info = new android.widget.TextView(getParentActivity());
            info.setTextSize(1, 13);
            info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, getResourceProvider()));
            info.setText(LocaleController.getString(R.string.BananaVal_TextReplacementRulesInfo));
            container.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
        }

        AlertDialog dialog = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(title))
                .setView(container)
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .setPositiveButton(LocaleController.getString(R.string.OK), (d, which) -> {
                    BananaConfig.setString(currentAccount, value, input.getText().toString());
                    listView.adapter.update(true);
                }).create();
        showDialog(dialog);
        input.requestFocus();
        AndroidUtilities.showKeyboard(input);
    }

    private static String percent(int value) {
        return value + "%";
    }

    private static String markDisplay(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }

    private static String photoQualityLabel(int value) {
        switch (value) {
            case 1: return LocaleController.getString(R.string.BananaVal_PhotoQualityBetter);
            case 2: return LocaleController.getString(R.string.BananaVal_PhotoQualityMax);
            default: return LocaleController.getString(R.string.BananaVal_PhotoQualityDefault);
        }
    }

    private static String seekLabel(int seconds) {
        return seconds <= 0 ? LocaleController.getString(R.string.BananaVal_SeekOff) : LocaleController.formatPluralString("Seconds", seconds);
    }

    private static String doubleTapLabel(int value) {
        switch (value) {
            case 1: return LocaleController.getString(R.string.BananaVal_TapReact);
            case 2: return LocaleController.getString(R.string.BananaVal_TapReply);
            case 3: return LocaleController.getString(R.string.BananaVal_TapRepeat);
            case 4: return LocaleController.getString(R.string.BananaVal_TapTranslate);
            case 6: return LocaleController.getString(R.string.BananaVal_TapEdit);
            default: return LocaleController.getString(R.string.BananaVal_TapOff);
        }
    }

    private static String stickerShapeLabel(int value) {
        switch (value) {
            case 1: return LocaleController.getString(R.string.BananaVal_ShapeRounded);
            case 2: return LocaleController.getString(R.string.BananaVal_ShapeMoreRounded);
            default: return LocaleController.getString(R.string.BananaVal_ShapeStock);
        }
    }

    private static String sendFormatLabel(int value) {
        switch (value) {
            case 1: return LocaleController.getString(R.string.BananaVal_FormatBold);
            case 2: return LocaleController.getString(R.string.BananaVal_FormatItalic);
            case 3: return LocaleController.getString(R.string.BananaVal_FormatUnderline);
            case 4: return LocaleController.getString(R.string.BananaVal_FormatStrike);
            case 5: return LocaleController.getString(R.string.BananaVal_FormatSpoiler);
            default: return LocaleController.getString(R.string.BananaVal_FormatOff);
        }
    }

    private static String videoQualityLabel(int value) {
        switch (value) {
            case 1: return LocaleController.getString(R.string.BananaVal_VideoHighest);
            case 2: return LocaleController.getString(R.string.BananaVal_VideoLowest);
            default: return LocaleController.getString(R.string.BananaVal_VideoAuto);
        }
    }

    private static String translationLabel(int value) {
        switch (value) {
            case 1: return LocaleController.getString(R.string.BananaVal_BackendGoogle);
            case 2: return "DeepL";
            case 3: return LocaleController.getString(R.string.BananaVal_BackendYandex);
            default: return LocaleController.getString(R.string.BananaVal_BackendDefault);
        }
    }

    /** A key is never shown, only whether one is set. */
    private static String keyDisplay(String key) {
        return key == null || key.trim().isEmpty() ? "—" : "••••";
    }

    private static String transcriptionLabel(int value) {
        return LocaleController.getString(value == 1 ? R.string.BananaVoiceRussian : value == 2 ? R.string.BananaVoiceEnglish : R.string.BananaVal_BackendDefault);
    }
}
