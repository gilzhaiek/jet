package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.ReconBaseDataSource;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

public class ReconBaseAreaRecord extends ReconBaseDataRecord 
{
	private final static String TAG = "ReconBaseAreaRecord";
	
	public ArrayList<PointXY>	mPolylineNodes;
	public RectXY				mBoundingBox;
	public PointXY				mCentroid;
	
	public ReconBaseAreaRecord(ReconBaseDataSource.BaseDataTypes type, String name, ArrayList<PointXY> nodes, boolean isUserLocationDependent) {
		super(type, name, isUserLocationDependent);
		mPolylineNodes	= nodes;
		
		mBoundingBox = CalcBB(mPolylineNodes);
	}
	
	@Override
	public int PackingSize() throws UnsupportedEncodingException {
		int size = super.PackingSize();
		size += BYTES_IN_SHORT;  							// for length of nodes array
		size += mPolylineNodes.size() * 2 * BYTES_IN_FLOAT; // for nodes array data, @ 2 floats per node
		return size;
	}
	
	@Override
	public void PackIntoBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		super.PackIntoBuf(packedTileData);
		
		packedTileData.putShort((short)(mPolylineNodes.size() ));	// add size of nodes array
		
		for(PointXY node : mPolylineNodes) {				// put node data into buffer
			packedTileData.putFloat(node.x);
			packedTileData.putFloat(node.y);
		}
	}

	public void UnpackFromBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		super.UnpackFromBuf(packedTileData);
		
		int numNodes = (int)packedTileData.getShort();	// get size of node array
		mPolylineNodes = new ArrayList<PointXY>();		// create new node array

		for(int i=0; i< numNodes; i++) {				// unpack nodes
			mPolylineNodes.add(new PointXY(packedTileData.getFloat(), packedTileData.getFloat() ));
		}
	}

	public RectXY CalcBB(ArrayList<PointXY> nodes) {
		mBoundingBox = new RectXY(999900.f, -999999.f, -999900.f, 999999.f);
		
		assert (nodes != null && nodes.size() > 0);
		
		for(PointXY node : nodes) {
			if(node.x < mBoundingBox.left)   mBoundingBox.left   = node.x;
			if(node.x > mBoundingBox.right)  mBoundingBox.right  = node.x;
			if(node.y < mBoundingBox.bottom) mBoundingBox.bottom = node.y;
			if(node.y > mBoundingBox.top)    mBoundingBox.top   = node.y;
		}
		mCentroid = new PointXY(mBoundingBox.right - mBoundingBox.left, mBoundingBox.top - mBoundingBox.bottom);

		return mBoundingBox;

	}

	@Override
	public boolean ContainedInGR(RectXY GRbb) {
		RectXY rBB = mBoundingBox;
		if(GRbb.Intersects(mBoundingBox)) {
//			Log.e(TAG, " - Area hit test: " + mName + ": " + GRbb.left + "|" + rBB.left + " : " + GRbb.right + "|" + rBB.right + " : " + GRbb.top + "|" + rBB.top + " : " + GRbb.bottom + "|" + rBB.bottom);
			return true;
		}
		else {
//			Log.d(TAG, " - Area hit test: " + mName + ": " + GRbb.left + "|" + rBB.left + " : " + GRbb.right + "|" + rBB.right + " : " + GRbb.top + "|" + rBB.top + " : " + GRbb.bottom + "|" + rBB.bottom);
			return false;
		}
	}

	public void AddPoint(float longitude, float latitude) {
		mPolylineNodes.add(new PointXY(longitude, latitude));
	}
	
}
