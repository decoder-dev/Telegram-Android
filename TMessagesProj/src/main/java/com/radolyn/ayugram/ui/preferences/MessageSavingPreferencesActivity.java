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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;

public class MessageSavingPreferencesActivity extends BasePreferencesActivity {

    private int mediaHeaderRow;
    private int saveMediaRow;
    private int saveMediaInPrivateChatsRow;
    private int saveMediaInPublicChannelsRow;
    private int saveMediaInPrivateChannelsRow;
    private int saveMediaInPublicGroupsRow;
    private int saveMediaInPrivateGroupsRow;
    private int mediaDividerRow;

    private int otherHeaderRow;
    private int saveFormatting;
    private int saveReactions;
    private int saveForBots;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        mediaHeaderRow = newRow();
        saveMediaRow = newRow();
        saveMediaInPrivateChatsRow = newRow();
        saveMediaInPublicChannelsRow = newRow();
        saveMediaInPrivateChannelsRow = newRow();
        saveMediaInPublicGroupsRow = newRow();
        saveMediaInPrivateGroupsRow = newRow();
        mediaDividerRow = newRow();

        otherHeaderRow = newRow();
        saveFormatting = newRow();
        saveReactions = newRow();
        saveForBots = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == saveMediaRow) {
            AyuConfig.editor.putBoolean("saveMedia", AyuConfig.saveMedia ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveMedia);

            listAdapter.notifyItemRangeChanged(saveMediaInPrivateChatsRow, saveMediaInPrivateGroupsRow - saveMediaInPrivateChatsRow + 1);
        } else if (position == saveMediaInPrivateChatsRow) {
            if (!AyuConfig.saveMedia) {
                return;
            }

            AyuConfig.editor.putBoolean("saveMediaInPrivateChats", AyuConfig.saveMediaInPrivateChats ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveMediaInPrivateChats);
        } else if (position == saveMediaInPublicChannelsRow) {
            if (!AyuConfig.saveMedia) {
                return;
            }

            AyuConfig.editor.putBoolean("saveMediaInPublicChannels", AyuConfig.saveMediaInPublicChannels ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveMediaInPublicChannels);
        } else if (position == saveMediaInPrivateChannelsRow) {
            if (!AyuConfig.saveMedia) {
                return;
            }

            AyuConfig.editor.putBoolean("saveMediaInPrivateChannels", AyuConfig.saveMediaInPrivateChannels ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveMediaInPrivateChannels);
        } else if (position == saveMediaInPublicGroupsRow) {
            if (!AyuConfig.saveMedia) {
                return;
            }

            AyuConfig.editor.putBoolean("saveMediaInPublicGroups", AyuConfig.saveMediaInPublicGroups ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveMediaInPublicGroups);
        } else if (position == saveMediaInPrivateGroupsRow) {
            if (!AyuConfig.saveMedia) {
                return;
            }

            AyuConfig.editor.putBoolean("saveMediaInPrivateGroups", AyuConfig.saveMediaInPrivateGroups ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveMediaInPrivateGroups);
        } else if (position == saveFormatting) {
            AyuConfig.editor.putBoolean("saveFormatting", AyuConfig.saveFormatting ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveFormatting);
        } else if (position == saveReactions) {
            AyuConfig.editor.putBoolean("saveReactions", AyuConfig.saveReactions ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveReactions);
        } else if (position == saveForBots) {
            AyuConfig.editor.putBoolean("saveForBots", AyuConfig.saveForBots ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveForBots);
        }
    }

    @Override
    public void onResume() {
        updateRowsId();

        super.onResume();
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.MessageSavingBtn);
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
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == mediaHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MessageSavingMediaHeader));
                    } else if (position == otherHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MessageSavingOtherHeader));
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    if (position == saveMediaRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMedia), AyuConfig.saveMedia, true);
                        textCheckCell.setEnabled(true, null);
                    } else if (position == saveMediaInPrivateChatsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateChats), AyuConfig.saveMediaInPrivateChats, true);
                        textCheckCell.setEnabled(AyuConfig.saveMedia, null);
                    } else if (position == saveMediaInPublicChannelsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPublicChannels), AyuConfig.saveMediaInPublicChannels, true);
                        textCheckCell.setEnabled(AyuConfig.saveMedia, null);
                    } else if (position == saveMediaInPrivateChannelsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateChannels), AyuConfig.saveMediaInPrivateChannels, true);
                        textCheckCell.setEnabled(AyuConfig.saveMedia, null);
                    } else if (position == saveMediaInPublicGroupsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPublicGroups), AyuConfig.saveMediaInPublicGroups, true);
                        textCheckCell.setEnabled(AyuConfig.saveMedia, null);
                    } else if (position == saveMediaInPrivateGroupsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateGroups), AyuConfig.saveMediaInPrivateGroups, true);
                        textCheckCell.setEnabled(AyuConfig.saveMedia, null);
                    } else if (position == saveFormatting) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveFormatting), AyuConfig.saveFormatting, true);
                        textCheckCell.setEnabled(true, null);
                    } else if (position == saveReactions) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveReactions), AyuConfig.saveReactions, true);
                        textCheckCell.setEnabled(true, null);
                    } else if (position == saveForBots) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MessageSavingSaveForBots), AyuConfig.saveForBots, true);
                        textCheckCell.setEnabled(true, null);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (
                    position == mediaDividerRow
            ) {
                return 1;
            } else if (
                    position == mediaHeaderRow ||
                            position == otherHeaderRow
            ) {
                return 3;
            }
            return 5;
        }
    }
}
