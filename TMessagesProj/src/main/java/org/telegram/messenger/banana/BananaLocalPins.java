package org.telegram.messenger.banana;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Pin Beyond the Limit (ported from BananaGram iOS): once Telegram's pinned chat limit is reached, a
 * chat in All Chats is pinned on this device only. Such pins are never sent to the server, and survive
 * its pinned-list reloads the way secret chat pins do.
 */
public final class BananaLocalPins {
    private static final String KEY = "bananaLocalPinnedDialogs";

    private BananaLocalPins() {}

    static List<Long> parse(String csv) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                try {
                    long id = Long.parseLong(part.trim());
                    if (id != 0) ids.add(id);
                } catch (NumberFormatException ignored) {
                    // Written by this class only; a damaged entry drops one local pin.
                }
            }
        }
        return new ArrayList<>(ids);
    }

    static String format(List<Long> ids) {
        StringBuilder out = new StringBuilder();
        for (long id : ids) {
            if (out.length() > 0) out.append(',');
            out.append(id);
        }
        return out.toString();
    }

    /** True when a new server pin would go over the limit, so it is kept on this device instead. */
    static boolean shouldPinLocally(int serverPinned, int limit) {
        return limit > 0 && serverPinned >= limit;
    }

    private static List<Long> load(int account) {
        return parse(MessagesController.getMainSettings(account).getString(KEY, ""));
    }

    private static void store(int account, List<Long> ids) {
        MessagesController.getMainSettings(account).edit().putString(KEY, format(ids)).apply();
    }

    public static boolean contains(int account, long dialogId) {
        return load(account).contains(dialogId);
    }

    public static void remove(int account, long dialogId) {
        List<Long> ids = load(account);
        if (ids.remove(dialogId)) store(account, ids);
    }

    public static boolean enabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.PIN_BEYOND_LIMIT);
    }

    /** Called right after a chat in All Chats is pinned or unpinned on this device. */
    public static void afterPinChange(int account, long dialogId, boolean pinned, boolean mainList) {
        if (!pinned) {
            remove(account, dialogId);
            return;
        }
        if (!mainList || !enabled(account) || DialogObject.isEncryptedDialog(dialogId)) return;
        MessagesController controller = MessagesController.getInstance(account);
        List<Long> local = load(account);
        int serverPinned = 0;
        for (TLRPC.Dialog dialog : controller.getDialogs(0)) {
            if (dialog == null || !dialog.pinned || dialog.id == dialogId || dialog instanceof TLRPC.TL_dialogFolder) continue;
            if (DialogObject.isEncryptedDialog(dialog.id) || local.contains(dialog.id)) continue;
            serverPinned++;
        }
        int limit = UserConfig.getInstance(account).isPremium() ? controller.maxPinnedDialogsCountPremium : controller.maxPinnedDialogsCountDefault;
        if (shouldPinLocally(serverPinned, limit) && !local.contains(dialogId)) {
            local.add(0, dialogId);
            store(account, local);
        }
    }
}
