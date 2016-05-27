package com.reconinstruments.motionlogger.utils;

import android.os.Message;
import android.util.Log;

import com.reconinstruments.motionlogger.containers.SensorValue;

public class RawDataLogger extends AbstractThreadedLogger {
    enum ColumnIndex {
        Second,
        Acc_X,
        Acc_Y,
        Acc_Z,
        Gyr_X,
        Gyr_Y,
        Gyr_Z,
        Mag_X,
        Mag_Y,
        Mag_Z,
        Latitude,
        Longitude,
        q0,
        q1,
        q2,
        Altitude,
        Vel_X,
        Pressure,
        HDOP,
        TEMP,
        XM_TEMP,
        MOVEMENT_STATE,
    }

    public boolean mWritingBipedal;

    public void isWritingBipedal(boolean state) {
        mWritingBipedal = state;
    }

    @Override
    protected void setFilePostfix() {
        if (mWritingBipedal) {
            mFilePostfix = "-BIPEDAL";
        }
        else {
            mFilePostfix = "-BICYCLE";
        }
    }

    @Override
    protected void printColumnTitles() {
        StringBuilder title = new StringBuilder();

        for (ColumnIndex c : ColumnIndex.values())
            title.append(c + ",");

        title.deleteCharAt(title.length() - 1);

        mPrinter.println(title);
    }

    @Override
    protected void write(Message msg) {
        SensorValue sensorValues = (SensorValue) ((Object[]) msg.obj)[0];

        String[] rowBuilder = new String[ColumnIndex.values().length];

        // Raw Results
        rowBuilder[ColumnIndex.Second.ordinal()] = (sensorValues.timestamp) + ",";

        float acc[] = sensorValues.getAcc();
        rowBuilder[ColumnIndex.Acc_X.ordinal()] = acc[0] + ",";
        rowBuilder[ColumnIndex.Acc_Y.ordinal()] = acc[1] + ",";
        rowBuilder[ColumnIndex.Acc_Z.ordinal()] = acc[2] + ",";

        float gyr[] = sensorValues.getGyr();
        rowBuilder[ColumnIndex.Gyr_X.ordinal()] = gyr[0] + ",";
        rowBuilder[ColumnIndex.Gyr_Y.ordinal()] = gyr[1] + ",";
        rowBuilder[ColumnIndex.Gyr_Z.ordinal()] = gyr[2] + ",";

        float mag[] = sensorValues.getMag();
        rowBuilder[ColumnIndex.Mag_X.ordinal()] = mag[0] + ",";
        rowBuilder[ColumnIndex.Mag_Y.ordinal()] = mag[1] + ",";
        rowBuilder[ColumnIndex.Mag_Z.ordinal()] = mag[2] + ",";

        rowBuilder[ColumnIndex.Latitude.ordinal()] = sensorValues.latitude + ",";
        rowBuilder[ColumnIndex.Longitude.ordinal()] = sensorValues.longitude + ",";

        rowBuilder[ColumnIndex.q0.ordinal()] = sensorValues.yaw + ",";
        rowBuilder[ColumnIndex.q1.ordinal()] = sensorValues.pitch + ",";
        rowBuilder[ColumnIndex.q2.ordinal()] = sensorValues.roll + ",";

        rowBuilder[ColumnIndex.Altitude.ordinal()] = sensorValues.altitude + ",";

        rowBuilder[ColumnIndex.Vel_X.ordinal()] = sensorValues.speed + ",";

        rowBuilder[ColumnIndex.Pressure.ordinal()] = sensorValues.getPres() + ",";

        rowBuilder[ColumnIndex.HDOP.ordinal()] = sensorValues.accuracy + ",";

        rowBuilder[ColumnIndex.TEMP.ordinal()] = sensorValues.pmic_temp + ",";
        rowBuilder[ColumnIndex.XM_TEMP.ordinal()] = sensorValues.xm_temp + ",";

        rowBuilder[ColumnIndex.MOVEMENT_STATE.ordinal()] = sensorValues.movementState + ",";
        StringBuilder row = new StringBuilder();
        for (String s : rowBuilder) {
            row.append(s);
        }

        row.deleteCharAt(row.length() - 1);

        mPrinter.println(row);
    }
}