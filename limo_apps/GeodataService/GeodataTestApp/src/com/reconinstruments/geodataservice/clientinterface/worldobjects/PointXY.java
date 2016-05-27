package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class PointXY implements Serializable 
{
	public float	x;
	public float   y;

	public PointXY(float _x, float _y) {
		x= _x;
		y= _y;
	}
	
}