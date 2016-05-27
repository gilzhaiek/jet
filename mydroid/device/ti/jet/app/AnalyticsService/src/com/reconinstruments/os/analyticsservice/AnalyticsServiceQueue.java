package com.reconinstruments.os.analyticsservice;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import android.content.SharedPreferences;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.reconinstruments.os.analyticsagent.AnalyticsEventQueue;
import com.reconinstruments.os.analyticsagent.AnalyticsEventRecord;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;


public class AnalyticsServiceQueue {
    // constants
    private final String TAG = this.getClass().getSimpleName();

    private static final String DATA_FORMAT_VER = "1";
    private final static String TEMP_FILE_EXTENSION = "json";
    private final static int MAX_Q_SIZE = 1000;
    private final Integer mQueueAccessLock = 0;
    private static final boolean DEBUG = false;

    private enum QueueState {
        QS_AVAILABLE,
        QS_NOT_AVAILABLE
    }

    // members
    String mAgentID;
    private int mCurrentQIndex = 0;
    private int mQSize = MAX_Q_SIZE;
    private AnalyticsEventQueue[] mEventRecordQueue = new AnalyticsEventQueue[2];
    private QueueState[] mQueueState = new QueueState[]{QueueState.QS_AVAILABLE, QueueState.QS_AVAILABLE};
    private String mLocalStoreRootPath = null;
    private Context mContext = null;
    private AnalyticsServiceCloudInterface mCloudInterface = null;
    private DumpEventsToZipFileTask mDumpEventsToZipFileTask = null;
    private int mIndexBeingFlushed = -1;
    private long mStaleZipTime;
    private long mZipFileSizeLimit;
    private String mOutputDropBox;
    private String mCurrentOutputFile = null;
    private String mHashedDeviceID = null;
    private String mOSVer = null;
    private String mConfigVer = null;
    private String mDeviceSVN = null;
    private boolean mResetStoredData = false;
    private int mTotalEventsRecordedInCurrentQueue = 0;
    private boolean mShuttingDown = false;
    private long mLastPushToDropboxTime;
    private SharedPreferences mAnalyticsServiceSharedPrefs;
    private String mZipEncryptionKey;

    public AnalyticsServiceQueue(AnalyticsServiceCloudInterface analyticsServiceCloudInterface, SharedPreferences sharedPref, long staleTime, long maxFileSize, String outputDropBox,
                                 int qSize, boolean resetStoredData, String configVer, String deviceSVN, String zipKey) {
        mCloudInterface = analyticsServiceCloudInterface;
        mContext = mCloudInterface.mContext;
        mEventRecordQueue[0] = new AnalyticsEventQueue();    // list that holds JSON records
        mEventRecordQueue[1] = new AnalyticsEventQueue();
        mCurrentQIndex = 0;
        mAnalyticsServiceSharedPrefs = sharedPref;
        mLastPushToDropboxTime = mAnalyticsServiceSharedPrefs.getLong("lastDropBoxPush", System.currentTimeMillis());
        mZipEncryptionKey = zipKey;
        mDeviceSVN = deviceSVN;

        mLocalStoreRootPath = mContext.getFilesDir().getAbsolutePath() + "/";    // save results in private app space

        // seed startup queue with startup event
        mEventRecordQueue[mCurrentQIndex].add(new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), "AnalyticsService", "startup",  "", "", "").mJSONString);

        mHashedDeviceID = obfuscateSerialNumber(Build.SERIAL);

        mOSVer = System.getProperty("os.version");
        mConfigVer = configVer;
        mStaleZipTime = staleTime;
        mZipFileSizeLimit = maxFileSize;
        mOutputDropBox = outputDropBox;
        mQSize = qSize;
        mResetStoredData = resetStoredData;

        // check if local zip file is stale, if so move to dropbox
        File dir = new File(mLocalStoreRootPath);
        File[] files = dir.listFiles();
        mCurrentOutputFile = null;

        long latestFileTime = 0; // if more than one for some reason
        for (File f : files) {
            if (DEBUG) Log.d(TAG, "Processing " + f.getAbsolutePath());
            String fileName = f.getName();
            String fileExt = getFileExt(fileName);

            if (fileExt.equalsIgnoreCase("zip")) {
                if (DEBUG) Log.d(TAG, "  it's a zip ");
                String fileCreationTimeStr = getFileCreationTimeFromName(fileName);

                try {
                    if (fileCreationTimeStr.equalsIgnoreCase("")) {
                        f.delete();    // garbage from somewhere, just delete it
                        if (DEBUG) Log.d(TAG, "  bad zip ");
                    } else {
                        // move previous zip to output dropbox
                        moveZipFileToDropbox(f, mLocalStoreRootPath, fileName, mOutputDropBox);
                    }
                } catch (Exception e) {
                    f.delete();    // garbage from somewhere, just delete it
                    Log.d(TAG, "  bad zip ");
                }
            }
        }
    }

    private String obfuscateSerialNumber(String serNumStr) {
        String resultStr = "";
        for (int i = 0; i < serNumStr.length(); i++) {
            char curChar = serNumStr.charAt(i);
            if (Character.isDigit(curChar)) {
                char prevChar = curChar;
                curChar = (char) ((int) '0' + ((int) (curChar - '0') + i) % 10);
            }
            resultStr += curChar;
        }
        return resultStr;
    }

    private String getFileExt(String fileName) {
        int periodIndex = fileName.lastIndexOf(".");
        if (periodIndex > 0) {
            return fileName.substring((periodIndex + 1), fileName.length());
        } else {
            return "";
        }
    }

    private String getFileCreationTimeFromName(String fileName) {    // android doesn't support getting file creation time
        int underscoreIndex = fileName.indexOf("_");
        if (underscoreIndex > 0) {
            return fileName.substring(0, (underscoreIndex));
        } else {
            return "";
        }
    }

    private void moveZipFileToDropbox(File f, String inputPath, String inputFile, String outputPath) throws Exception {

        InputStream in = null;
        OutputStream out = null;

        try {
            if (DEBUG)
                Log.d(TAG, "AnalyticsService queue - moving stale file " + inputPath + inputFile + " to " + outputPath + ".");

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output files
            out.flush();
            out.close();
            out = null;
            // delete the original file
            new File(inputPath + inputFile).delete();

            mLastPushToDropboxTime = System.currentTimeMillis();        // save last push time to preferences
            SharedPreferences.Editor editor = mAnalyticsServiceSharedPrefs.edit();
            editor.putLong("lastDropBoxPush", mLastPushToDropboxTime);
            editor.commit();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot find zip file during move to dropbox: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error moving file " + inputPath + inputFile + " to " + outputPath + ". File deleted. " + e.getMessage());
            f.delete();
        }
    }

    public void add(String newEventsString) {
        if (DEBUG) Log.d(TAG, "in add..." + mCurrentQIndex + " - " + mQueueState[mCurrentQIndex]);
        if (!mShuttingDown) {
            if(mQueueState[mCurrentQIndex] == QueueState.QS_AVAILABLE) {

                int dividerPos = newEventsString.indexOf(":");
                int numEvents = 0;
                if (dividerPos >= 0) {
                    try {
                        numEvents = Integer.parseInt(newEventsString.substring(0, dividerPos));
                    } catch (Exception e) {
                        Log.e(TAG, "Trying to add event with invalid format. Ignoring add request.");
                        numEvents = 0;
                    }

                    if(numEvents > 0) {
                        newEventsString = newEventsString.substring(dividerPos + 1, newEventsString.length());
                        
                        mEventRecordQueue[mCurrentQIndex].add(newEventsString);

                        mTotalEventsRecordedInCurrentQueue += numEvents;
                        if (DEBUG)
                            Log.d(TAG, "" + mEventRecordQueue[mCurrentQIndex].size() + " - " + (long) (System.currentTimeMillis() / 1000) +
                                    ":" + String.format("%03d", (int) (System.currentTimeMillis() % 1000)) + " " + mTotalEventsRecordedInCurrentQueue +
                                    " of " + mQSize + " - Service adding event bundle with " + numEvents + " events, str length: " + newEventsString.length());

                        long testTime = mLastPushToDropboxTime + (mStaleZipTime * 1000);
                        if (DEBUG)
                            Log.d(TAG, "  analytics zip - last event push time: " + mLastPushToDropboxTime + " test time: " + testTime + " current time: " + System.currentTimeMillis());

                        long currentTime = System.currentTimeMillis();
                        boolean staleZip = (testTime < currentTime); // file is stale (ie, HUD left on too long)
                        staleZip = staleZip || (currentTime < mLastPushToDropboxTime);   // trap clock reset to before last saved time (rare case as has to happen during use)

                        // flush Q if too large or stale (HUD been left on too long)
                        if (mTotalEventsRecordedInCurrentQueue >= mQSize || staleZip) {
                            flushQueueToZipFile(staleZip);
                        }
                    }
                }

            }
            else {  // highly-unlikely event where queues are not available... data overrun.  Can't think of why this would happen unless queue size is set really small
                Log.e(TAG, "Data overrun in AnalyticsService queue. Analytics data has been lost.");
            }
        }
    }

    public void flushQueueToZipFile(boolean staleZip) {

        synchronized (mQueueAccessLock) {
            if (mEventRecordQueue[mCurrentQIndex].size() > 0) {
                if (mDumpEventsToZipFileTask == null) {    // if data to dump and not in process of dumping a queue to the local zip file, dump queue

                    mIndexBeingFlushed = mCurrentQIndex;
                    mTotalEventsRecordedInCurrentQueue = 0;
                    mCurrentQIndex = (mCurrentQIndex == 0) ? 1 : 0;
                    mQueueState[mIndexBeingFlushed] = QueueState.QS_NOT_AVAILABLE;

                    mDumpEventsToZipFileTask = new DumpEventsToZipFileTask(mIndexBeingFlushed, staleZip);

                    mDumpEventsToZipFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
    }

    public void flushQueueToZipOnShutdown() { // blocking (not background task) during shutdown to ensure save to zip completes

        if (DEBUG) Log.i(TAG, "in flushQueueToZipOnShutdown");
        synchronized (mQueueAccessLock) {
            mShuttingDown = true;   // block any extraneous adds... shouldn't be any at this point

            mEventRecordQueue[mCurrentQIndex].add(new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), "AnalyticsService", "shutdown", "", "", "").mJSONString);

            if (mDumpEventsToZipFileTask == null) {             // if data to dump and not in process of dumping a queue to the local zip file, dump queue
                String rc = dumpEventsToZipFile(mCurrentQIndex, false);
                if (DEBUG) Log.d(TAG, "Queue " + mCurrentQIndex + " contents saved to zip file.");

                if (!rc.equals("")) {
                    Log.e(TAG, "Critical failure of Analytics Service queue. Cannot create or access zip file during shutdown. Analytics data has been lost.");
                }
            } else {
                int altIndex = mDumpEventsToZipFileTask.mIndex == 0 ? 1 : 0;
                if (mEventRecordQueue[altIndex].size() > 0) {    // if data in alternate queue, send this to zip as well
                    // if already dumping and more data to dump... wait for it to end
                    try {
                        mQueueAccessLock.wait();
                        String rc = dumpEventsToZipFile(altIndex, false);
                        if (DEBUG) Log.d(TAG, "Queue " + altIndex + " contents saved to zip file.");

                        if (!rc.equals("")) {
                            Log.e(TAG, "Critical failure of Analytics Service queue. Cannot create or access zip file during shutdown. Analytics data has been lost.");
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Failure of Analytics Service alternative queue to wait while primary queue writes to file. Some analytics data has been lost.");
                    }

                 }
            }
        }
    }

    private class DumpEventsToZipFileTask extends AsyncTask<Void, Void, String> {
        public int mIndex;
        boolean mStaleZip;

        public DumpEventsToZipFileTask(int index, boolean staleZip) {
            mStaleZip = staleZip;
            mIndex = index;      // Q index to flush
        }

        protected String doInBackground(Void... voids) {
            if (DEBUG) Log.i(TAG, "in DumpEventsToZipFileTask doInBackground");

            String rc = dumpEventsToZipFile(mIndex, mStaleZip);
            if (rc.equalsIgnoreCase("")) {
                return rc;
            }

            return "DONE";
        }

        @Override
        protected void onCancelled(String endString) {    // reached here after cancelled task has completed
            Log.e(TAG, "Analytics DumpEventsTask cancelled for some reason.  Analytics data may have been lost");
            synchronized (mQueueAccessLock) {
                mDumpEventsToZipFileTask = null;
                mQueueAccessLock.notify();
            }
        }

        protected void onPostExecute(String endString) {
            synchronized (mQueueAccessLock) {
                mDumpEventsToZipFileTask = null;
                mQueueAccessLock.notify();
            }
        }
    }

    public String dumpEventsToZipFile(int queueIndex, boolean forceMoveZipToDropbox) {
        ZipFile zipFile = null;
        File zfile = null;
        ArrayList filesToAdd = new ArrayList();

        File tempFileLocationPath = mContext.getFilesDir();
        String filePath = tempFileLocationPath.getAbsolutePath();

        try {

            if (mCurrentOutputFile == null) {
                mCurrentOutputFile = mLocalStoreRootPath + mHashedDeviceID + "_" + (long) (System.currentTimeMillis()) + ".zip";
                if (DEBUG) Log.i(TAG, "new zip file name: " + mCurrentOutputFile);
            }

            zfile = new File(mCurrentOutputFile);    // duplicate for basic file operations not supported by ZipFile
            zipFile = new ZipFile(mCurrentOutputFile);
            if (mResetStoredData || !zfile.exists()) {
                if (DEBUG) Log.d(TAG, "Generating key file for new zip file...");
                File keyFile = getZipKeyFile();
                filesToAdd.add(new File(keyFile.getAbsolutePath()));
                mResetStoredData = false;
            }

            RemoveTempFiles();        // clean up old temp files
            File newDataFileName = GenerateNewDataFileName();
            FileOutputStream fileout = mContext.openFileOutput(newDataFileName.getName(), mContext.MODE_PRIVATE); // will write to folder defined by mContext.getFilesDir()
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);

            outputWriter.write("{\"config_version\":\"" + mConfigVer + "\",\"product\":\"" + Build.PRODUCT
                    + "\",\"model\":\"" + Build.MODEL + "\",\"os_version\":\"" + mOSVer  + "\",\"svn\":\"" + mDeviceSVN
                    + "\",\"data_format_ver\":\"" + DATA_FORMAT_VER + "\",\"data\":[");

            int cnt = 0;
            for (String jsonString : mEventRecordQueue[queueIndex]) {
                if (cnt++ != 0) {
                    outputWriter.write(",");
                }
                outputWriter.write(jsonString);
            }
            outputWriter.write("]}\n");
            outputWriter.flush();
            outputWriter.close();
            if (DEBUG) Log.d(TAG, "Created data file " + newDataFileName.getName());

            filesToAdd.add(new File(newDataFileName.getAbsolutePath()));
        } catch (Exception e) {
            Log.e(TAG, "Critical failure of Analytics Service queue. Cannot create or access new data file. Analytics data has been lost.");
            mEventRecordQueue[queueIndex].clear();  // clear queue and reset it's availability
            mQueueState[queueIndex] = QueueState.QS_AVAILABLE;
            return "DATA_ERROR";
        }

        mEventRecordQueue[queueIndex].clear();  // clear queue and reset it's availability
        mQueueState[queueIndex] = QueueState.QS_AVAILABLE;

        try {
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE); // set compression method to deflate compression
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
            parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
            parameters.setPassword(mZipEncryptionKey); // do not change this !!!

            long startTime = System.currentTimeMillis();
            zipFile.addFiles(filesToAdd, parameters);
            if (DEBUG) Log.d(TAG,"zip dump time: " + (System.currentTimeMillis() - startTime));

            // check if need to move zip to dropbox for upload... (ie, stale or too big)
            if (zfile != null && (forceMoveZipToDropbox || zfile.length() > mZipFileSizeLimit)) {
                try {
                    moveZipFileToDropbox(zfile, mLocalStoreRootPath, zfile.getName(), mOutputDropBox);
                    mCurrentOutputFile = null;
                } catch (Exception e) {
                    Log.e(TAG, "Post-write Error: cannot move zip file " + mLocalStoreRootPath + zfile.getName() + " to " + mOutputDropBox + ": " + e.getMessage());
                }
            }

        } catch (ZipException e) {
            Log.e(TAG, "Critical failure of Analytics Service queue. Cannot add event data to zip " + mCurrentOutputFile + ". " + e.getMessage() + ". Analytics data has been lost.");
            return "ZIP_ERROR";
        }
        return "";
    }

    private File getZipKeyFile() {
        String HUDSerialNum = Build.SERIAL;
        String fileName = "AnalyticsZip.key";
        String path = mContext.getFilesDir() + "/" + fileName;
        File keyFile = new File(path);
        if (!keyFile.exists()) {
            try {
                FileOutputStream fileout = mContext.openFileOutput(fileName, mContext.MODE_PRIVATE);  // will write to folder defined by mContext.getFilesDir()
                OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
                outputWriter.write("{\"device_id\":\"" + HUDSerialNum + "\"}");
                outputWriter.write("\n");
                outputWriter.flush();
                outputWriter.close();
            } catch (Exception e) {
                Log.e(TAG, "Critical failure of Analytics Service local data store. Cannot create or access new key file. Saved analytics data will not be usable without key file.");
            }
        }
        return keyFile;
    }

    private void RemoveTempFiles() {
        File dirPath = new File(mContext.getFilesDir().getAbsolutePath());
        File[] files = dirPath.listFiles();
        for (File curFile : files) {
            String fileName = curFile.getName();
            String fileExtension = fileName.substring((fileName.lastIndexOf(".") + 1), fileName.length());
            if (!curFile.isDirectory() && fileExtension.equalsIgnoreCase(TEMP_FILE_EXTENSION)) {
                curFile.delete();
            }
        }
    }

    private File GenerateNewDataFileName() {
        String fileName = "Events_" + Long.toString(System.currentTimeMillis()) + "." + TEMP_FILE_EXTENSION;
        String path = mContext.getFilesDir() + "/" + fileName;
        File newFile = new File(path);
        return newFile;
    }
}
