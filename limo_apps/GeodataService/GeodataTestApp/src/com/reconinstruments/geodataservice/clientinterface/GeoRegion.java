package com.reconinstruments.geodataservice.clientinterface;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.RectXY;

public class GeoRegion implements Parcelable
{
	protected PointXY	mCenterPoint;
	protected PointXY	mSize;
	protected boolean	mCenterOnUser;
	public RectXY		mBoundingBox;
	
	public GeoRegion() {
		mCenterPoint = null;
		mSize = null;
		mCenterOnUser = false;
		mBoundingBox = null;
	}

	public GeoRegion MakeAroundCenterPoint(float _centerX, float _centerY, float _width, float _height ) {
		mCenterOnUser = false;
		mCenterPoint = new PointXY(_centerX, _centerY);
		mBoundingBox = new RectXY(_centerX - _width/2.f, _centerY + _height/2.f, _centerX + _width/2.f, _centerY - _height/2.f);
		mSize = new PointXY(_width, _height);
		return this;
	}

	public GeoRegion MakeUsingBoundingBox(float _left, float _top, float _right, float _bottom ) {
		mCenterOnUser = false;
		mCenterPoint = new PointXY((_right+_left)/2.f, (_top+_bottom)/2.f);
		mBoundingBox = new RectXY(_left, _top, _right, _bottom);
		mSize = new PointXY(_right-_left, _top-_bottom); 
		return this;
	}

	public GeoRegion MakeUsingCenteredOnUser(float _width, float _height) {  // center tracks user - boundary defined later when user location known
		mCenterOnUser = true;
		mSize = new PointXY(_width, _height);
		mCenterPoint = new PointXY(0.f, 0.f);
		mBoundingBox = new RectXY(mCenterPoint.x - _width/2.f, mCenterPoint.y + _height/2.f, mCenterPoint.x + _width/2.f, mCenterPoint.y - _height/2.f);

		return this;
	} 

	public GeoRegion ScaledCopy(float scalePercent) {
		return (new GeoRegion().MakeAroundCenterPoint(mCenterPoint.x, mCenterPoint.y, mSize.x * scalePercent/100.0f, mSize.y * scalePercent/100.0f));
	}


//============ support methods
	public boolean IsCenteredOnUser() {
		return mCenterOnUser;
	}
	
	public int describeContents() {
        return 0;
    }

//============ parcelable protocol handlers
    public static final Parcelable.Creator<GeoRegion> CREATOR  = new Parcelable.Creator<GeoRegion>() {
        public GeoRegion createFromParcel(Parcel _parcel) {
            return new GeoRegion(_parcel);
        }

        public GeoRegion[] newArray(int size) {
            return new GeoRegion[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeFloat(mCenterPoint.x);
    	_parcel.writeFloat(mCenterPoint.y);
    	_parcel.writeInt(mCenterOnUser ? 1 : 0);
    	_parcel.writeFloat(mBoundingBox.left);
    	_parcel.writeFloat(mBoundingBox.top);
    	_parcel.writeFloat(mBoundingBox.right);
    	_parcel.writeFloat(mBoundingBox.bottom);
   }

    private GeoRegion(Parcel _parcel) {				// constructor from parcel - data in (decoding)
    	mCenterPoint = new PointXY(_parcel.readFloat(), _parcel.readFloat());
    	mCenterOnUser = (_parcel.readInt() == 1 ? true : false);
    	mBoundingBox = new RectXY(_parcel.readFloat(), _parcel.readFloat(), _parcel.readFloat(), _parcel.readFloat());
    }


}

