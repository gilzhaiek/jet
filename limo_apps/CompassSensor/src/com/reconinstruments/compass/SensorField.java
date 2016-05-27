package com.reconinstruments.compass;

public class SensorField {
	
	public Key mKey;
	public float[] mValue = new float[3];
	public int mLine;
	public boolean modified = false;
	
	/**
	 * For fields with 3 value. IE: "rot_A = 1.0 0.0 0.0 
	 * @param key
	 * @param value (three) values
	 * @param line
	 */
	SensorField(Key key,float[] value,int line){
		mKey = key;
		mValue = value;
		mLine = line;

	}
	
	/**
	 * For fields with 1 value. IE: "conv_A = 0.0192
	 * @param key
	 * @param value (one) value
	 * @param line
	 */
	SensorField(Key key,float value,int line){
		mKey = key;
		mValue[0] = value;
		mValue[1] = value;
		mValue[2] = value;
		mLine = line;

	}
	
	/**
	 * For fields that are expected to be added. IE: "#conv_B"
	 * @param key
	 * @param line
	 */
	SensorField(Key key,int line){
		mKey = key;
		mLine = line;
		setDefaultValues(key);
	}
	
	/**
	 * Set default values for fields that are factory commented out
	 * @param key
	 */
	private void setDefaultValues(Key key){
		if (key == Key.conv_A){
			mValue[0] = 1;mValue[1] = 1;mValue[2] = 1;
		}else if(key == Key.conv_B){
			mValue[0] = 0;mValue[1] = 0;mValue[2] = 0;
		}			
	}
	

}
