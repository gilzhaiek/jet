

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;


public class OSMPOIRecord extends OSMDataRecord 
{
	private final static String TAG = "OSMPOIRecord";

	public PointXY	mLocation;
	
	public OSMPOIRecord(TileCompressor.OSMBaseDataTypes type, String name, PointXY location, boolean isUserLocationDependent) {
		super(type, name, isUserLocationDependent);
		mLocation = location;
	}
	
	@Override
	public int PackingSize() throws UnsupportedEncodingException {
		int size = super.PackingSize();
		size += 2 * BYTES_IN_FLOAT; 			// for gps location, PointXY = 2 floats
		return size;
	}
	
	@Override
	public void PackIntoBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		super.PackIntoBuf(packedTileData);
		
		packedTileData.putFloat(mLocation.x);
		packedTileData.putFloat(mLocation.y);
	}

	public void UnpackFromBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		super.UnpackFromBuf(packedTileData);
		
		mLocation = new PointXY(packedTileData.getFloat(), packedTileData.getFloat() );
	}


	@Override
	public boolean ContainedInGR(RectXY GRbb) {
		if(GRbb.Contains(mLocation.x, mLocation.y) ) {
//			Log.e(TAG, " - POI hit test: " + mName + ": " + mLocation.x + ", " + ": " + mLocation.x + ", | "+ GRbb.left +  " : " + GRbb.right + " : " + GRbb.top + " : " + GRbb.bottom );
			return true;
		}
		else {
//			Log.d(TAG, " - POI hit test: " + mName + ": " + mLocation.x + ", " + ": " + mLocation.x + ", | "+ GRbb.left +  " : " + GRbb.right + " : " + GRbb.top + " : " + GRbb.bottom );
			return false;
		}	
	}

	public void SetLocation(float longitude, float latitude) {
		mLocation = new PointXY(longitude, latitude);
	}
}
