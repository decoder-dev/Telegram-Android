package org.telegram.ui;

import android.text.InputType;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.banana.BananaDuress;
import org.telegram.messenger.banana.DuressVerifier;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/** Extras > Privacy > Duress passcode (ported from BananaGram iOS), built from dialogs. */
public final class BananaDuressSettings {
    private BananaDuressSettings() {}

    public static void show(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        if (!BananaDuress.hasPasscode()) {
            alert(fragment, LocaleController.getString(R.string.BananaDuressTitle), LocaleController.getString(R.string.BananaDuressNeedsPasscode));
            return;
        }
        boolean set = BananaDuress.isSet();
        boolean kindChanged = BananaDuress.kindChanged();
        ArrayList<CharSequence> items = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();
        items.add(LocaleController.getString(set ? R.string.BananaDuressChange : R.string.BananaDuressSet));
        actions.add(() -> confirmRealPasscode(fragment, () -> enterCode(fragment)));
        items.add(LocaleController.formatString(R.string.BananaDuressWhenEntered, actionName(BananaDuress.action())));
        actions.add(() -> pickAction(fragment));
        if (BananaDuress.action() == DuressVerifier.ACTION_LOG_OUT) {
            items.add(LocaleController.getString(R.string.BananaDuressAccounts));
            actions.add(() -> pickAccounts(fragment));
        }
        if (set || kindChanged) {
            items.add(LocaleController.getString(R.string.BananaDuressTurnOff));
            actions.add(BananaDuress::clear);
        }
        String message = LocaleController.getString(kindChanged ? R.string.BananaDuressKindChanged : R.string.BananaDuressInfo)
                + "\n\n" + actionInfo(BananaDuress.action());
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.BananaDuressTitle) + " · "
                + LocaleController.getString(set ? R.string.BananaDuressOn : R.string.BananaDuressOff));
        builder.setMessage(message);
        builder.setItems(items.toArray(new CharSequence[0]), (dialog, which) -> actions.get(which).run());
        builder.setNegativeButton(LocaleController.getString(R.string.Close), null);
        fragment.showDialog(builder.create());
    }

    private static String actionName(int action) {
        switch (action) {
            case DuressVerifier.ACTION_LOG_OUT: return LocaleController.getString(R.string.BananaDuressLogOut);
            case DuressVerifier.ACTION_ERASE: return LocaleController.getString(R.string.BananaDuressErase);
            default: return LocaleController.getString(R.string.BananaDuressHide);
        }
    }

    private static String actionInfo(int action) {
        switch (action) {
            case DuressVerifier.ACTION_LOG_OUT: return LocaleController.getString(R.string.BananaDuressLogOutInfo);
            case DuressVerifier.ACTION_ERASE: return LocaleController.getString(R.string.BananaDuressEraseInfo);
            default: return LocaleController.getString(R.string.BananaDuressHideInfo);
        }
    }

    private static void alert(BaseFragment fragment, String title, String text) {
        if (fragment.getParentActivity() == null) return;
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(title).setMessage(text)
                .setPositiveButton(LocaleController.getString(R.string.OK), null).create());
    }

    private static EditTextBoldCursor codeField(BaseFragment fragment, boolean pin) {
        EditTextBoldCursor input = new EditTextBoldCursor(fragment.getParentActivity());
        input.setTextSize(1, 18);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, fragment.getResourceProvider()));
        input.setBackgroundDrawable(Theme.createEditTextDrawable(fragment.getParentActivity(), true));
        input.setSingleLine(true);
        input.setInputType(pin ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        return input;
    }

    private static void askCode(BaseFragment fragment, String title, String info, java.util.function.Consumer<String> done) {
        if (fragment.getParentActivity() == null) return;
        boolean pin = SharedConfig.passcodeType == SharedConfig.PASSCODE_TYPE_PIN;
        EditTextBoldCursor input = codeField(fragment, pin);
        LinearLayout container = new LinearLayout(fragment.getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(title);
        if (info != null) builder.setMessage(info);
        builder.setView(container);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> done.accept(input.getText().toString()));
        fragment.showDialog(builder.create());
        input.requestFocus();
        AndroidUtilities.showKeyboard(input);
    }

    /**
     * The real passcode first, with its usual throttling: otherwise "this is your passcode" on the
     * next step would let anyone holding the unlocked phone guess the passcode without limits.
     */
    private static void confirmRealPasscode(BaseFragment fragment, Runnable next) {
        if (SharedConfig.passcodeRetryInMs > 0) {
            alert(fragment, LocaleController.getString(R.string.BananaDuressTitle),
                    LocaleController.formatString(R.string.BananaDuressTryAgain, Math.max(1, (int) Math.ceil(SharedConfig.passcodeRetryInMs / 1000.0))));
            return;
        }
        askCode(fragment, LocaleController.getString(R.string.BananaDuressEnterPasscode), null, code -> {
            if (!SharedConfig.checkPasscode(code)) {
                SharedConfig.increaseBadPasscodeTries();
                alert(fragment, LocaleController.getString(R.string.BananaDuressTitle), LocaleController.getString(R.string.BananaDuressWrongPasscode));
                return;
            }
            SharedConfig.badPasscodeTries = 0;
            SharedConfig.saveConfig();
            next.run();
        });
    }

    private static void enterCode(BaseFragment fragment) {
        askCode(fragment, LocaleController.getString(R.string.BananaDuressSet), LocaleController.getString(R.string.BananaDuressInfo), code -> {
            int result = BananaDuress.store(code);
            int text;
            switch (result) {
                case BananaDuress.RESULT_OK: text = R.string.BananaDuressSaved; break;
                case BananaDuress.RESULT_SAME_AS_PASSCODE: text = R.string.BananaDuressSameAsPasscode; break;
                case BananaDuress.RESULT_ARCHIVE_PASSWORD: text = R.string.BananaDuressSameAsArchive; break;
                case BananaDuress.RESULT_WRONG_KIND: text = R.string.BananaDuressSameKind; break;
                default: text = R.string.ErrorOccurred; break;
            }
            alert(fragment, LocaleController.getString(R.string.BananaDuressTitle), LocaleController.getString(text));
        });
    }

    private static void pickAction(BaseFragment fragment) {
        if (fragment.getParentActivity() == null) return;
        CharSequence[] names = {actionName(DuressVerifier.ACTION_HIDE), actionName(DuressVerifier.ACTION_LOG_OUT), actionName(DuressVerifier.ACTION_ERASE)};
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaDuressWhenEnteredHeader))
                .setItems(names, (dialog, which) -> {
                    if (which == DuressVerifier.ACTION_HIDE) {
                        BananaDuress.setAction(which);
                        return;
                    }
                    boolean erase = which == DuressVerifier.ACTION_ERASE;
                    AlertDialog confirm = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                            .setTitle(LocaleController.getString(erase ? R.string.BananaDuressEraseConfirmTitle : R.string.BananaDuressLogOutConfirmTitle))
                            .setMessage(LocaleController.getString(erase ? R.string.BananaDuressEraseConfirmText : R.string.BananaDuressLogOutConfirmText))
                            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                            .setPositiveButton(LocaleController.getString(R.string.BananaDuressConfirm), (d, w) -> {
                                BananaDuress.setAction(which);
                                if (!erase && BananaDuress.logoutIds().length == 0) {
                                    // Start from the current account rather than from nothing.
                                    BananaDuress.setLogoutIds(new long[]{UserConfig.getInstance(fragment.getCurrentAccount()).getClientUserId()});
                                }
                            })
                            .create();
                    fragment.showDialog(confirm);
                    android.widget.TextView button = (android.widget.TextView) confirm.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                    if (button != null) button.setTextColor(Theme.getColor(Theme.key_text_RedBold, fragment.getResourceProvider()));
                }).create());
    }

    private static void pickAccounts(BaseFragment fragment) {
        if (fragment.getParentActivity() == null) return;
        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<CharSequence> names = new ArrayList<>();
        long[] chosen = BananaDuress.logoutIds();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig config = UserConfig.getInstance(a);
            if (!config.isClientActivated() || config.getCurrentUser() == null) continue;
            long id = config.getClientUserId();
            boolean checked = false;
            for (long c : chosen) if (c == id) checked = true;
            ids.add(id);
            names.add((checked ? "☑ " : "☐ ") + UserObject.getUserName(config.getCurrentUser()));
        }
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaDuressAccounts))
                .setItems(names.toArray(new CharSequence[0]), (dialog, which) -> {
                    long id = ids.get(which);
                    ArrayList<Long> next = new ArrayList<>();
                    boolean removed = false;
                    for (long c : BananaDuress.logoutIds()) {
                        if (c == id) removed = true; else next.add(c);
                    }
                    if (!removed) next.add(id);
                    long[] stored = new long[next.size()];
                    for (int i = 0; i < stored.length; i++) stored[i] = next.get(i);
                    BananaDuress.setLogoutIds(stored);
                    pickAccounts(fragment);
                })
                .setPositiveButton(LocaleController.getString(R.string.Done), null)
                .create());
    }
}
