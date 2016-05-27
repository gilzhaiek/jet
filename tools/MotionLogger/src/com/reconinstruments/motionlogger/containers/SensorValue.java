package com.reconinstruments.motionlogger.containers;

import android.util.Log;

// A Container for Raw Sensor Values
public class SensorValue {
    public double timestamp;

    /** Motion and Orientation Data.
     *
     * Use setters and getters to read these value if you want to keep track of
     * whether the values have been newly set after getting.
     *
     */
    public float accX, accY, accZ;
    public float linearAccX, linearAccY, linearAccZ;
    public float gyroX, gyroY, gyroZ;
    public float magX, magY, magZ, pressure;
    public float pitch, roll, yaw;
    public int pmic_temp,xm_temp;
    public int movementState;

    private boolean hasNewAcc;
    private boolean hasNewLinearAcc;
    private boolean hasNewGyr;
    private boolean hasNewMag;
    private boolean hasNewPres;
    private boolean hasNewGps;

    // GPS Data
    public double longitude, latitude, altitude;
    public float speed, accuracy;

    // Recon Data
    public double reconAltitude;

    /**
     * Used to create a clone of this object, retaining all values including "hasNew" booleans.
     * The original caller instance will reset "hasNew" booleans of itself to false.
     *
     * @return A newly allocated copy of this object
     */
    synchronized public SensorValue clone() {
        SensorValue cloned = new SensorValue();

        cloned.timestamp = timestamp;
        cloned.accX = accX;
        cloned.accY = accY;
        cloned.accZ = accZ;
        cloned.linearAccX = linearAccX;
        cloned.linearAccY = linearAccY;
        cloned.linearAccZ = linearAccZ;
        cloned.gyroX = gyroX;
        cloned.gyroY = gyroY;
        cloned.gyroZ = gyroZ;
        cloned.magX = magX;
        cloned.magY = magY;
        cloned.magZ = magZ;
        cloned.longitude = longitude;
        cloned.latitude = latitude;
        cloned.altitude = altitude;
        cloned.pressure = pressure;

        cloned.speed = speed;
        cloned.accuracy = accuracy;

        cloned.yaw = yaw;
        cloned.pitch = pitch;
        cloned.roll = roll;

        cloned.pmic_temp = pmic_temp;
        cloned.xm_temp = xm_temp;

        cloned.movementState = movementState;

        cloned.hasNewAcc = hasNewAcc;
        cloned.hasNewLinearAcc = hasNewLinearAcc;
        cloned.hasNewGyr = hasNewGyr;
        cloned.hasNewMag = hasNewMag;
        cloned.hasNewPres = hasNewPres;
        cloned.hasNewGps = hasNewGps;

        return cloned;
    }

    synchronized public void setRead() {
        hasNewAcc = false;
        hasNewGyr = false;
        hasNewMag = false;
        hasNewPres = false;
    }

    synchronized public void setReadGps() {
        hasNewGps = false;
    }

    synchronized public void setAcc(float accX, float accY, float accZ) {
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.hasNewAcc = true;
    }

    synchronized public void setLinearAcc(float linearAccX, float linearAccY, float linearAccZ) {
        this.linearAccX = linearAccX;
        this.linearAccY = linearAccY;
        this.linearAccZ = linearAccZ;
        this.hasNewLinearAcc = true;
    }

    synchronized public void setGyr(float gyrX, float gyrY, float gyrZ) {
        this.gyroX = gyrX;
        this.gyroY = gyrY;
        this.gyroZ = gyrZ;
        this.hasNewGyr = true;
    }

    synchronized public void setMag(float magX, float magY, float magZ) {
        this.magX = magX;
        this.magY = magY;
        this.magZ = magZ;
        this.hasNewMag = true;
    }

    synchronized public void setPressure(float pressure) {
        this.pressure = pressure;
        this.hasNewPres = true;
    }

    synchronized public void setGps(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.hasNewGps = true;
    }

    /**
     * Get orientation data
     * @return [yaw, pitch, roll]
     */
    public float[] getOrientation() {
        float orientation[] = new float[3];
        orientation[0] = this.yaw;
        orientation[1] = this.pitch;
        orientation[2] = this.roll;
        return orientation;
    }

    public float[] getAcc() {
        float acc[] = new float[3];

        if (hasNewAcc) {
            acc[0] = accX;
            acc[1] = accY;
            acc[2] = accZ;
            hasNewAcc = false;
        } else {
            acc[0] = 0;
            acc[1] = 0;
            acc[2] = 0;
        }
        return acc;
    }

    public float[] getLinearAcc() {
        float linearAcc[] = new float[3];

        if (hasNewLinearAcc) {
            linearAcc[0] = linearAccX;
            linearAcc[1] = linearAccY;
            linearAcc[2] = linearAccZ;
            hasNewLinearAcc = false;
        } else {
            linearAcc[0] = 0;
            linearAcc[1] = 0;
            linearAcc[2] = 0;
        }
        return linearAcc;
    }

    public float[] peekAcc() {
        float acc[] = new float[3];

        acc[0] = accX;
        acc[1] = accY;
        acc[2] = accZ;

        return acc;
    }

    public float[] peekGyr() {
        float gyr[] = new float[3];

        gyr[0] = gyroX;
        gyr[1] = gyroY;
        gyr[2] = gyroZ;

        return gyr;

    }

    public float[] peekMag() {
        float mag[] = new float[3];

        mag[0] = magX;
        mag[1] = magY;
        mag[2] = magZ;

        return mag;

    }

    public float[] peekLinearAcc() {
        float linearAcc[] = new float[3];

        linearAcc[0] = linearAccX;
        linearAcc[1] = linearAccY;
        linearAcc[2] = linearAccZ;

        return linearAcc;
    }

    public float[] getGyr() {
        float gyro[] = new float[3];

        if (hasNewGyr) {
            gyro[0] = gyroX;
            gyro[1] = gyroY;
            gyro[2] = gyroZ;
            hasNewGyr = false;
        } else {
            gyro[0] = 0;
            gyro[1] = 0;
            gyro[2] = 0;
        }
        return gyro;
    }

    public float[] getMag() {
        float mag[] = new float[3];

        if (hasNewMag) {
            mag[0] = magX;
            mag[1] = magY;
            mag[2] = magZ;
            hasNewMag = false;
        } else {
            mag[0] = 0;
            mag[1] = 0;
            mag[2] = 0;
        }
        return mag;
    }

    public float getPres() {
        if (hasNewPres) {
            hasNewPres = false;
            return this.pressure;
        } else {
            return 0;
        }
    }

    /**
     *
     * @return lat, long, alt, accuracy
     */
    public double[] getGpsData() {
        double latLong[] = new double[4];

        if (hasNewGps) {
            latLong[0] = this.latitude;
            latLong[1] = this.longitude;
            latLong[2] = this.altitude;
            latLong[3] = this.accuracy;
        }

        return latLong;
    }
}