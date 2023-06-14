/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.sync;

import com.google.android.exoplayer2.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.radolyn.ayugram.AyuUtils;
import dev.gustavoavila.websocketclient.WebSocketClient;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;

public class AyuSyncWebSocketClient extends WebSocketClient {

    private static AyuSyncWebSocketClient instance;

    public static boolean create() {
        if (instance != null) {
            return true;
        }

        URI url;
        try {
            url = new URI(AyuSyncConfig.getWebSocketURL());
        } catch (URISyntaxException e) {
            return false;
        }

        instance = new AyuSyncWebSocketClient(url);

        instance.setConnectTimeout(5000);
        instance.setReadTimeout(60000);
        instance.addHeader("X-APP-PACKAGE", AyuUtils.getPackageName());
        instance.addHeader("X-DEVICE-IDENTIFIER", AyuUtils.getDeviceIdentifier());
        instance.addHeader("Authorization", AyuSyncConfig.getToken());
        instance.enableAutomaticReconnection(1500);
        instance.connect();

        return true;
    }

    public static AyuSyncWebSocketClient getInstance() {
        if (instance == null) {
            create();
        }

        return instance;
    }

    public static void nullifyInstance() {
        if (instance == null) {
            return;
        }

        AyuSyncState.setConnectionState(AyuSyncConnectionState.Disconnected);

        instance.close(200, 0, "nullified");
        instance = null;
    }

    private AyuSyncWebSocketClient(URI uri) {
        super(uri);
    }

    @Override
    public void send(String message) {
        super.send(message);

        AyuSyncState.setLastSent((int) (System.currentTimeMillis() / 1000));
    }

    @Override
    public void onOpen() {
        AyuSyncState.setConnectionState(AyuSyncConnectionState.Connected);

        Log.d("AyuSync", "Connected to the origin");
    }

    @Override
    public void onTextReceived(String message) {
        AyuSyncState.setLastReceived((int) (System.currentTimeMillis() / 1000));

        var response = new Gson().fromJson(message, JsonObject.class);
        AyuSyncController.getInstance().invokeHandler(response);
    }

    @Override
    public void onBinaryReceived(byte[] data) {
        Log.d("AyuSync", "binary received");
    }

    @Override
    public void onPingReceived(byte[] data) {
//        Log.d("AyuSync", "ping!");
    }

    @Override
    public void onPongReceived(byte[] data) {
//        Log.d("AyuSync", "pong!");
    }

    @Override
    public void onException(Exception e) {
        if (e instanceof SocketException || e instanceof SocketTimeoutException) {
            AyuSyncState.setConnectionState(AyuSyncConnectionState.Disconnected);
        }

        Log.e("AyuSync", e.toString());
    }

    @Override
    public void onCloseReceived(int reason, String description) {
        AyuSyncState.setConnectionState(AyuSyncConnectionState.Disconnected);

        Log.d("AyuSync", "Disconnected from the origin: " + description);
    }
}
