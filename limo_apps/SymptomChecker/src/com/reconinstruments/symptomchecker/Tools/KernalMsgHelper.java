package com.reconinstruments.symptomchecker.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import android.os.Environment;

public class KernalMsgHelper {
	
	public static void SaveLogcatToFile(String path){
		try {
            Runtime.getRuntime().exec("logcat -d -v time -f " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	/**
	 * Get LOGCAT message
	 * @param stringWriter where the message is going to be written to
	 */
	public static String GetLogcat() {
		try {
			Process process = Runtime.getRuntime().exec("logcat -d");
			String logcatResult = ProcessToString(process);
			return logcatResult;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get DMESG message
	 * @param stringWriter where the message is going to be written to
	 */
	public static void GetDmesg(StringWriter stringWriter) {
		try {
			Process process = Runtime.getRuntime().exec("dmesg");
			String logcatResult = ProcessToString(process);
			if(logcatResult == null){
				logcatResult = "null\n";
			}
			stringWriter.append(logcatResult);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks if external storage is available for read and write
	 * @return 
	 */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}	
	
	public static String ProcessToString(Process process) throws IOException {
        InputStream stdin = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdin);
        BufferedReader br = new BufferedReader(isr);
        String line;
        String val = null;
        while ((line = br.readLine()) != null) {
            if (val == null) {
                val = line + "\n";
            } else {
                val += line + "\n";
            }
        }
        return val;
    }
}
