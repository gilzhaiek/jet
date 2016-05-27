package com.dsi.ant.log;

import android.util.Log;

import com.dsi.ant.message.ChannelType;
import com.dsi.ant.message.MessageUtils;
import com.dsi.ant.message.fromant.AntMessageFromAnt;
import com.dsi.ant.message.fromhost.AntMessageFromHost;
import com.dsi.ant.message.ipc.AntMessageParcel;
import com.dsi.ant.stonestreetoneantservice.BuildConfig;

/**
 * Utility for logging serial messages, decoding if raw data is a message understood by ANTLib.
 */
public class SerialLog {
    /** The Logcat tag used for the log entries. */
    public static final String LOG_TAG = SerialLog.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG && true;

    /** Whether serial logging is turned on or not. */
    public static final boolean LOG_SERIAL = BuildConfig.DEBUG && true;

    private static final String LOG_TAG_DECODED = LOG_TAG + ":Decoded";
    private static final String LOG_TAG_RAW     = LOG_TAG + ":Raw";

    private static final String LOG_PREFIX_READ  = "Rx: ";
    private static final String LOG_PREFIX_WRITE = "Tx: ";

    /**
     * Log an ANT Message From Host.
     * 
     * @param message The raw data to be written.
     */
    public static void logWrite(byte[] message) {
        if(DEBUG) Log.v(LOG_TAG, "debugWrite");

        if(LOG_SERIAL) {
            // Make sure any logging issues do not stop standard operation
            try {
                Log.i(LOG_TAG_RAW, LOG_PREFIX_WRITE + MessageUtils.getHexString(message));

                int contentLength = MessageUtils.numberFromByte(message, 0);
                int messageId = MessageUtils.numberFromByte(message, 1);
                byte[] messageContent = new byte[contentLength];
                System.arraycopy(message, 2, messageContent, 0, contentLength);

                if(DEBUG) {
                    Log.d(LOG_TAG, "Write: Content length: " + contentLength);
                    Log.d(LOG_TAG, "Write: ID: " + MessageUtils.getHexString(messageId));
                    Log.d(LOG_TAG, "Write: Content: " + MessageUtils.getHexString(messageContent));
                }

                AntMessageFromHost antMessage = AntMessageFromHost.createAntMessage(
                        new AntMessageParcel(messageId, messageContent), ChannelType.UNKNOWN);

                String messageString;

                if(null != antMessage) {
                    messageString = antMessage.toString();
                } else {
                    messageString = MessageUtils.getHexString(message);
                }

                Log.i(LOG_TAG_DECODED, LOG_PREFIX_WRITE + messageString);
            } catch(Exception e) {
                handleLogException(e);
            }
        }
    }

    /**
     * Log an ANT Message From ANT.
     * 
     * @param message The raw data read.
     */
    public static void logRead(byte[] message) {
        if(DEBUG) Log.v(LOG_TAG, "debugRead");

        if(LOG_SERIAL) {
            // Make sure any logging issues do not stop standard operation
            try {
                Log.i(LOG_TAG_RAW, LOG_PREFIX_READ + MessageUtils.getHexString(message));

                AntMessageFromAnt antMessage = AntMessageFromAnt.createAntMessage(message);

                String messageString;

                if(null != antMessage) {
                    messageString = antMessage.toString();
                } else {
                    messageString = MessageUtils.getHexString(message);
                }

                Log.i(LOG_TAG_DECODED, LOG_PREFIX_READ + messageString);
            } catch(Exception e) {
                handleLogException(e);
            }
        }
    }

    private static void handleLogException(Exception e) {
        if(DEBUG) Log.e(LOG_TAG, "Log failure", e);
    }
}
