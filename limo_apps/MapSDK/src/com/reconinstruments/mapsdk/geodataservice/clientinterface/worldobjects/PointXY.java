package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

public class PointXY implements Parcelable 
{
	public float	x;
	public float   y;

	public PointXY(float _x, float _y) {
		x= _x;
		y= _y;
	}

	public PointXY(Parcel _parcel) {				// data in (decoding)
    	readFromParcel(_parcel);	
    }
	
	public double DistanceToPoint(PointXY p) {
		return Math.sqrt((double)((x-p.x)*(x-p.x) + (y-p.y)*(y-p.y)));
	}
	
//============ parcelable protocol handlers

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PointXY> CREATOR  = new Parcelable.Creator<PointXY>() {
        public PointXY createFromParcel(Parcel _parcel) {
            return new PointXY(_parcel);
        }

        public PointXY[] newArray(int size) {
            return new PointXY[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeFloat(x);
    	_parcel.writeFloat(y);
   }
    
    protected void readFromParcel(Parcel _parcel) {				
    	x = _parcel.readFloat();
    	y = _parcel.readFloat();
    }
    

}