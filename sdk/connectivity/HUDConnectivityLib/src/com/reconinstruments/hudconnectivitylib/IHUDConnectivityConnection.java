package com.reconinstruments.hudconnectivitylib;

import java.io.IOException;

import android.accounts.NetworkErrorException;
import android.os.RemoteException;

import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;

public interface IHUDConnectivityConnection {
    public void start() throws IOException, InterruptedException;

    public void DEBUG_ONLY_stop() throws IOException;

    public void connect(String address) throws IOException, RemoteException;

    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws IOException, NetworkErrorException, RemoteException;

    public boolean hasWebConnection() throws RemoteException;
}
