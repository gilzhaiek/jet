package com.reconinstruments.hudconnectivity.bluetooth;

public class HUDBTHeaderFactory {
    /**
     * /* Header Data
     * Byte [0]: Version
     * byte [1]: Data Length
     * Byte [2]: Transfer Method
     * Byte [3]: Application
     */
    public static final int HEADER_LENGTH = 32;

    private static final int VERSION_IDX = 0;
    private static final int CODE_IDX = 1;
    private static final int REQUEST_HDR_IDX = 2;
    private static final int APPLICATION_IDX = 3;
    private static final int CMD_IDX = 4;
    private static final int ARG1_IDX = 5;
    private static final int HAS_PAYLOAD_IDX = 14;
    private static final int PAYLOAD_LEN_BASE_IDX = 15;
    private static final int HAS_BODY_IDX = 23;
    private static final int BODY_LEN_BASE_IDX = 24;

    public static final byte HEADER_BYTE__NULL = 0x0;
    public static final byte VERSION__1 = 0x1;
    public static final byte CODE__NOCODE = 0x1;
    public static final byte CODE__ERROR = 0x2;
    public static final byte CODE__SUCCESS = 0x3;
    public static final byte REQUEST_HDR__RESPONSE = 0x1;
    public static final byte REQUEST_HDR__ONEWAY = 0x2;
    public static final byte APPLICATION__PHONE = 0x1;
    public static final byte APPLICATION__WEB = 0x2;
    public static final byte APPLICATION__CMD = 0x3;
    public static final byte CMD__CHECK_REMOTE_NETWORK = 0x1;
    public static final byte CMD__UPDATE_REMOTE_NETWORK = 0x2;
    public static final byte ARG1__HAS_NETWORK = 0x1;
    public static final byte ARG1__NO_NETWORK = 0x2;

    public static byte getVersion(byte[] header) {
        return header[VERSION_IDX];
    }

    public static byte getCode(byte[] header) {
        return header[CODE_IDX];
    }

    public static byte getRequestHeaderType(byte[] header) {
        return header[REQUEST_HDR_IDX];
    }

    public static byte getApplication(byte[] header) {
        return header[APPLICATION_IDX];
    }

    public static byte getCmd(byte[] header) {
        return header[CMD_IDX];
    }

    public static byte getArg1(byte[] header) {
        return header[ARG1_IDX];
    }

    public static void setCode(byte[] header, byte code) {
        header[CODE_IDX] = code;
    }

    public static boolean hasPayload(byte[] header) {
        return header[HAS_PAYLOAD_IDX] == 1;
    }

    private static int getInt(byte[] header, int baseAddr) {
        int value = header[baseAddr] +
                (header[baseAddr + 1] << 4) +
                (header[baseAddr + 2] << 8) +
                (header[baseAddr + 3] << 12) +
                (header[baseAddr + 4] << 16) +
                (header[baseAddr + 5] << 20) +
                (header[baseAddr + 6] << 24) +
                (header[baseAddr + 7] << 28);
        return value;
    }

    private static void setInt(byte[] header, int baseAddr, int value) {
        header[baseAddr] = (byte) (value & 0xf);
        header[baseAddr + 1] = (byte) (value >> 4 & 0xf);
        header[baseAddr + 2] = (byte) (value >> 8 & 0xf);
        header[baseAddr + 3] = (byte) (value >> 12 & 0xf);
        header[baseAddr + 4] = (byte) (value >> 16 & 0xf);
        header[baseAddr + 5] = (byte) (value >> 20 & 0xf);
        header[baseAddr + 6] = (byte) (value >> 24 & 0xf);
        header[baseAddr + 7] = (byte) (value >> 28 & 0xf);
    }

    public static int getPayloadLength(byte[] header) {
        return getInt(header, PAYLOAD_LEN_BASE_IDX);
    }

    public static void setPayloadLength(byte[] header, int length) {
        setInt(header, PAYLOAD_LEN_BASE_IDX, length);
    }

    public static boolean hasBody(byte[] header) {
        return header[HAS_BODY_IDX] == 1;
    }

    public static int getBodyLength(byte[] header) {
        return getInt(header, BODY_LEN_BASE_IDX);
    }

    public static void setBodyLength(byte[] header, int length) {
        setInt(header, BODY_LEN_BASE_IDX, length);
    }

    private static byte[] getBaseHeader(boolean hasPayload, byte requestType) {
        byte[] header = new byte[HEADER_LENGTH];

        header[VERSION_IDX] = VERSION__1;
        header[CODE_IDX] = CODE__NOCODE;
        header[HAS_PAYLOAD_IDX] = hasPayload ? (byte) 1 : (byte) 0;
        header[HAS_BODY_IDX] = 0;
        header[REQUEST_HDR_IDX] = requestType;

        return header;
    }

    /**
     * @return a command header with a cmd transfer
     */
    private static byte[] getCmdHeader(byte requestType) {
        byte[] header = getBaseHeader(false, requestType);

        header[APPLICATION_IDX] = APPLICATION__CMD;

        return header;
    }

    public static byte[] getErrorHeader() {
        byte[] header = getBaseHeader(false, REQUEST_HDR__ONEWAY);

        header[CODE_IDX] = CODE__ERROR;

        return header;
    }

    /**
     * @return a request command (header) to ask for the network status
     */
    public static byte[] getCheckNetworkHeader() {
        byte[] header = getCmdHeader(REQUEST_HDR__RESPONSE);

        header[CMD_IDX] = CMD__CHECK_REMOTE_NETWORK;

        return header;
    }

    /**
     * @return a update command (header) for the network status
     */
    public static byte[] getUpdateNetworkHeader(boolean hasNetwork) {
        byte[] header = getCmdHeader(REQUEST_HDR__ONEWAY);

        header[CMD_IDX] = CMD__UPDATE_REMOTE_NETWORK;
        header[ARG1_IDX] = hasNetwork ? ARG1__HAS_NETWORK : ARG1__NO_NETWORK;

        return header;
    }

    /**
     * @return a request header to transmit Internet request and an optional response
     */
    public static byte[] getInternetRequestHeader(boolean hasResponse, int requestLength, int bodyLength) {
        byte[] header = getBaseHeader(true, hasResponse ? REQUEST_HDR__RESPONSE : REQUEST_HDR__ONEWAY);

        header[APPLICATION_IDX] = APPLICATION__WEB;
        setPayloadLength(header, requestLength);

        if (bodyLength > 0) {
            header[HAS_BODY_IDX] = 1;
            setBodyLength(header, bodyLength);
        } else {
            header[HAS_BODY_IDX] = 0;
        }

        return header;
    }

    /**
     * @return a response header to transmit Internet response
     */
    public static byte[] getInternetResponseHeader(int responseLength, int bodyLength) {
        byte[] header = getBaseHeader(true, REQUEST_HDR__ONEWAY);

        header[APPLICATION_IDX] = APPLICATION__WEB;
        setPayloadLength(header, responseLength);

        if (bodyLength > 0) {
            header[HAS_BODY_IDX] = 1;
            setBodyLength(header, bodyLength);
        } else {
            header[HAS_BODY_IDX] = 0;
        }

        return header;
    }
}
