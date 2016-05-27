package com.reconinstruments.voltagelogger;

public interface BattLogListener {
    void onBatteryStatusChanged(BatteryStatus batteryStatus);
}
