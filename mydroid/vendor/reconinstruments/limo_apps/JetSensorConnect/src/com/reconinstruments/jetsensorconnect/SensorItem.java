
package com.reconinstruments.jetsensorconnect;

import com.reconinstruments.antplus.AntPlusSensor;

public class SensorItem {

    public String title = null;
    public String subTitle = null;
    public Integer subIconId = null;
    public AntPlusSensor sensor = null;
    public boolean combinedSpeed = false; // is power combined speed
    public boolean combinedCadence = false; // is power combined speed
    
    public SensorItem(String title, AntPlusSensor sensor) {
        this.title = title;
        this.sensor = sensor;
    }

    // title with subTitle
    public SensorItem(String title, String subTitle, AntPlusSensor sensor) {
        this.title = title;
        this.subTitle = subTitle;
        this.sensor = sensor;
    }

    // title with subIcon
    public SensorItem(String title, int subIconId, AntPlusSensor sensor) {
        this.title = title;
        this.subIconId = subIconId;
        this.sensor = sensor;
    }

    // title with subTitle
    public SensorItem(String title, String subTitle, AntPlusSensor sensor, boolean combinedSpeed, boolean combinedCadence) {
        this.title = title;
        this.subTitle = subTitle;
        this.combinedSpeed = combinedSpeed;
        this.combinedCadence = combinedCadence;
        this.sensor = sensor;
    }
}
