package com.example.audiorecord;
//File
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

//media
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	static final String LOG_TAG= "PCMRecord";

	private static final boolean OPTIMIZATION=true;
	private static final int AUDIOBUFFER_SIZE=24;
	private static final int RECORDER_BPP = 16;
	private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private static final int RECORDER_SAMPLERATE = 44100;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;//CHANNEL_IN_STEREO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	public AudioRecord mRecorder = null;
	private int mBufferSize = 0;
	private static boolean misRecording = false;

	private Thread recordingThread = null;

	private ArrayList<AudioBuffer> mArrayBuffer;
	private RecordThread mRecordThread= null;
	private static RingBufferControl mBufferControl=null;
	public static WriteThread  mWriteSDThread= null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setButtonHandlers();
		enableButtons(false);
		 
		mBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING);
		Log.d(LOG_TAG,"buffer size="+mBufferSize);

	}

	private void setButtonHandlers() {
		((Button)findViewById(R.id.start)).setOnClickListener(btnClick);
		((Button)findViewById(R.id.close)).setOnClickListener(btnClick);
	}

	private void enableButton(int id,boolean isEnable){
		((Button)findViewById(id)).setEnabled(isEnable);
	}

	private void enableButtons(boolean isRecording) {
			enableButton(R.id.start,!isRecording);
			enableButton(R.id.close,isRecording);
		}

	private String getFilename(){
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath,AUDIO_RECORDER_FOLDER);
		 
		if(!file.exists()){
				file.mkdirs();
		}
		 
		//return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
		return (file.getAbsolutePath() + "/test" + AUDIO_RECORDER_FILE_EXT_WAV);
	}

	public static String getTempFilename(){

		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath,AUDIO_RECORDER_FOLDER);
		 
		if(!file.exists()){
				file.mkdirs();
		}
		 
		File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
		 
		if(tempFile.exists())
				tempFile.delete();
		 
		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
		//Test ram write purpose, mkdir test and chown system.system
		//return ("/test/" + AUDIO_RECORDER_TEMP_FILE);
	}

	public static boolean getRecordState(){
		return misRecording;
	};
	
	public synchronized static void setRecordState(boolean state){
		misRecording=state;
	};
	@SuppressWarnings("unused")
	private void startRecording(){
		mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
					RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, mBufferSize);

		int i = mRecorder.getState();
		if(i==1){
			Log.d(LOG_TAG, "start Recording");
			mRecorder.startRecording();
		}
		misRecording = true;
		//None optimization------------
		if(OPTIMIZATION==false){
			recordingThread = new Thread(new Runnable() {
					 
					@Override
					public void run() {
							writeAudioDataToFile();
					}
			},"AudioRecorder Thread");
			 
			recordingThread.start();
		} else {//Optimization read and write----------
			try {
				//Create buffer
				//=new AudioBuffer[AUDIOBUFFER_SIZE];
				mArrayBuffer=new ArrayList<AudioBuffer>();
				
				for(i=0; i<AUDIOBUFFER_SIZE; i++){
					//mArrayBuffer[i]=new AudioBuffer(mBufferSize);
					mArrayBuffer.add(new AudioBuffer(mBufferSize));
				}
				Log.d(LOG_TAG, "AudioBuffer init");
				setmBufferControl(new RingBufferControl(AUDIOBUFFER_SIZE));
				//Create thread
				mRecordThread = new RecordThread(mRecorder, mBufferSize, mArrayBuffer);
				mWriteSDThread = new WriteThread(mBufferSize,mArrayBuffer);

				mRecordThread.start();
				mWriteSDThread.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void writeAudioDataToFile(){
		byte data[] = new byte[mBufferSize];
		String filename = getTempFilename();
		FileOutputStream os = null;

		try {
				os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_TAG, "fail to open file");
			e.printStackTrace();
		}

		int read = 0;

		if(null != os){
			while(misRecording){
				read = mRecorder.read(data, 0, mBufferSize);
				//TODO: Critical part, read then immediately write, need to optimize
				if(AudioRecord.ERROR_INVALID_OPERATION != read){
					try {
							os.write(data);
					} catch (IOException e) {
							Log.e(LOG_TAG, "write fail");
							e.printStackTrace();
					}
				}
				else
				{
					Log.e(LOG_TAG, "read audio fail");
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

	@SuppressWarnings("unused")
	private void stopRecording(){
		if(null != mRecorder){
			setRecordState(false);
			 
			int i = mRecorder.getState();
			if(i==1){
				Log.d(LOG_TAG, "stop Recording now");
				mRecorder.stop();
			}
			mRecorder.release();

			mRecorder = null;
			if(OPTIMIZATION==false){
				recordingThread = null;
			} else {
				mRecordThread=null;
				mWriteSDThread=null;
				mArrayBuffer.clear();
				mArrayBuffer=null;
			}
		}

		copyWaveFile(getTempFilename(),getFilename());
		deleteTempFile();
	}
 
	private void deleteTempFile() {
		File file = new File(getTempFilename());
		 
		file.delete();
	}

	private void copyWaveFile(String inFilename,String outFilename){
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = RECORDER_SAMPLERATE;
		int channels = 1;
		long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
		 
		byte[] data = new byte[mBufferSize];
		 
		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
			 
			Log.d(LOG_TAG,"File size: " + totalDataLen);
			 
			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
							longSampleRate, channels, byteRate);
			 
			while(in.read(data) != -1){
					out.write(data);
			}
			 
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void WriteWaveFileHeader(
				FileOutputStream out, long totalAudioLen,
				long totalDataLen, long longSampleRate, int channels,
				long byteRate) throws IOException {
		 
		byte[] header = new byte[44];
		 
		header[0] = 'R';  // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f';  // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1;  // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8);  // block align
		header[33] = 0;
		header[34] = RECORDER_BPP;  // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		out.write(header, 0, 44);
	}

	 private View.OnClickListener btnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch(v.getId()){
					case R.id.start:{
						//Log.i(LOG_TAG, "Start Recording");

						enableButtons(true);
						startRecording();
						break;
					}
					case R.id.close:{
						//Log.i(LOG_TAG, "Stop Recording");

						enableButtons(false);
						stopRecording();
						break;
					}
			}
		}
	}; 

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public static RingBufferControl getmBufferControl() {
		return mBufferControl;
	}

	public static void setmBufferControl(RingBufferControl mBufferControl) {
		MainActivity.mBufferControl = mBufferControl;
	}

}