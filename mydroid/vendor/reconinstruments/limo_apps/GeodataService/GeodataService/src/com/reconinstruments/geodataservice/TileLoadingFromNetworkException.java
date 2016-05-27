package com.reconinstruments.geodataservice;

public class TileLoadingFromNetworkException extends Exception {

    public TileLoadingFromNetworkException(String message) {
        super(message);
    }

    public TileLoadingFromNetworkException(String message, Throwable throwable) {
        super(message, throwable);
    }

}