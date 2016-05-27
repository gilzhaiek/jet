package com.reconinstruments.hudconnectivity.bluetooth;

public interface IHUDBTConsumer {
    public boolean consumeBTData(byte[] header, byte[] payload, byte[] body);
}
