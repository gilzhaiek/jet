package com.reconinstruments.profile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import android.os.Environment;

/**
 * Describe class <code>ProfileManager</code> here.
 * 
 * @author <a href="mailto:ali@recon-lnx-02">ali</a>
 * @version 1.0
 */
public class ProfileManager {
    /**
     * The folder in which all the profile XMLs reside
     * 
     */
    public static final String PROFILE_FOLDER = "ReconApps/Profiles";
    /**
     * The name of the file whose content is the name of the chosen profile file
     * 
     */
    public static final String CHOSEN_PROFILE_NAME = "ChosenProfile";
    private static final String TAG = "ProfileManager";

    /**
     * Gets the content of the file corresponding to the chosen profile into a
     * string
     * 
     * @return a <code>String</code> value
     */
    public static String fetchTheChosenProfile() {
	try {
	    File file2 = fetchTheChosenProfileFile();
	    String profile = readFileAsString(file2.getAbsolutePath());
	    return profile;
	} catch (IOException e) {
	    Log.w(TAG, "couldnt read file");
	    e.printStackTrace();
	    return ProfileParser.SAMPLE_PROFILE;
	}
    }

    /**
     * Returns the xml file corresponding to the chosen profile.
     * 
     * @return a <code>File</code> value
     * @exception java.io.IOException
     *                if an error occurs
     */
    public static File fetchTheChosenProfileFile() throws java.io.IOException {
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, PROFILE_FOLDER + "/" + CHOSEN_PROFILE_NAME);
	String profilename = readFileAsString(file.getAbsolutePath());
	File file2 = new File(path, PROFILE_FOLDER + "/" + profilename);
	return file2;
    }

    /**
     * Helper function to read and write the contents of a file into a string.
     * 
     * @param f
     *            a <code>File</code> value
     * @return a <code>String</code> value
     * @exception java.io.IOException
     *                if an error occurs
     */
    public static String readFileAsString(File f) throws java.io.IOException {
	return readFileAsString(f.getAbsolutePath());
    }

    /**
     * Helper function to read and write the contents of a file into a string.
     * 
     * @param filePath
     *            a <code>String</code> value
     * @return a <code>String</code> value
     * @exception java.io.IOException
     *                if an error occurs
     */
    public static String readFileAsString(String filePath)
	throws java.io.IOException {
	BufferedReader reader = new BufferedReader(new FileReader(filePath));
	String line, results = "";
	while ((line = reader.readLine()) != null) {
	    results += line;
	}
	reader.close();
	Log.d(TAG, "results=" + results);
	return results;
    }

    /**
     * Update the content of ChosenProfile which holds the filename of the
     * chosen profile.
     * 
     * @param txt
     *            the file name.
     */
    public static void updateChosenProfile(String txt) {
	// Writing the stuff to the file
	PrintStream out = null;
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, PROFILE_FOLDER + "/" + CHOSEN_PROFILE_NAME);
	try {
	    out = new PrintStream(new FileOutputStream(file.getAbsolutePath()));
	    out.print(txt);
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (out != null)
		out.close();
	}
    }

}