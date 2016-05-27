package com.reconinstruments.externalsensors;
public interface IExternalBikeSpeedListener extends IExternalSensorListener {
    public void onBikeSpeedChanged(int speed);
}