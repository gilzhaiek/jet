package com.reconinstruments.hudconnectivitylib.bluetooth;

import java.io.IOException;

import com.reconinstruments.hudconnectivitylib.IHUDConnectivity.ConnectionState;
import com.reconinstruments.hudconnectivitylib.bluetooth.HUDBTBaseService.OutputStreamContainer;

public interface IHUDBTService {
    public void addConsumer(IHUDBTConsumer hudBTConsumer);

    public void removeConsumer(IHUDBTConsumer hudBTConsumer);

    public ConnectionState getState();

    public String getDeviceName();

    public void connect(String address) throws IOException;

    public void startListening() throws IOException;

    public void stopListening() throws IOException;

    public void write(OutputStreamContainer osContainer, byte[] buffer) throws Exception;

    public OutputStreamContainer obtainOutputStreamContainer() throws InterruptedException;

    public void releaseOutputStreamContainer(OutputStreamContainer osContainer);
}
