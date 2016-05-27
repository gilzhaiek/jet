package com.dsi.ant.chip;

import android.util.Log;

import com.dsi.ant.message.MessageUtils;
import com.dsi.ant.stonestreetoneantservice.BuildConfig;

import java.util.Arrays;

/**
 * Formats data in to burst packets and writes them to the chip.
 * 
 * This class is generic, and will eventually be moved to the ANT Radio Service.
 */
//TODO Move class to ANT Radio Service when the library is updated. Remove comments above.
public class BurstWriter {
    private static final String TAG = BurstWriter.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG && true;

    /**
     * The low level chip interface.
     */
    public interface IHostControllerInterface {
        /**
         * Forwards the given message to the chip.
         * 
         * @param message Raw data to be forwarded to the chip.
         *
         * @return true if the message was written to the chip.
         */
        public HciResult txBurstMessage(byte[] message);
    }

    /** The result of a write request to the HCI. */
    public enum HciResult {
        /** Packet written */
        SUCCESS,
        /**
         * Packet could not be written right now; retry after a small delay.
         * This is used by the WiLink 7 as ANT has been patched in and the Bluetooth core does
         * not know how to report flow control.
         */
        RETRY_DELAYED,
        /** Writing packet failed. */
        FAIL
    }

    /** Channel number to use for "bursting channel" if there is no burst being written */
    public static final int BURSTING_CHANNEL_NONE = -1;

    /** How long to wait before retrying writing a burst packet when the buffer is full. */
    private static final int RETRY_BURST_DELAY_MS = 50;
    /** How long to keep retrying writing a burst packet before giving up. */
    private static final int MAX_RETRY_BURST_PACKET_TIME_MS = 10000; // 10s

    private IHostControllerInterface mChip;

    private PacketFormat mPacketFormat;

    /**
     * Is a burst currently being constructed/written to the chip.
     * Can not start another burst if this is set
     */
    private boolean mWritingBurst;

    /**
     * The channel the a burst is being constructed for, or {@link #BURSTING_CHANNEL_NONE} if not
     * currently writing a burst.
     */
    private int mBurstingChannel;

    public BurstWriter(IHostControllerInterface chip, PacketFormat packetFormat) {
        mChip = chip;
        mPacketFormat = packetFormat;

        mWritingBurst = false;
        mBurstingChannel = BURSTING_CHANNEL_NONE;
    }

    /**
     * Is a burst currently being constructed/written to the chip.
     * Can not start another burst if this is true
     *
     * @return true if a burst is being written.
     */
    public boolean isWritingBurst() { return mWritingBurst; }

    /** Abort the burst being written. */
    public void cancel() {
        synchronized (SEND_SINGLE_BURST_LOCK) {
            mWritingBurst = false;
            mBurstingChannel = BURSTING_CHANNEL_NONE;
        }
    }

    /** For synchronising code that ensures only one burst is written at a time. */
    private final Object SEND_SINGLE_BURST_LOCK = new Object();

    /**
     * Converts the provided data in to burst packets as specified by the {@link PacketFormat} and
     * writes them to {@link IHostControllerInterface#txBurstMessage(byte[])} of the
     * {@link IHostControllerInterface} (both provided in
     * {@link #BurstWriter(IHostControllerInterface, PacketFormat)}).
     *
     * This will block until all packets are written. Writing packets will only automatically be
     * stopped if there is a serial write error. Chip implementations should provide all received
     * packets to {@link #cancelIfNecessary(byte[])} so writing bursts will stop if the burst
     * transmit has failed.
     *
     * @param channelNumber The ANT channel to write the burst to.
     * @param data The app data to burst.
     *
     * @return <code>true</code> if all packets were written to the chip, <code>false</code> if
     * they could not all be written (including if another burst is currently being written).
     */
    public boolean write(int channelNumber, byte[] data) {
        if(DEBUG) Log.v(TAG, "write");

        return writeStandardBurstData(channelNumber, data);
    }

    private boolean writeStandardBurstData(int channelNumber, byte[] data) {
        if(DEBUG) Log.v(TAG, "writeStandardBurstData");

        int bytesLeftToSend = data.length;
        int dataOffset = 0;
        int hciPacketCount = 0;

        int antPacketsLeft =
                (int) Math.ceil(data.length/(double)PacketFormat.ANT_STANDARD_DATA_PAYLOAD_SIZE);

        byte sequenceNumber = 0;

        synchronized (SEND_SINGLE_BURST_LOCK) {
            if(mWritingBurst) {
                if(DEBUG) {
                    Log.i(TAG, "Not starting write due to burst in progress");
                }

                return false;
            }

            mWritingBurst = true;
            mBurstingChannel = channelNumber;
        }

        if(DEBUG) {
            Log.i(TAG, "Writing " + data.length + " bytes, composing " + antPacketsLeft +
                    " ANT packets.");
        }

        // Initialise burst packet buffer to max size of a write request. Then this can be reused by each write.
        byte[] burstBuffer = new byte[mPacketFormat.NUM_BURST_MESSAGES_CONCATENATED *
                                      mPacketFormat.ANT_DATA_MESSAGE_TOTAL_SIZE];

        // For each write to the HCI (HCI packet)
        do {
            int numAntPacketsCombined = 0;
            int numAntPacketsCombinedTarget =
                    Math.min(antPacketsLeft, mPacketFormat.NUM_BURST_MESSAGES_CONCATENATED);

            int burstBufferOffset = 0;

            if(DEBUG) {
                Log.i(TAG, "Combining next " + numAntPacketsCombinedTarget +
                        " ANT packets inside 1 HCI packet");
            }

            // If we are on to the last write the burst packet buffer may need to be smaller than usual
            int writeSize = numAntPacketsCombinedTarget * mPacketFormat.ANT_DATA_MESSAGE_TOTAL_SIZE;
            if(writeSize < burstBuffer.length) {
                burstBuffer = new byte[writeSize];
            }

            // For each ANT burst packet
            do {
                // Compose the individual burst messages and concatenate them into the buffer to
                // send.

                //Add the Message Header bytes
                burstBuffer[burstBufferOffset++] = PacketFormat.MESG_DATA_SIZE;
                burstBuffer[burstBufferOffset++] = PacketFormat.MESG_BURST_DATA_ID;

                //If this is the last packet we set LAST flag, pad data to 8 bytes
                if(bytesLeftToSend <= PacketFormat.ANT_STANDARD_DATA_PAYLOAD_SIZE) {
                    sequenceNumber |= PacketFormat.SEQUENCE_LAST_MESSAGE;
                    Arrays.fill(burstBuffer, burstBufferOffset+bytesLeftToSend,
                            burstBuffer.length, (byte)0);
                }

                //Add the Burst Flags and Channel Number byte
                burstBuffer[burstBufferOffset++] = (byte) (channelNumber | sequenceNumber);

                //Add the payload
                int dataCopyLength = Math.min(bytesLeftToSend, PacketFormat.ANT_STANDARD_DATA_PAYLOAD_SIZE);
                System.arraycopy(data, dataOffset, burstBuffer, burstBufferOffset,
                        dataCopyLength);

                //Update counters
                bytesLeftToSend -= dataCopyLength;
                dataOffset += dataCopyLength;
                burstBufferOffset += dataCopyLength;
                numAntPacketsCombined++;

                // Calculate sequence number for next packet
                if((sequenceNumber & PacketFormat.SEQUENCE_NUMBER_ROLLOVER) == PacketFormat.SEQUENCE_NUMBER_ROLLOVER) {
                    sequenceNumber = PacketFormat.SEQUENCE_NUMBER_INC;
                } else {
                    sequenceNumber += PacketFormat.SEQUENCE_NUMBER_INC;
                }

            } while((numAntPacketsCombined < numAntPacketsCombinedTarget) && mWritingBurst);
            // Exit when we have concatenated all the packets for this message or quit early if we
            // already know we are dead.

            if(DEBUG) Log.d(TAG, "Writing HCI packet " + ++hciPacketCount);
            long startWriteTimeMillis = System.currentTimeMillis();
            boolean retryWrite;
            do {
                retryWrite = false;
                switch(mChip.txBurstMessage(burstBuffer)) {
                    case FAIL:
                        if(DEBUG) {
                            Log.e(TAG, "Cancelling burst write due to transmit error," +
                                    " still had " + antPacketsLeft + " ANT packets left");
                        }
                        cancel();
                        break;
                    case RETRY_DELAYED:
                        if((startWriteTimeMillis + MAX_RETRY_BURST_PACKET_TIME_MS)
                                < System.currentTimeMillis()) {
                            // Retried for too long
                            cancel();
                        } else {
                            // Retry, after a delay
                            retryWrite = true;
                            if(DEBUG) Log.d(TAG, "Burst TX retry");
                            try {
                                // TODO Delay and max retry time implementation should be improved
                                Thread.sleep(RETRY_BURST_DELAY_MS);
                            } catch (InterruptedException e) {
                                // Ignore, as we are in a retry loop anyway
                            }
                        }
                        break;
                    case SUCCESS:
                        antPacketsLeft -= numAntPacketsCombined;
                        break;
                }
            } while(retryWrite && mWritingBurst);
            // Exit when we have written the packet, or cancelled.

        } while((antPacketsLeft > 0) && mWritingBurst);

        if(!mWritingBurst) {
            if(DEBUG) {
                Log.i(TAG, "Write cancelled with " + antPacketsLeft + " ANT packets left");
            }
        }

        cancel();

        return (antPacketsLeft == 0);
    }

    /**
     * Check if the chip has indicated an error, and stop writing 
     *
     * @param message An ANT message received from the chip/read from HCI. This must be a single
     * packet in the standard Android format (no sync byte or checksum).
     *
     * @return <code>true</code> if writing the burst has been cancelled.
     */
    // Assumes only one burst processing at a time
    public boolean cancelIfNecessary(byte[] message) {
        boolean cancel = false;

        // Check Message ID
        switch (message[mPacketFormat.OFFSET_MESG_ID]) {
            case PacketFormat.MESG_SERIAL_ERROR_ID:
                if(DEBUG) Log.d(TAG, "Serial error encountered.");
                cancel = true;
                break;
            case PacketFormat.MESG_RESPONSE_EVENT_ID:
                int channelNumber = message[mPacketFormat.OFFSET_RESP_CHANNELNUM];
                if(channelNumber == mBurstingChannel) {
                    // This is a Response or RF Event on the bursting channel

                    switch(message[mPacketFormat.OFFSET_RESP_MSGID]) {
                        case 1: // RF Event
                            // Check the possible  channel events
                            /*
                             * EVENT_RX_SEARCH_TIMEOUT
                             * EVENT_RX_FAIL
                             * EVENT_TX
                             * EVENT_TRANSFER_RX_FAILED
                             * EVENT_TRANSFER_TX_COMPLETED
                             * EVENT_TRANSFER_TX_FAILED
                             * EVENT_CHANEL_CLOSED
                             * EVENT_RX_FAIL_GO_TO_SEARCH
                             * EVENT_CHANNEL_COLLISIONS
                             * EVENT_TRANSFER_TX_START
                             * EVENT_TRANSFER_NEXT_DATA_BLOCK ???
                             * EVENT_SERIAL_QUE_OVERFLOW
                             * EVENT_QUE_OVERFLOW
                             */
                            switch(message[mPacketFormat.OFFSET_RESP_EVENT_ID]) {
                                case PacketFormat.EVENT_RX_FAIL_GO_TO_SEARCH:
                                case PacketFormat.EVENT_RX_SEARCH_TIMEOUT:
                                    // Connection lost before burst transfer started 
                                case PacketFormat.EVENT_TRANSFER_TX_COMPLETED:
                                    // Should have finished writing, but cancel to be safe
                                case PacketFormat.EVENT_TRANSFER_TX_FAILED: // Failed
                                case PacketFormat.EVENT_CHANNEL_CLOSED: // Cancelled

                                    if(DEBUG) {
                                        Log.i(TAG, "Cancelling due to bursting channel " +
                                                "receiving RF event: " + MessageUtils.getHexString(
                                                        message[mPacketFormat.OFFSET_RESP_EVENT_ID]
                                                        ));
                                    }

                                    cancel = true;
                                    break;
                                default:
                                    // Not a burst error
                                    break;
                            }
                            break;
                        case PacketFormat.MESG_BURST_DATA_ID: 
                            // This is a response to our write burst data message, which can only be errors.
                            if(DEBUG) {
                                Log.i(TAG, "Burst: Cancelling due to receiving burst response: " +
                                        Arrays.toString(message));
                            }

                            cancel = true;
                            break;
                        default:
                            // N/A Don't care about responses to commands other than writing burst data
                            break;
                    }
                }
                break;
            default:
                // Do nothing with a non response or error message
        }

        if(cancel) {
            cancel();
        }

        return cancel;
    }
}
