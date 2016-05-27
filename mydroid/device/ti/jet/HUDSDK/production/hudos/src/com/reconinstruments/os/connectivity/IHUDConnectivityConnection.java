package com.reconinstruments.os.connectivity;

import java.io.IOException;

import android.accounts.NetworkErrorException;
import android.os.RemoteException;

import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;

/** {@hide} */
public interface IHUDConnectivityConnection {
    public void start() throws IOException, InterruptedException;

    public void stop() throws IOException;

    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws IOException, NetworkErrorException, RemoteException;

    public boolean hasWebConnection() throws RemoteException;
}
