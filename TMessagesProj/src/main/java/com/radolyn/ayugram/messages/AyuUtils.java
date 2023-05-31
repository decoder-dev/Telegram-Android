package com.radolyn.ayugram.messages;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

public class AyuUtils {
    public static void markRead(MessagesController messagesController, ConnectionsManager connectionsManager, MessageObject message) {
        TLRPC.TL_messages_readHistory request = new TLRPC.TL_messages_readHistory();
        request.peer = messagesController.getInputPeer(message.messageOwner.peer_id);
        request.max_id = message.getId();

        AyuState.setAllowReadPacket();
        connectionsManager.sendRequest(request, (response, error) -> {});
    }
}
