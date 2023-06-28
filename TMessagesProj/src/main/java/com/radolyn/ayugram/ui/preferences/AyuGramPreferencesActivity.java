/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ui.preferences;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.messages.AyuMessagesController;
import com.radolyn.ayugram.sync.AyuSyncState;
import com.radolyn.ayugram.ui.preferences.utils.AyuUi;
import com.radolyn.ayugram.utils.AyuState;
import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;

public class AyuGramPreferencesActivity extends BasePreferencesActivity implements NotificationCenter.NotificationCenterDelegate {

    private static final int TOGGLE_BUTTON_VIEW = 1000;

    private int ghostEssentialsHeaderRow;
    private int ghostFastToggleRow;
    private int sendReadPacketsRow;
    private int sendOnlinePacketsRow;
    private int sendUploadProgressRow;
    private int sendOfflinePacketAfterOnlineRow;
    private int markReadAfterSendRow;
    private int useScheduledMessagesRow;
    private int ghostDividerRow;

    private int spyHeaderRow;
    private int keepDeletedMessagesRow;
    private int keepMessagesHistoryRow;
    private int spyDividerRow;

    private int qolHeaderRow;
    private int showFromChannelRow;
    private int keepAliveServiceRow;
    private int enableAdsRow;
    private int localPremiumRow;
    private int filtersRow;
    private int qolDividerRow;

    private int customizationHeaderRow;
    private int deletedMarkTextRow;
    private int editedMarkTextRow;
    private int showGhostToggleInDrawerRow;
    private int showKillButtonInDrawerRow;
    private int customizationDividerRow;

    private int ayuSyncHeaderRow;
    private int ayuSyncStatusBtnRow;
    private int ayuSyncDividersRow;

    private int debugHeaderRow;
    private int WALModeRow;
    private int cleanDatabaseBtnRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        ghostEssentialsHeaderRow = newRow();
        ghostFastToggleRow = newRow();
        sendReadPacketsRow = newRow();
        sendOnlinePacketsRow = newRow();
        sendUploadProgressRow = newRow();
        sendOfflinePacketAfterOnlineRow = newRow();
        markReadAfterSendRow = newRow();
        useScheduledMessagesRow = newRow();
        ghostDividerRow = newRow();

        spyHeaderRow = newRow();
        keepDeletedMessagesRow = newRow();
        keepMessagesHistoryRow = newRow();
        spyDividerRow = newRow();

        qolHeaderRow = newRow();
        showFromChannelRow = newRow();
        keepAliveServiceRow = newRow();
        enableAdsRow = newRow();
        localPremiumRow = newRow();
        filtersRow = newRow();
        qolDividerRow = newRow();

        customizationHeaderRow = newRow();
        deletedMarkTextRow = newRow();
        editedMarkTextRow = newRow();
        showGhostToggleInDrawerRow = newRow();
        showKillButtonInDrawerRow = newRow();
        customizationDividerRow = newRow();

        ayuSyncHeaderRow = newRow();
        ayuSyncStatusBtnRow = newRow();
        ayuSyncDividersRow = newRow();

        debugHeaderRow = newRow();
        WALModeRow = newRow();
        cleanDatabaseBtnRow = newRow();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        // todo: register `MESSAGES_DELETED_NOTIFICATION` on all notification centers, not only on the current account
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, AyuConstants.MESSAGES_DELETED_NOTIFICATION);
        NotificationCenter.getGlobalInstance().addObserver(this, AyuConstants.AYUSYNC_STATE_CHANGED);
        return true;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == AyuConstants.MESSAGES_DELETED_NOTIFICATION) {
            // recalculate database size
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(cleanDatabaseBtnRow);
            }
        } else if (id == AyuConstants.AYUSYNC_STATE_CHANGED) {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(ayuSyncStatusBtnRow);
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, AyuConstants.MESSAGES_DELETED_NOTIFICATION);
        NotificationCenter.getGlobalInstance().removeObserver(this, AyuConstants.AYUSYNC_STATE_CHANGED);
    }

    private void updateGhostViews() {
        var msg = AyuConfig.isGhostModeActive()
                ? LocaleController.getString(R.string.DisableGhostMode)
                : LocaleController.getString(R.string.EnableGhostMode);

        listAdapter.notifyItemChanged(ghostFastToggleRow, msg);
        listAdapter.notifyItemChanged(sendReadPacketsRow, !AyuConfig.isGhostModeActive());
        listAdapter.notifyItemChanged(sendOnlinePacketsRow, !AyuConfig.isGhostModeActive());
        listAdapter.notifyItemChanged(sendUploadProgressRow, !AyuConfig.isGhostModeActive());
        listAdapter.notifyItemChanged(sendOfflinePacketAfterOnlineRow, AyuConfig.isGhostModeActive());

        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == ghostFastToggleRow) {
            AyuConfig.toggleGhostMode();

            updateGhostViews();
        } else if (position == sendReadPacketsRow) {
            AyuConfig.editor.putBoolean("sendReadPackets", AyuConfig.sendReadPackets ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendReadPackets);

            AyuState.setAllowReadPacket(false, -1);
            updateGhostViews();
        } else if (position == sendOnlinePacketsRow) {
            AyuConfig.editor.putBoolean("sendOnlinePackets", AyuConfig.sendOnlinePackets ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendOnlinePackets);

            updateGhostViews();
        } else if (position == sendUploadProgressRow) {
            AyuConfig.editor.putBoolean("sendUploadProgress", AyuConfig.sendUploadProgress ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendUploadProgress);

            updateGhostViews();
        } else if (position == sendOfflinePacketAfterOnlineRow) {
            AyuConfig.editor.putBoolean("sendOfflinePacketAfterOnline", AyuConfig.sendOfflinePacketAfterOnline ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendOfflinePacketAfterOnline);

            updateGhostViews();
        } else if (position == markReadAfterSendRow) {
            AyuConfig.editor.putBoolean("markReadAfterSend", AyuConfig.markReadAfterSend ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.markReadAfterSend);

            AyuState.setAllowReadPacket(false, -1);
        } else if (position == useScheduledMessagesRow) {
            AyuConfig.editor.putBoolean("useScheduledMessages", AyuConfig.useScheduledMessages ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.useScheduledMessages);

            AyuState.setAutomaticallyScheduled(false, -1);
        } else if (position == keepDeletedMessagesRow) {
            AyuConfig.editor.putBoolean("keepDeletedMessages", AyuConfig.keepDeletedMessages ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.keepDeletedMessages);
        } else if (position == keepMessagesHistoryRow) {
            AyuConfig.editor.putBoolean("keepMessagesHistory", AyuConfig.keepMessagesHistory ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.keepMessagesHistory);
        } else if (position == showFromChannelRow) {
            AyuConfig.editor.putBoolean("showFromChannel", AyuConfig.showFromChannel ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.showFromChannel);
        } else if (position == keepAliveServiceRow) {
            AyuConfig.editor.putBoolean("keepAliveService", AyuConfig.keepAliveService ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.keepAliveService);
        } else if (position == enableAdsRow) {
            AyuConfig.editor.putBoolean("enableAds", AyuConfig.enableAds ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.enableAds);
        } else if (position == localPremiumRow) {
            AyuConfig.editor.putBoolean("localPremium", AyuConfig.localPremium ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.localPremium);

            getMessagesController().updatePremium(AyuConfig.localPremium);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.premiumStatusChangedGlobal);

            getMediaDataController().loadPremiumPromo(false);
            getMediaDataController().loadReactions(false, true);
        } else if (position == filtersRow) {
            NotificationsCheckCell checkCell = (NotificationsCheckCell) view;
            if (LocaleController.isRTL && x <= AndroidUtilities.dp(76) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(76)) {
                AyuConfig.editor.putBoolean("regexFiltersEnabled", AyuConfig.regexFiltersEnabled ^= true).apply();
                checkCell.setChecked(AyuConfig.regexFiltersEnabled, 0);
            } else {
                presentFragment(new RegexFiltersPreferencesActivity());
            }
        } else if (position == showGhostToggleInDrawerRow) {
            AyuConfig.editor.putBoolean("showGhostToggleInDrawer", AyuConfig.showGhostToggleInDrawer ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.showGhostToggleInDrawer);

            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == showKillButtonInDrawerRow) {
            AyuConfig.editor.putBoolean("showKillButtonInDrawer", AyuConfig.showKillButtonInDrawer ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.showKillButtonInDrawer);

            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == deletedMarkTextRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.DeletedMarkText),
                    AyuConfig::getDeletedMark,
                    "deletedMarkText",
                    AyuConstants.DEFAULT_DELETED_MARK
            );
        } else if (position == editedMarkTextRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.EditedMarkText),
                    AyuConfig::getEditedMark,
                    "editedMarkText",
                    LocaleController.getString("EditedMessage", R.string.EditedMessage) // don't remove key
            );
        } else if (position == ayuSyncStatusBtnRow) {
            presentFragment(new AyuSyncPreferencesActivity());
        } else if (position == WALModeRow) {
            AyuConfig.editor.putBoolean("WALMode", AyuConfig.WALMode ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.WALMode);
        } else if (position == cleanDatabaseBtnRow) {
            AyuMessagesController.getInstance().clean();

            // reset size
            ((TextCell) view).setValue("…");

            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.CleanDatabaseNotification)).show();
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.AyuPreferences);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == ghostFastToggleRow) {
                        var msg = AyuConfig.isGhostModeActive()
                                ? LocaleController.getString(R.string.DisableGhostMode)
                                : LocaleController.getString(R.string.EnableGhostMode);

                        textCell.setTextAndIcon(msg, R.drawable.ayu_ghost, true);
                    } else if (position == deletedMarkTextRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.DeletedMarkText), AyuConfig.getDeletedMark(), true);
                    } else if (position == editedMarkTextRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.EditedMarkText), AyuConfig.getEditedMark(), true);
                    } else if (position == ayuSyncStatusBtnRow) {
                        var status = AyuSyncState.getConnectionStateString();

                        textCell.setTextAndValue(LocaleController.getString(R.string.AyuSyncStatusTitle), status, true);
                    } else if (position == cleanDatabaseBtnRow) {
                        var file = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE);
                        var size = file.exists() ? file.length() : 0;

                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.CleanDatabase), AndroidUtilities.formatFileSize(size), R.drawable.msg_clearcache, false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == ghostEssentialsHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.GhostEssentialsHeader));
                    } else if (position == spyHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.SpyEssentialsHeader));
                    } else if (position == qolHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.QoLTogglesHeader));
                    } else if (position == customizationHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.CustomizationHeader));
                    } else if (position == ayuSyncHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.AyuSyncHeader));
                    } else if (position == debugHeaderRow) {
                        headerCell.setText(LocaleController.getString("SettingsDebug", R.string.SettingsDebug));
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == sendReadPacketsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SendReadPackets), AyuConfig.sendReadPackets, true);
                    } else if (position == sendOnlinePacketsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SendOnlinePackets), AyuConfig.sendOnlinePackets, true);
                    } else if (position == sendUploadProgressRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SendUploadProgress), AyuConfig.sendUploadProgress, true);
                    } else if (position == sendOfflinePacketAfterOnlineRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SendOfflinePacketAfterOnline), AyuConfig.sendOfflinePacketAfterOnline, true);
                    } else if (position == markReadAfterSendRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MarkReadAfterSend), AyuConfig.markReadAfterSend, true);
                    } else if (position == useScheduledMessagesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.UseScheduledMessages), AyuConfig.useScheduledMessages, true);
                    } else if (position == keepDeletedMessagesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.KeepDeletedMessages), AyuConfig.keepDeletedMessages, true);
                    } else if (position == keepMessagesHistoryRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.KeepMessagesHistory), AyuConfig.keepMessagesHistory, true);
                    } else if (position == showFromChannelRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.ShowFromChannel), AyuConfig.showFromChannel, true);
                    } else if (position == keepAliveServiceRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.KeepAliveService) + " β", AyuConfig.keepAliveService, true);
                    } else if (position == enableAdsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.EnableAds), AyuConfig.enableAds, true);
                    } else if (position == localPremiumRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.LocalPremium) + " β", AyuConfig.localPremium, true);
                    } else if (position == showGhostToggleInDrawerRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.ShowGhostToggleInDrawer), AyuConfig.showGhostToggleInDrawer, true);
                    } else if (position == showKillButtonInDrawerRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.ShowKllButtonInDrawer), AyuConfig.showKillButtonInDrawer, true);
                    } else if (position == WALModeRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.WALMode), AyuConfig.WALMode, true);
                    }
                    break;
                case TOGGLE_BUTTON_VIEW:
                    NotificationsCheckCell notificationsCheckCell = (NotificationsCheckCell) holder.itemView;
                    if (position == filtersRow) {
                        var count = AyuConfig.getRegexFilters().size();
                        notificationsCheckCell.setTextAndValueAndCheck(LocaleController.getString(R.string.RegexFilters), count + " " + LocaleController.getString(R.string.RegexFiltersSubText), AyuConfig.regexFiltersEnabled, true);
                    }
                    break;
            }
        }

        @NonNull
        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            if (viewType == TOGGLE_BUTTON_VIEW) {
                var view = new NotificationsCheckCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                return new RecyclerListView.Holder(view);
            }
            return super.onCreateViewHolder(parent, viewType);
        }

        @Override
        public int getItemViewType(int position) {
            if (
                    position == ghostDividerRow ||
                            position == spyDividerRow ||
                            position == qolDividerRow ||
                            position == customizationDividerRow ||
                            position == ayuSyncDividersRow
            ) {
                return 1;
            } else if (
                    position == ghostFastToggleRow ||
                            position == deletedMarkTextRow ||
                            position == editedMarkTextRow ||
                            position == ayuSyncStatusBtnRow ||
                            position == cleanDatabaseBtnRow
            ) {
                return 2;
            } else if (
                    position == ghostEssentialsHeaderRow ||
                            position == spyHeaderRow ||
                            position == qolHeaderRow ||
                            position == customizationHeaderRow ||
                            position == ayuSyncHeaderRow ||
                            position == debugHeaderRow
            ) {
                return 3;
            } else if (
                    position == filtersRow
            ) {
                return TOGGLE_BUTTON_VIEW;
            }
            return 5;
        }
    }
}
