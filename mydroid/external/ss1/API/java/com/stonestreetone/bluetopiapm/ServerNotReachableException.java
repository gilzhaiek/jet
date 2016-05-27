package com.stonestreetone.bluetopiapm;

/**
 * Thrown when the BluetopiaPM client library cannot find or connect to the
 * BluetopiaPM server process.
 */
public class ServerNotReachableException extends BluetopiaPMException {

    public ServerNotReachableException() {
        super("BluetopiaPM server is not running or cannot be found.");
    }

    public ServerNotReachableException(Throwable throwable) {
        super("BluetopiaPM server is not running or cannot be found.", throwable);
    }
}
