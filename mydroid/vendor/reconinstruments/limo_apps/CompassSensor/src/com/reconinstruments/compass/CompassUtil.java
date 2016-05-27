package com.reconinstruments.compass;

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
	/** Parses /data/system/sensors.conf to extract the applied matrix
	 * @return calibrated conf file is calibrated
	 */
	public static boolean getSensorMatrix(float[] matrix) {
		boolean FoundMag = false;
		boolean A,B,C;
		A=B=C = false;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(DATA_SENSORS_CONF));
			String line = null;
			int counter = 0;
			while ((line = reader.readLine()) != null) {
					if (line.matches("Handle.[ \t]*= 0x02.*")) {
					FoundMag = true;
				}
				if (FoundMag == true)
				{
					if (line.matches("^rot_[ABC].*"))
					{
						if (line.matches("^rot_A.*"))
						{
							String vals[] = line.split(" ");
							matrix[0] = Float.parseFloat(vals[2]);
							matrix[1] = Float.parseFloat(vals[3]);
							matrix[2] = Float.parseFloat(vals[4]);
							A = true;
						}
						if (line.matches("^rot_B.*"))
						{
							String vals[] = line.split(" ");
							matrix[3] = Float.parseFloat(vals[2]);
							matrix[4] = Float.parseFloat(vals[3]);
							matrix[5] = Float.parseFloat(vals[4]);
							B = true;
						}
						if (line.matches("^rot_C.*"))
						{
							String vals[] = line.split(" ");
							matrix[6] = Float.parseFloat(vals[2]);
							matrix[7] = Float.parseFloat(vals[3]);
							matrix[8] = Float.parseFloat(vals[4]);
							C = true;
						}
						if (A && B && C)
							return true;
					}
				}
			}

		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException Reading sensors.conf");
		} catch (IOException e) {
			Log.e(TAG, "IOException Reading sensors.conf");
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
}
