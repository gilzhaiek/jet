package com.dsi.ant.chip;

/**
 * Provides details for the HCI packets of the correct format for the platform in use.
 * 
 * This class is generic, and will eventually be moved to ANTChipLib.
 */
//TODO Move class to ANTChipLib when the library is updated. Remove comments above and constants
//duplicated from ANTLib.
public class PacketFormat {

    /** For the standard Android implementation, there is no sync byte. */
    private static final boolean HAS_SYNC_BYTE_DEFAULT = false;
    /** For the standard Android implementation, there is no checksum. */
    private static final boolean HAS_CHECKSUM_DEFAULT = false;

    /**
     * The number of messages to concatenate is chip specific, but this is generally a good value.
     */
    private static final int NUM_BURST_MESSAGES_CONCATENATED_DEFAULT = 8;

    // ANT constants
    //Sizes used to calculate offsets
    public static final int MESG_SYNC_SIZE_IF_INCLUDED     = 1;
    public static final int MESG_CHECKSUM_SIZE_IF_INCLUDED = 1;

    public static final int MESG_SIZE_SIZE                 = 1;
    public static final int MESG_ID_SIZE                   = 1;
    public static final int MESG_CHANNEL_NUM_SIZE          = 1;
    public static final int MESG_RESPONSE_EVENT_ID_SIZE    = 1;

    public final int MESG_SYNC_SIZE;
    private final int calculateMessageSyncSize() {
        if(HAS_SYNC_BYTE) {
            return MESG_SYNC_SIZE_IF_INCLUDED;
        } else {
            return 0;
        }
    }

    public final int MESG_CHECKSUM_SIZE;
    private static final int calculateMessageChecksumSize() {
        if(HAS_CHECKSUM_DEFAULT) {
            return MESG_CHECKSUM_SIZE_IF_INCLUDED;
        } else {
            return 0;
        }
    }

    /** The number of bytes before the payload in an ANT data message */
    public final int ANT_DATA_HEADER_SIZE;
    private int calculateAntDataHeaderSize() {// ANT_DATA_HEADER_SIZE
        return calculateMessageSyncSize() + 
                MESG_SIZE_SIZE + MESG_ID_SIZE + MESG_CHANNEL_NUM_SIZE;
    }

    /** The number of bytes after the payload in an ANT data message */
    public final int ANT_DATA_FOOTER_SIZE;
    @SuppressWarnings("static-method") // May use local variables later
    private int calculateAntDataFooterSize() {
        return calculateMessageChecksumSize();
    }

    /** The number of bytes of payload in a standard ANT data message */
    public static final int ANT_STANDARD_DATA_PAYLOAD_SIZE = 8;

    /** The value in the size byte of an ANT data message. */
    public static final byte MESG_DATA_SIZE
    = MESG_ID_SIZE + ANT_STANDARD_DATA_PAYLOAD_SIZE;

    public final int ANT_DATA_MESSAGE_TOTAL_SIZE;
    /** The number of bytes in each ANT data message */
    private int calculateAntDataMessageTotalSize() { //BURST_PACKET_TOTAL_SIZE
        return calculateAntDataHeaderSize() + ANT_STANDARD_DATA_PAYLOAD_SIZE + calculateAntDataFooterSize();
    }

    // Message contents
    public static final byte MESG_RESPONSE_EVENT_ID        =((byte)0x40);
    public static final byte MESG_BURST_DATA_ID            = ((byte)0x50);
    public static final byte MESG_SERIAL_ERROR_ID          =((byte)0xAE);

    // RF Events that signal a burst write error/cancel
    public static final byte EVENT_RX_SEARCH_TIMEOUT       =((byte)0x01);
    public static final byte EVENT_TRANSFER_TX_COMPLETED   =((byte)0x05);
    public static final byte EVENT_TRANSFER_TX_FAILED      =((byte)0x06);
    public static final byte EVENT_CHANNEL_CLOSED          =((byte)0x07);
    public static final byte EVENT_RX_FAIL_GO_TO_SEARCH    =((byte)0x08);
    public static final byte EVENT_TRANSFER_NEXT_DATA_BLOCK=((byte)0x11);

    public static final byte CHANNEL_NUMBER_MASK           = ((byte)0x1F);
    public static final byte SEQUENCE_NUMBER_MASK          = ((byte)0xE0);
    public static final byte SEQUENCE_NUMBER_ROLLOVER      = ((byte)0x60);
    public static final byte SEQUENCE_FIRST_MESSAGE        = ((byte)0x00);
    public static final byte SEQUENCE_LAST_MESSAGE         = ((byte)0x80);
    public static final byte SEQUENCE_NUMBER_INC           = ((byte)0x20);


    //Offsets into ANT message
    public final int OFFSET_MESG_SIZE;
    private int calculateMessageSizeOffset() {
        return calculateMessageSyncSize();
    }

    public final int OFFSET_MESG_ID;
    private int calculateMessageIdOffset() {
        return calculateMessageSizeOffset() + MESG_SIZE_SIZE;
    }

    public final int OFFSET_MESG_DATA;
    private int calculateMessageDataOffset() {
        return calculateMessageIdOffset() + MESG_ID_SIZE;
    }

    //Offsets for response messages
    public final int OFFSET_RESP_CHANNELNUM;
    private int calculateResponseChannelNumberOffset() {
        return calculateMessageDataOffset();
    }

    public final int OFFSET_RESP_MSGID;
    private int calculateResponseMessageIdOffset() {
        return calculateResponseChannelNumberOffset() + MESG_CHANNEL_NUM_SIZE;
    }

    public final int OFFSET_RESP_EVENT_ID;
    private int calculateResponseEventIdOffset() {
        return calculateResponseMessageIdOffset() + MESG_RESPONSE_EVENT_ID_SIZE;
    }

    public final boolean HAS_SYNC_BYTE;
    public final boolean HAS_CHECKSUM;

    public final int NUM_BURST_MESSAGES_CONCATENATED;

    public PacketFormat() {
        this(HAS_SYNC_BYTE_DEFAULT, HAS_CHECKSUM_DEFAULT, NUM_BURST_MESSAGES_CONCATENATED_DEFAULT);
    }

    public PacketFormat(boolean hasSyncByte, boolean hasChecksum, int concatenatedBurstPackets) {
        if(concatenatedBurstPackets < 1) {
            throw new IllegalArgumentException("Concatenated packets must be at least 1");
        }

        HAS_SYNC_BYTE = hasSyncByte;
        HAS_CHECKSUM = hasChecksum;
        NUM_BURST_MESSAGES_CONCATENATED = concatenatedBurstPackets;

        // Set sizes
        MESG_SYNC_SIZE = calculateMessageSyncSize();
        MESG_CHECKSUM_SIZE = calculateMessageChecksumSize();
        ANT_DATA_HEADER_SIZE = calculateAntDataHeaderSize();
        ANT_DATA_FOOTER_SIZE = calculateAntDataFooterSize();
        ANT_DATA_MESSAGE_TOTAL_SIZE = calculateAntDataMessageTotalSize();

        //Set offsets
        OFFSET_MESG_SIZE = calculateMessageSizeOffset();
        OFFSET_MESG_ID = calculateMessageIdOffset();
        OFFSET_MESG_DATA = calculateMessageDataOffset();
        OFFSET_RESP_CHANNELNUM = calculateResponseChannelNumberOffset();
        OFFSET_RESP_MSGID = calculateResponseMessageIdOffset();
        OFFSET_RESP_EVENT_ID = calculateResponseEventIdOffset();
    }
}
