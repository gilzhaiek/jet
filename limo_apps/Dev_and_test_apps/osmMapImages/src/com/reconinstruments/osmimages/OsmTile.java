/**
 * 
 */
package com.reconinstruments.osmimages;


/**
 * @author simonwang
 *
 */
public class OsmTile {

	/**
	 * 
	 */
	private static final String TAG = "MapImages";
	protected Long mTileId = 0L;
	protected OsmBoundingBox mBound = null;
	

	
	
	public OsmTile(Long id) {
	
		this.mTileId = id;
		
		SetAttrs();
		
	}
	
	public void SetAttrs() {
		mBound = this.GetBoundingBox();
	}
	
	public OsmBoundingBox GetBoundingBox() {
		
		return OsmBoundingBox.GetBoundingBoxFromTileId(mTileId);
	}
	

	public static long GetTileIdlong(Long tileId) {
		long id;
		try {
			id = tileId.longValue();
		}
		catch(Exception e) {
			id = -1L;
		}
		return id;
	}

	public static String GetTileIdString(Long tileId) {
		String id = "" + tileId;
		return id;
	}


	
}
