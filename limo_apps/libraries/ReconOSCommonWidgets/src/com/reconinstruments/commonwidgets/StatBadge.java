package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class StatBadge extends View {
	public static final String TAG = "StatBadge";


	/*
	 * Defining Constants
	 */
	public static final int DEFAULT_STRING_COLOR		= Color.WHITE;
	public static final int DEFAULT_SUBTITLE_COLOR		= Color.GRAY;
	public static final int DEFAULT_UNIT_COLOR			= Color.LTGRAY;
	public static final int DEFAULT_SUBSTAT_COLOR		= Color.WHITE;
	
	public static final int DEFAULT_SHAPE_COLOR			= 0xFFAAAAAA;
	public static final int DEFAULT_SHAPE_BELOW_COLOR	= 0xFF4D4D4D;
	
	public static final int DEFAULT_SHAPE_COLOR_SPEED	= 0xFF59D300;
	public static final int DEFAULT_SHAPE_COLOR_ALTITUDE= 0xFF00AEEF;
	public static final int DEFAULT_SHAPE_COLOR_VERTICAL= 0xFFFFF200;
	public static final int DEFAULT_SHAPE_COLOR_DISTANCE= 0xFF9D49E5;
	public static final int DEFAULT_SHAPE_COLOR_JUMP	= 0xFFFFB600;
	
	public static final int SHAPE_STYLE_SPEED			= 0x0000;
	public static final int SHAPE_STYLE_ALTITUDE		= 0x0001;
	public static final int SHAPE_STYLE_VERTICAL		= 0x0002;
	public static final int SHAPE_STYLE_JUMP			= 0x0003;
	public static final int SHAPE_STYLE_DISTANCE		= 0x0004;
	
	public static final int SHAPE_START_DEGREE			= -90;
	public static final float SHAPE_SWEEP_INC			= 10.0f;
	
	/** Decrease this to increase the thickness */
	public static final float SHAPE_RING_THICKESS		= 12.0f;
	
	public static final float MAINSTAT_FONT_SIZE		= 4.0f;
	public static final float SUBSTAT_FONT_SIZE			= 12.0f;
	public static final float SUBTITLE_FONT_SIZE		= 12.0f;
	
	public static final String SUBTITLE_MAX_TODAY		= "MAX TODAY";
	public static final String SUBTITLE_ALLTIME_MAX		= "ALLTIME MAX";
	
	public static final String UNIT_IMPERIAL_SPEED		= "mph";
	public static final String UNIT_METRIC_SPEED		= "km/h";
	
	public static final String UNIT_IMPERIAL_DISTANCE	= "ft";
	public static final String UNIT_METRIC_DISTANCE		= "m";

	public static final String UNIT_IMPERIAL_DISTANCE_B	= "mi";
	public static final String UNIT_METRIC_DISTANCE_B	= "km";
	
	public static final String UNIT_TIME_SECOND			= "s";
	public static final String UNIT_TIME_MINUTE			= "m";
	public static final String UNIT_TIME_HOUR			= "h";
	
	public static enum DRAWING_MODE {
		ICON, STAT
	};
	
	/**
	 * Context Used For Lots of Purpose
	 */
	@SuppressWarnings("unused")
	private Context context = null;

	/*
	 * Drawing Mode Default = stat
	 */
	private DRAWING_MODE drawingMode = DRAWING_MODE.STAT;
	
	/*
	 * Strings
	 */
	private String mainStatText = null;
	private String subStatText	= null;
	private String subTitleText = null;
	
	private float mainStat;
	private float subStat;
	
	private String unitSpeed = UNIT_METRIC_SPEED;
	private String unitDistance = UNIT_METRIC_DISTANCE;
	private String unitDistanceBig = UNIT_METRIC_DISTANCE_B;
	
	//private ArrayList<String> effectText = null;

	/*
	 * Bitmap of icon
	 */
	private Bitmap statBitmap = null;
	//private ArrayList<Bitmap> effectBitmapMapList = null;

	/*
	 * Declaring objects for Drawings
	 */
	private Paint shapeBackPaint = null;
	private Paint shapePaint = null;

	private Paint mainStatPaint = null;
	private Paint subStatPaint = null;
	private Paint subStatUnitPaint = null;
	private Paint subTitlePaint = null;
	
	private float mSweep = 0;
	//private Paint effectPaint = null;

	//private Path mPath = null;
	//private Region rgn = null;
	private RectF shapeRectF = null;
	private Rect srcRect = null;
	private Rect dstRect = null;

	private float shapeWidth;
	private float shapeHeight;
	
	/**
	 * Type of Stats that can have different settings, if this is not set from
	 * XML, it will throw exception.
	 * TODO define exceptions for UI Libraries
	 */
	public static enum StatType
	{ SPEED, ALTITUDE, VERTICAL, JUMP, DISTANCE, OTHER };
	private StatType statType = null;
	
	public static enum Unit
	{ IMPERIAL, METRIC };
	private Unit unit = Unit.METRIC; // TODO metric FOR now, 


	/**
	 * This is used only when initialized by code
	 * @param context
	 */
	public StatBadge(Context context) {
		super(context);
		this.context = context;
		initShape();
	}

	// TODO not sure when this is used exactly
	public StatBadge(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		initShape();
	}

	/**
	 * This is used when initialized from XML
	 * @param context
	 * @param attrs
	 */
	public StatBadge(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		initShape();

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StatBadge);

		int tempStatType = a.getInt(R.styleable.StatBadge_statType, -1);
		if (tempStatType == -1) {
			Log.d(TAG, "stat Type is not set, color is not set automatically, and will throw error if drawing without setting colors explicilty");
			//throw new RuntimeException("StatType is not set");
		} else {
			setStatType(StatType.values()[tempStatType]);
		}
		
		setShapeDimension(a.getDimension(R.styleable.StatBadge_shapeWidth, -1), a.getDimension(R.styleable.StatBadge_shapeHeight, -1));

		String tempMainStatText = a.getString(R.styleable.StatBadge_mainStatText);
		String tempSubStatText = a.getString(R.styleable.StatBadge_subStatText);
		String tempSubTitleText = a.getString(R.styleable.StatBadge_subTitleText);
		
		int mainStatTextColor = a.getColor(R.styleable.StatBadge_mainStatTextColor, DEFAULT_STRING_COLOR);
		int subStatTextColor = a.getColor(R.styleable.StatBadge_subStatTextColor, DEFAULT_SUBSTAT_COLOR);
		int subTitleTextColor = a.getColor(R.styleable.StatBadge_subTitleTextColor, DEFAULT_SUBTITLE_COLOR);
		
		mainStatText = tempMainStatText;
		subStatText = tempSubStatText;
		
		if (mainStatText!=null)
			mainStat = Float.parseFloat(mainStatText);
		if (subStatText!=null)
			subStat = Float.parseFloat(subStatText);
		
		setMainStatTextColor(mainStatTextColor);
		setSubStatTextColor(subStatTextColor);
		setSubTitleTextColor(subTitleTextColor);
		// TODO Decide wether to add override feature to the size of the font
		
		/**
		 * If Stat is set, colors of ring will automatically set unless it is overriden
		 */
		if (statType!=null)
		switch (statType) {
		case SPEED:		// SPEED has only one stat field
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_SPEED);
			break;
		case ALTITUDE:	// ALT can be large, number format is important
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_ALTITUDE);
			break;
		case DISTANCE:	// DIS similar to ALT, number format is important
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_DISTANCE);
			break;
		case JUMP:		// JUMP has 3 stats, requires deeper implementation
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_JUMP);
			break;
		case VERTICAL:	// VERT has only on stat field, typically not large
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_VERTICAL);
			break;
		case OTHER:		// This is for other use of StatBadge for versatile use of library
			setShapeColor(DEFAULT_SHAPE_COLOR);
			break;
		}
		// Override subtitle
		if (tempSubTitleText != null)
			subTitleText = tempSubTitleText;
		
		/**
		 * Following Lines will override the color of Ring
		 * BackColor should always be set
		 */
		int shapeColor = a.getColor(R.styleable.StatBadge_shapeColor, -1);
		if (shapeColor != -1)
			setShapeColor(shapeColor);
		setShapeBackColor(a.getColor(R.styleable.StatBadge_shapeBackColor, DEFAULT_SHAPE_BELOW_COLOR));

		a.recycle();

	}

	private void initShape() {
		
		srcRect = new Rect();
		dstRect = new Rect();

		// ShapePaint+ShapeBackPaint Initialize
		shapeBackPaint = new Paint();
		shapeBackPaint.setAntiAlias(true);
		shapeBackPaint.setDither(true);
		shapeBackPaint.setStrokeWidth(20.0f);
		shapeBackPaint.setStyle(Style.STROKE);
		shapeBackPaint.setShadowLayer(1.5f, 0, 1.0f, Color.BLACK);

		shapePaint = new Paint();
		shapePaint.setAntiAlias(true);
		shapePaint.setDither(true);
		shapePaint.setStrokeWidth(20.0f);
		shapePaint.setStyle(Style.STROKE);

		// Center Initialize
		mainStatPaint = new Paint();
		mainStatPaint.setAntiAlias(true);
		mainStatPaint.setDither(true);
		mainStatPaint.setColor(Color.WHITE);
		mainStatPaint.setStyle(Style.STROKE);
		mainStatPaint.setShadowLayer(1.5f, 0, 1.0f, Color.BLACK);

		subStatPaint = new Paint();
		subStatPaint.setAntiAlias(true);
		subStatPaint.setDither(true);
		subStatPaint.setColor(Color.WHITE);
		subStatPaint.setStyle(Style.STROKE);
		subStatPaint.setShadowLayer(0.5f, 0, 1.0f, Color.BLACK);
		
		subStatUnitPaint = new Paint();
		subStatUnitPaint.setAntiAlias(true);
		subStatUnitPaint.setDither(true);
		subStatUnitPaint.setColor(Color.LTGRAY);
		subStatUnitPaint.setStyle(Style.STROKE);
		subStatUnitPaint.setShadowLayer(0.5f, 0, 1.0f, Color.BLACK);
		
		subTitlePaint = new Paint();
		subTitlePaint.setAntiAlias(true);
		subTitlePaint.setDither(true);
		subTitlePaint.setColor(Color.GRAY);
		subTitlePaint.setStyle(Style.STROKE);
		subTitlePaint.setShadowLayer(0.5f, 0, 1.0f, Color.BLACK);

		//mPath = new Path();
		//rgn = new Region();

		shapeRectF = new RectF();
	}
	
	public void setUnit(Unit unit) {
		this.unit = unit;
		switch (unit) {
		case IMPERIAL:
			unitSpeed		= UNIT_IMPERIAL_SPEED;
			unitDistance	= UNIT_IMPERIAL_DISTANCE;
			unitDistanceBig	= UNIT_IMPERIAL_DISTANCE_B;
			break;
		case METRIC:
			unitSpeed		= UNIT_METRIC_SPEED;
			unitDistance	= UNIT_METRIC_DISTANCE;
			unitDistanceBig	= UNIT_METRIC_DISTANCE_B;
			break;
		}
	}

	///////////////////////////////////////////////////////////
	// Shape Part
	///////////////////////////////////////////////////////////
	
	/**
	 * If this is the method that is called last time, then the color of ring will be overriden too, no matter
	 * what is set on at the shape color explicitly
	 * 
	 * @param style
	 */
	public void setStatType(StatType style) {
		statType = style;
		
		switch (statType) {
		case SPEED:		// SPEED has only one stat field
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_SPEED);
			setDrawable(R.drawable.speed_icon);
			break;
		case ALTITUDE:	// ALT can be large, number format is important
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_ALTITUDE);
			setDrawable(R.drawable.altitude_icon);
			break;
		case DISTANCE:	// DIS similar to ALT, number format is important
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_DISTANCE);
			setDrawable(R.drawable.distance_icon);
			break;
		case JUMP:		// JUMP has 3 stats, requires deeper implementation
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_JUMP);
			setDrawable(R.drawable.vert_icon);
			break;
		case VERTICAL:	// VERT has only on stat field, typically not large
			subTitleText = SUBTITLE_MAX_TODAY;
			setShapeColor(DEFAULT_SHAPE_COLOR_VERTICAL);
			setDrawable(R.drawable.vert_icon);
			break;
		case OTHER:		// This is for other use of StatBadge for versatile use of library
			setShapeColor(DEFAULT_SHAPE_COLOR);
			break;
		}
		
	}

	public void setShapeBackColor(int color) {
		shapeBackPaint.setColor(color);
	}

	public void setShapeColor(int color) {
		shapePaint.setColor(color);
	}

	public void setShapeDimension(float width, float height) {
		shapeWidth = width;
		shapeHeight = height;
	}

	public void setMainStatText(String text) {
		mainStatText = text;
		mainStat = Float.parseFloat(mainStatText);
		requestLayout();
		invalidate();
	}
	public void setSubStatText(String text) {
		subStatText = text;
		subStat = Float.parseFloat(subStatText);
		requestLayout();
		invalidate();
	}
	public void setTitleText(String text) {
		subTitleText = text;
		requestLayout();
		invalidate();
	}

//	public void setMainStatTextSize(int size) {
//		// This text size has been pre-scaled by the getDimensionPixelOffset method
//		mainStatPaint.setTextSize(size);
//		requestLayout();
//		invalidate();
//	}

	public void setMainStatTextColor(int color) {
		mainStatPaint.setColor(color);
		invalidate();
	}
	public void setSubStatTextColor(int color) {
		subStatPaint.setColor(color);
		invalidate();
	}
	public void setSubTitleTextColor(int color) {
		subTitlePaint.setColor(color);
		invalidate();
	}

	/**
	 * Sets the Drawable for the center area.
	 * @param color ARGB value for the text
	 */
//	public void setCenterDrawable(int resId) {
//		centerBitmap = ((BitmapDrawable) context.getResources().getDrawable(resId)).getBitmap();
//		requestLayout(); // Since size can be different between drawables
//		invalidate();
//	}

	public void setTypeFace(Typeface typeface) {
		mainStatPaint.setTypeface(typeface);
		subStatPaint.setTypeface(typeface);
		subStatUnitPaint.setTypeface(typeface);
		subTitlePaint.setTypeface(typeface);
		requestLayout();
		invalidate();
	}
	
	public void setDrawingMode(DRAWING_MODE mode) {
		drawingMode = mode;
	}
	
	public void setDrawable(int dRsc) {
		Drawable drawable = context.getResources().getDrawable(dRsc);
		statBitmap = ((BitmapDrawable)drawable).getBitmap();
		srcRect.set(0,0,statBitmap.getWidth(),statBitmap.getHeight());
	}
	
	public void measureSizes() {
		shapePaint.setStrokeWidth(shapeWidth/SHAPE_RING_THICKESS);
		shapeBackPaint.setStrokeWidth(shapeWidth/SHAPE_RING_THICKESS);
		
		mainStatPaint.setTextSize(shapeWidth/MAINSTAT_FONT_SIZE);
		subTitlePaint.setTextSize(shapeWidth/SUBTITLE_FONT_SIZE);
		subStatPaint.setTextSize(shapeWidth/SUBSTAT_FONT_SIZE);
		subStatUnitPaint.setTextSize(shapeWidth/SUBSTAT_FONT_SIZE);
		
		invalidate();
	}
	
	public void reDraw(boolean reAnimate) {
		
		if (reAnimate) {
			mSweep = 0;
		}
		else {
			mSweep = mainStat/subStat * 360f;
		}
			
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
		//Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int chosenWidth = (int) chooseWidthDimension(widthMode, widthSize);
		int chosenHeight = (int) chooseHeightDimension(heightMode, heightSize);

		// If shapeSize is not expilictly stated, override with layout size
		if (shapeWidth < 0 ) 
			setShapeDimension(chosenWidth, shapeHeight);
		if (shapeHeight < 0)
			setShapeDimension(shapeWidth, chosenWidth);

		measureSizes();
		
		int chosenDimension = Math.min(chosenWidth, chosenHeight);

		setMeasuredDimension(chosenDimension, chosenDimension);
	}

	private float chooseWidthDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return shapeWidth;
		} 
	}

	private float chooseHeightDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return shapeHeight;
		} 
	}

	@Override
	protected void onDraw(Canvas canvas) {
		setLayerType(View.LAYER_TYPE_HARDWARE, shapeBackPaint);

		shapeRectF.set(
				(float)(canvas.getWidth()/2 - 8*shapeWidth/20),		// Left Bound
				(float)(canvas.getHeight()/2 - 8*shapeHeight/20),	// Top Bound
				(float)(canvas.getWidth()/2 + 8*shapeWidth/20),		// Right Bound
				(float)(canvas.getHeight()/2 + 8*shapeHeight/20)	// Bottom Bound
				);
		
		String tempSubStat = null;
		switch (statType) {
		case ALTITUDE :
			tempSubStat = subStatText + unitDistance;
			break;
		case SPEED :
			tempSubStat = subStatText + unitSpeed;
			break;
		case DISTANCE :
			tempSubStat = subStatText + unitDistance;
			break;
		case JUMP :
			tempSubStat = subStatText + unitSpeed;
			break;
		case VERTICAL :
			tempSubStat = subStatText + unitDistance;
			break;
		case OTHER :
			tempSubStat = subStatText;
			break;
		}
		
		

		switch (drawingMode) {
		case ICON :
			dstRect.set((int)(shapeWidth*0.25), (int)(shapeHeight*0.25), (int)(shapeWidth * 0.75), (int)(shapeHeight * 0.75));
			canvas.drawBitmap(statBitmap, srcRect, dstRect, null);
			
			// Draw arc above drawable
			canvas.drawArc(shapeRectF, 0, 360, false, shapeBackPaint);
			canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, mSweep, false, shapePaint);
			
			break;
		case STAT :
			// Draw arc under stat
			canvas.drawArc(shapeRectF, 0, 360, false, shapeBackPaint);
			canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, mSweep, false, shapePaint);
			
			float mainStatWidth = mainStatPaint.measureText(mainStatText);
			float subTitleWidth = subTitlePaint.measureText(subTitleText);
			float subStatWidth = subStatPaint.measureText(tempSubStat);
			
			//float mainStatTextSize = mainStatPaint.getTextSize();
			float subTitleTextSize = subTitlePaint.getTextSize();
			float subStatTextSize = subStatPaint.getTextSize();
			
			canvas.drawText(mainStatText, canvas.getWidth()/2 - mainStatWidth/2, canvas.getHeight()/2	, mainStatPaint);
			canvas.drawText(subTitleText, canvas.getWidth()/2 - subTitleWidth/2, 2*canvas.getHeight()/3	, subTitlePaint);
			canvas.drawText(tempSubStat, canvas.getWidth()/2 - subStatWidth/2, 2*canvas.getHeight()/3 + subStatTextSize , subStatPaint);
			break;
		}
		
		float statRatio = mainStat/subStat; // If substat is max stat all the time TODO : check with glenn
		float graphRatio = mSweep/360f;
		float inc = SHAPE_SWEEP_INC * (1.0f - graphRatio);
		if(graphRatio < statRatio && Math.abs(inc) > 2)
		{
			mSweep += inc;
			invalidate(); // Dont draw if not needed LOL
		}
		
		//super.onDraw(canvas);
	}

}
