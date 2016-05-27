package com.example.audiorecord;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class WriteThread extends Thread implements Runnable {
	int bufferSize;
	ArrayList<AudioBuffer> arrayBuffer;

	public WriteThread(int bufferSize, ArrayList<AudioBuffer> arrayBuffer) {
		this.bufferSize=bufferSize;
		this.arrayBuffer=arrayBuffer;
	}

	public void run(){
		String filename = MainActivity.getTempFilename();
		FileOutputStream os = null;

		int index = 0;
		try {
				os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			Log.e(MainActivity.LOG_TAG, "fail to open file");
			e.printStackTrace();
		}
		
		RingBufferControl bufferControl=MainActivity.getmBufferControl();
		if(os!=null){
			while(MainActivity.getRecordState()){
				
				if(bufferControl.setorgetState(false,false)==RingBufferControl.BUFFER_EMPTY){
					//Log.w(MainActivity.LOG_TAG, "Buffer empty");
					continue;
				}
				//Write to SD card
				index=bufferControl.GetPopIndex();

				try{
					os.write(arrayBuffer.get(index).mBuffer, 0, bufferSize);
					Log.v(MainActivity.LOG_TAG, "write "+index);
					bufferControl.UpdatePopIndex();
				} catch (IOException e) {
					MainActivity.setRecordState(false);
					Log.e(MainActivity.LOG_TAG, "write fail");
					e.printStackTrace();
					break;
				}
		}

			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
