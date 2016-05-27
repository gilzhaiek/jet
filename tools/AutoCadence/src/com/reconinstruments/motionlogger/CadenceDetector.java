package com.reconinstruments.autocadence;

import android.util.Log;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;

import com.reconinstruments.autocadence.util.FileLogger;

public class CadenceDetector {
    private static final String TAG = "CadenceDetector";

    /* The number of past Cadences held for averaging (smoothing) */
    private static final int HELD_CADENCES = 3;

    /* Miscellaneous constants for dealing with frequency
     * 10 seconds of 25Hz values which will be filtered above 2Hz */
    private static final int SENSOR_SAMPLES = 10;
    private static final int SAMPLING_FREQUENCY = 25;
    private static final int HELD_VALUES = SENSOR_SAMPLES*SAMPLING_FREQUENCY;
    private static final int CUTOFF_FREQ = 2; //Hertz
    private static final int MINIMUM_FREQUENCY = 30;
    private static final int MAXIMUM_FREQUENCY = 180;

    /* The order of the butterworth filter */
    private static final int FILTER_ORDER = 2;

    /* Require 6 stored peaks to get 5 peak deltas
     * Peaks must be 0.7m/s^2 higher than surrounding values */
    private static final int STORED_PEAKS = 6;
    private static final int PEAK_DELTAS = STORED_PEAKS - 1;
    private static final double PEAK_DET_DELTA = 0.8;

    /* Bins for weighting similar time deltas
     * Bin values correspond to time deltas giving cadence by the formula
     * MAXIMUM_FREQUENCY - 3 * bin_index */
    private static final int BIN_COUNT = 52;
    private static final double[] BINS = {0.3333,0.3390,0.3448,0.3509,0.3571,0.3636,0.3704,0.3774,0.3846,0.3922,0.4000,0.4082,0.4167,0.4255,0.4348,0.4444,0.4545,0.4651,0.4762,0.4878,0.5000,0.5128,0.5263,0.5405,0.5556,0.5714,0.5882,0.6061,0.6250,0.6452,0.6667,0.6897,0.7143,0.7407,0.7692,0.8000,0.8333,0.8696,0.9091,0.9524,1.0000,1.0526,1.1111,1.1765,1.2500,1.3333,1.4286,1.5385,1.6667,1.8182,2.0000,2.0690};

    /* HELD_CADENCES number of previous + current cadences used to get smoothed average*/
    private static double[] mHeldCadences;

    /* External system of butterworth filters provided by dsp-collection.jar
     *
     * Author: Christian d'Heureuse
     *
     * See: http://www.source-code.biz/dsp/java/ */
    private IirFilter mIirFilter;
    private IirFilterCoefficients mIirFilterCoefficients;

    /* File logger*/
    private FileLogger mFileLogger;

    /* The bin count for binned time deltas */
    private int[] mBinCount;

    /* The raw and filtered accelerometer X data received */
    private float[] mRawAccelData;
    private double[] mFilteredData;

    /* The timestamps corresponding to mRawAccelData and mFilteredData indices */
    private double[] mTimestampData;

    /* The timestamps of peak locations and their respective deltas*/
    private double[] mPeakLocations;
    private double[] mPeakDeltas;

    /* Constructor */
    public CadenceDetector(long systemTime) {

	this.mFileLogger = new FileLogger();

	this.mFileLogger.Activate("test_data/", "" + systemTime + "-DEBUG.csv");
	this.mFileLogger.WriteToFile("Second, DELTA1, DELTA2, DELTA3, DELTA4, DELTA5, 1SECOND_CADENCE, RETURNED_CADENCE\n");

        //Create a lowpass butterworth filter of order FILTER_ORDER with cutoff frequency of CUTOFF_FREQ
        this.mIirFilterCoefficients = IirFilterDesignExstrom.design(FilterPassType.lowpass, FILTER_ORDER, (double) (CUTOFF_FREQ)/SAMPLING_FREQUENCY, 0);
        this.mIirFilter = new IirFilter(mIirFilterCoefficients);

        this.mHeldCadences = new double[HELD_CADENCES];
        this.mRawAccelData = new float[SENSOR_SAMPLES * SAMPLING_FREQUENCY];
        this.mFilteredData = new double[SENSOR_SAMPLES * SAMPLING_FREQUENCY];
        this.mTimestampData = new double[SENSOR_SAMPLES * SAMPLING_FREQUENCY];
    }

    /* Receive new data and dump old*/
    public void receiveAccelDump(float[] rawAccelDump, double[] timestampDump) {

        //Shift accelerometer and timestamp data down by 25 values to erase old and make room for new
        System.arraycopy(mRawAccelData, 0, mRawAccelData, SAMPLING_FREQUENCY, HELD_VALUES - SAMPLING_FREQUENCY);
        System.arraycopy(mTimestampData, 0, mTimestampData, SAMPLING_FREQUENCY, HELD_VALUES - SAMPLING_FREQUENCY);
        //Downsample and store new values
        for (int i = 0; i < SAMPLING_FREQUENCY; i++) {
            mRawAccelData[i] = rawAccelDump[4 * i];
            mTimestampData[i] = timestampDump[4 * i];
        }
    }

    /* Calculate the cadence of the held data set*/
    public int getCadence() {
        //Purge temporary values
        cleanOldData();
        //Remove oldest calculated cadence to make room for new
        shiftOldCadences();
        //Filter the raw data to smooth curve
        applyFilter();
        //Ensure there are at least 6 peaks and store their locations
        if (!countPeaks())
            return -1;
        //Calculate the deltas of the 6 peaks
        calcDeltas();
        //Bin deltas to weight similar values higher
        binData();
        //Calculate an instantaneous cadence
        getWeightedCadence();
	//DEBUG
	mFileLogger.WriteToFile(System.currentTimeMillis() + "," + mPeakDeltas[0] + "," +
				mPeakDeltas[1] + "," + mPeakDeltas[2] + "," + mPeakDeltas[3] + "," +
				mPeakDeltas[4] + "," + mHeldCadences[0] + "," + getCalculatedCadence() + "\n");
        //Calculate the true cadence; if it is below 30 return 0
        if (getCalculatedCadence() < MINIMUM_FREQUENCY)
            return 0;
        return getCalculatedCadence();
    }

    private void shiftOldCadences() {
        //Simple shift
        for (int i = HELD_CADENCES - 1; i > 0; i--) {
            mHeldCadences[i] = mHeldCadences[i-1];
        }
        mHeldCadences[0] = 0;
    }

    private void applyFilter() {
        //Step through raw data to filter it
        for (int i = 0; i < HELD_VALUES; i++) {
            mFilteredData[i] = mIirFilter.step(mRawAccelData[i]);
        }
     }

    private boolean countPeaks() {
        double maxx = Double.MIN_VALUE;
        double minx = Double.MAX_VALUE;

        int foundPeaks = 0;
	boolean peakInNewestTwoDumps = false;

        boolean lookForMax = true;

        for (int i = 0; i < HELD_VALUES; i++) {
            if (foundPeaks == STORED_PEAKS)
                break;

            double point = mFilteredData[i];
            if (point > maxx) {
                maxx = point;
            }
            if (point < minx) {
                minx = point;
            }

            if (lookForMax) {
                if (point < maxx - PEAK_DET_DELTA) {
                    mPeakLocations[foundPeaks] = mTimestampData[i];
		    if (i <= 2*SAMPLING_FREQUENCY)
			peakInNewestTwoDumps = true;
                    foundPeaks += 1;
                    minx = point;
                    lookForMax = false;
                }
            }
            else
                if (point > minx + PEAK_DET_DELTA) {
                    maxx = point;
                    lookForMax = true;
                }
        }

        return (foundPeaks >= STORED_PEAKS && peakInNewestTwoDumps);
    }

    private void calcDeltas() {
        for (int i = 0; i < PEAK_DELTAS; i++) {
            mPeakDeltas[i] = mPeakLocations[i] - mPeakLocations[i+1];
        }
    }

    private void binData() {
        //Bins are labelled with their average value, so you must average two
        //To get the border between them
        for (int i = 0; i < PEAK_DELTAS; i++) {
            if (mPeakDeltas[i] > (BINS[BIN_COUNT - 1] + BINS[BIN_COUNT - 2]) / 2) {
                mBinCount[BIN_COUNT-1]++;
                continue;
            }
            for (int j = 0; j < BIN_COUNT - 1; j++) {
                if (mPeakDeltas[i] < (BINS[j] + BINS[j+1]) / 2) {
                    mBinCount[j]++;
                    break;
                }
            }
        }
    }

    private void getWeightedCadence() {
        //Cadences are weight by the raw number of time deltas binned
        for (int i = 0; i < BIN_COUNT; i++) {
            if (mBinCount[i] != 0) {
                mHeldCadences[0] += (MAXIMUM_FREQUENCY - 3*i)*mBinCount[i] / PEAK_DELTAS;
            }
        }
    }

    private int getCalculatedCadence() {
        float sum = 0;
        for (int i = 0; i < HELD_CADENCES; i++) {
            sum += mHeldCadences[i];
        }
        return (int) (sum / HELD_CADENCES);
    }

    private void cleanOldData() {
        this.mBinCount = new int[BIN_COUNT];
        this.mPeakLocations = new double[STORED_PEAKS];
        this.mPeakDeltas = new double[PEAK_DELTAS];
    }

}