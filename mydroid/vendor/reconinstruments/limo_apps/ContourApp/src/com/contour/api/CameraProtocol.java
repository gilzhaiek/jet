package com.contour.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.FloatMath;
import android.util.Log;

import com.contour.connect.debug.CLog;

public class CameraProtocol {
    // Debugging
    static final String TAG                         = "CameraProtocol";
    static final boolean        D                           = true;
    // EPacketType
    public static final byte    PT_UNKNOWN                  = 0x00;
    public static final byte    PT_ACK                      = 0x01;
    public static final byte    PT_CMD                      = 0x02;

    // ECommand
    public static final short   CMD_VIDEO_STREAM_ON         = 0x01;
    public static final short   CMD_REQUEST_IDENTIFY        = 0x02;
    public static final short   CMD_VIDEO_STREAM_OFF        = 0x03;
    public static final short   CMD_JPEG                    = 0x04;
    public static final short   CMD_VIDEO_STREAM_STARTED    = 0x05;
    public static final short   CMD_VIDEO_STREAM_STOPPED    = 0x06;
    public static final byte    CMD_REQUEST_RESEND          = 0x07;

    public static final short   CMD_GET_CAMERA_STATUS       = 0x75;
    public static final short   CMD_RET_CAMERA_STATUS       = 0x74;
    public static final short   CMD_RET_CAMERA_STATUS_NEW   = 0x76;

    public static final short   CMD_SET_CAMERA_SETTINGS_OLD = 0x53;

    public static final short   CMD_SET_CAMERA_SETTINGS     = 0x57;
    public static final short   CMD_GET_CAMERA_SETTINGS     = 0x55;
    public static final short   CMD_RET_CAMERA_SETTINGS_OLD = 0x56;

    public static final short   CMD_RET_CAMERA_SETTINGS     = 0x58;
    public static final short   CMD_SET_CAMERA_CONFIGURE    = 0x5A;
    public static final short   CMD_RET_CAMERA_CONFIGURE    = 0x5B;

    public static final short   CMD_SET_GPSASSIST           = 0x31;

    public static final short   CMD_START_CUSTOMWB          = 0x70;
    public static final short   CMD_SAVE_CUSTOMWB           = 0x71;

    public static final short   CMD_RECORD_STREAM_START     = 0x90;
    public static final short   CMD_RECORD_STREAM_STOP      = 0x91;
    public static final short   CMD_RECORD_STREAM_STARTED   = 0x92;
    public static final short   CMD_RECORD_STREAM_STOPPED   = 0x93;

    public static final short   CMD_STARTUP_UPDATE          = 0xAA;
    public static final short   RET_DEBUG_TEXT              = 0xDD;

    // CMD id for packet acknowledgement
    public static int           sCmdID                      = 0;

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
            }
        } catch (Exception e) {
            CLog.e(e, TAG, "deserializePacket");
            packet = null;
        }

        return packet;
    }

    static public class PacketHeader implements BasePacketHeader {
        public static final int LENGTH = 11;

        private byte             type;
        private int              length;
       private short            checksum;
        public int              pkt_id;

        public PacketHeader() {
            this.type = PT_UNKNOWN;
            this.length = 0;
            this.checksum = 0;
            this.pkt_id = CameraProtocol.nextCmdID();
        }

        public PacketHeader(byte type, int length, short checksum) {
            this.type = type;
            this.length = length;
            this.checksum = checksum;
            this.pkt_id = CameraProtocol.nextCmdID();
        }

        public PacketHeader(byte type, int length, short checksum, int pkt_id) {
            this.type = type;
            this.length = length;
            this.checksum = checksum;
            this.pkt_id = pkt_id;
        }

        public PacketHeader(ByteBuffer buffer) {
            this.type = buffer.get();
            this.length = buffer.getInt();
            this.checksum = buffer.getShort();
            this.pkt_id = buffer.getInt();

        }

        public PacketHeader(PacketHeader packetHeader) {
            this.type = packetHeader.type;
            this.length = packetHeader.length;
            this.checksum = packetHeader.checksum;
            this.pkt_id = packetHeader.pkt_id;

        }

        public boolean isValid() {
            return (type == CameraProtocol.PT_ACK || type == CameraProtocol.PT_CMD) && this.checksum == 0;
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
        @Override
        public String toString() {
            return String.format("PacketHeader [type=%d, length=%d, checksum=%x, pckt_id=%d]", type, length, checksum, this.pkt_id);
        }
    }

    static public class Packet implements BasePacket {
        public PacketHeader header;
        protected int       mDataLength = 0;

        public void addToByteBuffer(ByteBuffer buffer) {
            buffer.put(header.type);
            buffer.putInt(header.length);
            buffer.putShort(header.checksum);
            buffer.putInt(header.pkt_id);
        }

        public byte[] toByteArray() {
            byte[] byteArray = new byte[PacketHeader.LENGTH];

            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
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
        public static final int LENGTH = 0;

        // Note this should really be uint32

        public AckPacket() {
            this.header = new PacketHeader(PT_ACK, AckPacket.LENGTH, (short) 0);
            this.mDataLength = 0;
        }

        public AckPacket(int packetId) {
            this.header = new PacketHeader(PT_ACK, AckPacket.LENGTH, (short) 0, packetId + 1);
            this.mDataLength = 0;
        }

        public AckPacket(PacketHeader header, ByteBuffer buffer) {
            this.header = new PacketHeader(PT_ACK, AckPacket.LENGTH, (short) 0, header.pkt_id);
            this.mDataLength = 0;
        }

        @Override
        public void addToByteBuffer(ByteBuffer buffer) {
            // Header
            super.addToByteBuffer(buffer);
        }

        @Override
        public byte[] toByteArray() {
            byte[] byteArray = new byte[PacketHeader.LENGTH + AckPacket.LENGTH];

            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            addToByteBuffer(buffer);

            return byteArray;
        }

        @Override
        public String toString() {
            return "AckPacket [" + header + " ackID=" + header.pkt_id + "]";
        }

    }

    static public class CmdPacket extends Packet implements BaseCmdPacket {
        public static final int MIN_LENGTH    = 1;

        public static final int LENGTH_NODATA = PacketHeader.LENGTH + 4;
        // public static final int LENGTH_NODATA = 0;

        public byte             cmd;
        // public int param;
        public byte[]           data;

        CmdPacket() {

        }

        public CmdPacket(byte cmd) {
            this.header = new PacketHeader(PT_CMD, MIN_LENGTH, (short) 0);

            this.cmd = cmd;
            // this.param = 0;
            this.data = null;
        }

        public CmdPacket(byte cmd, int param, byte[] data) {
            mDataLength = (data == null ? 0 : data.length);
            this.header = new PacketHeader(PT_CMD, MIN_LENGTH + mDataLength, (short) 0);

            this.cmd = cmd;
            // this.param = param;
            this.data = data;
        }

        public CmdPacket(PacketHeader header, ByteBuffer buffer) {
            this.header = header;
            // this.param = buffer.getInt();
            this.cmd = buffer.get();

            // Calculate any extra data attached to the packet'
            if (this.cmd == CMD_JPEG) {
                mDataLength = header.length - 4;
            } else
                mDataLength = header.length - 1;

            // CLog.out(TAG, "new CmdPacket() Payload Size",
            // mDataLength,buffer.position(),bufferLength);
            if (mDataLength > 0) {
                // Read the attached data into the data field
                if (this.cmd == CMD_JPEG) {
                    this.data = new byte[mDataLength];
                    buffer.position(buffer.position() + 3);
                    buffer.get(this.data);
                    // mDataLength = header.length;
//                    if (D) CLog.out(TAG, "new video packet", header, "data.len", data.length, "mDataLength", mDataLength);
                } else {
                    this.data = new byte[mDataLength];
                    buffer.get(this.data);
                }
                // CLog.out(TAG,"new CmdPacket() has new Position",buffer.position(),this.data.length);
            }
        }

        @Override
        public void addToByteBuffer(ByteBuffer buffer) {
            // Write header
            super.addToByteBuffer(buffer);
            // Write cmd packet fields
            buffer.put(cmd);
            // buffer.putInt(param);

            if (data != null)
                buffer.put(data);
        }

        @Override
        public byte[] toByteArray() {
            byte[] byteArray = new byte[PacketHeader.LENGTH + MIN_LENGTH + mDataLength];

            // TODO
            {
                final int N = data != null ? data.length : 0;
                byte[] checkData = new byte[N + 1];
                checkData[0] = this.cmd;
                for (int i = 1; i < N + 1; i++) {
                    checkData[i] = data[i - 1];
                }
                header.checksum = CRC16.checksum(checkData);
            }

            // Prepare the packet to be checksummed
            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            addToByteBuffer(buffer);

            return byteArray;
        }

        @Override
        public String toString() {
            return String.format("CmdPacket [%s, cmd=%x, data=%d bytes]", header.toString(), cmd, (data != null ? data.length : 0));
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
            //DO NOTHING, UNNECCESSARY
            return 0;
        }

        @Override
        public int getPacketId() {
            return this.header.pkt_id;
        }
    }

    static public class VideoPacket extends CmdPacket {
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
