package com.reconinstruments.os.connectivity.bluetooth;

/** {@hide}*/
public interface IHUDBTConsumer {
    public boolean consumeBTData(byte[] header, byte[] payload, byte[] body);
}
