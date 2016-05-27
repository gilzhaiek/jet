package com.contour.api;

public interface BasePacketHeader {
    public byte getType();
    public int getLength();
    public short getChecksum();
    public boolean isValid();
}
