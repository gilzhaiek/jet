package com.reconinstruments.symptomchecker.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import android.os.Build;
import android.text.format.Time;
import android.util.Log;

import com.reconinstruments.symptomchecker.Config;
import com.reconinstruments.symptomchecker.TestBase;

public class ReportGenerator {

	private final static String TAG = "SymptomChecker:ReportGenerator";
	public static void Generate(List<TestBase> testList, String softwareVersion, String kernalVersion){
		StringWriter stringWriter = new StringWriter();

		Time now = new Time();
		now.setToNow();
		String timeString = now.format("%Y-%m-%d-%H%M%S"); // "YYYY-mm-dd-HHMMSS"

		CreateHeader(stringWriter, timeString, softwareVersion, kernalVersion);
		CreateTestResult(stringWriter, testList);
		CreateLineBreak(stringWriter);
		GetDmesg(stringWriter);
		CreateLineBreak(stringWriter);
		GetLogcat(stringWriter);
		CreateLineBreak(stringWriter);
		SaveToFile(stringWriter, Config.Directory, Config.ReportFile);
	}

	private static void GetDmesg(StringWriter stringWriter){
		stringWriter.append("Dmesg Info\n");
		//TODO Not working yet
		//KernalMsgHelper.GetDmesg(stringWriter);
	}

	private static void GetLogcat(StringWriter stringWriter){
		stringWriter.append("Logcat Info\n");
		String logcatPath = Config.Directory + "/" + Config.LogcatFile;
		KernalMsgHelper.SaveLogcatToFile(logcatPath);
		stringWriter.append("File Saved to " + logcatPath + "\n");
		//Not used because somehow taking too long
		//saved to another file instead
		/*
		String logcat = KernalMsgHelper.GetLogcat();
		if(logcat == null){
			logcat = "null";
		}
		stringWriter.append(logcat);
		 */
	}

	private static void CreateTestResult(StringWriter stringWriter,	List<TestBase> testList) {
		for(TestBase test : testList){
			stringWriter.append(test.GetTestName() + "	=	");
			if(test.IsSelected()){
				if(test.GetTestResult()){
					stringWriter.append("Passed ");
				}else{
					stringWriter.append("Failed ");
				}
			}
			else {
				stringWriter.append("not selected ");
			}
			stringWriter.append("	Comments: " + test.GetTestComments() + "\n");
		}
	}

	private static void CreateLineBreak(StringWriter stringWriter) {
		stringWriter.append("************************************************************\n");
	}

	private static void CreateHeader(StringWriter stringWriter, String timeString, String softwareVersion, String kernalVersion) {
		stringWriter.append("************************** Report **************************\n");
		stringWriter.append("Date : " + timeString + "\n");
		stringWriter.append("SerialNumber : " + Build.SERIAL + "\n");
		stringWriter.append("UnitModel : " + Build.MODEL + "\n");
		stringWriter.append("SoftwareVersion : " + softwareVersion + "\n");
		stringWriter.append("KernelVersion : " + kernalVersion + "\n\n");
				
	}

	private static void SaveToFile(StringWriter stringWriter, String directory,	String reportFile) {
		FileOutputStream fileOutputStream = null;
		try {

			//Create if Doesn't exist
			File folders = new File(directory);
			if (!folders.exists()) {
				folders.mkdirs();
			}

			File file = new File(directory, reportFile);

			//Create if Doesn't exist
			if (!file.exists()) {
				file.createNewFile();
			}

			//write to file
			fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(stringWriter.toString().getBytes());
			fileOutputStream.flush();
			fileOutputStream.close();
		} catch (IOException e) {
			Log.e("ReportGenerator", "Saving To File Erro" + e.toString());
		} finally {
			try {
				if (fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				Log.e("ReportGenerator", "Saving To File Erro" + e.toString());
			}
		}
	}	
}
