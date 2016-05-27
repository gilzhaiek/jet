package com.reconinstruments.hudconnectivitylib.bluetooth;


public interface IHUDBTConsumer {
    public boolean consumeBTData(byte[] header, byte[] payload, byte[] body);
}
