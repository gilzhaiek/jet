package com.example.audiorecord;

import java.util.ArrayList;

import android.media.AudioRecord;
import android.util.Log;

public class RecordThread extends Thread implements Runnable{
	AudioRecord recorder;
	int bufferSize;
	ArrayList<AudioBuffer> arrayBuffer;
	
	public RecordThread(AudioRecord recorder, int bufferSize, ArrayList<AudioBuffer>arrayBuffer) {
		this.recorder=recorder;
		this.bufferSize=bufferSize;
		this.arrayBuffer=arrayBuffer;
	}

	public void run(){
		int read_size = 0;
		int index;
		RingBufferControl bufferControl=MainActivity.getmBufferControl();
		while(MainActivity.getRecordState()){
			
			if(bufferControl.setorgetState(false,false)== RingBufferControl.BUFFER_FULL){
				Log.w(MainActivity.LOG_TAG, "Buffer full");
				continue;
			}
			
			index=bufferControl.GetPushIndex();
			
			read_size = recorder.read(arrayBuffer.get(index).mBuffer, 0, bufferSize);
			
			if(AudioRecord.ERROR_INVALID_OPERATION != read_size){
				Log.v(MainActivity.LOG_TAG, "record "+index);
				bufferControl.UpdatePushIndex();
				/*
				if(MainActivity.mWriteSDThread.getState()== Thread.State.NEW){
					Log.w(MainActivity.LOG_TAG, "Write begin");
					MainActivity.mWriteSDThread.start();
				}
				*/
			}
			else
			{
				MainActivity.setRecordState(false);
				Log.e(MainActivity.LOG_TAG, "record error");
				break;
			}
		}
	}
}
