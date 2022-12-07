package com.aj.videplayertest;

import android.graphics.Bitmap;

public interface OnClientConnected {
    void onMessageRecived();
    void onClientConnected();
    void onClientDisconnected(String message);
    void onFrameRecieved(Bitmap frame);
    void onClientError(String message);
    void updateFPS(String message);


}
