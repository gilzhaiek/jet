package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords;

import java.util.ArrayList;

import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.MDDataSource;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

public class MDTrailRecord extends MDDataRecord 
{
	private final static String TAG = "MDTrailRecord";
	public static final int NO_SPEED_LIMIT		= -1;

	public ArrayList<PointXY>	mPolylineNodes;
	public int					mSpeedLimit;
	public boolean				mIsOneWay;
	public RectXY				mBoundingBox;
	public PointXY				mCentroid;
	
	public MDTrailRecord(MDDataSource.MDBaseDataTypes type, String name, ArrayList<PointXY> nodes, int speedLimit, boolean isOneWay, boolean isUserLocationDependent) {
		super(type, name, isUserLocationDependent);
		mPolylineNodes = nodes;
		mSpeedLimit = speedLimit;
		mIsOneWay = isOneWay;		
		
		mBoundingBox = CalcBB(mPolylineNodes);
	}
	
	public RectXY CalcBB(ArrayList<PointXY> nodes) {
		mBoundingBox = new RectXY(999900.f, -999999.f, -999900.f, 999999.f);
		
		assert (nodes != null && nodes.size() > 0);
		
		for(PointXY node : nodes) {
			if(node.x < mBoundingBox.left)   mBoundingBox.left   = node.x;
			if(node.x > mBoundingBox.right)  mBoundingBox.right  = node.x;
			if(node.y < mBoundingBox.bottom) mBoundingBox.bottom = node.y;
			if(node.y > mBoundingBox.top)    mBoundingBox.top    = node.y;
		}
		mCentroid = new PointXY(mBoundingBox.right - mBoundingBox.left, mBoundingBox.top - mBoundingBox.bottom);
		
		return mBoundingBox;
	}

	@Override
	public boolean ContainedInGR(RectXY GRbb) {
		RectXY rBB = mBoundingBox;
		if(GRbb.Intersects(mBoundingBox)) {
//			Log.e(TAG, " - Trail hit test: " + mName + ": " + GRbb.left + "|" + rBB.left + " : " + GRbb.right + "|" + rBB.right + " : " + GRbb.top + "|" + rBB.top + " : " + GRbb.bottom + "|" + rBB.bottom);
			return true;
		}
		else {
//			Log.d(TAG, " - Trail hit test: " + mName + ": " + GRbb.left + "|" + rBB.left + " : " + GRbb.right + "|" + rBB.right + " : " + GRbb.top + "|" + rBB.top + " : " + GRbb.bottom + "|" + rBB.bottom);
			return false;
		}
	}


}
