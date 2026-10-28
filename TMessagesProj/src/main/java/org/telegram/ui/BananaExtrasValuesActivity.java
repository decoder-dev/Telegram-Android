package org.telegram.ui;

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
import org.telegram.ui.Cells.BananaValueCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Non-boolean BananaGram Extras (ints, marks, backends). */
public final class BananaExtrasValuesActivity extends UniversalFragment {
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
        items.add(cell(ID_ACCENT, R.string.BananaVal_AccentSaturation, percent(BananaConfig.accentColorSaturation(currentAccount))));
        items.add(cell(ID_STICKER_SIZE, R.string.BananaVal_StickerSize, percent(BananaConfig.stickerSizePercent(currentAccount))));
        items.add(cell(ID_STICKER_SHAPE, R.string.BananaVal_StickerShape, stickerShapeLabel(BananaConfig.stickerShape(currentAccount))));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesGlass)));
        items.add(cell(ID_GLASS_TINT, R.string.BananaVal_GlassTint, percent(BananaConfig.glassBubbleTint(currentAccount))));
        items.add(cell(ID_GLASS_TOP, R.string.BananaVal_GlassRimTop, percent(BananaConfig.glassBubbleRimTop(currentAccount))));
        items.add(cell(ID_GLASS_SIDES, R.string.BananaVal_GlassRimSides, percent(BananaConfig.glassBubbleRimSides(currentAccount))));
        items.add(cell(ID_GLASS_BOTTOM, R.string.BananaVal_GlassRimBottom, percent(BananaConfig.glassBubbleRimBottom(currentAccount))));
        items.add(UItem.asShadow(null));

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
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesBackends)));
        items.add(cell(ID_TRANSLATION, R.string.BananaVal_TranslationBackend, translationLabel(BananaConfig.translationBackend(currentAccount))));
        items.add(cell(ID_TRANSCRIPTION, R.string.BananaVal_TranscriptionBackend, transcriptionLabel(BananaConfig.transcriptionBackend(currentAccount))));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaValuesProfile)));
        items.add(cell(ID_VISUAL_PHONE, R.string.BananaVal_VisualPhoneNumber, markDisplay(BananaConfig.visualPhoneNumber(currentAccount))));
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaValuesInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
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
                        new CharSequence[]{
                                LocaleController.getString(R.string.BananaVal_SeekOff),
                                "5s", "10s", "15s", "30s"
                        }, new int[]{0, 5, 10, 15, 30});
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
                                LocaleController.getString(R.string.BananaVal_BackendSystem)
                        }, new int[]{0, 1});
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
        input.setMaxLines(1);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(hint);
        input.setText(BananaConfig.getString(currentAccount, value));
        input.setSelection(input.length());

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

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
        return seconds <= 0 ? LocaleController.getString(R.string.BananaVal_SeekOff) : seconds + "s";
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
        return LocaleController.getString(value == 1 ? R.string.BananaVal_BackendSystem : R.string.BananaVal_BackendDefault);
    }

    private static String transcriptionLabel(int value) {
        return LocaleController.getString(value == 1 ? R.string.BananaVoiceRussian : value == 2 ? R.string.BananaVoiceEnglish : R.string.BananaVal_BackendDefault);
    }
}
