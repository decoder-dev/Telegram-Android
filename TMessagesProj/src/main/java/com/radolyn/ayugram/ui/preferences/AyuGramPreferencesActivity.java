/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.radolyn.ayugram.ui.preferences;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.messages.AyuMessagesController;
import com.radolyn.ayugram.messages.AyuState;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;

public class AyuGramPreferencesActivity extends BasePreferencesActivity {
    private int ghostEssentialsHeaderRow;
    private int sendReadPacketsRow;
    private int sendOnlinePacketsRow;
    private int sendUploadProgressRow;
    private int sendOfflinePacketAfterOnlineRow;
    private int markReadAfterSendRow;
    private int useScheduledMessagesRow;

    private int spyHeaderRow;
    private int keepDeletedMessagesRow;
    private int keepMessagesHistoryRow;

    private int qolHeaderRow;
    private int showFromChannelRow;
    private int keepAliveServiceRow;
    private int enableAdsRow;

    private int customizationHeaderRow;
    private int deletedMarkTextRow;
    private int customizationDividerRow;

    private int debugHeaderRow;
    private int cleanDatabaseBtnRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        ghostEssentialsHeaderRow = newRow();
        sendReadPacketsRow = newRow();
        sendOnlinePacketsRow = newRow();
        sendUploadProgressRow = newRow();
        sendOfflinePacketAfterOnlineRow = newRow();
        markReadAfterSendRow = newRow();
        useScheduledMessagesRow = newRow();

        spyHeaderRow = newRow();
        keepDeletedMessagesRow = newRow();
        keepMessagesHistoryRow = newRow();

        qolHeaderRow = newRow();
        showFromChannelRow = newRow();
        keepAliveServiceRow = newRow();
        enableAdsRow = newRow();

        customizationHeaderRow = newRow();
        deletedMarkTextRow = newRow();
        customizationDividerRow = newRow();

        debugHeaderRow = newRow();
        cleanDatabaseBtnRow = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == sendReadPacketsRow) {
            AyuConfig.editor.putBoolean("sendReadPackets", AyuConfig.sendReadPackets ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendReadPackets);
        } else if (position == sendOnlinePacketsRow) {
            AyuConfig.editor.putBoolean("sendOnlinePackets", AyuConfig.sendOnlinePackets ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendOnlinePackets);
        } else if (position == sendUploadProgressRow) {
            AyuConfig.editor.putBoolean("sendUploadProgress", AyuConfig.sendUploadProgress ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendUploadProgress);
        } else if (position == sendOfflinePacketAfterOnlineRow) {
            AyuConfig.editor.putBoolean("sendOfflinePacketAfterOnline", AyuConfig.sendOfflinePacketAfterOnline ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendOfflinePacketAfterOnline);
        } else if (position == markReadAfterSendRow) {
            AyuConfig.editor.putBoolean("markReadAfterSend", AyuConfig.markReadAfterSend ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.markReadAfterSend);
        } else if (position == useScheduledMessagesRow) {
            AyuConfig.editor.putBoolean("useScheduledMessages", AyuConfig.useScheduledMessages ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.useScheduledMessages);
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
        } else if (position == deletedMarkTextRow) {
            var builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("DeletedMarkText", R.string.DeletedMarkText));
            var layout = new LinearLayout(getParentActivity());
            var input = new EditTextSettingsCell(getParentActivity());
            input.setText(AyuConfig.getDeletedMark(), true);

            layout.setGravity(LinearLayout.VERTICAL);
            layout.addView(input);
            builder.setView(layout);
            builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
                AyuConfig.editor.putString("deletedMarkText", input.getText()).apply();
                ((TextCell) view).setTextAndValue(LocaleController.getString("DeletedMarkText", R.string.DeletedMarkText), AyuConfig.getDeletedMark(), true);
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> dialog.cancel());

            builder.show();
        } else if (position == cleanDatabaseBtnRow) {
            AyuMessagesController.getInstance().clean();
            AyuState.reset();

            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString("CleanDatabaseNotification", R.string.CleanDatabaseNotification)).show();
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString("AyuPreferences", R.string.AyuPreferences);
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
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == deletedMarkTextRow) {
                        textCell.setTextAndValue(LocaleController.getString("DeletedMarkText", R.string.DeletedMarkText), AyuConfig.getDeletedMark(), true);
                    } else if (position == cleanDatabaseBtnRow) {
                        textCell.setTextAndIcon(LocaleController.getString("CleanDatabase", R.string.CleanDatabase), R.drawable.msg_clearcache, false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == ghostEssentialsHeaderRow) {
                        headerCell.setText(LocaleController.getString("GhostEssentialsHeader", R.string.GhostEssentialsHeader));
                    } else if (position == spyHeaderRow) {
                        headerCell.setText(LocaleController.getString("SpyEssentialsHeader", R.string.SpyEssentialsHeader));
                    } else if (position == qolHeaderRow) {
                        headerCell.setText(LocaleController.getString("QoLTogglesHeader", R.string.QoLTogglesHeader));
                    } else if (position == customizationHeaderRow) {
                        headerCell.setText(LocaleController.getString("CustomizationHeader", R.string.CustomizationHeader));
                    } else if (position == debugHeaderRow) {
                        headerCell.setText(LocaleController.getString("SettingsDebug", R.string.SettingsDebug));
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == sendReadPacketsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("SendReadPackets", R.string.SendReadPackets), AyuConfig.sendReadPackets, true);
                    } else if (position == sendOnlinePacketsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("SendOnlinePackets", R.string.SendOnlinePackets), AyuConfig.sendOnlinePackets, true);
                    } else if (position == sendUploadProgressRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("SendUploadProgress", R.string.SendUploadProgress), AyuConfig.sendUploadProgress, true);
                    } else if (position == sendOfflinePacketAfterOnlineRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("SendOfflinePacketAfterOnline", R.string.SendOfflinePacketAfterOnline) + " β", AyuConfig.sendOfflinePacketAfterOnline, true);
                    } else if (position == markReadAfterSendRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("MarkReadAfterSend", R.string.MarkReadAfterSend) + " β", AyuConfig.markReadAfterSend, true);
                    } else if (position == useScheduledMessagesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("UseScheduledMessages", R.string.UseScheduledMessages) + " β", AyuConfig.useScheduledMessages, true);
                    } else if (position == keepDeletedMessagesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("KeepDeletedMessages", R.string.KeepDeletedMessages) + " β", AyuConfig.keepDeletedMessages, true);
                    } else if (position == keepMessagesHistoryRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("KeepMessagesHistory", R.string.KeepMessagesHistory) + " β", AyuConfig.keepMessagesHistory, true);
                    } else if (position == showFromChannelRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("ShowFromChannel", R.string.ShowFromChannel), AyuConfig.showFromChannel, true);
                    } else if (position == keepAliveServiceRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("KeepAliveService", R.string.KeepAliveService) + " β", AyuConfig.keepAliveService, true);
                    } else if (position == enableAdsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("EnableAds", R.string.EnableAds), AyuConfig.enableAds, true);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == customizationDividerRow) {
                return 1;
            } else if (position == deletedMarkTextRow || position == cleanDatabaseBtnRow) {
                return 2;
            } else if (
                    position == ghostEssentialsHeaderRow ||
                            position == spyHeaderRow ||
                            position == qolHeaderRow ||
                            position == customizationHeaderRow ||
                            position == debugHeaderRow
            ) {
                return 3;
            }
            return 5;
        }
    }
}
