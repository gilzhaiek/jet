package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class WO_Polyline extends WorldObject  implements Parcelable
{
	private final static String TAG = "Polyline";
	private static final long serialVersionUID = 1L;
	
	public ArrayList<PointXY> 	mPolylineNodes = new ArrayList<PointXY>();	// in world/GPS coords
	RectXY				mBoundingBox = new RectXY(0,0,0,0);
	PointXY				mCentroid = new PointXY(0,0);

	public WO_Polyline(WorldObjectTypes _type, String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(_type, name, dataSource);
		mPolylineNodes = _nodes;
		setBoundingBoxFromPolyline(mPolylineNodes);
		
		SetObjectID();
	}

	public WO_Polyline(Parcel _parcel) {				
		super(_parcel);			// calls (overridden) readFromParcel(_parcel);
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<WO_Polyline> CREATOR  = new Parcelable.Creator<WO_Polyline>() {
        public WO_Polyline createFromParcel(Parcel _parcel) {
            return new WO_Polyline(_parcel);
        }

        public WO_Polyline[] newArray(int size) {
            return new WO_Polyline[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
		super.writeToParcel(_parcel, flags);					// write all super member data to parcel first
//    	_parcel.writeValue(mBoundingBox);
//    	_parcel.writeValue(mCentroid);
//    	_parcel.writeList(mPolylineNodes);						// then write out members
		
		// revised to not store PointXY and RectXY member class information - highly redundant and wasteful especially for large polylines
    	_parcel.writeFloat(mBoundingBox.left);
    	_parcel.writeFloat(mBoundingBox.top);
    	_parcel.writeFloat(mBoundingBox.right);
    	_parcel.writeFloat(mBoundingBox.bottom);
    	_parcel.writeFloat(mCentroid.x);
    	_parcel.writeFloat(mCentroid.y);
    	_parcel.writeInt((int)mPolylineNodes.size());
    	for(PointXY point : mPolylineNodes) {
        	_parcel.writeFloat(point.x);
        	_parcel.writeFloat(point.y);
    	}
   }
    
    @Override
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	super.readFromParcel(_parcel);
//    	mBoundingBox = (RectXY) _parcel.readValue(getClass().getClassLoader());
//    	mCentroid = (PointXY) _parcel.readValue(getClass().getClassLoader());
//    	mPolylineNodes = new ArrayList<PointXY>();
//    	_parcel.readList(mPolylineNodes, getClass().getClassLoader());
    	
    	
    	mBoundingBox = new RectXY(_parcel.readFloat(), _parcel.readFloat(), _parcel.readFloat(), _parcel.readFloat());
    	mCentroid = new PointXY(_parcel.readFloat(), _parcel.readFloat());
    	int numNodes = _parcel.readInt();
//    	if(numNodes > 50) numNodes = 50;  // temp limit during debug to avoid memory overflow
    	mPolylineNodes = new ArrayList<PointXY>();
    	for(int i= 0; i<numNodes; i++) {
    		mPolylineNodes.add(new PointXY(_parcel.readFloat(), _parcel.readFloat()) );
    	}
    	numNodes = 0;
    }
    
//======================================
// methods
	protected void setBoundingBoxFromPolyline (ArrayList<PointXY> _nodes){
		mBoundingBox = new RectXY(999900.f, -999999.f, -999900.f, 999999.f);
		
		assert (_nodes != null && _nodes.size() > 0);
		
		for(PointXY node : _nodes) {
			if(node.x < mBoundingBox.left)   mBoundingBox.left   = node.x;
			if(node.x > mBoundingBox.right)  mBoundingBox.right  = node.x;
			if(node.y < mBoundingBox.bottom) mBoundingBox.bottom = node.y;
			if(node.y > mBoundingBox.top)    mBoundingBox.top    = node.y;
		}
		mCentroid = new PointXY((mBoundingBox.right + mBoundingBox.left)/2.f, (mBoundingBox.top + mBoundingBox.bottom)/2.f);
	}

	@Override
	public boolean InGeoRegion(GeoRegion geoRegion) {
		return mBoundingBox.Intersects(geoRegion.mBoundingBox);
	}

	public void SetObjectID() {	// virtual to be overwritten
		float sumX = 0.f;
		float sumY = 0.f;
		int cnt = 0;
		for(PointXY point : mPolylineNodes) {
			sumX += point.x;
			sumY += point.y;
			cnt ++;
		}
		mObjectID = String.format("T%d_%s%011d%011d", mType.ordinal(), (mName+WorldObject.IDNAME_PADDING).substring(0, WorldObject.OBJECTID_NAME_LENGTH), (int)(sumX/cnt *1000000), (int)(sumY/cnt *1000000));
	}
}
