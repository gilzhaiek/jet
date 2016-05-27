package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;

public class WO_Polyline extends WorldObject  implements Serializable
{
	private final static String TAG = "Polyline";
	private static final long serialVersionUID = 1L;
	
	ArrayList<PointXY> 	mPolylineNodes;	// in world/GPS coords
	RectXY				mBoundingBox;
	PointXY				mCentroid;

	
	public WO_Polyline(WorldObjectTypes _type, String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(_type, name, dataSource);
		mPolylineNodes = _nodes;
		setBoundingBoxFromPolyline(mPolylineNodes);
		
		SetObjectID();
	}
	
	protected void setBoundingBoxFromPolyline (ArrayList<PointXY> _nodes){
		mBoundingBox = new RectXY(999900.f, -999999.f, -999900.f, 999999.f);
		
		assert (_nodes != null && _nodes.size() > 0);
		
		for(PointXY node : _nodes) {
			if(node.x < mBoundingBox.left)   mBoundingBox.left   = node.x;
			if(node.x > mBoundingBox.right)  mBoundingBox.right  = node.x;
			if(node.y < mBoundingBox.bottom) mBoundingBox.bottom = node.y;
			if(node.y > mBoundingBox.top)    mBoundingBox.left   = node.y;
		}
		mCentroid = new PointXY(mBoundingBox.right - mBoundingBox.left + 1.f, mBoundingBox.top - mBoundingBox.bottom + 1.f);
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
