



public class GeoRegion 
{
	private final static String TAG = "GeoRegion";
	private final static double meridonalCircumference = 40007860.0; // m  - taken from wikipedia/Earth
	private final static double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth

	
	public PointXY	mCenterPoint;
	protected PointXY	mSize;
	protected boolean	mCenterOnUser;
	public RectXY		mBoundingBox;
	
	public GeoRegion() {
		mCenterPoint = null;
		mSize = null;
		mCenterOnUser = false;
		mBoundingBox = null;
	}

	public GeoRegion(GeoRegion templateGeoRegion) {
		mCenterPoint = new PointXY(templateGeoRegion.mCenterPoint.x, templateGeoRegion.mCenterPoint.y);
		mSize = new PointXY(templateGeoRegion.mSize.x, templateGeoRegion.mSize.y);
		mCenterOnUser = templateGeoRegion.mCenterOnUser;
		mBoundingBox = new RectXY(templateGeoRegion.mBoundingBox.left, templateGeoRegion.mBoundingBox.top, templateGeoRegion.mBoundingBox.right, templateGeoRegion.mBoundingBox.bottom);
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

	public GeoRegion ScaledCopy(float scaleFactor) {
		PointXY center = mCenterPoint;
		PointXY size = mSize;
//		Log.e(TAG,"Scaling GR: " + center.x + ", " + center.y + ", " + size.x  + ", " + size.y + ", " + scaleFactor);
		return (new GeoRegion().MakeAroundCenterPoint(center.x, center.y, size.x * scaleFactor, size.y * scaleFactor));
	}


//============ support methods
	public boolean IsCenteredOnUser() {
		return mCenterOnUser;
	}
	
	public int describeContents() {
        return 0;
    }

	public boolean Contains(GeoRegion geoRegion) {
		// TODO Auto-generated method stub
		if(mBoundingBox == null) return false;
		return mBoundingBox.Contains(geoRegion.mBoundingBox);
	}

	public boolean Contains(float longitude, float latitude) {
		// TODO Auto-generated method stub
		if(mBoundingBox == null) return false;
		return mBoundingBox.Contains(longitude, latitude);
	}

    public float widthInMeters() {

        float widthInM = 0.f;
        float widthDegrees = 0.f;
        
        if(mBoundingBox.right > mBoundingBox.left) {  // normal case
            widthDegrees = mBoundingBox.right - mBoundingBox.left;
        }
        else {  // crossing the +/- 180 longitude line (rare case on land)
            widthDegrees = mBoundingBox.right + 360.f - mBoundingBox.left;
        }

        widthInM = (float)(widthDegrees / 360.0f * (equitorialCircumference*Math.cos(Math.toRadians(mCenterPoint.y))) );    
//      Log.e(TAG, "in widthInMeters: degDiff=" + widthDegrees + ", meterDiff=" + widthInM);
        
        return widthInM;
    }

    public float heightInMeters() {

        float heightInM = 0.f;
        float heightDegrees = 0.f;
        
        heightDegrees = mBoundingBox.top - mBoundingBox.bottom;

        heightInM = (float)(heightDegrees / 360.0f * meridonalCircumference );    
//      Log.e(TAG, "in widthInMeters: degDiff=" + widthDegrees + ", meterDiff=" + widthInM);
        
        return heightInM;
    }


}

