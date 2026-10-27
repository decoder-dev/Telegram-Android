/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import android.app.Activity;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import org.telegram.messenger.*;

import java.util.ArrayList;
import java.util.Arrays;

public class AyuConfig {
    private static final Object sync = new Object();

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    public static boolean sendReadPackets;
    public static boolean sendOnlinePackets;
    public static boolean sendOfflinePacketAfterOnline;
    public static boolean sendUploadProgress;
    public static boolean useScheduledMessages;
    public static boolean saveDeletedMessages;
    public static boolean saveMessagesHistory;

    public static boolean saveMedia;
    public static boolean saveMediaInPrivateChats;
    public static boolean saveMediaInPublicChannels;
    public static boolean saveMediaInPrivateChannels;
    public static boolean saveMediaInPublicGroups;
    public static boolean saveMediaInPrivateGroups;
    public static boolean saveFormatting;
    public static boolean saveReactions;
    public static boolean saveForBots;

    public static boolean markReadAfterSend;
    public static boolean keepAliveService;
    public static boolean disableAds;
    public static boolean localPremium;
    public static boolean regexFiltersEnabled;
    public static boolean regexFiltersInChats;
    public static boolean regexFiltersCaseInsensitive;
    public static boolean showGhostToggleInDrawer;
    public static boolean showKillButtonInDrawer;
    public static boolean showFromChannel;
    public static boolean syncEnabled;
    public static boolean useSecureConnection;
    public static boolean WALMode;

    private static boolean configLoaded;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }

            preferences = ApplicationLoader.applicationContext.getSharedPreferences("ayuconfig", Activity.MODE_PRIVATE);
            editor = preferences.edit();

            // ~ Ghost essentials
            sendReadPackets = preferences.getBoolean("sendReadPackets", true);
            sendOnlinePackets = preferences.getBoolean("sendOnlinePackets", true);
            sendUploadProgress = preferences.getBoolean("sendUploadProgress", true);
            sendOfflinePacketAfterOnline = preferences.getBoolean("sendOfflinePacketAfterOnline", false);

            markReadAfterSend = preferences.getBoolean("markReadAfterSend", true);
            useScheduledMessages = preferences.getBoolean("useScheduledMessages", false);

            // ~ Message edits & deletion history
            saveDeletedMessages = preferences.getBoolean("saveDeletedMessages", true);
            saveMessagesHistory = preferences.getBoolean("saveMessagesHistory", true);

            // ~ Message saving preferences
            saveMedia = preferences.getBoolean("saveMedia", true);
            saveMediaInPrivateChats = preferences.getBoolean("saveMediaInPrivateChats", true);
            saveMediaInPublicChannels = preferences.getBoolean("saveMediaInPublicChannels", false);
            saveMediaInPrivateChannels = preferences.getBoolean("saveMediaInPrivateChannels", true);
            saveMediaInPublicGroups = preferences.getBoolean("saveMediaInPublicGroups", false);
            saveMediaInPrivateGroups = preferences.getBoolean("saveMediaInPrivateGroups", true);
            saveForBots = preferences.getBoolean("saveForBots", true);

            saveFormatting = preferences.getBoolean("saveFormatting", true);
            saveReactions = preferences.getBoolean("saveReactions", true);

            // ~ Useful features
            keepAliveService = preferences.getBoolean("keepAliveService", true);
            disableAds = preferences.getBoolean("disableAds", true);
            localPremium = preferences.getBoolean("localPremium", false);
            regexFiltersEnabled = preferences.getBoolean("regexFiltersEnabled", false);
            regexFiltersInChats = preferences.getBoolean("regexFiltersInChats", false);
            regexFiltersCaseInsensitive = preferences.getBoolean("regexFiltersCaseInsensitive", true);
            // regexFilters

            // ~ Customization
            // deletedMarkText
            // editedMarkText
            showGhostToggleInDrawer = preferences.getBoolean("showGhostToggleInDrawer", true);
            showKillButtonInDrawer = preferences.getBoolean("showKillButtonInDrawer", false);

            // ~ AyuSync
            // syncServerURL
            // syncServerToken
            syncEnabled = preferences.getBoolean("syncEnabled", false);
            useSecureConnection = preferences.getBoolean("useSecureConnection", !BuildVars.isBetaApp());

            // ~ Debug
            WALMode = preferences.getBoolean("walMode", true);

            configLoaded = true;
        }
    }

    public static boolean isGhostModeActive() {
        return !sendReadPackets && !sendOnlinePackets && !sendUploadProgress && sendOfflinePacketAfterOnline;
    }

    public static void setGhostMode(boolean enabled) {
        sendReadPackets = !enabled;
        sendOnlinePackets = !enabled;
        sendUploadProgress = !enabled;
        sendOfflinePacketAfterOnline = enabled;

        AyuConfig.editor.putBoolean("sendReadPackets", AyuConfig.sendReadPackets).apply();
        AyuConfig.editor.putBoolean("sendOnlinePackets", AyuConfig.sendOnlinePackets).apply();
        AyuConfig.editor.putBoolean("sendUploadProgress", AyuConfig.sendUploadProgress).apply();
        AyuConfig.editor.putBoolean("sendOfflinePacketAfterOnline", AyuConfig.sendOfflinePacketAfterOnline).apply();
    }

    public static void toggleGhostMode() {
        // giga move
        setGhostMode(!isGhostModeActive());
    }

    public static boolean saveDeletedMessageFor(int accountId, long dialogId) {
        boolean banana = org.telegram.messenger.banana.BananaHooks.saveDeleted(accountId);
        if (!AyuConfig.saveDeletedMessages && !banana) {
            return false;
        }
        return saveForDialog(accountId, dialogId);
    }

    public static boolean saveEditedMessageFor(int accountId, long dialogId) {
        boolean banana = org.telegram.messenger.banana.BananaHooks.saveEdits(accountId);
        if (!AyuConfig.saveMessagesHistory && !banana) {
            return false;
        }
        return saveForDialog(accountId, dialogId);
    }

    /**
     * Bot exclusion applies only to private chats. {@code Math.abs} on a group or channel id
     * can collide with a cached user and skip retention for that whole chat.
     */
    private static boolean saveForDialog(int accountId, long dialogId) {
        if (!DialogObject.isUserDialog(dialogId)) {
            return true;
        }
        org.telegram.tgnet.TLRPC.User user = MessagesController.getInstance(accountId).getUser(dialogId);
        if (user == null) {
            return true;
        }
        return !user.bot || AyuConfig.saveForBots;
    }

    public static String getDeletedMark() {
        String banana = org.telegram.messenger.banana.BananaConfig.deletedMessageMark(UserConfig.selectedAccount);
        if (banana != null && !banana.isEmpty()) {
            return banana;
        }
        return AyuConfig.preferences.getString("deletedMarkText", AyuConstants.DEFAULT_DELETED_MARK);
    }

    public static String getEditedMark() {
        String banana = org.telegram.messenger.banana.BananaConfig.editedMessageMark(UserConfig.selectedAccount);
        if (banana != null && !banana.isEmpty()) {
            return banana;
        }
        String stored = AyuConfig.preferences.getString("editedMarkText", null);
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        return LocaleController.getString("EditedMessage", R.string.EditedMessage);
    }

    public static String getWALMode() {
        return AyuConfig.WALMode ? "WAL" : "OFF";
    }

    public static String getSyncServerURL() {
        return preferences.getString("syncServerURL", AyuConstants.DEFAULT_AYUSYNC_SERVER);
    }

    public static String getSyncServerToken() {
        return preferences.getString("syncServerToken", "");
    }

    public static ArrayList<String> getRegexFilters() {
        var str = preferences.getString("regexFilters", "[]");
        var arr = new Gson().fromJson(str, String[].class);

        return new ArrayList<>(Arrays.asList(arr));
    }

    public static void addFilter(String text) {
        var list = getRegexFilters();
        list.add(0, text);

        var str = new Gson().toJson(list);
        editor.putString("regexFilters", str).apply();

        AyuFilter.rebuildCache();
    }

    public static void editFilter(int filterIdx, String text) {
        var list = getRegexFilters();
        list.set(filterIdx, text);

        var str = new Gson().toJson(list);
        editor.putString("regexFilters", str).apply();

        AyuFilter.rebuildCache();
    }

    public static void removeFilter(int filterIdx) {
        var list = getRegexFilters();
        list.remove(filterIdx);

        var str = new Gson().toJson(list);
        editor.putString("regexFilters", str).apply();

        AyuFilter.rebuildCache();
    }
}
