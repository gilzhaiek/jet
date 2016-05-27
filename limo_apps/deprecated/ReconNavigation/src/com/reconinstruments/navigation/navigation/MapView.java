package com.reconinstruments.navigation.navigation;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.reconinstruments.navigation.R;
import com.recon.prim.PointD;
import com.reconinstruments.navigation.routing.RePath;
import com.reconinstruments.reconsettings.ReconSettingsUtil;

public class MapView extends View
{

	// the bits for setting various rendering feature
	static public final int MAP_FEATURE_COMPASS = 0x00000001;
	static public final int MAP_FEATURE_SCALEMETRIC = 0x00000002;
	static public final int MAP_FEATURE_SPEED = 0x00000004;
	static public final int MAP_FEATURE_CHRONO = 0x00000008;
	static public final int MAP_FEATURE_LOCATION = 0x00000010;
	static public final int MAP_FEATURE_CENTERICON = 0x00000020;
	static public final int MAP_FEATURE_CENTERPIN = 0x00000040;
	static public final int MAP_FEATURE_ROTATE = 0x00000080;

	static final float PATH_WIDTH = 100.f;
	static final float PATH_HEIGHT = 350.f;
	static final int PATH_ALPHA = 64;
	static final float STROKE_WIDTH = 10.f;
	static final int STROKE_COLOR = 0xffff0000;
	static public final float MOVEMENT_DELTA_X = 20.f;
	static public final float MOVEMENT_DELTA_Y = 20.f;
	static public final float SCALE_DELTA = 1.2f;
	static public final float ROTATION_DELTA = 5.f;
	static final float MAX_AUTO_ROTATION = 30.f;
	static final boolean TRY_CAMERA = false;
	static final int VIEW_BACKGROUND_COLOR = 0xff909090;
	static final float DRAW_TRAIL_NAME_THRESHOLD = -4;
	static final float DRAW_POI_THRESHOLD = -6;
	static final float DRAW_POI_NAME_THRESHOLD = -3;
	static final float DRAW_TRACKED_POI_THRESHOLD = -12;
	static final float DRAW_TRACKED_NAMES_THRESHOLD = -9;
	static final float ROTATOR_RADIUS = 60;

	// metric related drawing attributes
	static final float COMPASS_METRIC_START_OFFSETX = 20;
	static final float COMPASS_METRIC_START_OFFSETY = 16;
	static final float METRIC_RULE_LEN_IN_PIXEL = 100;
	static final float METRIC_RULE_ENDLINE_LEN = 8;
	static final float METRIC_TEXT_SIZE = 20;
	static final float METRIC_STROKE_WIDTH = 4;

	static final float NODATASIGN_TEXT_SIZE = 25;
	static final float LOCATION_TEXT_SIZE = 24;

	static final int LOCATION_SCALE_PADDING = 5;

	static final boolean DRAW_GPS_IN_DEGREE = true;

	// the minimum distance to set the Pre-offset
	static final float MINI_OFFSET_RECORD_DISTANCE = 2.f / (float) Util.DISTANCE_PER_PIXEL;

	static private int PREVIOUS_OFFSET_QUEUE_DEPTH = 10; // MUST BE EVEN
	
	static private int DEFAULT_ZOOM_LEVEL = -4;

	private float mOffsetX; // the current centerX of the viewport
	private float mOffsetY; // the current centerY of the viewport
	private int mPreviouseOffsetLoc = -1;
	private PointF[] mPreviousOffsets = new PointF[PREVIOUS_OFFSET_QUEUE_DEPTH];
	// private float mPreOffsetX; //the last set of centerX of the viewport
	// private float mPreOffsetY; //the last set of centerY of the viewport
	private float mScale;
	private float mRotX;
	private Camera mCamera;
	private Paint mPaint;
	private Paint mMetricPaint; // paint for drawing metric line
	private Paint mMetricLabelPaint; // paint for drawing metric lable
	private Paint mLocationPaint;
	private Paint mPinLabelBgPaint;
	private Paint mNoDataSignPaint;
	private float mAngle = 0;
	private ShpMap mMap = null;
	private boolean mMapMoved = false;
	private boolean mViewLocked = false;
	private boolean mMapRotated = false;

	// bitmap icons for different feature
	private Bitmap mCompass = null; // compass icon
	private Bitmap mRotate = null; // compass icon
	private Bitmap mMyPlace = null; // myplace icon
	private Bitmap mNoMapSign = null; // the icon for no map hint
	private Bitmap mBigPin = null; // the big pin icon for drop pin view
	private Bitmap mArrowLeft = null;
	private Bitmap mArrowRight = null;

	private float mMinZoomLevel = 0; // to be calculated when a map is assigned
	private float mMaxZoomLevel = 5;
	private float mZoomLevel = 0;

	// touch related atttributes for Android phone testing.
	// should be disabled for the final limo version.
	private float mTouchOffsetX = 0;
	private float mTouchOffsetY = 0;
	private float mTouchScale = 0; // record the original scale when touched;
	private float mTouchX, mTouchY;
	private float mTouchMultiPointerDistance = 0;
	private boolean mPinchZoom = false;
	private float mTouchRotate = 0;
	private boolean mIsTouchRotating = false;

	private GestureDetector mGestureProcessor;
	private KeyEvent.Callback mLostFocusHandler = null;
	public boolean mDrawDestination = false;
	private PointF mDestination = new PointF();
	private RePath mPlannedRoute = null;
	private PointF mViewportOrigin = null; // defined the origin of the view port, where the user will be stay on always
	private int mFeatureFlag = 0;
	private boolean mDrawRotator = true;

	// temporary point/rect for calculation only
	private PointF mTempPoint = new PointF();
	private Rect mTempRect = new Rect();
	private RectF mTempRectF = new RectF();
	private float mSpeed = 0;

	public MapView(Context context)
	{
		super(context);
		initView(context);
	}

	public MapView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);

	}

	/*
	 * Set the speed of the device owner
	 */
	public void setSppeed(float speed)
	{
		mSpeed = speed;
	}

	public float getMinZoomLevel()
	{
		if (mMinZoomLevel == 0)
		{
			calMinZoomLevel();
		}
		return mMinZoomLevel;
	}

	public float getMaxZoomLevel()
	{
		return mMaxZoomLevel;
	}

	public float getZoomLevel()
	{
		return mZoomLevel;
	}

	/**
	 * 
	 * Turn on/off drawing rotator
	 */
	public void setDrawRotator(boolean flag)
	{
		mDrawRotator = flag;
	}

	/**
	 * 
	 * For turning on/off certain map features
	 */
	public void setFeature(int featureFlag, boolean on)
	{
		if (on == true)
		{
			mFeatureFlag = mFeatureFlag | featureFlag;
		}
		else
		{
			mFeatureFlag = mFeatureFlag & (~featureFlag);
		}
		invalidate();
	}

	public void setForceDrawPOIs(boolean value)
	{
		if (mMap != null)
		{
			mMap.mForceDrawPOIs = value;
		}
	}

	/**
	 * Turn off all map extra overlay features
	 */
	public void clearFeatures()
	{
		mFeatureFlag = 0;
		invalidate();
	}

	/**
	 * @return the current map feature
	 */
	public int getFeature()
	{
		return mFeatureFlag;
	}

	public boolean isFeatureOn(int featureFlag)
	{
		return (mFeatureFlag & featureFlag) != 0;
	}

	private PointF getViewportOrigin()
	{
		if (mViewportOrigin == null)
		{
			mViewportOrigin = new PointF();
		}

		mViewportOrigin.x = getWidth() / 2.f;
		mViewportOrigin.y = getHeight() / 2.f;
		return mViewportOrigin;
	}

	private void initView(Context context)
	{
		reset();
		Util.mUnits = ReconSettingsUtil.getUnits(this.getContext());

		mCamera = new Camera();
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setAntiAlias(true);
		mPaint.setColor(0xff00ff00);

		// paint style for metric
		mMetricPaint = new Paint();
		mMetricPaint.setStyle(Paint.Style.STROKE);
		mMetricPaint.setStrokeWidth(MapView.METRIC_STROKE_WIDTH);
		mMetricPaint.setColor(0xffffffff);

		// paint style for metric label
		mMetricLabelPaint = new Paint();
		mMetricLabelPaint.setTextSize(MapView.METRIC_TEXT_SIZE);
		mMetricLabelPaint.setTextAlign(Paint.Align.CENTER);
		mMetricLabelPaint.setTypeface(Util.getMenuFont(context));
		mMetricLabelPaint.setFakeBoldText(true);
		mMetricLabelPaint.setAntiAlias(true);
		mMetricLabelPaint.setColor(0xffffffff);

		// paint style for metric label
		mLocationPaint = new Paint();
		mLocationPaint.setTextSize(MapView.LOCATION_TEXT_SIZE);
		mLocationPaint.setTypeface(Util.getMenuFont(context));
		mLocationPaint.setFakeBoldText(true);
		mLocationPaint.setAntiAlias(true);
		mLocationPaint.setColor(0xffffffff);

		// paint style for pin label
		mPinLabelBgPaint = new Paint();
		mPinLabelBgPaint.setAntiAlias(true);
		mPinLabelBgPaint.setColor(Util.HILITE_LABLE_BG_COLOR);
		mPinLabelBgPaint.setStyle(Paint.Style.FILL);

		// the paint style for no data sign
		mNoDataSignPaint = new Paint();
		mNoDataSignPaint.setTextSize(MapView.NODATASIGN_TEXT_SIZE);
		mNoDataSignPaint.setTypeface(Util.getMenuFont(context));
		mNoDataSignPaint.setAntiAlias(true);
		mNoDataSignPaint.setColor(0xffBBBBBB);

		// disable focus to MapView
		setFocusable(false);

		// Initializing the painting resources for trails and area.
		Trail.InitPaints(context);
		Area.InitPaints(context);
		PoInterest.InitPaints(context);

		try
		{
			// InputStream stream = context.getAssets().open("compass.png");
			// mCompass = BitmapFactory.decodeStream(stream);
			// stream.close();
			mCompass = BitmapFactory.decodeResource(getResources(), R.drawable.compass);
			mRotate = BitmapFactory.decodeResource(getResources(), R.drawable.rotate);
			mMyPlace = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_myplaces);
			mNoMapSign = BitmapFactory.decodeResource(getResources(), R.drawable.no_data_sign);
			mBigPin = BitmapFactory.decodeResource(getResources(), R.drawable.pindrop);
			mArrowRight = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_right);
			mArrowLeft = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_left);

		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
			Log.i("ShpMap", "ShpMap - Loading compass file failed", e);
		}

		mGestureProcessor = new GestureDetector(context, new SimpleGestureListener());
	}

	public void reset()
	{
		mOffsetX = 0.f;
		mOffsetY = 0.f;
		mScale = 1.f;
		mRotX = 0;
		mPlannedRoute = null;
		mDrawDestination = false;
		mMinZoomLevel = 0;

		for (int i = 0; i < mPreviousOffsets.length; i++)
		{
			mPreviousOffsets[i] = null;
		}
		
		// Set mZoomLevel to DEFAULT_ZOOM_LEVEL
		setZoomLevel(DEFAULT_ZOOM_LEVEL);
	}

	public void setPlannedRoute(RePath path)
	{
		mPlannedRoute = path;
	}

	public void setDestination(float x, float y)
	{
		mDestination.x = x;
		mDestination.y = y;
	}

	public void setLostFocusHandler(KeyEvent.Callback callback)
	{
		mLostFocusHandler = callback;
	}

	public void setMap(ShpMap map)
	{
		mMap = map;
	}

	public void LockView()
	{
		mViewLocked = true;
	}

	public void UnlockView()
	{
		mViewLocked = false;
	}

	protected boolean IsMapRotated()
	{
		return (mMapRotated);
	}

	protected boolean IsCenterUserDefined()
	{
		return (mViewLocked || mMapMoved);
	}

	protected void ClearUserDefinedCenter()
	{
		UnlockView();
		mMapMoved = false;
		mMapRotated = false;
	}

	// set the center of the View to be the specified latitude, and longitude
	public void setCenterLatLng(double latitude, double longitude, boolean autoAligned)
	{
		// update only when the map has been bound
		if (mMap != null)
		{
			// x: longitude, y: latitude
			PointD p = new PointD(longitude, latitude);
			Util.mapShpContentPoint(p);
			if (!IsCenterUserDefined())
			{
				setCenter((float) p.x, (float) p.y, autoAligned);
			}
			invalidate();
		}
	}

	public void setCenter(float x, float y, boolean autoAligned, boolean lockView)
	{
		if (lockView)
		{
			LockView();
		}

		setCenter(x, y, autoAligned);
	}

	protected void drawLastLocations(Canvas canvas, RectF viewPortBBox, Matrix transform)
	{
		int offsetLoc = mPreviouseOffsetLoc;

		if (offsetLoc < 0)
		{
			return;
		}

		for (int i = 1; i < PREVIOUS_OFFSET_QUEUE_DEPTH; i++)
		{
			if (--offsetLoc < 0)
			{
				offsetLoc = (PREVIOUS_OFFSET_QUEUE_DEPTH - 1);
			}

			if (mPreviousOffsets[offsetLoc] != null)
			{
				mMap.drawPoint(canvas, viewPortBBox, transform, mPreviousOffsets[offsetLoc].x, mPreviousOffsets[offsetLoc].y, PREVIOUS_OFFSET_QUEUE_DEPTH - i);
			}
		}
	}

	// set the center of the View to be the specified local X, Y
	public void setCenter(float x, float y, boolean autoAligned)
	{
		if (mMap != null)
		{
			mOffsetX = -x;
			mOffsetY = -y;

			addOffset(mOffsetX, mOffsetY);

			if (autoAligned)
			{
				/*
				 * float dx = mOffsetX - getLastOffset().x; float dy = mOffsetY - getLastOffset().y; float len = dx*dx + dy*dy;
				 */

				// if( len > (MINI_OFFSET_RECORD_DISTANCE*MINI_OFFSET_RECORD_DISTANCE) )
				{
					alignTo(getAvgDirection());
				}
			}

			invalidate();
		}
	}

	public PointF getCenter()
	{
		return new PointF(-mOffsetX, -mOffsetY);
	}

	// align the viewport`s UP vector which is ( 0, 1 ) to the vector specified by (dx, dy)
	public void alignTo(PointF pointToAlign)
	{
		if (IsMapRotated())
		{
			return;
		}

		float len = (float) Math.sqrt(pointToAlign.x * pointToAlign.x + pointToAlign.y * pointToAlign.y);

		// if the length of the vector is zero, dont
		// try alignment
		if (len > 0.f)
		{

			pointToAlign.x /= len;
			pointToAlign.y /= len;

			// (dx, dy) dotProd (0, 1)
			float dotProd = pointToAlign.y;
			double angle = Math.acos(dotProd);

			// if dx < 0.f, increase the angle by PI
			// angle = dx < 0.f ? angle + Math.PI : angle;
			angle = pointToAlign.x > 0.f ? angle : -angle;
			float degreeAngle = (float) (angle / Math.PI) * 180;

			float deltaAngle = Math.abs(mRotX - degreeAngle);
			deltaAngle = (deltaAngle > 180) ? Math.abs(deltaAngle - 360.f) : deltaAngle;

			if (deltaAngle > MAX_AUTO_ROTATION)
			{
				if (mRotX > 0)
				{
					if (mRotX > degreeAngle)
					{
						degreeAngle = mRotX - MAX_AUTO_ROTATION;
					}
					else
					{
						degreeAngle = mRotX + MAX_AUTO_ROTATION;
					}
				}
				else
				{
					if (mRotX < degreeAngle)
					{
						degreeAngle = mRotX + MAX_AUTO_ROTATION;
					}
					else
					{
						degreeAngle = mRotX - MAX_AUTO_ROTATION;
					}
				}
			}

			setRotation(degreeAngle);
		}
	}

	// set the rotation of the map
	// i.e.: where the True North should be aligned to
	public void setRotation(float degreeAngle)
	{
		if (IsMapRotated())
		{
			return;
		}

		mRotX = degreeAngle;
		invalidate();
	}

	// calculate the minimum zoom level
	private void calMinZoomLevel()
	{
		float cx = this.getWidth();
		float cy = this.getHeight();
		if (mMap != null)
		{
			cx = cx / mMap.mBBox.width();
			cy = cy / mMap.mBBox.height();
		}
		mMinZoomLevel = (float) (Math.log(cx > cy ? cy : cx) / Math.log(SCALE_DELTA));
	}

	private void drawDestination(Canvas canvas, Matrix transform)
	{
		if (mDrawDestination)
		{
			float[] pos = new float[2];
			pos[0] = mDestination.x;
			pos[1] = mDestination.y;
			transform.mapPoints(pos);

			mPaint.setColor(0xffff00ff);
			canvas.drawCircle(pos[0], pos[2], 5.0f, mPaint);
		}
	}

	private void drawSpeed(Canvas canvas)
	{
		if ((mFeatureFlag & MAP_FEATURE_SPEED) != 0)
		{

			int hPadding = 5;
			int vPadding = 5;

			String speed = (mSpeed >= 0) ? (int) mSpeed + "" : "--";
			// cache old font size
			float oldSize = mLocationPaint.getTextSize();
			int oldColor = mLocationPaint.getColor();

			mLocationPaint.setColor(0xffffffff);
			mLocationPaint.setTextSize(50);

			// measure the bounding box of the speed
			float width, height, h1;
			mLocationPaint.getTextBounds(speed, 0, speed.length(), mTempRect);
			width = mTempRect.width();
			h1 = height = mTempRect.height();

			int unitSetting = ReconSettingsUtil.getUnits(this.getContext());

			String speedUnit = null;

			if (unitSetting == ReconSettingsUtil.RECON_UINTS_METRIC)
			{
				speedUnit = this.getContext().getResources().getString(R.string.speed_unit_km);
			}
			else
			{
				speedUnit = this.getContext().getResources().getString(R.string.speed_unit_mph);
			}

			// measure "KM/H"
			mLocationPaint.setTextSize(12);
			mLocationPaint.getTextBounds(speedUnit, 0, 3, mTempRect);
			width = mTempRect.width() > width ? mTempRect.width() : width;
			height += 12; // For the KM/H text
			height += 3 * vPadding; // added three line padding (top, bottom and middle);
			width += 2 * hPadding; // added two horizontal padding

			if (mSpeed < 0)
			{
				height += 10;
				width += 4;
			}

			mTempRectF.left = this.getWidth() - width - hPadding;
			mTempRectF.top = this.getHeight() - height - vPadding;
			mTempRectF.right = mTempRectF.left + width;
			mTempRectF.bottom = mTempRectF.top + height;

			// draw the background
			canvas.drawRoundRect(mTempRectF, 5, 5, mPinLabelBgPaint);

			mLocationPaint.setTextSize(12);
			canvas.drawText(speedUnit, mTempRectF.left + hPadding, mTempRectF.bottom - vPadding, mLocationPaint);

			if (mSpeed < 0)
			{
				mTempRectF.left += 2;
			}

			mLocationPaint.setTextSize(50);
			canvas.drawText(speed, mTempRectF.left + hPadding, mTempRectF.bottom - 12 - 2 * vPadding, mLocationPaint);

			// restore the old size
			mLocationPaint.setTextSize(oldSize);
			mLocationPaint.setColor(oldColor);
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		// Debug.startMethodTracing("mapdraw");

		// fill the view port with the default background color
		canvas.drawColor(MapView.VIEW_BACKGROUND_COLOR);

		float cx = getViewportOrigin().x;
		float cy = getViewportOrigin().y;

		canvas.save();

		// canvas.scale( mScale, mScale, mOffsetX, mOffsetY );
		// canvas.rotate(mRotX, mOffsetX, mOffsetY);
		// canvas.translate(mOffsetX, mOffsetY);

		if (TRY_CAMERA)
		{
			Matrix cameraMatrix;
			cameraMatrix = new Matrix();
			mCamera.save();

			mCamera.rotateX(mAngle); // perspective projections
			// mCamera.translate(mOffsetX, mOffsetY, 0);
			// mCamera.rotateZ(mRotX); //rotate around the z
			// Output the camera rotations to a matrix
			mCamera.getMatrix(cameraMatrix);
			mCamera.restore();

			// canvas.Matrix*cameraMatrix
			canvas.concat(cameraMatrix);
		}

		// canvas.Matrix * Translate * Rotate * Scale
		Matrix transform = new Matrix();
		transform.reset();
		transform.preTranslate(mOffsetX + cx, mOffsetY + cy);
		transform.preRotate(mRotX, -mOffsetX, -mOffsetY);
		transform.preScale(mScale, mScale, -mOffsetX, -mOffsetY);

		canvas.concat(transform);

		/*
		 * if( TRY_CAMERA ) { Matrix cameraMatrix; cameraMatrix = new Matrix(); mCamera.save(); mCamera.getMatrix(cameraMatrix);
		 * 
		 * mCamera.rotateX(mAngle); //perspective projections //mCamera.translate(mOffsetX, -mOffsetY, 0); //mCamera.rotateZ(mRotX); //rotate around the z
		 * mCamera.getMatrix(cameraMatrix); //mCamera.translate(cx, -cy, 0); // Output the camera rotations to a matrix mCamera.getMatrix(cameraMatrix);
		 * mCamera.restore();
		 * 
		 * //canvas.Matrix*Translate*Rotate*Scale*cameraMatrix canvas.concat(cameraMatrix); }
		 */

		// construct the viewport of the MapView;
		RectF viewportRect = new RectF(0, 0, this.getWidth(), this.getHeight());

		if (mMap != null)
		{
			if (mZoomLevel < MapView.DRAW_TRAIL_NAME_THRESHOLD)
			{
				mMap.mDrawTrailNames = false;
			}
			else
			{
				mMap.mDrawTrailNames = true;
			}

			mMap.setScaleFactor(mScale);
			mMap.drawAreas(canvas, viewportRect, transform);
			mMap.drawTrails(canvas, viewportRect, transform);
		}

		// draw the center of the world
		// mPaint.setColor(0xff00ff00);
		// canvas.drawCircle(0, 0, 5/mScale, mPaint);

		if (mPlannedRoute != null)
		{
			Trail.updateHiliteTrailWidth(mScale);
			mPlannedRoute.drawPath(canvas, transform);
		}

		canvas.restore();

		if (mMap != null)
		{

			mMap.mDrawPOIs = (mZoomLevel >= MapView.DRAW_POI_THRESHOLD);
			mMap.mDrawPOIsNames = (mZoomLevel >= MapView.DRAW_POI_NAME_THRESHOLD);
			mMap.mDrawTrackedPOIs = (mZoomLevel >= MapView.DRAW_TRACKED_POI_THRESHOLD);
			mMap.mDrawTrackedNames = (mZoomLevel >= MapView.DRAW_TRACKED_NAMES_THRESHOLD);

			mMap.mDistancePerPixel = (float) (Util.DISTANCE_PER_PIXEL);
			mMap.mScale = (float) (mScale);
			// move the POI rendering out of the canvas
			// transform scope, since we dont want the icon
			// being affected by the rotating and scale
			mMap.drawPOIs(canvas, viewportRect, transform);

			// For Debugging
			// drawLastLocations(canvas, viewportRect, transform);
		}

		drawMyPlace(canvas);

		drawCompass(canvas);

		drawRotate(canvas);

		drawMetric(canvas);

		drawMyLocation(canvas);

		drawCenterPin(canvas);

		if (RuntimeConfig.TOUCH_TEST_ON && mDrawRotator)
		{
			drawRotator(canvas);
		}

		drawDestination(canvas, transform);

		// draw no-data-sign if no map is available
		drawNoDataSign(canvas);

		// draw speed if the speed feature turned on
		drawSpeed(canvas);

		// Debug.stopMethodTracing();
	}

	public void move(float deltaX, float deltaY)
	{
		mMapMoved = true;
		Matrix transform = new Matrix();
		Matrix inverse = new Matrix();

		transform.reset();
		transform.preRotate(mRotX);
		transform.preScale(mScale, mScale);
		transform.invert(inverse);

		float[] point = new float[2];
		point[0] = deltaX;
		point[1] = deltaY;

		inverse.mapPoints(point);

		mOffsetY += point[1];
		mOffsetX += point[0];
		this.invalidate();

	}

	public void rotate(float rot)
	{
		mMapRotated = true;
		mRotX += rot;
		this.invalidate();
	}

	public void zoomOut()
	{
		if (mMinZoomLevel == 0)
		{
			calMinZoomLevel();
		}

		mZoomLevel = mZoomLevel - 1 < mMinZoomLevel ? mMinZoomLevel : mZoomLevel - 1;

		calcScale();

		this.invalidate();
	}

	public void zoomIn()
	{
		mZoomLevel = mZoomLevel + 1 > mMaxZoomLevel ? mMaxZoomLevel : mZoomLevel + 1;
		calcScale();

		this.invalidate();
	}

	public void zoomFit()
	{
		if (mMinZoomLevel == 0)
		{
			calMinZoomLevel();
		}

		mZoomLevel = mMinZoomLevel;
		calcScale();

		this.invalidate();
	}

	public void zoomRestore()
	{
		mZoomLevel = 0;
		calcScale();

		this.invalidate();
	}

	public void setZoomLevel(int level)
	{
		mZoomLevel = level;
		calcScale();

		this.invalidate();
	}

	private void drawCompass(Canvas canvas)
	{
		if ((mFeatureFlag & MAP_FEATURE_COMPASS) != 0)
		{
			if (mCompass != null)
			{
				Matrix matrix = new Matrix();
				// matrix.preTranslate(this.getWidth() - mCompass.getWidth(), 0);
				matrix.preTranslate(COMPASS_METRIC_START_OFFSETX, COMPASS_METRIC_START_OFFSETY);
				matrix.preRotate(mRotX, mCompass.getWidth() / 2, mCompass.getHeight() / 2);
				mPaint.setColor(0xffff0000);
				canvas.drawBitmap(mCompass, matrix, mPaint);
			}
		}
	}

	private void drawRotate(Canvas canvas)
	{
		if ((mFeatureFlag & MAP_FEATURE_ROTATE) != 0)
		{
			if (mRotate != null)
			{
				float cx = getViewportOrigin().x;
				float cy = getViewportOrigin().y;
				Matrix matrix = new Matrix();
				matrix.preTranslate(cx - mRotate.getWidth() / 2.f, cy - mRotate.getHeight() / 2.f);
				canvas.drawBitmap(mRotate, matrix, mPaint);

				matrix = new Matrix();
				matrix.preTranslate(cx - mRotate.getWidth() / 2.f - mArrowLeft.getWidth() - 5, cy - mArrowLeft.getHeight() / 2.f);
				canvas.drawBitmap(mArrowLeft, matrix, mPaint);

				matrix = new Matrix();
				matrix.preTranslate(cx + mRotate.getWidth() / 2.f + 5, cy - mArrowRight.getHeight() / 2.f);
				canvas.drawBitmap(mArrowRight, matrix, mPaint);
			}
		}
	}

	private void drawMetric(Canvas canvas)
	{
		if ((mFeatureFlag & MAP_FEATURE_SCALEMETRIC) != 0)
		{
			int len = (int) (METRIC_RULE_LEN_IN_PIXEL * Util.DISTANCE_PER_PIXEL / mScale);
			float x1 = MapView.COMPASS_METRIC_START_OFFSETX + LOCATION_SCALE_PADDING;
			float x2 = x1 + MapView.METRIC_RULE_LEN_IN_PIXEL;
			float y1 = getHeight() - MapView.COMPASS_METRIC_START_OFFSETY - LOCATION_SCALE_PADDING;
			float y2 = y1;

			int unitSetting = ReconSettingsUtil.getUnits(this.getContext());

			String label = null;
			if (unitSetting == ReconSettingsUtil.RECON_UINTS_METRIC)
			{
				label = len + " m";
			}
			else
			{
				label = (int) Util.meterToFeet(len) + " ft";
			}

			mMetricLabelPaint.getTextBounds(label, 0, label.length(), mTempRect);
			mTempRectF.left = x1 - LOCATION_SCALE_PADDING;
			mTempRectF.right = x2 + LOCATION_SCALE_PADDING;
			mTempRectF.top = y1 - mTempRect.height() - LOCATION_SCALE_PADDING;
			mTempRectF.bottom = y1 + 2 * LOCATION_SCALE_PADDING;

			canvas.drawRoundRect(mTempRectF, 5, 5, mPinLabelBgPaint);

			// beginning vertical line
			canvas.drawLine(x1, y1 - MapView.METRIC_RULE_ENDLINE_LEN, x1, y1 + MapView.METRIC_RULE_ENDLINE_LEN, mMetricPaint);

			// ending vertical line
			canvas.drawLine(x2, y2 - MapView.METRIC_RULE_ENDLINE_LEN, x2, y2 + MapView.METRIC_RULE_ENDLINE_LEN, mMetricPaint);

			// the metric line
			canvas.drawLine(x1, y1, x2, y2, mMetricPaint);

			// draw the text
			canvas.drawText(label, mTempRectF.centerX(), y1 - LOCATION_SCALE_PADDING, mMetricLabelPaint);
		}
	}

	private void drawMyPlace(Canvas canvas)
	{
		// draw the center of canvas
		// mPaint.setColor(0xff0000ff);
		// canvas.drawCircle(cx, cy, 5, mPaint);

		if ((mFeatureFlag & MAP_FEATURE_CENTERICON) != 0)
		{
			if (mMyPlace != null)
			{
				float cx = getViewportOrigin().x;
				float cy = getViewportOrigin().y;
				Matrix matrix = new Matrix();
				matrix.preTranslate(cx - mMyPlace.getWidth() / 2.f, cy - mMyPlace.getHeight() / 2.f);
				mPaint.setColor(0xffff0000);
				canvas.drawBitmap(mMyPlace, matrix, mPaint);
			}
		}
	}

	private void drawCenterPin(Canvas canvas)
	{
		// draw the center of canvas
		// mPaint.setColor(0xff0000ff);
		// canvas.drawCircle(cx, cy, 5, mPaint);

		if ((mFeatureFlag & MAP_FEATURE_CENTERPIN) != 0)
		{
			if (mBigPin != null)
			{
				float cx = getViewportOrigin().x;
				float cy = getViewportOrigin().y;
				Matrix matrix = new Matrix();
				matrix.preTranslate(cx, cy - mBigPin.getHeight());
				canvas.drawBitmap(mBigPin, matrix, mPaint);

				mTempPoint.x = -mOffsetX;
				mTempPoint.y = -mOffsetY;
				Util.mapLocalToLatLng(mTempPoint);

				String latStr = null;
				String lngStr = null;
				if (DRAW_GPS_IN_DEGREE)
				{
					latStr = Util.latLngToDegree(mTempPoint.x);
					latStr = (mTempPoint.x > 0) ? "E " + latStr : "W " + latStr;

					lngStr = Util.latLngToDegree(mTempPoint.y);
					lngStr = (mTempPoint.y > 0) ? "N " + lngStr : "S " + lngStr;
				}
				else
				{
					latStr = "LON:" + mTempPoint.x;
					lngStr = "LAT:" + mTempPoint.y;
				}

				float width = 0;
				float height = 0;
				mLocationPaint.getTextBounds(latStr, 0, latStr.length(), mTempRect);
				width = mTempRect.width();
				height = mTempRect.height();

				mLocationPaint.getTextBounds(lngStr, 0, lngStr.length(), mTempRect);
				width = mTempRect.width() > width ? mTempRect.width() : width;
				width += 8;
				height += mTempRect.height() + 8;

				int vPadding = 5;
				int hPadding = 5;
				mTempRectF.set(cx - width / 2 - hPadding, cy - vPadding, cx + width / 2 + hPadding, cy + height + vPadding);
				canvas.drawRoundRect(mTempRectF, 5, 5, mPinLabelBgPaint);

				canvas.drawText(latStr, cx - width / 2, cy + height / 2, mLocationPaint);
				canvas.drawText(lngStr, cx - width / 2, cy + height, mLocationPaint);

			}
		}
	}

	private void drawRotator(Canvas canvas)
	{
		// draw the center of canvas
		if (mIsTouchRotating)
		{
			mPaint.setColor(0x20ff0000);
		}
		else
		{
			mPaint.setColor(0x20ffffff);
		}
		canvas.drawCircle(getViewportOrigin().x, getViewportOrigin().y, ROTATOR_RADIUS, mPaint);

	}

	private void drawMyLocation(Canvas canvas)
	{
		if ((mFeatureFlag & MAP_FEATURE_LOCATION) != 0)
		{
			// String loc = mOffsetX + "," + mOffsetY;
			mTempPoint.x = -mOffsetX;
			mTempPoint.y = -mOffsetY;
			Util.mapLocalToLatLng(mTempPoint);

			String loc1 = null;
			String loc2 = null;
			if (DRAW_GPS_IN_DEGREE)
			{
				loc1 = Util.latLngToDegree(Math.abs(mTempPoint.x));
				loc1 = (mTempPoint.x > 0) ? "E " + loc1 : "W " + loc1;

				loc2 = Util.latLngToDegree(Math.abs(mTempPoint.y));
				loc2 = (mTempPoint.y > 0) ? "N " + loc2 : "S " + loc2;

			}
			else
			{
				loc1 = "LON:" + mTempPoint.x;
				loc2 = "LAT:" + mTempPoint.y;
			}

			float width = 0;
			float height = 0;

			float lineHeight;
			// calculate the bounding box of the location string
			mLocationPaint.getTextBounds(loc1, 0, loc1.length(), mTempRect);
			width = mTempRect.width();
			height = mTempRect.height();
			lineHeight = height;
			mLocationPaint.getTextBounds(loc2, 0, loc2.length(), mTempRect);
			width = width > mTempRect.width() ? width : mTempRect.width();
			height += mTempRect.height();

			mTempRectF.left = (int) (getWidth() - width - 2 * LOCATION_SCALE_PADDING - 2 * LOCATION_SCALE_PADDING);
			mTempRectF.right = (int) (getWidth() - LOCATION_SCALE_PADDING);
			mTempRectF.top = (int) (getHeight() - height - 3 * LOCATION_SCALE_PADDING - LOCATION_SCALE_PADDING);
			mTempRectF.bottom = (int) (getHeight() - LOCATION_SCALE_PADDING);

			// the dark bk-ground
			canvas.drawRoundRect(mTempRectF, 5, 5, mPinLabelBgPaint);

			// the text
			canvas.drawText(loc1, mTempRectF.left + LOCATION_SCALE_PADDING, mTempRectF.top + LOCATION_SCALE_PADDING + lineHeight, mLocationPaint);
			canvas.drawText(loc2, mTempRectF.left + LOCATION_SCALE_PADDING, mTempRectF.top + 2 * LOCATION_SCALE_PADDING + 2 * lineHeight, mLocationPaint);
		}
	}

	private void drawNoDataSign(Canvas canvas)
	{
		if (mMap == null || mMap.isEmpty())
		{
			// clear the whole canvas with black
			canvas.drawRGB(0, 0, 0);

			if (mNoMapSign != null)
			{
				float cx = getWidth() / 2;
				float cy = getHeight() / 2;
				Matrix matrix = new Matrix();
				matrix.preTranslate(cx - mNoMapSign.getWidth() / 2.f, cy - mNoMapSign.getHeight() / 2.f);
				mPaint.setColor(0xffffffff);
				canvas.drawBitmap(mNoMapSign, matrix, mPaint);
			}

			// get the text bounds
			String msg = getResources().getString(R.string.no_data_text);
			mNoDataSignPaint.getTextBounds(msg, 0, msg.length(), mTempRect);
			canvas.drawText(msg, (getWidth() - mTempRect.width()) / 2, getHeight() / 2 + mNoMapSign.getHeight(), mNoDataSignPaint);
		}
	}

	private void calcScale()
	{
		mScale = (float) Math.pow(SCALE_DELTA, mZoomLevel);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		boolean result = true;
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_DPAD_UP:
			move(0, MOVEMENT_DELTA_Y);
			break;

		case KeyEvent.KEYCODE_DPAD_DOWN:
			move(0, -MOVEMENT_DELTA_Y);
			break;

		case KeyEvent.KEYCODE_DPAD_LEFT:
			move(MOVEMENT_DELTA_X, 0);
			break;

		case KeyEvent.KEYCODE_DPAD_RIGHT:
			move(-MOVEMENT_DELTA_X, 0);
			break;

		case KeyEvent.KEYCODE_1:
			zoomOut();
			break;

		case KeyEvent.KEYCODE_2:
			zoomIn();
			break;

		// rotate the viewport to left
		case KeyEvent.KEYCODE_3:
			rotate(ROTATION_DELTA);
			break;

		// rotate the viewport to right
		case KeyEvent.KEYCODE_4:
			rotate(-ROTATION_DELTA);
			break;

		// zoom to fit
		case KeyEvent.KEYCODE_0:
			// case KeyEvent.KEYCODE_DPAD_UP:
			zoomFit();
			break;

		case KeyEvent.KEYCODE_5:
			mAngle += 1;
			this.invalidate();
			break;

		case KeyEvent.KEYCODE_6:
			mAngle -= 1;
			this.invalidate();
			break;

		case KeyEvent.KEYCODE_9:
			zoomRestore();
			break;

		case KeyEvent.KEYCODE_DPAD_CENTER:
			// the select button will re-set the focus
			// back to the zoomout button. This is ugly though
			clearFocus();
			if (mLostFocusHandler != null)
			{
				event.dispatch(mLostFocusHandler);
			}
			break;

		default:
			result = false;
		}

		return result;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (RuntimeConfig.TOUCH_TEST_ON && mMap != null)
		{
			if (mGestureProcessor.onTouchEvent(event) == true)
			{
				return true;
			}
			else
			{
				int touchAction = event.getAction();
				float cx = getViewportOrigin().x;
				float cy = getViewportOrigin().y;

				switch (touchAction)
				{
				case MotionEvent.ACTION_DOWN:
					mTouchOffsetX = mOffsetX;
					mTouchOffsetY = mOffsetY;
					mTouchScale = mScale;
					mTouchX = event.getX();
					mTouchY = event.getY();
					mTouchRotate = mRotX;

					if ((mTouchX - cx) * (mTouchX - cx) + (mTouchY - cy) * (mTouchY - cy) < ROTATOR_RADIUS * ROTATOR_RADIUS)
					{
						mIsTouchRotating = true;
						invalidate();
					}
					else
					{
						mIsTouchRotating = false;
					}

					break;

				case MotionEvent.ACTION_UP:
					mPinchZoom = false;
					mIsTouchRotating = false;
					invalidate();
					break;

				case MotionEvent.ACTION_MOVE:
					if (event.getPointerCount() == 1)
					{
						mPinchZoom = false;

						if (mIsTouchRotating)
						{
							// handle moving
							float x1 = event.getX() - cx;
							float y1 = event.getY() - cy;

							float x2 = mTouchX - cx;
							float y2 = mTouchY - cy;

							float len1 = (float) Math.sqrt(x1 * x1 + y1 * y1);
							float len2 = (float) Math.sqrt(x2 * x2 + y2 * y2);
							if (len1 > 0 && len2 > 0)
							{
								float angle1 = (float) Math.atan2(x1, y1);

								float angle2 = (float) Math.atan2(x2, y2);

								angle1 -= angle2;

								angle1 *= 180;
								mRotX = mTouchRotate - angle1;
								invalidate();
							}

						}
						else
						{
							// handle moving
							float deltX = event.getX() - mTouchX;
							float deltY = event.getY() - mTouchY;

							Matrix transform = new Matrix();
							Matrix inverse = new Matrix();

							transform.reset();
							transform.preRotate(mRotX);
							transform.preScale(mScale, mScale);
							transform.invert(inverse);

							float[] point = new float[2];
							point[0] = deltX;
							point[1] = deltY;

							inverse.mapPoints(point);
							mOffsetX = point[0] + mTouchOffsetX;
							mOffsetY = point[1] + mTouchOffsetY;
							this.invalidate();
						}
					}
					else if (event.getPointerCount() == 2)
					{

						if (mPinchZoom == false)
						{
							mPinchZoom = true;
							float x = event.getX(1);
							float y = event.getY(1);
							mTouchMultiPointerDistance = (float) Math.sqrt((double) ((x - mTouchX) * (x - mTouchX) + (y - mTouchY) * (y - mTouchY)));
							Log.d("MapView Touch", "Start scaling: " + mTouchMultiPointerDistance);
						}
						// let's try handling scale
						float x1 = event.getX(1);
						float y1 = event.getY(1);
						float x2 = event.getX();
						float y2 = event.getY();

						float dist = (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));

						float scale = 1;
						if (mTouchMultiPointerDistance > 0)
						{
							scale = dist / mTouchMultiPointerDistance;
						}

						// calculate the zoomLevel
						mZoomLevel = (float) (Math.log(mTouchScale * scale) / Math.log(SCALE_DELTA));
						calcScale();
						invalidate();

						Log.d("MapView Touch", "Pointer count:" + event.getPointerCount() + "(" + x1 + "," + y1 + ")  " + "(" + x2 + "," + y2 + ")  ");
					}

					break;
				}
				return true;
			}
		}
		else
		{
			return true;
		}

	}

	protected PointF getAvgDirection()
	{
		PointF retOffset = new PointF(0, 0);
		int offsetLoc = mPreviouseOffsetLoc;

		PointF firstAvg = new PointF(0, 0);
		PointF lastAvg = new PointF(0, 0);

		for (int i = 0; i < PREVIOUS_OFFSET_QUEUE_DEPTH; i++)
		{
			if (mPreviousOffsets[offsetLoc] != null)
			{
				if (i < (PREVIOUS_OFFSET_QUEUE_DEPTH / 2))
				{
					lastAvg.x += mPreviousOffsets[offsetLoc].x;
					lastAvg.y += mPreviousOffsets[offsetLoc].y;
				}
				if (i >= (PREVIOUS_OFFSET_QUEUE_DEPTH / 2))
				{
					firstAvg.x += mPreviousOffsets[offsetLoc].x;
					firstAvg.y += mPreviousOffsets[offsetLoc].y;
				}
			}

			if (--offsetLoc < 0)
			{
				offsetLoc = (PREVIOUS_OFFSET_QUEUE_DEPTH - 1);
			}
		}

		retOffset.x = lastAvg.x - firstAvg.x;
		retOffset.y = lastAvg.y - firstAvg.y;

		return retOffset;
	}

	protected void filterOffsetSpikes()
	{
		if (PREVIOUS_OFFSET_QUEUE_DEPTH < 3)
		{
			return;
		}

		int offsetLoc = mPreviouseOffsetLoc;
		PointF offset1, offset2, offset3;

		offset1 = mPreviousOffsets[offsetLoc];
		if (offset1 == null)
		{
			return;
		}

		if (--offsetLoc < 0)
		{
			offsetLoc = (PREVIOUS_OFFSET_QUEUE_DEPTH - 1);
		}
		int offsetLoc2 = offsetLoc;
		offset2 = mPreviousOffsets[offsetLoc];
		if (offset2 == null)
		{
			return;
		}

		if (--offsetLoc < 0)
		{
			offsetLoc = (PREVIOUS_OFFSET_QUEUE_DEPTH - 1);
		}
		offset3 = mPreviousOffsets[offsetLoc];
		if (offset3 == null)
		{
			return;
		}

		if (Math.abs(offset1.x - offset3.x) < Math.abs(offset2.x - offset3.x))
		{
			mPreviousOffsets[offsetLoc2].x = (offset1.x + offset3.x) / 2.0f;
		}
		if (Math.abs(offset1.y - offset3.y) < Math.abs(offset2.y - offset3.y))
		{
			mPreviousOffsets[offsetLoc2].y = (offset1.y + offset3.y) / 2.0f;
		}
	}

	protected void addOffset(float x, float y)
	{
		if (++mPreviouseOffsetLoc >= mPreviousOffsets.length)
		{
			mPreviouseOffsetLoc = 0;
		}

		if (mPreviousOffsets[mPreviouseOffsetLoc] == null)
		{
			mPreviousOffsets[mPreviouseOffsetLoc] = new PointF(x, y);
		}
		else
		{
			mPreviousOffsets[mPreviouseOffsetLoc].x = x;
			mPreviousOffsets[mPreviouseOffsetLoc].y = y;
		}

		filterOffsetSpikes();
	}

	protected void addOffset(PointF offset)
	{
		if (++mPreviouseOffsetLoc >= mPreviousOffsets.length)
		{
			mPreviouseOffsetLoc = 0;
		}

		mPreviousOffsets[mPreviouseOffsetLoc] = offset;

		filterOffsetSpikes();
	}

	protected PointF getLastOffset()
	{
		if (mPreviouseOffsetLoc < 0 || mPreviouseOffsetLoc >= mPreviousOffsets.length)
		{
			return null;
		}

		return mPreviousOffsets[mPreviouseOffsetLoc];
	}

	private class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			/*
			 * float x = e.getX(); float width = getWidth(); if (x < width / 3) { rotate(-5); } else if (x > 2 * width / 3) { rotate(5); } else { float y =
			 * e.getY(); x -= getViewportOrigin().x; y -= getViewportOrigin().y;
			 * 
			 * if (x * x + y * y < ROTATOR_RADIUS * ROTATOR_RADIUS) { zoomFit(); } }
			 * 
			 * return true;
			 */
			// for testing auto align
			ceterAlignTabbed(e.getX(), e.getY());
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e)
		{
			/*
			 * float cx = getWidth()/2.f; float cy = getHeight()/2.f;
			 * 
			 * mTouchX = e.getX(); mTouchY = e.getY(); mTouchRotate = mRotX;
			 * 
			 * if( (mTouchX - cx)*(mTouchX-cx) + (mTouchY-cy)*(mTouchY-cy) < ROTATOR_RADIUS*ROTATOR_RADIUS ) { mIsTouchRotating = true; invalidate(); } else {
			 * mIsTouchRotating = false; }
			 */
		}

		private void ceterAlignTabbed(float x, float y)
		{

			x -= getWidth() / 2;
			y -= getHeight() / 2;

			Matrix transform = new Matrix();
			Matrix inverse = new Matrix();

			transform.reset();
			transform.preRotate(mRotX);
			transform.preScale(mScale, mScale);
			transform.invert(inverse);

			float[] point = new float[2];
			point[0] = x;
			point[1] = y;

			inverse.mapPoints(point);

			float cx = mOffsetX - point[0];
			float cy = mOffsetY - point[1];
			// invalidate();
			MapView.this.setCenter(-cx, -cy, true);
		}
	};
}
