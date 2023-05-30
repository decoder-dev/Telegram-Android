package com.radolyn.ayugram.messages;

import org.telegram.tgnet.TLRPC;

public class AyuFileLocation extends TLRPC.FileLocation {
    public String path;

    public AyuFileLocation(String path) {
        this.path = path;
    }
}
