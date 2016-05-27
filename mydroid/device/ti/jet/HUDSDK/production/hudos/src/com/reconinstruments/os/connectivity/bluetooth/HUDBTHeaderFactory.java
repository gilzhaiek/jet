package com.reconinstruments.os.connectivity.bluetooth;

/** {@hide}*/
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
    private static final int REQUEST_ID_IDX = 1; // TODO: if the requesting thread died - we remove this request from the memory
    private static final int CODE_IDX = 2;
    private static final int MESSAGE_TYPE_IDX = 3;
    private static final int APPLICATION_IDX = 4;
    private static final int CMD_IDX = 5;
    private static final int ARG1_IDX = 6;
    private static final int HEADER_CODE_0_IDX = 16;
    private static final int HEADER_CODE_1_IDX = 17;
    private static final int HEADER_CODE_2_IDX = 18;
    private static final int HEADER_CODE_3_IDX = 19;
    private static final int HEADER_CODE_4_IDX = 20;
    private static final int HEADER_CODE_5_IDX = 21;
    private static final int HAS_PAYLOAD_IDX = 22;
    private static final int PAYLOAD_LEN_BASE_IDX = 23;
    private static final int HAS_BODY_IDX = 27;
    private static final int BODY_LEN_BASE_IDX = 28;

    public static final byte HEADER_BYTE__NULL = 0x0;
    public static final byte VERSION__1 = 0x1;
    public static final byte CODE__NOCODE = 0x1;
    public static final byte CODE__ERROR = 0x2;
    public static final byte CODE__SUCCESS = 0x3;
    public static final byte MESSAGE_TYPE__RESPONSE = 0x1;
    public static final byte MESSAGE_TYPE__REQUEST = 0x2;
    public static final byte MESSAGE_TYPE__ONEWAY = 0x3;
    public static final byte APPLICATION__PHONE = 0x1;
    public static final byte APPLICATION__WEB = 0x2;
    public static final byte APPLICATION__CMD = 0x3;
    public static final byte CMD__CHECK_REMOTE_NETWORK = 0x1;
    public static final byte CMD__UPDATE_REMOTE_NETWORK = 0x2;
    public static final byte ARG1__HAS_NETWORK = 0x1;
    public static final byte ARG1__NO_NETWORK = 0x2;

    private static final byte HEADER_CODE_0 = (byte) 0x8F;
    private static final byte HEADER_CODE_1 = (byte) 0x0C;
    private static final byte HEADER_CODE_2 = (byte) 0x41;
    private static final byte HEADER_CODE_3 = (byte) 0x5A;
    private static final byte HEADER_CODE_4 = (byte) 0x6B;
    private static final byte HEADER_CODE_5 = (byte) 0x9B;

    public static boolean isRequest(byte[] header) {
        return (getMessageType(header) == MESSAGE_TYPE__REQUEST);
    }

    public static boolean isResponse(byte[] header) {
        return (getMessageType(header) == MESSAGE_TYPE__RESPONSE);
    }

    public static byte getVersion(byte[] header) {
        return header[VERSION_IDX];
    }

    public static byte getRequestID(byte[] header) {
        return header[REQUEST_ID_IDX];
    }

    public static byte getCode(byte[] header) {
        return header[CODE_IDX];
    }

    public static byte getMessageType(byte[] header) {
        return header[MESSAGE_TYPE_IDX];
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
        int value = (header[baseAddr] & 0x0ff) +
                ((header[baseAddr + 1] & 0x0ff) << 8) +
                ((header[baseAddr + 2] & 0x0ff) << 16) +
                ((header[baseAddr + 3] & 0x0ff) << 24);
        return value;
    }

    private static void setInt(byte[] header, int baseAddr, int value) {
        header[baseAddr] = (byte) (value & 0xff);
        header[baseAddr + 1] = (byte) (value >> 8 & 0xff);
        header[baseAddr + 2] = (byte) (value >> 16 & 0xff);
        header[baseAddr + 3] = (byte) (value >> 24 & 0xff);
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

    public static void setRequestID(byte[] header, byte requestID) {
        header[REQUEST_ID_IDX] = requestID;
    }

    public static void setBodyLength(byte[] header, int length) {
        setInt(header, BODY_LEN_BASE_IDX, length);
    }

    private static byte[] getBaseHeader(boolean hasPayload, byte messageType) {
        byte[] header = new byte[HEADER_LENGTH];

        header[VERSION_IDX] = VERSION__1;
        header[CODE_IDX] = CODE__NOCODE;
        header[HAS_PAYLOAD_IDX] = hasPayload ? (byte) 1 : (byte) 0;
        header[HAS_BODY_IDX] = 0;
        header[MESSAGE_TYPE_IDX] = messageType;

        header[HEADER_CODE_0_IDX] = HEADER_CODE_0;
        header[HEADER_CODE_1_IDX] = HEADER_CODE_1;
        header[HEADER_CODE_2_IDX] = HEADER_CODE_2;
        header[HEADER_CODE_3_IDX] = HEADER_CODE_3;
        header[HEADER_CODE_4_IDX] = HEADER_CODE_4;
        header[HEADER_CODE_5_IDX] = HEADER_CODE_5;

        return header;
    }

    /**
     * Validate the header byte array by matching 6 byte header code
     *
     * @param header byte array of header
     * @return true if matches the 10 byte header code, otherwise false
     */
    public static boolean isHeader(byte[] data) {
        if (data.length < HEADER_LENGTH) {
            return false;
        }

        return (data[HEADER_CODE_0_IDX] == HEADER_CODE_0) &&
                (data[HEADER_CODE_1_IDX] == HEADER_CODE_1) &&
                (data[HEADER_CODE_2_IDX] == HEADER_CODE_2) &&
                (data[HEADER_CODE_3_IDX] == HEADER_CODE_3) &&
                (data[HEADER_CODE_4_IDX] == HEADER_CODE_4) &&
                (data[HEADER_CODE_5_IDX] == HEADER_CODE_5);
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
        byte[] header = getBaseHeader(false, MESSAGE_TYPE__ONEWAY);

        header[CODE_IDX] = CODE__ERROR;

        return header;
    }

    /**
     * @return a request command (header) to ask for the network status
     */
    public static byte[] getCheckNetworkHeader() {
        byte[] header = getCmdHeader(MESSAGE_TYPE__REQUEST);

        header[CMD_IDX] = CMD__CHECK_REMOTE_NETWORK;

        return header;
    }

    /**
     * @return a update command (header) for the network status
     */
    public static byte[] getUpdateNetworkHeaderResponse(boolean hasNetwork, byte requestID) {
        byte[] header = getCmdHeader(MESSAGE_TYPE__RESPONSE);

        header[CMD_IDX] = CMD__UPDATE_REMOTE_NETWORK;
        header[ARG1_IDX] = hasNetwork ? ARG1__HAS_NETWORK : ARG1__NO_NETWORK;
        setRequestID(header, requestID);

        return header;
    }

    /**
     * @return a update command (header) for the network status
     */
    public static byte[] getUpdateNetworkHeaderOneWay(boolean hasNetwork) {
        byte[] header = getCmdHeader(MESSAGE_TYPE__ONEWAY);

        header[CMD_IDX] = CMD__UPDATE_REMOTE_NETWORK;
        header[ARG1_IDX] = hasNetwork ? ARG1__HAS_NETWORK : ARG1__NO_NETWORK;

        return header;
    }

    /**
     * @return a request header to transmit Internet request and an optional response
     */
    public static byte[] getInternetRequestHeader(boolean hasResponse, int requestLength, int bodyLength) {
        byte[] header = getBaseHeader(true, hasResponse ? MESSAGE_TYPE__REQUEST : MESSAGE_TYPE__ONEWAY);

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
        byte[] header = getBaseHeader(true, MESSAGE_TYPE__RESPONSE);

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
