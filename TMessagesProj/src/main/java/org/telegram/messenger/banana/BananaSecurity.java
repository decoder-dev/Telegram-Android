package org.telegram.messenger.banana;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.BananaExtrasSectionActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Scam protections ported from BananaGram iOS (Extras > Privacy > Scam protection). */
public final class BananaSecurity {
    private static final String MINI_APP_WARNED = "bananaMiniAppWarned";
    private static final String FAKE_SUPPORT_WARNED = "bananaFakeSupportWarned";

    private BananaSecurity() {}

    /** "Blocked" with a way to the switch that blocked it. */
    public static void showBlocked(BaseFragment fragment, int textRes) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        final int account = fragment.getCurrentAccount();
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.BananaSecurityBlocked));
        builder.setMessage(LocaleController.getString(textRes));
        builder.setNeutralButton(LocaleController.getString(R.string.BananaSecuritySettings),
                (dialog, which) -> fragment.presentFragment(new BananaExtrasSectionActivity(account, "PRIVACY")));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        fragment.showDialog(builder.create());
    }

    /** Telegram's (777000) latest message, when it is in memory and still fresh. */
    private static String freshServiceText(int account) {
        ArrayList<MessageObject> top = MessagesController.getInstance(account).dialogMessage.get(LoginCodeGuard.SERVICE_ID);
        if (top == null || top.isEmpty() || top.get(0) == null || top.get(0).messageOwner == null) return null;
        TLRPC.Message message = top.get(0).messageOwner;
        if (!LoginCodeGuard.isFresh(message.date, ConnectionsManager.getInstance(account).getCurrentTime())) return null;
        return message.message;
    }

    /** Whether a text (or a forward of Telegram's message) would hand out a fresh login code. */
    public static boolean blocksLoginCode(int account, String text, boolean forwardsServiceMessage) {
        if (!BananaConfig.get(account, BananaConfig.Option.SECURITY_GUARD_LOGIN_CODES)) return false;
        if (!forwardsServiceMessage && !LoginCodeGuard.mayHoldCode(text)) return false;
        Set<String> codes = LoginCodeGuard.codesIn(freshServiceText(account));
        if (codes.isEmpty()) return false;
        return forwardsServiceMessage || LoginCodeGuard.containsCode(text, codes);
    }

    /** Once per account and key: true the first time, after which the key is remembered. */
    private static boolean firstTime(String setKey, String key) {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        Set<String> stored = preferences.getStringSet(setKey, null);
        if (stored != null && stored.contains(key)) return false;
        HashSet<String> next = stored == null ? new HashSet<>() : new HashSet<>(stored);
        if (next.size() > 2000) next.clear();
        next.add(key);
        preferences.edit().putStringSet(setKey, next).apply();
        return true;
    }

    /** A one-time reminder when a mini app opens: never enter codes, passwords or the number there. */
    public static void maybeWarnMiniApp(Context context, Theme.ResourcesProvider resourcesProvider, int account, long botId) {
        if (context == null || botId == 0 || account < 0 || !BananaConfig.get(account, BananaConfig.Option.SECURITY_WARN_MINI_APPS)) return;
        if (!firstTime(MINI_APP_WARNED, UserConfig.getInstance(account).getClientUserId() + ":" + botId)) return;
        TLRPC.User bot = MessagesController.getInstance(account).getUser(botId);
        String name = bot != null ? UserObject.getUserName(bot) : "";
        AndroidUtilities.runOnUIThread(() -> {
            if (!AndroidUtilities.isSafeToShow(context)) return;
            new AlertDialog.Builder(context, resourcesProvider)
                    .setTitle(LocaleController.getString(R.string.BananaSecurityMiniAppTitle))
                    .setMessage(LocaleController.formatString(R.string.BananaSecurityMiniAppText, name))
                    .setPositiveButton(LocaleController.getString(R.string.OK), null)
                    .show();
        }, 500);
    }

    /** A one-time warning in an unverified chat that poses as Telegram staff. */
    public static void maybeWarnFakeSupport(BaseFragment fragment, TLRPC.User user) {
        if (fragment == null || user == null || UserObject.isUserSelf(user)) return;
        int account = fragment.getCurrentAccount();
        if (!BananaConfig.get(account, BananaConfig.Option.SECURITY_FLAG_FAKE_SUPPORT)) return;
        if (!FakeSupport.looksFake(user.id, user.verified, user.first_name, user.last_name, UserObject.getPublicUsername(user))) return;
        if (!firstTime(FAKE_SUPPORT_WARNED, UserConfig.getInstance(account).getClientUserId() + ":" + user.id)) return;
        String name = UserObject.getUserName(user);
        AndroidUtilities.runOnUIThread(() -> {
            if (fragment.getParentActivity() == null || fragment.isFinished) return;
            fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.BananaSecurityFakeSupportTitle))
                    .setMessage(LocaleController.formatString(R.string.BananaSecurityFakeSupportText, name))
                    .setPositiveButton(LocaleController.getString(R.string.OK), null)
                    .create());
        }, 800);
    }
}
