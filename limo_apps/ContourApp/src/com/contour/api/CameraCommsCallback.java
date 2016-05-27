package com.contour.api;

import android.graphics.Bitmap;

public interface CameraCommsCallback {
    public void sendData(BasePacket packet);

    public void onSettngsReceived(CameraSettings settings, int switchPos);
    public void onVideoFrameReceived(Bitmap bitmap);
}