package com.reconinstruments.autocadence.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.reconinstruments.autocadence.containers.SensorValue;

/**
 * Handles the file creation, printing and threading of CSV logging.
 * Writing is done in a new handler that puts print requests in a message queue.
 *
 * This means that if data isn't being written fast enough, it's okay as long as there is enough memory to be used as buffer.
 *
 * Not very modular yet. Intended for internal use with LogActivity
 *
 * @author Wesley Tsai
 *
 */
public abstract class AbstractThreadedLogger {
    @SuppressWarnings("unused")
    private static final String TAG = AbstractThreadedLogger.class.getSimpleName();

    private File mFile;
    private File mRoot = android.os.Environment.getExternalStorageDirectory();
    private File mDir = new File(mRoot.getAbsolutePath() + "/test_data");

    protected PrintWriter mPrinter;
    protected String mFilePostfix = "Override_Postfix ";

    private boolean mIsWriting;

    PrintHandler mHandler;
    HandlerThread mThread;

    public AbstractThreadedLogger() {
        mIsWriting = false;
        mDir.mkdirs();
    }

    public void startWriting(long timestamp) throws FileNotFoundException {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        setFilePostfix();
        currentDateTimeString = "" + timestamp + mFilePostfix + ".csv";
        mFile = new File(mDir, currentDateTimeString);

        if (mHandler == null) {
            mThread = new HandlerThread("CSVLoggerThread");
            mThread.start();
            mHandler = new PrintHandler(mThread.getLooper());
        }

        try {
            mPrinter = new PrintWriter(new BufferedWriter(new FileWriter(mFile)));
            //printColumnTitles();
            mIsWriting = true;
        } catch (FileNotFoundException e) {
            mIsWriting = false;
            e.printStackTrace();
            throw new FileNotFoundException("CVSLogger");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopWriting() {
        mPrinter.close();
        mIsWriting = false;
    }

    public boolean isWriting() {
        return mIsWriting;
    }

    /**
     * Needs to overwrite mFilePostfix in this function
     */
    protected abstract void setFilePostfix();
    protected abstract void printColumnTitles();

    // TODO: make this abstract or pass object array instead of proprietary container
    public void println(SensorValue sensorValues) {
        if (mIsWriting == true) {
            Message msg = mHandler.obtainMessage();

            Object[] objects = new Object[1];
            objects[0] = sensorValues.clone();

            msg.obj = objects;
            msg.sendToTarget();
        }
    }

    private class PrintHandler extends Handler {
        PrintHandler (Looper loop) {
            super(loop);
        }

        @Override
        public void handleMessage(Message msg) {
            write(msg);
        }
    }

    protected abstract void write(Message msg);
}