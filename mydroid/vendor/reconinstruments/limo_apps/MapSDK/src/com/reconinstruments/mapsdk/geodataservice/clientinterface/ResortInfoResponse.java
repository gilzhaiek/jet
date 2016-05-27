package com.reconinstruments.mapsdk.geodataservice.clientinterface;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;

public class ResortInfoResponse  implements Parcelable {
// constants
	private static final long serialVersionUID = 745688859829339L;
// members
	public String	mName;
	public float	mDistanceFromTargetPoint;
	public boolean  mTargetPointWithinResortBoundingBox;
	public PointXY	mLocation;
	public RectXY	mGPSBoundaryBox;
	public PointXY	mScreenLocation;	// used only in rendering
	public MeshGL 	mMesh;
		
// constructors
	public ResortInfoResponse(String name, float distance, PointXY location, RectXY boundingBox, boolean inBB){
		mName					 = name;
		mDistanceFromTargetPoint = distance;
		mLocation = location;
		mTargetPointWithinResortBoundingBox = inBB;
		mGPSBoundaryBox = boundingBox;
	}
	
	public ResortInfoResponse(Parcel _parcel) {				
    	readFromParcel(_parcel);
    }
	
//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ResortInfoResponse> CREATOR  = new Parcelable.Creator<ResortInfoResponse>() {
        public ResortInfoResponse createFromParcel(Parcel _parcel) {
            return new ResortInfoResponse(_parcel);
        }

        public ResortInfoResponse[] newArray(int size) {
            return new ResortInfoResponse[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeString(mName);
    	_parcel.writeFloat(mDistanceFromTargetPoint);
    	_parcel.writeInt(mTargetPointWithinResortBoundingBox ? 1 : 0);
    	_parcel.writeFloat(mLocation.x);
    	_parcel.writeFloat(mLocation.y);
    	_parcel.writeFloat(mGPSBoundaryBox.left);
    	_parcel.writeFloat(mGPSBoundaryBox.top);
    	_parcel.writeFloat(mGPSBoundaryBox.right);
    	_parcel.writeFloat(mGPSBoundaryBox.bottom);
   }
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	mName = _parcel.readString();
    	mDistanceFromTargetPoint = _parcel.readFloat();
    	mTargetPointWithinResortBoundingBox = (_parcel.readInt() == 1);
    	mLocation = new PointXY(_parcel.readFloat(), _parcel.readFloat());
    	mGPSBoundaryBox = new RectXY(_parcel.readFloat(), _parcel.readFloat(), _parcel.readFloat(), _parcel.readFloat());
    }
}
