package com.reconinstruments.externalsensors;
public interface IExternalBikePowerListener extends IExternalSensorListener {
    public void onPowerChanged(int power);
}