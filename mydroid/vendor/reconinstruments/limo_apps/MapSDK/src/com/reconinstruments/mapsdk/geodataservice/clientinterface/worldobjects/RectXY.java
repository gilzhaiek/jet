package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

public class RectXY implements Parcelable 	// designed for world coordinates where top > bottom (implemented to avoid confusion related to RectF usage, where Y is +ve down)
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
	
	public RectXY(Parcel _parcel) {				// data in (decoding)
    	readFromParcel(_parcel);	
    }
	
//============ parcelable protocol handlers

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<RectXY> CREATOR  = new Parcelable.Creator<RectXY>() {
        public RectXY createFromParcel(Parcel _parcel) {
            return new RectXY(_parcel);
        }

        public RectXY[] newArray(int size) {
            return new RectXY[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeFloat(left);
    	_parcel.writeFloat(top);
    	_parcel.writeFloat(right);
    	_parcel.writeFloat(bottom);
   }
    
    protected void readFromParcel(Parcel _parcel) {				
    	left = _parcel.readFloat();
    	top = _parcel.readFloat();
    	right = _parcel.readFloat();
    	bottom = _parcel.readFloat();
    }
    
// ===========
// methods
	
	public boolean Contains(float _x, float _y) {
		return (_x>=left && _x<=right && _y>=bottom && _y<=top);
	}
	
	public boolean Contains(RectXY _rect) {
		return (_rect.left >= left && _rect.right <= right && _rect.top <= top && _rect.bottom >= bottom);
	}
	
//	public boolean Intersects(RectXY _rect) {
//		return !(_rect.right<left || _rect.left>right || _rect.bottom >top || _rect.top< bottom);
//	}
	
	public boolean Intersects(RectXY _rect) {
		return _rect.left < right && left < _rect.right && bottom < _rect.top && _rect.bottom < top;
	}
}