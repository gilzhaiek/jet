package com.contour.api;

public interface CommThreadCallback {
    public void onCameraCommStarted(boolean useOldProtocol);

    public void onCameraCommDisconnected();

    public void onReceivedPacket(BasePacket packet);

}