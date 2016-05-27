package com.reconinstruments.compass;
import java.util.ArrayList;
import java.util.List;


public class Sensor {

	private List<SensorField> mFieldList = new ArrayList<SensorField>() ;
	
	public Sensor(){
		
	}
	
	public void addField(Key key, float[] value, int line){
		mFieldList.add(new SensorField(key,value.clone(),line));
	}
	public void addField(Key key, float value, int line){
		mFieldList.add(new SensorField(key,value,line));
	}
	public void addField(Key key, int line){
		mFieldList.add(new SensorField(key,line));
	}
	
	/**
	 * Get field value
	 * 
	 * If FieldType is ONE, the array will be filled with the single conv_A value
	 * Returns null if FieldType is COMMENT.
	 * 
	 * @param sensor
	 * @return conv_A array
	 */
	public float[] getFieldValue(Key key){		
		for (SensorField fieldList : mFieldList){
			if (fieldList.mKey == key){
				return fieldList.mValue;
				
			}
		}
		return null;
	}
	
	public int getFieldLine(Key key){
		for (SensorField fieldList : mFieldList){
			if (fieldList.mKey == key)
				return fieldList.mLine;
		}
		return -1;
	}
	
	public void updateField(Key key, float[] value){
		for (SensorField fieldList : mFieldList){
			if (fieldList.mKey == key){
				fieldList.mValue = value;
				fieldList.modified = true;
			}
		}
	}
	
	public void updateField(Key key, double[] value){
		float[] value_f = new float[3];
		for (int i=0; i<3; i++){
			value_f[i] = (float)value[i];
		}
		
		for (SensorField fieldList : mFieldList){
			if (fieldList.mKey == key){
				fieldList.mValue = value_f;
				fieldList.modified = true;
			}
		}
	}
	
	public boolean checkModified(Key key){
		for (SensorField fieldList : mFieldList){
			if (fieldList.mKey == key)
				if (fieldList.modified)
					return true;
		}
		return false;		
	}	
}
