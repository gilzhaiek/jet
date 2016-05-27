/**
 *	Copyright 2013 Recon Instruments
 *	All Rights Reserved.
 *
 *	Author : Patrick Cho
 *
 *	This class is a AsyncTask that decodes system RIF files and checks and installs it.
 *
 */

package com.reconinstruments.installer.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.installer.InstallerService;
import com.reconinstruments.installer.rif.RifUtils;
import com.reconinstruments.installer.rif.RifUtils.MD5MismatchException;

public class SystemDecodeTask extends AsyncTask<RifInfo, Integer, Void> {
    public static final String TAG = "SystemDecodeTask";
    public static final boolean DEBUG = InstallerService.DEBUG;

    /**
     * This is the action for status broadcast when
     */
    public static final String ACTION_DECODED = "reconinstruments.intent.action.applauncher.DECODED";
    /**
     * This is the action for status broadcast when decoding failed for some reason
     */
    public static final String ACTION_FAILED = "reconinstruments.intent.action.applauncher.FAILED";
    /**
     * This is the extra key name for RifInfo parcelable data inside
     */
    public static final String EXTRA_RIF_INFO = "reconinstruments.intent.extra.applauncher.RIF_INFO";
    /**
     * This is the extra key name for Status code for Failure
     */
    public static final String EXTRA_FAIL_STATUS = "reconinstruments.intent.extra.applauncher.FAIL_STATUS";

    /*
     * Enumerations of Status
     */
    public static final int DECODING_STATUS_NOTSTARTED      = 0;
    public static final int DECODING_STATUS_DECODING        = 1;
    public static final int DECODING_STATUS_NO_FILE         = 2;
    public static final int DECODING_STATUS_IO_ERROR        = 3;
    public static final int DECODING_STATUS_DECODING_ERROR  = 4;
    public static final int DECODING_STATUS_INVALID_ASSET   = 5;
    public static final int DECODING_STATUS_SUCCEED         = 6;
    public static final int DECODING_STATUS_MD5_MISMATCH    = 7;

    private Context mContext = null;
    private int mTaskStatus = DECODING_STATUS_NOTSTARTED;
    // the apk to be decoded
    private RifInfo mRifInfo = null;

    public SystemDecodeTask(Context context) {
        mContext = context;
    }

    /**
     * This method will be called in the UI thread by the main Activity before task execution
     */
    @Override
    protected void onPreExecute() {
        if (DEBUG)
            Log.d(TAG, "onPreExecute");
    }

    @Override
    protected Void doInBackground(RifInfo... file) {

        mRifInfo = file[0];
        File apkFile = null;

        try {

            mTaskStatus = DECODING_STATUS_DECODING;

            publishProgress(DECODING_STATUS_DECODING);
            
            RifUtils.decodeRif(mContext,mRifInfo.getRifPath(),mRifInfo.getApkMD5());

            mTaskStatus = DECODING_STATUS_SUCCEED;
            if (DEBUG)
                Log.d(TAG, "STATUS_SUCCEED");

        } catch (FileNotFoundException e) {
            publishProgress(DECODING_STATUS_NO_FILE);
            Log.e(TAG, e.getLocalizedMessage());
        } catch (MD5MismatchException e) {
            publishProgress(DECODING_STATUS_MD5_MISMATCH);
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            publishProgress(DECODING_STATUS_IO_ERROR);
            Log.e(TAG, e.getLocalizedMessage());
        } catch (InvalidKeyException e) {
            publishProgress(DECODING_STATUS_INVALID_ASSET);
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IllegalBlockSizeException e) {
            publishProgress(DECODING_STATUS_DECODING_ERROR);
            Log.e(TAG, e.getLocalizedMessage());
        } catch (BadPaddingException e) {
            publishProgress(DECODING_STATUS_INVALID_ASSET);
            Log.e(TAG, e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            publishProgress(DECODING_STATUS_DECODING_ERROR);
            Log.e(TAG, e.getLocalizedMessage());
        } catch (NoSuchPaddingException e) {
            publishProgress(DECODING_STATUS_DECODING_ERROR);
            Log.e(TAG, e.getLocalizedMessage());
        }

        return null;
    }

    /**
     * Report status by very brief log message
     */
    @Override
    protected void onProgressUpdate(Integer... progress) {
        mTaskStatus = progress[0];
        switch (mTaskStatus) {
            case DECODING_STATUS_NOTSTARTED: // Not used
                break;
            case DECODING_STATUS_DECODING:
                if (DEBUG)
                    Log.d(TAG, "STATUS_DECODING");
                break;
            case DECODING_STATUS_NO_FILE:
                if (DEBUG)
                    Log.d(TAG, "STATUS_INVALID_ASSET");
                break;
            case DECODING_STATUS_IO_ERROR:
                if (DEBUG)
                    Log.d(TAG, "STATUS_INVALID_ASSET");
                break;
            case DECODING_STATUS_DECODING_ERROR:
                if (DEBUG)
                    Log.d(TAG, "STATUS_INVALID_ASSET");
                break;
            case DECODING_STATUS_INVALID_ASSET:
                if (DEBUG)
                    Log.d(TAG, "STATUS_INVALID_ASSET");
                break;
            case DECODING_STATUS_SUCCEED:
                if (DEBUG)
                    Log.d(TAG, "STATUS_SUCCEED");
                break;
            case DECODING_STATUS_MD5_MISMATCH:
                if (DEBUG)
                    Log.d(TAG, "MD5 MISMATCH, CANCEL THE TASK");
                break;
        }

    }

    private void sendStatusBroadcast(String action) {
        if (DEBUG)
            Log.d(TAG,
                    "SENDING OUT BROADCAST INTENT : " + action + ": FILE " + mRifInfo.getRifPath());

        Intent decodedIntent = new Intent(action);
        decodedIntent.putExtra(EXTRA_RIF_INFO, mRifInfo);
        decodedIntent.putExtra(EXTRA_FAIL_STATUS, mTaskStatus);
        mContext.sendBroadcast(decodedIntent);
    }

    /**
     * After decoding is successfully finished, notify InstallerService that task has successfully
     * finished decoding the Rif file and it is ready to install decoded apk file
     */
    @Override
    protected void onPostExecute(Void result) {

        if (DEBUG)
            Log.d(TAG, "STATUS = " + mTaskStatus);

        if (mTaskStatus == DECODING_STATUS_SUCCEED) {
            sendStatusBroadcast(ACTION_DECODED);
        } else {
            sendStatusBroadcast(ACTION_FAILED);
        }
    }

}
