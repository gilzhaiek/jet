package com.contour.api;

public interface BaseCmdPacket {

    public short getCmd();
    public byte[] getData();
    public int getParam();
    public int getPacketId();
}
