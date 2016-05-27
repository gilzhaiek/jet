
public class S3Tile {
	Integer	mIndex;
	boolean mUsed;
	RectXY  mBounds;
	
	public S3Tile(Integer index, RectXY bounds) {
		mIndex = index;
		mUsed = false;
		mBounds = bounds;
	}
}
