//package com.reconinstruments.applauncher.transcend;
package com.reconinstruments.applauncher.transcend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

/**
 * 
 * this class is internal helper to log some stuff during
 * jump air time processing;  key reason is to move code
 * away from main JumpAnalyzer & make it a bit cleaner
 * 
 * @author patrickcho
 *
 */
public class JetJumpDebugLog {
	private static final String TAG = JetJumpDebugLog.class.getSimpleName();

	/**
	 * Date Format for the file
	 */
	private static final String JUMP_FILE_DATE	= "yyyy_MM_dd_HH";

	/**
	 * Jump Logging Directory
	 */
	private static final String JUMP_FILE_PATH = "/jump_log";

	/**
	 * Prefix for the name of video file
	 */
	public static final String JUMP_FILE_PREFIX	= "jump_";

	/**
	 * Jump File Related Objects
	 */
	private static FileWriter	jumpWriter;
	private static File			jumpFile;

	/**
	 * Is it initialised ?
	 */
	private static boolean isInit = false;

	public static void startLine(long timeStamp) {
		if (!isInit) {
			JetJumpDebugLog.init();
		}
		
		synchronized (jumpWriter) {
			try {
				jumpWriter.write(timeStamp + " , ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void init () {
		jumpFile = JetJumpDebugLog.getJumpFile();
		try
		{
			jumpWriter = new FileWriter(jumpFile, true);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		JetJumpDebugLog.isInit = true;
	}

	public static void pressure(float pressure) {
		synchronized (jumpWriter) {
			try { jumpWriter.write("P: " + pressure); }
			catch (IOException e) {	e.printStackTrace(); }
		}
	}
	
	public static void accel(float[] accel) {
		synchronized (jumpWriter) {
			try { jumpWriter.write("Acc: " + accel[0] + "," + accel[1] + "," + accel[2]); }
			catch (IOException e) {	e.printStackTrace(); }
		}
	}
	
	public static void append(String key, String value) {
		synchronized (jumpWriter) {
			try { jumpWriter.write(key + ": " + value ); }
			catch (IOException e) {	e.printStackTrace(); }
		}
	}
	
	public static void append(String key) {
		synchronized (jumpWriter) {
			try { jumpWriter.write(key); }
			catch (IOException e) {	e.printStackTrace(); }
		}
	}
	
	public static void endLine() {
		synchronized (jumpWriter) {
			try { jumpWriter.write("\n"); }
			catch (IOException e) {	e.printStackTrace(); }
		}
	}
	
	/**
	 * 
	 * @param jump
	 * @param P0
	 * @param P1
	 * @param Ptemp
	 * @param Ptemp1
	 */
	public static void logJump(ReconJump jump, float P0, float P1, float Ptemp, float Ptemp1)
	{
		synchronized (jumpWriter) {
			try {
				jumpWriter.write(
						"Drop: " + jump.mDrop + ","
						+ "Height: "  + jump.mHeight + ","
						+ "Air: "     + jump.mAir + ","
						+ "mP0: "     + P0 + "," 
						+ "mP1: "     + P1 + "," 
						+ "mPTemp: "  + Ptemp + "," 
						+ "mPtemp1: " + Ptemp1 + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param recheck
	 * @param time
	 */
	public static void logJumpEndTime (long recheck, long time) {
		synchronized (jumpWriter) {
			try {
				jumpWriter.write("Recheck Time: " + recheck + " End Time: " + time + "\n");
			} catch (IOException e)	{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Finishes Logging
	 */
	public static void endLog() {
		synchronized (jumpWriter) {
			try { jumpWriter.close(); } 
			catch (IOException e) { e.printStackTrace(); }
		}
	}

	/*********************** Helper Methods ***************************************************/
	
	public static File getDir() {
		File sdDir = new File(Environment.getExternalStorageDirectory(), JetJumpDebugLog.JUMP_FILE_PATH);
		if (!sdDir.exists() && !sdDir.mkdirs()) {
			Log.e(TAG, "Can't create directory to save jump Logging file.");
			return null;
		}
		return sdDir;
	}

	public static String getFileName() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(JUMP_FILE_DATE, Locale.US);
		String date = dateFormat.format(new Date());
		return File.separator + JetJumpDebugLog.JUMP_FILE_PREFIX + date + ".txt";
	}

	public static File getJumpFile() {
		File jumpFile = null;
		try {
			jumpFile = new File(JetJumpDebugLog.getDir() + JetJumpDebugLog.getFileName());
		} catch (NullPointerException e){
			Log.e(TAG, "Return Jump File Failed");
		}
		return jumpFile;
	}
}
