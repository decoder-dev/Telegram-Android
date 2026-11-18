package org.telegram.messenger.banana;

import com.google.gson.GsonBuilder;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/** A message as readable JSON with Bot API names (ported from BananaGram iOS "Message JSON"). */
public final class MessageJson {
    private MessageJson() {}

    static long botApiId(TLRPC.Peer peer) {
        if (peer instanceof TLRPC.TL_peerChannel) return -(1000000000000L + peer.channel_id);
        if (peer instanceof TLRPC.TL_peerChat) return -peer.chat_id;
        return peer != null ? peer.user_id : 0;
    }

    static Map<String, Object> peer(TLRPC.Peer peer) {
        Map<String, Object> map = new TreeMap<>();
        map.put("id", botApiId(peer));
        map.put("type", peer instanceof TLRPC.TL_peerChannel ? "channel" : peer instanceof TLRPC.TL_peerChat ? "group" : "user");
        return map;
    }

    /** "TL_messageEntityTextUrl" becomes "text_url", the Bot API spelling. */
    static String entityType(TLRPC.MessageEntity entity) {
        String name = entity.getClass().getSimpleName().replace("TL_messageEntity", "");
        int layer = name.indexOf("_layer");
        if (layer > 0) name = name.substring(0, layer);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && result.length() > 0) result.append('_');
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    public static String toJson(TLRPC.Message message) {
        Map<String, Object> json = new TreeMap<>();
        json.put("id", message.id);
        json.put("date", message.date);
        json.put("text", message.message != null ? message.message : "");
        json.put("out", message.out);
        if (message.peer_id != null) json.put("chat", peer(message.peer_id));
        if (message.from_id != null) json.put("from", peer(message.from_id));
        if (message.edit_date != 0) json.put("edit_date", message.edit_date);
        if (message.views != 0) json.put("views", message.views);
        if (message.forwards != 0) json.put("forwards", message.forwards);
        if (message.grouped_id != 0) json.put("media_group_id", Long.toString(message.grouped_id));
        if (message.post_author != null) json.put("author_signature", message.post_author);
        if (message.via_bot_id != 0) json.put("via_bot_id", message.via_bot_id);
        if (message.ttl_period != 0) json.put("ttl_period", message.ttl_period);
        if (message.reply_to != null) {
            Map<String, Object> reply = new TreeMap<>();
            reply.put("message_id", message.reply_to.reply_to_msg_id);
            if (message.reply_to.reply_to_peer_id != null) reply.put("chat", peer(message.reply_to.reply_to_peer_id));
            if (message.reply_to.quote_text != null) reply.put("quote", message.reply_to.quote_text);
            json.put("reply_to", reply);
        }
        if (message.fwd_from != null) {
            Map<String, Object> forward = new TreeMap<>();
            forward.put("date", message.fwd_from.date);
            if (message.fwd_from.from_id != null) forward.put("from", peer(message.fwd_from.from_id));
            if (message.fwd_from.from_name != null) forward.put("from_name", message.fwd_from.from_name);
            if (message.fwd_from.channel_post != 0) forward.put("message_id", message.fwd_from.channel_post);
            if (message.fwd_from.post_author != null) forward.put("signature", message.fwd_from.post_author);
            if (message.fwd_from.psa_type != null) forward.put("psa_type", message.fwd_from.psa_type);
            json.put("forward", forward);
        }
        if (message.entities != null && !message.entities.isEmpty()) {
            ArrayList<Object> list = new ArrayList<>();
            for (TLRPC.MessageEntity entity : message.entities) {
                if (entity == null) continue;
                Map<String, Object> item = new TreeMap<>();
                item.put("type", entityType(entity));
                item.put("offset", entity.offset);
                item.put("length", entity.length);
                if (entity.url != null && !entity.url.isEmpty()) item.put("url", entity.url);
                if (entity.language != null && !entity.language.isEmpty()) item.put("language", entity.language);
                list.add(item);
            }
            json.put("entities", list);
        }
        if (message.media != null) {
            Map<String, Object> media = new TreeMap<>();
            media.put("type", message.media.getClass().getSimpleName());
            if (message.media.document != null) {
                media.put("id", Long.toString(message.media.document.id));
                if (message.media.document.mime_type != null) media.put("mime_type", message.media.document.mime_type);
                media.put("file_size", message.media.document.size);
            } else if (message.media.photo != null) {
                media.put("id", Long.toString(message.media.photo.id));
            }
            if (message.media.geo != null) {
                media.put("latitude", message.media.geo.lat);
                media.put("longitude", message.media.geo._long);
            }
            json.put("media", media);
        }
        if (message.action != null) json.put("action", message.action.getClass().getSimpleName());
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeSpecialFloatingPointValues().create().toJson(json);
    }
}
