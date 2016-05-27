/*
 * ReconMODServiceMessage
 *
 * set of message ids used by TranscendService Bindings
 * references to this should be cleaned up, for now,
 * better to be here than in a set of static jar files
 */

package com.reconinstruments.modservice;

public class ReconMODServiceMessage {
    public static final int MSG_RESULT = 1;
    public static final int MSG_RESULT_CHRONO = 2;
    public static final int MSG_GET_ALTITUDE_BUNDLE = 3;
    public static final int MSG_GET_DISTANCE_BUNDLE = 4;
    public static final int MSG_GET_JUMP_BUNDLE = 5;
    public static final int MSG_GET_LOCATION_BUNDLE = 6;
    public static final int MSG_GET_RUN_BUNDLE = 7;
    public static final int MSG_GET_SPEED_BUNDLE = 8;
    public static final int MSG_GET_TEMPERATURE_BUNDLE = 9;
    public static final int MSG_GET_TIME_BUNDLE = 10;
    public static final int MSG_GET_VERTICAL_BUNDLE = 11;
    public static final int MSG_GET_CHRONO_BUNDLE = 12;
    public static final int MSG_GET_FULL_INFO_BUNDLE = 13;
    public static final int MSG_CHRONO_START_STOP = 14;
    public static final int MSG_CHRONO_LAP_TRIAL = 15;
    public static final int MSG_CHRONO_START_NEW_TRIAL = 16;
    public static final int MSG_CHRONO_STOP_TRIAL = 17;
    public static final int MSG_RESET_STATS = 18;
    public static final int MSG_RESET_ALLTIME_STATS = 19;

    public ReconMODServiceMessage() {
    }
}
