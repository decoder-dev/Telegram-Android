package com.radolyn.ayugram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.messages.AyuFileLocation;
import com.radolyn.ayugram.messages.AyuMessagesController;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AyuMessageHistory extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private final List<EditedMessage> messages;
    private final int rowCount;
    private RecyclerListView listView;

    public AyuMessageHistory(long userId, MessageObject messageObject) {
        var messagesController = AyuMessagesController.getInstance();
        messages = messagesController.getRevisions(userId, messageObject.messageOwner.dialog_id, messageObject.messageOwner.id);
        rowCount = messages.size();
    }

    @Override
    public View createView(Context context) {
        var firstMsg = messages.get(0);
        var peer = getAccountInstance().getMessagesController().getUserOrChat(firstMsg.dialogId);
        // todo: check sender of the message

        String name;
        if (peer == null) {
            name = "?"; // wtf
        } else if (peer instanceof TLRPC.User) {
            name = ((TLRPC.User) peer).first_name;
        } else if (peer instanceof TLRPC.Chat) {
            name = ((TLRPC.Chat) peer).title;
        } else {
            name = LocaleController.getString("EditsHistoryTitle", R.string.EditsHistoryTitle);
        }

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(name);
        actionBar.setSubtitle(String.valueOf(firstMsg.messageId));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        LinearLayoutManager layoutManager;
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        ListAdapter adapter;
        listView.setAdapter(adapter = new AyuMessageHistory.ListAdapter(context));

        return fragmentView;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return new ArrayList<>();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        // todo: update list in real time
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) {
                view = new AyuMessageCell(context, getParentActivity(), AyuMessageHistory.this);
            } else {
                view = null;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 1) {
                var ayuMessageDetailCell = (AyuMessageCell) holder.itemView;

                var editedMessage = messages.get(position);

                var msg = createMessageObject(editedMessage);

                ayuMessageDetailCell.setMessageObject(msg, null, false, false);
                ayuMessageDetailCell.setEditedMessage(editedMessage);
                ayuMessageDetailCell.setId(position);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position >= 0 && position < messages.size() ? 1 : 0;
        }

        private MessageObject createMessageObject(EditedMessage editedMessage) {
            // shamefully copied from Extera's sticker size preview
            var msg = new TLRPC.TL_message();
            msg.message = editedMessage.text;
            msg.date = (int) editedMessage.date;
            msg.dialog_id = -1;
            msg.flags = 512;
            msg.id = Utilities.random.nextInt();
            msg.out = false;
            msg.peer_id = new TLRPC.TL_peerUser();
            msg.peer_id.user_id = 1;

            if (editedMessage.path != null) {
                msg.attachPath = editedMessage.path;
                var file = new File(editedMessage.path);

                if (!editedMessage.isDocument) {
                    msg.media = new TLRPC.TL_messageMediaPhoto();
                    msg.media.flags |= 3;
                    msg.media.photo = new TLRPC.TL_photo();
                    msg.media.photo.file_reference = new byte[]{
                            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
                    };
                    msg.media.photo.has_stickers = false;
                    msg.media.photo.id = Utilities.random.nextInt();
                    msg.media.photo.access_hash = Utilities.random.nextInt();
                    msg.media.photo.date = (int) editedMessage.date;
                    msg.media.photo.dc_id = 2;
                    msg.media.photo.user_id = 1338;

                    TLRPC.TL_photoSize photoSize = new TLRPC.TL_photoSize();
                    photoSize.size = (int) file.length();
                    photoSize.w = 500;
                    photoSize.h = 302;
                    photoSize.type = "s";
                    photoSize.location = new AyuFileLocation(editedMessage.path);
                    msg.media.photo.sizes.add(photoSize);
                    msg.attachPath = editedMessage.path;
                } else {
                    msg.media = new TLRPC.TL_messageMediaDocument();
                    msg.media.flags |= 1;

                    msg.media.document = new TLRPC.TL_document();
                    msg.media.document.file_reference = new byte[0];
                    msg.media.document.access_hash = 0;
                    msg.media.document.date = (int) editedMessage.date;
                    msg.media.document.localPath = editedMessage.path;
                    msg.media.document.file_name = file.getName();
                    msg.media.document.size = file.length();
                }
            }

            return new MessageObject(getCurrentAccount(), msg, true, true);
        }
    }
}