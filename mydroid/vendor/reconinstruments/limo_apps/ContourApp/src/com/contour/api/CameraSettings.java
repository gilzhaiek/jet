package com.contour.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.contour.api.CameraProtocolOld.CameraSettingsOld;
import com.contour.connect.debug.CLog;

public class CameraSettings implements Parcelable, Cloneable {
    private final static int LENGTH           = 96;
    private final static int AGPS_DATE_LENGTH = 0;

    public byte              switchPosition   = 1;

    private boolean          gps1;
    private boolean          beep1;
    private int              videoRes1;            // 0-4
    private int              quality1;             // 0-2
    private int              meteringMode1;        // 0-2
    private int              contrast1;            // 1-255
    private int              exposure1;            // 0-8
    private int              sharpness1;           // 1-5
    private int              mic1;                 // 0-59
    private int              photoMode1;           // > 0
    private int              videoFormat1;         // 0-1

    private boolean          gps2;
    private boolean          beep2;
    private int              videoRes2;            // 0-4
    private int              quality2;             // 0-2
    private int              meteringMode2;        // 0-2
    private int              contrast2;            // 1-255
    private int              exposure2;            // 0-8
    private int              sharpness2;           // 1-5
    private int              mic2;                 // 0-59
    private int              photoMode2;           // > 0
    private int              videoFormat2;         // 0-1

    // Contour+ Settings
    private byte             gpsRate1;
    public byte              extMic1;
    private byte             whiteBalance1;

    private byte             gpsRate2;
    public byte              extMic2;
    private byte             whiteBalance2;

    public byte              cameraModel;

    public byte              majorVersion;
    public byte              minorVersion;
    public byte              buildVersion;

    public byte[]                    agpsDate = new byte[AGPS_DATE_LENGTH];


    public static class VideoRes {
        public static final int CTRVideoRes1920x1080fps30  = 0;
        public static final int CTRVideoRes1280x960fps30   = 1;
        public static final int CTRVideoRes1280x720fps30   = 2;
        public static final int CTRVideoRes1280x720fps60   = 3;
        public static final int CTRVideoResContinuousPhoto = 4;
        public static final int CTRVideoRes848x480fps30    = 5;
        public static final int CTRVideoRes848x480fps60    = 6;
        public static final int CTRVideoRes848x480fps120   = 7;
    };
    
    public static CameraSettings defaultSettings(int cameraModel) {
        if(cameraModel == BaseCameraComms.MODEL_GPS) {
            return defaultSettingsGps();
        } else if(cameraModel == BaseCameraComms.MODEL_PLUS) {
            return defaultSettingsPlus();
        } else if(cameraModel == BaseCameraComms.MODEL_PLUS_2) {
            return defaultSettingsPlusTwo();
        } else {
            Log.e(CLog.TAG, "BAD CAMERA MODEL TO APPLY DEFAULT SETTINGS " + cameraModel);
            return defaultSettingsPlusTwo();
        }
    }

    public static CameraSettings defaultSettingsPlus() {
        CameraSettings settings = new CameraSettings();
        settings.gps1 = true;
        settings.beep1 = true;
        settings.videoRes1 = 3;
        settings.quality1 = 2;
        settings.meteringMode1 = 0;
        settings.contrast1 = 62;
        settings.exposure1 = 0;
        settings.sharpness1 = 3;
        settings.mic1 = 33;
        settings.photoMode1 = 3;
        settings.videoFormat1 = 1;

        settings.gps2 = true;
        settings.beep2 = true;
        settings.videoRes2 = 0;
        settings.quality2 = 2;
        settings.meteringMode2 = 0;
        settings.contrast2 = 62;
        settings.exposure2 = 0;
        settings.sharpness2 = 3;
        settings.mic2 = 33;
        settings.photoMode2 = 3;
        settings.videoFormat2 = 1;

        settings.gpsRate1 = 1;
        settings.extMic1 = 40;
        settings.whiteBalance1 = 0;
        settings.gpsRate2 = 1;
        settings.extMic2 = 40;
        settings.whiteBalance2 = 0;

        settings.cameraModel = BaseCameraComms.MODEL_PLUS;
        settings.majorVersion = 0;
        settings.minorVersion = 0;
        settings.buildVersion = 0;
        return settings;
    }
    
    public static CameraSettings defaultSettingsPlusTwo() {
        CameraSettings settings = defaultSettingsPlus();
        settings.cameraModel = BaseCameraComms.MODEL_PLUS_2;
        settings.extMic2 = 30;
        settings.extMic1 = 30;
        settings.mic2 = 45;
        settings.mic1 = 45;
        return settings;
    }
    
    public static CameraSettings defaultSettingsGps() {
        CameraSettings settings = defaultSettingsPlus();
        settings.videoRes1 = 0;
        settings.videoRes2 = 2;
        settings.cameraModel = BaseCameraComms.MODEL_GPS;
        return settings;
    }

    
    public CameraSettingsOld convertToOld(int switchPos) {
        CameraSettingsOld s = new CameraSettingsOld();
        if (switchPos == 1) {
            s.switchPosition = 0;
            s.gpsOn = gps1;
            s.beepOn = beep1;
            s.videoMode = (byte) videoRes1;
            s.videoQuality = (byte) quality1;
            s.meteringMode = (byte) meteringMode1;
            s.contrast = (byte) contrast1;
            s.exposure = (byte) exposure1;
            s.sharpness = (byte) sharpness1;
            s.micGain = (byte) mic1;
            s.photoModeInterval = (byte)photoMode1;
            s.videoRefresh = (byte) videoFormat1;

            s.gpsRate = gpsRate1;
            s.extMicGain = extMic1;
            s.whiteBalance = whiteBalance1;
        } else {
            s.switchPosition = 1;
            s.gpsOn = gps2;
            s.beepOn = beep2;
            s.videoMode = (byte) videoRes2;
            s.videoQuality = (byte) quality2;
            s.meteringMode = (byte) meteringMode2;
            s.contrast = (byte) contrast2;
            s.exposure = (byte) exposure2;
            s.sharpness = (byte) sharpness2;
            s.micGain = (byte) mic2;
            s.photoModeInterval = (byte)photoMode2;
            s.videoRefresh = (byte) videoFormat2;

            s.gpsRate = gpsRate2;
            s.extMicGain = extMic2;
            s.whiteBalance = whiteBalance2;
        }
        
        s.cameraModel = cameraModel;
        s.majorVersion = majorVersion;
        s.minorVersion = minorVersion;
        s.buildVersion = buildVersion;
        
//        s.agpsDate = agpsDate;
        return s;
    }
    public static CameraSettings convertFromOld(CameraSettingsOld s1, CameraSettingsOld s2, byte switchPos) {
        CameraSettings settings = new CameraSettings();
        settings.switchPosition = switchPos;
        
        settings.gps1 = s1.gpsOn;
        settings.beep1 = s1.beepOn;
        settings.videoRes1 = s1.videoMode;
        settings.quality1 = s1.videoQuality;
        settings.meteringMode1 = s1.meteringMode;
        settings.contrast1 = s1.contrast;
        settings.exposure1 = s1.exposure;
        settings.sharpness1 = s1.sharpness;
        settings.mic1 = s1.micGain;
        settings.photoMode1 = s1.photoModeInterval;
        settings.videoFormat1 = s1.videoRefresh;

        settings.gpsRate1 = s1.gpsRate;
        settings.extMic1 = s1.extMicGain;
        settings.whiteBalance1 =  s1.whiteBalance;
        
        settings.gps2 = s2.gpsOn;
        settings.beep2 = s2.beepOn;
        settings.videoRes2 = s2.videoMode;
        settings.quality2 = s2.videoQuality;
        settings.meteringMode2 = s2.meteringMode;
        settings.contrast2 = s2.contrast;
        settings.exposure2 = s2.exposure;
        settings.sharpness2 = s2.sharpness;
        settings.mic2 = s2.micGain;
        settings.photoMode2 = s2.photoModeInterval;
        settings.videoFormat2 = s2.videoRefresh;

        
        settings.gpsRate2 = s2.gpsRate;
        settings.extMic2 = s2.extMicGain;
        settings.whiteBalance2 =  s2.whiteBalance;
       

        settings.cameraModel = s1.cameraModel;
        settings.majorVersion = s1.majorVersion;
        settings.minorVersion = s1.minorVersion;
        settings.buildVersion = s1.buildVersion;
        
        settings.agpsDate = s1.agpsDate;
        
        return settings;
    }
    
    public static CameraSettings decodeByteArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        CameraSettings settings = new CameraSettings();

        settings.gps1 = buffer.get() > 0;
        settings.beep1 = buffer.get() > 0;
        settings.videoRes1 = buffer.getInt();
        settings.quality1 = buffer.getInt();
        settings.meteringMode1 = buffer.getInt();
        settings.contrast1 = buffer.getInt();
        settings.exposure1 = buffer.getInt();
        settings.sharpness1 = buffer.getInt();
        settings.mic1 = buffer.getInt();
        settings.photoMode1 = buffer.getInt();
        settings.videoFormat1 = buffer.getInt();

        settings.gps2 = buffer.get() > 0;
        settings.beep2 = buffer.get() > 0;
        settings.videoRes2 = buffer.getInt();
        settings.quality2 = buffer.getInt();
        settings.meteringMode2 = buffer.getInt();
        settings.contrast2 = buffer.getInt();
        settings.exposure2 = buffer.getInt();
        settings.sharpness2 = buffer.getInt();
        settings.mic2 = buffer.getInt();
        settings.photoMode2 = buffer.getInt();
        settings.videoFormat2 = buffer.getInt();

        settings.gpsRate1 = buffer.get();
        settings.extMic1 = buffer.get();
        settings.whiteBalance1 = buffer.get();
        settings.gpsRate2 = buffer.get();
        settings.extMic2 = buffer.get();
        settings.whiteBalance2 = buffer.get();

        settings.cameraModel = buffer.get();
        settings.majorVersion = buffer.get();
        settings.minorVersion = buffer.get();
        settings.buildVersion = buffer.get();

        buffer.get(settings.agpsDate);

        return settings;
    }

    private CameraSettings() {
    }

    public byte[] toByteArray() {
        byte[] byteArray = new byte[LENGTH + AGPS_DATE_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.put(gps1 ? (byte) 1 : (byte) 0);
        buffer.put(beep1 ? (byte) 1 : (byte) 0);
        buffer.putInt(videoRes1);
        buffer.putInt(quality1);
        buffer.putInt(meteringMode1);
        buffer.putInt(contrast1);
        buffer.putInt(exposure1);
        buffer.putInt(sharpness1);
        buffer.putInt(mic1);
        buffer.putInt(photoMode1);
        buffer.putInt(videoFormat1);

        buffer.put(gps2 ? (byte) 1 : (byte) 0);
        buffer.put(beep2 ? (byte) 1 : (byte) 0);
        buffer.putInt(videoRes2);
        buffer.putInt(quality2);
        buffer.putInt(meteringMode2);
        buffer.putInt(contrast2);
//        buffer.put((byte) (contrast2 & 0xFF));
//        buffer.put((byte) (contrast2 >> 8));
//        buffer.put((byte) (contrast2 >> 16));
//        buffer.put((byte) (contrast2 >> 24));

        buffer.putInt(exposure2);
        buffer.putInt(sharpness2);
        buffer.putInt(mic2);
        buffer.putInt(photoMode2);
        buffer.putInt(videoFormat2);

        buffer.put(gpsRate1);
        buffer.put(extMic1);
        buffer.put(whiteBalance1);

        buffer.put(gpsRate2);
        buffer.put(extMic2);
        buffer.put(whiteBalance2);

        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        
//        buffer.put(this.agpsDate);
        return byteArray;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("\nSwitchPos ").append(this.switchPosition)
                .append("\nGps ").append(this.getGps())
                .append("\nBeep ").append(this.getBeep())
                .append("\nVideoRes ").append(getVideoRes())
                .append("\nQuality ").append(getQuality())
                .append("\nMetering ").append(this.getMeteringMode())
                .append("\nContrast ").append(getContrast())
                .append("\nExposure ").append(getExposure())
                .append("\nSharpness ").append(getSharpness())
                .append("\nInternalMic ").append(this.getInternalMic())
                .append("\nPhotoFreq ").append(this.getPhotoMode())
                .append("\nVideoFormat ").append(this.getVideoFormat())
                .append("\nGpsRate ").append(this.getGpsRate())
                .append("\nExternalMic ").append(this.getExternalMic())
                .append("\nWhiteBalance ").append(this.getWhiteBalance())
                .append("\nDeviceModel ").append(cameraModel)
                .append("\nMajorVersion ").append(this.majorVersion)
                .append("\nMinorVersion ").append(this.minorVersion)
                .append("\nBuildVersion ").append(this.buildVersion).toString();
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(gps1 ? (byte) 1 : (byte) 0);
        dest.writeByte(beep1 ? (byte) 1 : (byte) 0);
        dest.writeInt(videoRes1);
        dest.writeInt(quality1);
        dest.writeInt(meteringMode1);
        dest.writeInt(contrast1);
        dest.writeInt(exposure1);
        dest.writeInt(sharpness1);
        dest.writeInt(mic1);
        dest.writeInt(photoMode1);
        dest.writeInt(videoFormat1);

        dest.writeByte(gps2 ? (byte) 1 : (byte) 0);
        dest.writeByte(beep2 ? (byte) 1 : (byte) 0);
        dest.writeInt(videoRes2);
        dest.writeInt(quality2);
        dest.writeInt(meteringMode2);
        dest.writeInt(contrast2);
        dest.writeInt(exposure2);
        dest.writeInt(sharpness2);
        dest.writeInt(mic2);
        dest.writeInt(photoMode2);
        dest.writeInt(videoFormat2);

        dest.writeByte(gpsRate1);
        dest.writeByte(extMic1);
        dest.writeByte(whiteBalance1);

        dest.writeByte(gpsRate2);
        dest.writeByte(extMic2);
        dest.writeByte(whiteBalance2);

        dest.writeByte(switchPosition);
        dest.writeByte(cameraModel);
        
        dest.writeByte(majorVersion);
        dest.writeByte(minorVersion);
        dest.writeByte(buildVersion);
    }

    public static final Parcelable.Creator<CameraSettings> CREATOR = new Parcelable.Creator<CameraSettings>() {
                                                                       public CameraSettings createFromParcel(
                                                                               Parcel in) {
                                                                           return new CameraSettings(in);
                                                                       }

                                                                       public CameraSettings[] newArray(
                                                                               int size) {
                                                                           return new CameraSettings[size];
                                                                       }
                                                                   };

    private CameraSettings(Parcel in) {
        gps1 = in.readByte() == 1 ? true : false;
        beep1 = in.readByte() == 1 ? true : false;
        videoRes1 = in.readInt();
        quality1 = in.readInt();
        meteringMode1 = in.readInt();
        contrast1 = in.readInt();
        exposure1 = in.readInt();
        sharpness1 = in.readInt();
        mic1 = in.readInt();
        photoMode1 = in.readInt();
        videoFormat1 = in.readInt();

        gps2 = in.readByte() == 1 ? true : false;
        beep2 = in.readByte() == 1 ? true : false;
        videoRes2 = in.readInt();
        quality2 = in.readInt();
        meteringMode2 = in.readInt();
        contrast2 = in.readInt();
        exposure2 = in.readInt();
        sharpness2 = in.readInt();
        mic2 = in.readInt();
        photoMode2 = in.readInt();
        videoFormat2 = in.readInt();

        gpsRate1 = in.readByte();
        extMic1 = in.readByte();
        whiteBalance1 = in.readByte();

        gpsRate2 = in.readByte();
        extMic2 = in.readByte();
        whiteBalance2 = in.readByte();

        switchPosition = in.readByte();
        cameraModel = in.readByte();
        
        majorVersion = in.readByte();
        minorVersion = in.readByte();
        buildVersion = in.readByte();
    }

    public boolean getGps() {
        if (switchPosition == 1)
            return this.gps1;
        return this.gps2;
    }

    public boolean getBeep() {
        if (switchPosition == 1)
            return this.beep1;
        return this.beep2;
    }

    public int getVideoRes() {
        if (switchPosition == 1)
            return this.videoRes1;
        return this.videoRes2;
    }
    
    public int getVideoRes(int switchPos) {
        if (switchPos == 1)
            return this.videoRes1;
        return this.videoRes2;
    }
    
    public int getVideoResIndex() {
        int videoRes = getVideoRes();
        if(this.cameraModel == CameraComms.MODEL_PLUS_2) {
            if(videoRes == 4) return 7;
            if(videoRes == 5 || videoRes == 6 || videoRes == 7)
                return videoRes - 1;
        }
        return videoRes;
    }

    public int getQuality() {
        if (switchPosition == 1)
            return this.quality1;
        return this.quality2;
    }

    public int getMeteringMode() {
        if (switchPosition == 1)
            return this.meteringMode1;
        return this.meteringMode2;
    }

    public int getPhotoMode() {
        if (switchPosition == 1)
            return this.photoMode1;
        return this.photoMode2;
    }
    
    public int getPhotoMode(int switchPos) {
        if (switchPos == 1)
            return this.photoMode1;
        return this.photoMode2;
    }
    
    public int getPhotoModeIndex() {
        int photoMode = this.getPhotoMode();
        if(this.cameraModel == CameraComms.MODEL_PLUS_2) {
            switch (photoMode) {
            case 1: return 0;
            case 3: return 1;
            case 5: return 2;
            case 10: return 3;
            case 15: return 4;
            case 20: return 5;
            case 30: return 6;
            case 45: return 7;
            case 60: return 8;
            default:
            return 0;
            }
        } else {
            switch (photoMode) {
            case 3: return 0;
            case 5: return 1;
            case 10: return 2;
            case 30: return 3;
            case 60: return 4;
            default:
            return 0;
            }
        }
    }

    public byte getGpsRate() {
        if (switchPosition == 1)
            return this.gpsRate1;
        return this.gpsRate2;
    }
    
    public int getGpsRateIndex() {
        int gpsr = this.getGpsRate();
        if(gpsr > 2) gpsr -=1;
        return gpsr;
    }


    public byte getWhiteBalance() {
        if (switchPosition == 1)
            return this.whiteBalance1;
        return this.whiteBalance2;
    }

    public int getContrast() {
        if (switchPosition == 1)
            return this.contrast1;
        return this.contrast2;
    }

    public int getExposure() {
        if (switchPosition == 1)
            return this.exposure1;
        return this.exposure2;
    }

    public int getSharpness() {
        if (switchPosition == 1)
            return this.sharpness1;
        return this.sharpness2;
    }

    public int getInternalMic() {
        if (switchPosition == 1)
            return this.mic1;
        return this.mic2;
    }

    public int getExternalMic() {
        if (switchPosition == 1)
            return this.extMic1;
        return this.extMic2;
    }

    public int getVideoFormat() {
        if (switchPosition == 1)
            return this.videoFormat1;
        return this.videoFormat2;
    }

    /* *********** SETTERS *********** */

    public void setGps(boolean gps) {
        if (switchPosition == 1) {
            this.gps1 = gps;
        } else {
            this.gps2 = gps;
        }
    }

    public void setBeep(boolean beep) {
        if (switchPosition == 1) {
            this.beep1 = beep;
        } else {
            this.beep2 = beep;
        }
    }
    
    public void setVideoResByIndex(int index) {
        int videoRes = index;
        if (this.cameraModel == CameraComms.MODEL_PLUS_2) {
            if (index == 4 || index == 5 || index == 6)
                videoRes = index + 1;
            if (index == 7)
                videoRes = 4;
        }
        setVideoRes(videoRes);
    }

    public void setVideoRes(int videoRes) {
        if (switchPosition == 1) {
            this.videoRes1 = videoRes;
        } else {
            this.videoRes2 = videoRes;
        }
    }

    public void setQuality(int quality) {
        if (switchPosition == 1) {
            this.quality1 = quality;
        } else {
            this.quality2 = quality;
        }
    }

    public void setMeteringMode(int meteringMode) {
        if (switchPosition == 1) {
            this.meteringMode1 = meteringMode;
        } else {
            this.meteringMode2 = meteringMode;
        }
    }

    public void setContrast(int contrast) {
        if (switchPosition == 1) {
            this.contrast1 = contrast;
        } else {
            this.contrast2 = contrast;
        }
    }

    public void setExposure(int exposure) {
        if (switchPosition == 1) {
            this.exposure1 = exposure;
        } else {
            this.exposure2 = exposure;
        }
    }

    public void setSharpness(int sharpness) {
        if (switchPosition == 1) {
            this.sharpness1 = sharpness;
        } else {
            this.sharpness2 = sharpness;
        }
    }

    public void setMic(int mic) {
        if (switchPosition == 1) {
            this.mic1 = mic;
        } else {
            this.mic2 = mic;
        }
    }

    public void setPhotoModeByIndex(int index) {
        int photoMode = 3;

        if (this.cameraModel == CameraComms.MODEL_PLUS_2) {
            if (index == 0)
                photoMode = 1;
            if (index == 1)
                photoMode = 3;
            if (index == 2)
                photoMode = 5;
            if (index == 3)
                photoMode = 10;
            if (index == 4)
                photoMode = 15;
            if (index == 5)
                photoMode = 20;
            if (index == 6)
                photoMode = 30;
            if (index == 7)
                photoMode = 45;
            if (index == 8)
                photoMode = 60;
        } else {
            if (index == 0)
                photoMode = 3;
            if (index == 1)
                photoMode = 5;
            if (index == 2)
                photoMode = 10;
            if (index == 3)
                photoMode = 30;
            if (index == 4)
                photoMode = 60;
        }
        setPhotoMode(photoMode);
    }
    
    public void setPhotoMode(int photoMode) {
        if (switchPosition == 1) {
            this.photoMode1 = photoMode;
        } else {
            this.photoMode2 = photoMode;
        }
    }

    public void setVideoFormat(int videoFormat) {
            this.videoFormat1 = videoFormat;
            this.videoFormat2 = videoFormat;
    }

    public void setGpsRate(byte gpsRate) {
        byte gpsr = gpsRate;
        if(gpsr <= 0) 
            gpsr = 0;
        else if(gpsr == 3) 
            gpsr = 4;
        else if (gpsr >= 4)
            gpsr = 5;
        
        if (switchPosition == 1) {
            this.gpsRate1 = gpsr;
        } else {
            this.gpsRate2 = gpsr;
        }
    }

    public void setWhiteBalance(byte whiteBalance) {
        if (switchPosition == 1) {
            this.whiteBalance1 = whiteBalance;
        } else {
            this.whiteBalance2 = whiteBalance;
        }
    }

    public void setInternalMic(int mic) {
        if (switchPosition == 1) {
            this.mic1 = mic;
        } else {
            this.mic2 = mic;
        }
    }

    public void setExternalMic(byte mic) {
        if (switchPosition == 1) {
            this.extMic1 = mic;
        } else {
            this.extMic2 = mic;
        }
    }
    
    public int getModelMajorVersionMin() {
        if(this.cameraModel == 0)
            return 1;
        if(this.cameraModel == 1)
            return 1;
        return 2;
    }

    public int getModelMinorVersionMin() {
        if(this.cameraModel == 0)
            return 19;
        if(this.cameraModel == 1)
            return 35;
        return 0;

    }

    public int getModelBuildVersionMin() {
        if(this.cameraModel == 0)
            return 0;
        if(this.cameraModel == 1)
            return 13;
        return 20;
    }
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}