package com.reconinstruments.externalsensors;
public interface IExternalCadenceListener extends IExternalSensorListener {
    public void onCadenceChanged(int cadence);
}