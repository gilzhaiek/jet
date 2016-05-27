package com.contour.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.os.Parcel;
import android.os.Parcelable;

public class CameraStatus implements Parcelable {
    public byte             gpsHdop;
    public byte             batteryPercentage;
    public int              storageCapacity;
    public int              storageKbRemaining;
    public int              cameraState;
    public int              elapsedRecordSeconds;
    public byte             switchPos;

    public static final int CAMERA_STATE_UNKNOWN           = -1;
    public static final int CAMERA_STATE_PREVIEW_PAUSED    = 0;
    public static final int CAMERA_STATE_PREVIEW           = 1;
    public static final int CAMERA_STATE_RECORDING         = 2;
    public static final int CAMERA_STATE_RECORDING_STOPPED = 3;

    public static CameraStatus decodeByteArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        CameraStatus payload = new CameraStatus();
        payload.gpsHdop = buffer.get();
        payload.batteryPercentage = buffer.get();
        payload.storageCapacity = buffer.getInt();
        payload.storageKbRemaining = buffer.getInt();
        payload.cameraState = buffer.getInt();
        payload.elapsedRecordSeconds = buffer.getInt();
        payload.switchPos = buffer.get();
        return payload;
    }
    
    public static CameraStatus decodeByteArrayOld(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        CameraStatus payload = new CameraStatus();
        payload.storageKbRemaining = buffer.getInt()/1024;
        payload.storageCapacity = buffer.getInt()/1024;
        payload.batteryPercentage = (byte) buffer.getInt();
        payload.gpsHdop = buffer.get();
        payload.cameraState = buffer.getInt();
        payload.elapsedRecordSeconds = buffer.getInt();
        payload.switchPos = buffer.get();
        return payload;

    }
    private CameraStatus() {
        
    }

    @Override
    public String toString() {
        return "CameraStatus [storageKbRemaining=" + storageKbRemaining + ",storageCapacity=" + storageCapacity + ", batteryPercentage=" + batteryPercentage + ", gpsOn=" + gpsHdop + ", cameraState=" + cameraState + ", elapsedRecordSeconds=" + elapsedRecordSeconds + ", switchPos=" + this.switchPos + " ]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(gpsHdop);
        dest.writeByte(batteryPercentage);
        dest.writeInt(storageCapacity);
        dest.writeInt(storageKbRemaining);
        dest.writeInt(cameraState);
        dest.writeInt(elapsedRecordSeconds);
        dest.writeByte(switchPos);
    }

    public static final Parcelable.Creator<CameraStatus> CREATOR = new Parcelable.Creator<CameraStatus>() {
                                                                     public CameraStatus createFromParcel(
                                                                             Parcel in) {
                                                                         return new CameraStatus(in);
                                                                     }

                                                                     public CameraStatus[] newArray(
                                                                             int size) {
                                                                         return new CameraStatus[size];
                                                                     }
                                                                 };

    private CameraStatus(Parcel in) {
        gpsHdop = in.readByte();
        batteryPercentage = in.readByte();
        storageCapacity = in.readInt();
        storageKbRemaining = in.readInt();
        cameraState = in.readInt();
        elapsedRecordSeconds = in.readInt();
        switchPos = in.readByte();
    }

}