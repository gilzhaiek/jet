package com.contour.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.FloatMath;
import android.util.Log;

import com.contour.connect.debug.CLog;

public class CameraProtocolOld
{
    private static final String TAG                       = "CameraProtocolOld";
    private static final boolean D                       = true;

    // EPacketType
    public static final byte    PT_UNKNOWN                = 0x00;
    public static final byte    PT_ACK                    = 0x01;
    public static final byte    PT_CMD                    = 0x02;
    public static final byte    PT_VIDEO                  = 0x03;

    // ECommand
    public static final short   CMD_UNKNOWN               = 0x01;
    public static final short   CMD_REQUEST_IDENTIFY      = 0x02;
    public static final short   CMD_PING                  = 0x03;
    public static final short   CMD_RESEND                = 0x04;

    public static final short   CMD_GET_CAMERA_STATUS     = 0x10;
    public static final short   CMD_RET_CAMERA_STATUS     = 0x11;

    public static final short   CMD_VIDEO_STREAM_ON       = 0x12;
    public static final short   CMD_VIDEO_STREAM_OFF      = 0x13;
    public static final short   CMD_VIDEO_STREAM_STARTED  = 0x14;
    public static final short   CMD_VIDEO_STREAM_STOPPED  = 0x15;

    public static final short   CMD_SET_CAMERA_SETTINGS   = 0x20;
    public static final short   CMD_GET_CAMERA_SETTINGS   = 0x21;
    public static final short   CMD_RET_CAMERA_SETTINGS   = 0x22;

    public static final short   CMD_SET_GPSASSIST         = 0x30;

    public static final short   CMD_START_CUSTOMWB        = 0x70;
    public static final short   CMD_SAVE_CUSTOMWB         = 0x71;

    public static final short   CMD_RECORD_STREAM_START   = 0x80;
    public static final short   CMD_UPDATE_TIME   = 0x90;

    //used to initiate a custom trigger request for non-command packets
    public static final short   CMD_CUSTOM   = 0x99;

    public static final short   CMD_RECORD_STREAM_STOP    = 0x81;
    public static final short   CMD_RECORD_STREAM_STARTED = 0x82;
    public static final short   CMD_RECORD_STREAM_STOPPED = 0x83;

    // CMD id for packet acknowledgement
    public static int           sCmdID                    = 0;

    public static int nextCmdID() {
        return ++sCmdID;
    }

    public static Packet deserializePacket(PacketHeader header, ByteBuffer buffer) {
        Packet packet = null;

        try {
            switch (header.type) {
            case PT_UNKNOWN:
                Log.w(TAG, "Packet deserialization not implemented for PT_UNKNOWN");
                // Skip the packet
                buffer.position(buffer.position() + header.length);
                break;

            case PT_ACK:
                packet = new AckPacket(header, buffer);
                break;

            case PT_CMD:
                packet = new CmdPacket(header, buffer);
                break;

            case PT_VIDEO:
                packet = new VideoPacket(header, buffer);
                break;
            }
        } catch (Exception e) {
            packet = null;
        }

        return packet;
    }

    static public class PacketHeader implements BasePacketHeader {
        public static final int LENGTH = 7;

        private byte             type;
        private int              length;
        private short            checksum;

        public PacketHeader() {
            this.type = PT_UNKNOWN;
            this.length = 0;
            this.checksum = 0;
        }

        public PacketHeader(byte type, int length, short checksum) {
            this.type = type;
            this.length = length;
            this.checksum = checksum;
        }

        public PacketHeader(ByteBuffer buffer) {
            this.type = buffer.get();
            this.length = buffer.getInt();
            this.checksum = buffer.getShort();
        }

        public boolean isValid() {
            return (type != CameraProtocolOld.PT_UNKNOWN && length > 0);
        }

        @Override
        public String toString() {
            return String.format("PacketHeader [type=%d, length=%d, checksum=%x]", type, length, checksum);
        }

        @Override
        public byte getType() {
           return type;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public short getChecksum() {
            return checksum;
        }
    }

    static public class Packet implements BasePacket {
        public PacketHeader header;

        public void addToByteBuffer(ByteBuffer buffer) {
            buffer.put(header.type);
            buffer.putInt(header.length);
            buffer.putShort(header.checksum);
        }

        public byte[] toByteArray() {
            byte[] byteArray = new byte[PacketHeader.LENGTH];

            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.BIG_ENDIAN);
            addToByteBuffer(buffer);

            return byteArray;
        }

        @Override
        public String toString() {
            return "Packet [header=" + header + "]";
        }
        
        @Override
        public byte getHeaderType() {
            return this.header.type;
        }
    }

    static public class AckPacket extends Packet {
        public static final int LENGTH = PacketHeader.LENGTH + 4;

        // Note this should really be uint32
        public int              ackID;

        public AckPacket() {
            this.header = new PacketHeader(PT_ACK, AckPacket.LENGTH, (short) 0);

            this.ackID = 0;
        }

        public AckPacket(int ackID) {
            this.header = new PacketHeader(PT_ACK, AckPacket.LENGTH, (short) 0);

            this.ackID = ackID;
        }

        public AckPacket(PacketHeader header, ByteBuffer buffer) {
            this.header = header;

            this.ackID = buffer.getInt();
        }

        @Override
        public void addToByteBuffer(ByteBuffer buffer) {
            // Header
            super.addToByteBuffer(buffer);

            buffer.putInt(ackID);
        }

        @Override
        public byte[] toByteArray() {
            byte[] byteArray = new byte[AckPacket.LENGTH];

            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            addToByteBuffer(buffer);

            return byteArray;
        }

        @Override
        public String toString() {
            return "AckPacket [" + header + " ackID=" + ackID + "]";
        }

    }

    static public class CmdPacket extends Packet implements BaseCmdPacket {
        public static final int LENGTH_NODATA = PacketHeader.LENGTH + 10;

        public short            cmd;
        public int              param;
        public int              cmd_id;
        public byte[]           data;

        public CmdPacket() {
            this.header = new PacketHeader(PT_CMD, CmdPacket.LENGTH_NODATA, (short) 0);

            this.cmd = CMD_UNKNOWN;
            this.cmd_id = 0;
            this.param = 0;
            this.data = null;
        }

        public CmdPacket(short cmd, int param, byte[] data) {
            int dataLen = (data == null ? 0 : data.length);
            this.header = new PacketHeader(PT_CMD, CmdPacket.LENGTH_NODATA + dataLen, (short) 0);

            this.cmd = cmd;
            this.param = param;
            this.cmd_id = CameraProtocolOld.nextCmdID();
            this.data = data;
        }

        public CmdPacket(PacketHeader header, ByteBuffer buffer) {
            this.header = header;
            this.cmd = buffer.getShort();
            this.param = buffer.getInt();
            this.cmd_id = buffer.getInt();
            
            // Calculate any extra data attached to the packet
            int extraData = header.length - LENGTH_NODATA;
            if (extraData > 0) {
                // Read the attached data into the data field
                this.data = new byte[extraData];
                buffer.get(this.data);
            }
        }

        @Override
        public void addToByteBuffer(ByteBuffer buffer) {
            // Write header
            super.addToByteBuffer(buffer);

            // Write cmd packet fields
            buffer.putShort(cmd);
            buffer.putInt(param);
            buffer.putInt(cmd_id);
            if (data != null)
                buffer.put(data);
        }

        @Override
        public byte[] toByteArray() {
            byte[] byteArray = new byte[header.length];

            // Calculate data checksum
            if (data != null)
                header.checksum = CRC16.checksum(data);

            // Prepare the packet to be checksummed
            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            addToByteBuffer(buffer);
            if(D) CLog.out(TAG,"Cmd Packet toByteArray",Arrays.toString(byteArray));
            return byteArray;
        }

        @Override
        public String toString() {
            return String.format("CmdPacket [%s, cmd=%d, cmd_id=%d, param=%d, data=%d bytes]", header.toString(), cmd, cmd_id, param, (data != null ? data.length : 0));
        }
        
        @Override
        public short getCmd() {
            return cmd;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public int getParam() {
            return param;
        }

        @Override
        public int getPacketId() {
           return cmd_id;
        }
    }

    static public class VideoPacket extends Packet {
        public static int LENGTH_NODATA = PacketHeader.LENGTH;

        public int        video_id;
        public byte[]     data;

        public VideoPacket(PacketHeader header, ByteBuffer buffer) {
            this.header = header;
            this.data = new byte[header.length - LENGTH_NODATA];

            // buffer.getInt(video_id);
            buffer.get(data);
        }

        public Bitmap getBitmap() {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        @Override
        public String toString() {
            return "VideoPacket [header=" + header + ", video_id=" + video_id + ", data=" + data.length + " bytes]";
        }
    }

    static public class CameraSettingsOld {
        private final static int LENGTH           = 19;
        private final static int AGPS_DATE_LENGTH = 20;

        public byte              switchPosition;       // 0-1

        public byte              videoMode;            // 0-4
        public byte              videoQuality;         // 0-2
        public byte              videoRefresh;         // 0-1
        public byte               photoModeInterval;    // > 0

        public byte              meteringMode;         // 0-2
        public int             contrast;             // 1-255
        public byte              sharpness;            // 1-5
        public byte              exposure;             // 0-8

        public byte              micGain;              // 0-59
        public boolean           beepOn;

        public boolean           gpsOn;
        public byte[]            agpsDate;

        public byte              majorVersion;
        public byte              minorVersion;
        public byte              buildVersion;

        public byte              cameraModel;

        // Contour+ Settings
        public byte              gpsRate;
        public byte              extMicGain;
        public byte              whiteBalance;

        public static CameraSettingsOld decodeByteArray(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            CameraSettingsOld settings = new CameraSettingsOld();
            settings.switchPosition = buffer.get();
            settings.videoMode = buffer.get();
            settings.beepOn = buffer.get() > 0;
            settings.gpsOn = buffer.get() > 0;
            settings.videoQuality = buffer.get();
            settings.meteringMode = buffer.get();
            settings.contrast = buffer.get() & 0xFF;
            settings.exposure = (byte) (buffer.get()-4);
            settings.sharpness = buffer.get();
            settings.micGain = buffer.get();
            settings.videoRefresh = buffer.get();
            settings.photoModeInterval = buffer.get();
            //hack fix for P/0 bug
            if(settings.photoModeInterval == (byte)0) 
                settings.photoModeInterval = (byte)3;
            buffer.get(settings.agpsDate);
            settings.majorVersion = buffer.get();
            settings.minorVersion = buffer.get();
            settings.buildVersion = buffer.get();

            // ContourGPS FW < 1.17.11 will not send anything past this
            if (buffer.hasRemaining()) {
                settings.cameraModel = buffer.get();

                settings.gpsRate = buffer.get();
                settings.extMicGain = buffer.get();
                settings.whiteBalance = buffer.get();
            }
            
            return settings;
        }

        public CameraSettingsOld() {
            agpsDate = new byte[AGPS_DATE_LENGTH];
        }

        public byte[] toByteArray() {
            byte[] byteArray = new byte[LENGTH + AGPS_DATE_LENGTH];
            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            buffer.put(switchPosition);
            buffer.put(videoMode);
            buffer.put(beepOn ? (byte) 1 : (byte) 0);
            buffer.put(gpsOn ? (byte) 1 : (byte) 0);
            buffer.put(videoQuality);
            buffer.put(meteringMode);
            buffer.put((byte) (contrast & 0xFF));
            buffer.put((byte) (exposure+4));
            buffer.put(sharpness);
            buffer.put(micGain);
            buffer.put(videoRefresh);
            buffer.put((byte) photoModeInterval);
            buffer.put(agpsDate);
            buffer.put(majorVersion);
            buffer.put(minorVersion);
            buffer.put(buildVersion);

            buffer.put(cameraModel);
            buffer.put(gpsRate);
            buffer.put(extMicGain);
            buffer.put(whiteBalance);

            return byteArray;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("**OLD SETTINGS**\n").append("\nSwitchPos ").append(this.switchPosition)
                    .append("\nGps ").append(this.gpsOn)
                    .append("\nBeep ").append(this.beepOn)
                    .append("\nVideoRes ").append(this.videoMode)
                    .append("\nQuality ").append(this.videoQuality)
                    .append("\nMetering ").append(this.meteringMode)
                    .append("\nContrast ").append((int)this.contrast)
                    .append("\nExposure ").append(this.exposure)
                    .append("\nSharpness ").append(this.sharpness)
                    .append("\nInternalMic ").append(this.micGain)
                    .append("\nPhotoFreq ").append(this.photoModeInterval)
                    .append("\nVideoFormat ").append(this.videoRefresh)
                    .append("\nGpsRate ").append(this.gpsRate)
                    .append("\nExternalMic ").append(this.extMicGain)
                    .append("\nWhiteBalance ").append(this.whiteBalance)
                    .append("\nDeviceModel ").append(cameraModel)
                    .append("\nMajorVersion ").append(this.majorVersion)
                    .append("\nMinorVersion ").append(this.minorVersion)
                    .append("\nBuildVersion ").append(this.buildVersion).toString();
        }
    }

    static public class GPSAssistPayload {
        private static int   LENGTH_NODATA    = 12;
        private static float MAX_PAYLOAD_SIZE = 146;

        private int          totalPackets;
        private int          packetNumber;
        private int          dataSize;
        private byte[]       data;

        public static List<GPSAssistPayload> createGPSAssistPayloads(File gpsFile) {
            LinkedList<GPSAssistPayload> payloads = new LinkedList<GPSAssistPayload>();

            try {
                int payloadChunks = (int) FloatMath.ceil(gpsFile.length() / MAX_PAYLOAD_SIZE) + 1;
                BufferedInputStream input = new BufferedInputStream(new FileInputStream(gpsFile));

                long dataLeft = gpsFile.length();

                for (int i = 0; i < (payloadChunks - 1); ++i) {
                    GPSAssistPayload payload = new GPSAssistPayload();
                    payload.totalPackets = payloadChunks;
                    payload.packetNumber = i + 1;
                    payload.dataSize = (int) ((dataLeft > MAX_PAYLOAD_SIZE) ? MAX_PAYLOAD_SIZE : dataLeft);
                    payload.data = new byte[payload.dataSize];

                    int dataRead = 0;
                    while (dataRead < payload.dataSize)
                        dataRead += input.read(payload.data, dataRead, payload.dataSize - dataRead);

                    dataLeft -= dataRead;
                    payloads.add(payload);
                }

                // Construct a date string with the date of the GPS file
                Date date = new Date(gpsFile.lastModified());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                byte[] dateStringBytes = sdf.format(date).getBytes("UTF-8");

                // Add the last payload
                GPSAssistPayload datePayload = new GPSAssistPayload();
                datePayload.totalPackets = payloadChunks;
                datePayload.packetNumber = payloadChunks;
                datePayload.dataSize = dateStringBytes.length;
                datePayload.data = dateStringBytes;

                payloads.add(datePayload);
            } catch (Exception e) {
                Log.e("contour", "Error creating gps payloads: ", e);
            }

            return payloads;
        }

        private GPSAssistPayload() {

        }

        public int getTotalPackets() {
            return totalPackets;
        }

        public int getPacketNumber() {
            return packetNumber;
        }

        public int getDataSize() {
            return dataSize;
        }

        public byte[] getData() {
            return data;
        }

        public byte[] toByteArray() {
            int totalLength = LENGTH_NODATA + dataSize;
            byte[] byteArray = new byte[totalLength];

            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            buffer.putInt(totalPackets);
            buffer.putInt(packetNumber);
            buffer.putInt(dataSize);
            buffer.put(data);

            return byteArray;
        }

        @Override
        public String toString() {
            return "GPSAssistPayload [totalPackets=" + totalPackets + ", packetNumber=" + packetNumber + ", dataSize=" + dataSize + ", dataLen=" + data.length + "]";
        }
    }
}
