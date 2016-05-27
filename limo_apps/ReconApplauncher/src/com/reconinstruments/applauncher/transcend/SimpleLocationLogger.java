//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.location.Location;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import android.util.Log;
// Commente for the future reader. In this file I am experimenting
// with Lisp-Style code indentation. The advantage is that the code
// looks like python code with no lines wasted for brackets. The
// information density is the highest. The block depth can easilty be
// deduced from indentation.

/**
 * class <code>SimpleLocationLogger</code> is a simple class that
 * writes locations.  It is a helper class that is used by
 * ReconLocatoin Manager. In its guts it holds a list of lats and
 * lngs. The owner of this class pushes locatoin objects to this
 * class. Once this class has accumulated enough points it will write
 * a file in burst appending all the location data.
 *
 */
public class SimpleLocationLogger {
    private static final String TAG = "SimpleLocationLogger";
    public static String tempFileName =
        "/sdcard/ReconApps/TripData/simpleLatLng.tmp.txt";
    public static String saveFileBaseName =
        "/sdcard/ReconApps/TripData/simpleLatLng";
    // TODO ^rewrite based on ExternalStorage
    private double[] mLats;
    private double[] mLngs;
    private int mBufferSize;
    private int mCounter=0;
    private static final int DEFAULT_BUFFER_SIZE = 60;
    /**
     * Creates a new <code>SimpleLocationLogger</code> instance.
     *
     * @param bufferSize an <code>int</code> value
     */
    public SimpleLocationLogger(int bufferSize) {
        mBufferSize = bufferSize;
        mLats = new double[bufferSize];
        mLngs = new double[bufferSize];
    }
    /**
     * Creates a new <code>SimpleLocationLogger</code> instance with
     * the buffer size of  DEFAULT_BUFFER_SIZE.
     *
     */
    public SimpleLocationLogger() {
        this(DEFAULT_BUFFER_SIZE);
    }
    private void clearTempFile() {
        File file = new File(tempFileName);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);}
        catch (IOException e) {
            Log.e(TAG,"Can't clear file");}
        finally {
            try {
                if (writer != null) {
                    writer.close();}}
            catch (Exception e) {
                Log.e(TAG,"Couldn't close writer");}}
    }
    public void reset() {
        mCounter = 0;
        clearTempFile();
    }
    /**
     * pushes a new element to the buffer. When the buffer gets full
     * all the lat, lng info is written to a temp File. If the
     * activity is later decided to be saved by the user then this
     * file is moved to a proper name
     *
     * @param loc a <code>Location</code> value
     */
    public void push(Location loc) {
        if (loc == null) {return;}
        mLats[mCounter] = loc.getLatitude();
        mLngs[mCounter] = loc.getLongitude();
        mCounter++;
        if (mCounter >= mBufferSize) {
            // ^Have reached the limit
            dumpBuffersToFile();}
    }
    /**
     * <code>dumpBuffersToFile</code> Dumps the contnet of the buffer
     * into the tempFile
     *
     */
    public void dumpBuffersToFile() {
        Log.v(TAG,"dumping buffers to file");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(tempFileName, true)));
            for (int i = 0; i < mCounter; i++) {
                out.println(mLats[i]+","+mLngs[i]);}}
        catch (IOException e) {
            Log.w(TAG, "Error writing "+tempFileName);}
        finally {
            try {
                mCounter = 0;
                if (out != null) {
                    out.close();}}
            catch (Exception e) {
                Log.e(TAG, "Error in finally");}}
    }
    /**
     * <code>saveTempFile</code> moves the tempFile that it has
     * created
     *
     * @param type an <code>int</code> value
     */
    public static void saveTempFile(int type) {
        try {
            File file = new File(tempFileName);
            File file2 = new File(saveFileBaseName+type+".csv");
            file2.delete();
            if (!file.renameTo(file2)) {
                Log.w(TAG,"Could not save location file");
            }
        } catch (Exception e) {
            Log.e(TAG,"Can't rename file");}
    }
}