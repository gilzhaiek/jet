package com.reconinstruments.externalsensors;
public interface IExternalHeartrateListener extends IExternalSensorListener {
    public void onHeartrateChanged(int hr);
}