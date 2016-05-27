package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class RectXY implements Serializable 	// designed for world coordinates where top > bottom (implemented to avoid confusion related to RectF usage, where Y is +ve down)
{
	public float	left;		 
	public float   	top;
	public float 	right;
	public float	bottom;

	public RectXY(float _left, float _top, float _right, float _bottom) {
		left = _left;
		top = _top;
		right = _right;
		bottom = _bottom;
	}
	
	public boolean Contains(float _x, float _y) {
		return (_x>=left && _x<=right && _y>=bottom && _y<=top);
	}
	
	public boolean Contains(RectXY _rect) {
		return (_rect.left >= left && _rect.right <= right && _rect.top <= top && _rect.bottom >= bottom);
	}
	
	public boolean Intersects(RectXY _rect) {
		return !(_rect.right<left || _rect.left>right || _rect.bottom >top || _rect.top< bottom);
	}
	
}