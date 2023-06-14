/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.utils;

import android.util.Pair;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public class AyuGhostUtils {
    public static void markRead(int accountId, int messageId, TLRPC.InputPeer peer) {
        var connectionsManager = ConnectionsManager.getInstance(accountId);

        TLRPC.TL_messages_readHistory request = new TLRPC.TL_messages_readHistory();
        request.peer = peer;
        request.max_id = messageId;

        AyuState.setAllowReadPacket(true, 1);
        connectionsManager.sendRequest(request, (response, error) -> {
        });
    }

    public static Long getDialogId(TLRPC.InputPeer peer) {
        long dialogId;
        if (peer.chat_id != 0) {
            dialogId = -peer.chat_id;
        } else if (peer.channel_id != 0) {
            dialogId = -peer.channel_id;
        } else {
            dialogId = peer.user_id;
        }

        return dialogId;
    }

    public static Long getDialogId(TLRPC.InputChannel peer) {
        return -peer.channel_id;
    }

    public static Pair<Long, Integer> getDialogIdAndMessageIdFromRequest(TLObject req) {
        if (req instanceof TLRPC.TL_messages_readHistory) {
            var readHistory = (TLRPC.TL_messages_readHistory) req;
            var peer = readHistory.peer;
            var maxId = readHistory.max_id;

            var dialogId = getDialogId(peer);

            return new Pair<>(dialogId, maxId);
        } else if (req instanceof TLRPC.TL_messages_readDiscussion) {
            var readDiscussion = (TLRPC.TL_messages_readDiscussion) req;
            var peer = readDiscussion.peer;
            var maxId = readDiscussion.read_max_id;

            var dialogId = getDialogId(peer);

            return new Pair<>(dialogId, maxId);
        } else if (req instanceof TLRPC.TL_channels_readHistory) {
            var readHistory = (TLRPC.TL_channels_readHistory) req;
            var peer = readHistory.channel;
            var maxId = readHistory.max_id;

            var dialogId = getDialogId(peer);

            return new Pair<>(dialogId, maxId);
        }

        // not implemented:
        // - TL_messages_readEncryptedHistory
        // - TL_messages_readMessageContents
        // - TL_channels_readMessageContents

        return null;
    }
}
