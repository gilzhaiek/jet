package com.reconinstruments.dashcompass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class CompassUtil {
	private static final String TAG = CompassUtil.class.getSimpleName();

	public static final File SYSTEM_SENSORS_CONF	= new File("/system/lib/hw/sensors.conf");
	public static final File DATA_SENSORS_CONF		= new File("/data/system/sensors.conf");

	/**
	 * Checks if /data/system/sensors.conf has been calibrated
	 * @return calibrated conf file is calibrated   
	 */
	public static boolean checkSensorsIsCalibrated() { 

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(DATA_SENSORS_CONF));
			String line = null;
			int counter = 0;
			while ((line = reader.readLine()) != null) {
				if (line.contains("conv_B")) {
					Log.d(TAG, line);
					counter++;
					if (counter == 3) {
						return line.matches("conv_B.*[1-9].*");
					}
				}
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException Reading sensors.conf");
		} catch (IOException e) {
			Log.e(TAG, "IOException Reading sonsors.conf");
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e2) {
					Log.e(TAG, "IOException closing BufferedReader");
				}
			}
		}

		return false;
	}

	/**
	 * Simple Method for copying file
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static boolean copy(File src, File dst) {
		InputStream in;
		OutputStream out;
		try {
			in = new FileInputStream(src);
			out = new FileOutputStream(dst);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage());
			return false;
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
			return false;
		}
		Log.d(TAG, "Copied " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
		return true;
	}

}
