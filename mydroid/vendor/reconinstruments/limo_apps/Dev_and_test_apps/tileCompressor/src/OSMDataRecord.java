

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;


public class OSMDataRecord {
	private final static String TAG = "MDDataRecord";
	protected final static int BYTES_IN_FLOAT = Float.SIZE/8;
	protected final static int BYTES_IN_SHORT = Short.SIZE/8;

	public TileCompressor.OSMBaseDataTypes	mOSMType;
	public String							mName;
	
	public OSMDataRecord(TileCompressor.OSMBaseDataTypes osmType, String name, boolean isUserLocationDependent) {
		mOSMType = osmType;
		mName = name;
	}	
	
	public boolean ContainedInGR(RectXY GRbb) {
		return false;
	}
	
	public int PackingSize() throws UnsupportedEncodingException {
		return  BYTES_IN_SHORT + mName.getBytes("UTF-8").length;
	}
	
	public void PackIntoBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		byte[] nameAsByteArray =  mName.getBytes("UTF-8");
		packedTileData.putShort((short)nameAsByteArray.length);	// add size of name array
		packedTileData.put(nameAsByteArray);					// add name
	}
	
	public void UnpackFromBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		int lengthNameInBytes = (int)packedTileData.getShort();	// get size of name array
		byte[] nameBuf = new byte[lengthNameInBytes];
		packedTileData.get(nameBuf, 0, lengthNameInBytes);		// get name bytes into nameBuf
		mName = new String(nameBuf, "UTF-8");
	}
	
}
