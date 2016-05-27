package com.reconinstruments.os.metrics;

public interface ListenerChangedHandler {

    public final static int STATUS_ENABLE_SERVICE = 1;
    public final static int STATUS_DISABLE_SERVICE = 2;

    public void onServiceStatusChange(int status);

}
