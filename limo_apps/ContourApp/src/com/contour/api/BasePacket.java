package com.contour.api;

import java.nio.ByteBuffer;

public interface BasePacket {
    public void addToByteBuffer(ByteBuffer buffer);
    public byte[] toByteArray();
    public byte getHeaderType();
}
