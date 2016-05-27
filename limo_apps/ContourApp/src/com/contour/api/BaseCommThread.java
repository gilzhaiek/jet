package com.contour.api;

public interface BaseCommThread {
    public void cancel();
    public void write(byte[] bytes);
}
