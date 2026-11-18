package org.telegram.messenger.banana;

import org.telegram.tgnet.TLRPC;

/**
 * Send Stickers Anonymously (ported from BananaGram iOS): the sticker is uploaded as a new file
 * without its set, so the recipient can't open the pack it came from.
 */
public final class AnonymousStickers {
    private AnonymousStickers() {}

    /** A copy the server doesn't know (id and access hash 0), so it is uploaded; the sticker attribute loses its set. */
    public static TLRPC.TL_document setlessCopy(TLRPC.Document source, long size, int date) {
        TLRPC.TL_document copy = new TLRPC.TL_document();
        copy.id = 0;
        copy.access_hash = 0;
        copy.file_reference = new byte[0];
        copy.date = date;
        copy.mime_type = source.mime_type;
        copy.size = size;
        copy.dc_id = 0;
        for (TLRPC.DocumentAttribute attribute : source.attributes) {
            if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                TLRPC.TL_documentAttributeSticker sticker = new TLRPC.TL_documentAttributeSticker();
                sticker.alt = attribute.alt != null ? attribute.alt : "";
                sticker.stickerset = new TLRPC.TL_inputStickerSetEmpty();
                copy.attributes.add(sticker);
            } else {
                copy.attributes.add(attribute);
            }
        }
        return copy;
    }
}
