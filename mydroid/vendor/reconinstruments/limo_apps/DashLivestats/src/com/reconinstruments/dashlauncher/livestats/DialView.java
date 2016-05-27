package com.reconinstruments.dashlauncher.livestats;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.reconinstruments.utils.ConversionUtil;

/**
 * 	Speedometer Custom DialView
 * 	
 *  Shows current speed 
 *  
 * @author patrickcho
 *
 */
public class DialView extends View {
	public static final String TAG = "DialView";
	public static final boolean DEBUG = false;

	public static final int SHAPE_START_DEGREE			= 150;
	public static final int SHAPE_SWEEP_OFFSET			= 240;

	/**
	 * offset number of change between sweeps
	 */
	public static final float SHAPE_SWEEP_CHANGE_OFFSET	= 20.0f;

	/**
	 *  At least this much of change should occur before drawing new sweep.
	 *	For performance optimization.
	 */
	public static final float SHAPE_CHANGE_THRESHHOLD	= 2.0f;

	public static final float RATIO_FAST					= 0.5f;
	public static final float RATIO_FASTER				= 0.75f;
	public static final float RATIO_FASTEST				= 0.85f;

	public static final float SHAPE_RING_THICKNESS		= 27.0f;
	public static final float BLUR_RADIUS				= 5.0f;

	public static final int STAT_TYPE_VERTICAL_OFFSET		= -57;
	public static final int STAT_TYPE_HORIZONTAL_OFFSET	= -15;

	public static final int MAIN_STAT_VERTICAL_OFFSET		= 20;
	public static final int MAIN_STAT_HORIZONTAL_OFFSET_1	= 10;
	public static final int MAIN_STAT_HORIZONTAL_OFFSET_2= 3;

	public static final int MAIN_STAT_HORIZONTAL_OFFSET_3= 0;

	public static final int MAIN_STAT_TEXT_SIZE_1			= 100;
	public static final int MAIN_STAT_TEXT_SIZE_2			= 90;
	public static final int MAIN_STAT_TEXT_SIZE_3			= 80;
	
	public static final int MAX_STAT_TEXT_SIZE			= 22;
	public static final int MAX_STAT_UNIT_SIZE			= 20;
	public static final int MAX_STAT_VALUE_SIZE			= 24;

	/** In metric */
	public static final int MAIN_MAX_LOWER_LIMIT			= 25;

	public static final int MAIN_MAX_VERTICAL_OFFSET		= 54;
	public static final int MAIN_MAX_HORIZONTAL_OFFSET	= -6;
	public static final int MAIN_MAX_GAP_OFFSET			= 5;

	public static final int LINE_VERTICAL_OFFSET			= 10;

	public static final int	SLIDE_DOWN_OFFSET			= 57;

	private static final int COLOR_FAST = 0xfffbe80f;
	private static final int COLOR_FASTER = 0xffff8a00;
	private static final int COLOR_FASTEST = 0xffdf271e;//0xfff63434;//0xffff0c00

	private Context mContext = null;

	private int mainStatHorizontalOffset = MAIN_STAT_HORIZONTAL_OFFSET_1;
	private float currentStat = -1;
	private float maxStat = -1;
	private float maxLowerLimit = -1;
	private String statUnit = "--";
	private boolean maxStatSet = false;

	private Paint shapeBackPaint = null;

	private Paint mainStatPaint = null;
	private Paint statTypePaint = null;
	private Paint statMaxPaint = null;

	private Paint shapePaint = null;
	private Paint shapeFastPaint = null;
	private Paint shapeFasterPaint = null;
	private Paint shapeFastestPaint = null;
	//private Paint glowPaint = null;

	private float currentSweep = 0;

	private Rect drawRect = null;
	private RectF shapeRectF = null;

	public DialView(Context context) {
		super(context);
		mContext = context;
		initShape();
	}

	public DialView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initShape();
	}
	public DialView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initShape();
	}
	private void initShape(){

		// default max value is set
		maxStat = MAIN_MAX_LOWER_LIMIT;

		boolean isMetric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UNITS_METRIC;
		maxLowerLimit = isMetric ? MAIN_MAX_LOWER_LIMIT : (float)ConversionUtil.kmsToMiles(MAIN_MAX_LOWER_LIMIT);

		shapePaint = new Paint();
		shapePaint.setAntiAlias(true);
		shapePaint.setDither(true);
		shapePaint.setColor(0xFF59d300);
		shapePaint.setStyle(Style.STROKE);
		shapePaint.setStrokeCap(Cap.BUTT);
		shapePaint.setShadowLayer(0.5f, 0, -1.0f, Color.WHITE);
		shapePaint.setStrokeWidth(SHAPE_RING_THICKNESS-2 );

		/*
		shapePaint.setShadowLayer(10.0f, 0.0f, 0.0f, 0xFF00FFFF);
		int[] colours = new int[]{0xFFFF0000,0xFFFF0000,0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFFFFFF00,0xFFFF0000};
		float[] positions = new float[]{0.0f,0.25f,0.25f,0.4f,0.7f,0.8f,1.0f};
		Shader shader = new SweepGradient(95.0f,95.0f,colours,positions);
		shapePaint.setShader(shader);
		 */

		shapeFastPaint = new Paint(shapePaint);
		shapeFastPaint.setShader(null);
		shapeFastPaint.setColor(COLOR_FAST);
		shapeFastPaint.setShadowLayer(5.0f, 0, 0, COLOR_FAST);
		shapeFastPaint.setAlpha(0);

		shapeFasterPaint = new Paint(shapePaint);
		shapeFasterPaint.setShader(null);
		shapeFasterPaint.setColor(COLOR_FASTER);
		shapeFasterPaint.setShadowLayer(5.0f, 0, 0, COLOR_FASTER);
		shapeFasterPaint.setAlpha(0);

		shapeFastestPaint = new Paint(shapePaint);
		shapeFastestPaint.setShader(null);
		shapeFastestPaint.setColor(COLOR_FASTEST); 
		shapeFastestPaint.setShadowLayer(5.0f, 0, 0, COLOR_FASTEST);
		shapeFastestPaint.setAlpha(0);

		shapeBackPaint = new Paint();
		shapeBackPaint.setAntiAlias(true);
		shapeBackPaint.setDither(true);
		shapeBackPaint.setColor(0xFF393939);
		shapeBackPaint.setStrokeWidth(SHAPE_RING_THICKNESS);
		shapeBackPaint.setStyle(Style.STROKE);
		shapeBackPaint.setShadowLayer(1.5f, 0, 0, Color.BLACK);

		/* glowPaint = new Paint();
		glowPaint.setAntiAlias(true);
		glowPaint.setDither(true);
		glowPaint.setStyle(Style.STROKE); 
		glowPaint.setStrokeWidth(SHAPE_RING_THICKNESS);
		glowPaint.setShadowLayer(10.0f, 0.0f, 0.0f, 0xFF00FFFF);
		glowPaint.setColor(0xFF0000FF);
		glowPaint.setStrokeCap(Cap.SQUARE); */

		mainStatPaint = new Paint();
		mainStatPaint.setAntiAlias(true);
		mainStatPaint.setDither(true);
		mainStatPaint.setColor(Color.WHITE);
		mainStatPaint.setTextSize(MAIN_STAT_TEXT_SIZE_1);
		mainStatPaint.setStyle(Style.STROKE);
		mainStatPaint.setShadowLayer(3.0f, 0, 2.0f, 0xAA000000);
		mainStatPaint.setTypeface(Typeface.createFromAsset(mContext.getAssets(),"fonts/sans_pro_bold.ttf"));

		statTypePaint = new Paint();
		statTypePaint.setAntiAlias(true);
		statTypePaint.setDither(true);
		statTypePaint.setColor(0xFFAAAAAA);
		statTypePaint.setTextSize(MAX_STAT_TEXT_SIZE);
		statTypePaint.setStyle(Style.STROKE);
		
		statMaxPaint = new Paint(statTypePaint);
		statMaxPaint.setColor(Color.WHITE);
		statMaxPaint.setTextSize(MAX_STAT_VALUE_SIZE);
		statMaxPaint.setTypeface(Typeface.DEFAULT_BOLD);

		shapeRectF = new RectF();
		drawRect = new Rect();
	}

	public void setMaxVal(float max, String unit){
		// refresh screen only when needed
		if (maxStat != max) {
			if (max <= 0) {
				max = 0;
			}
			maxStat = max;
			statUnit = unit;
			maxStatSet = true;
			if (DEBUG) Log.d(TAG, "maxValue invalidate maxStat : " + maxStat + " max : " + max);
			invalidate();
		}
	}

	public void setCurrentVal(float val, String unit){
		if (currentStat != val) {
			currentStat = val;
			statUnit = unit;

			if (currentStat >= 100) {
				mainStatPaint.setTextSize(MAIN_STAT_TEXT_SIZE_2);
				mainStatHorizontalOffset = MAIN_STAT_HORIZONTAL_OFFSET_3;
			}
			else if (currentStat >= 10) {
				mainStatPaint.setTextSize(MAIN_STAT_TEXT_SIZE_1);
				mainStatHorizontalOffset = MAIN_STAT_HORIZONTAL_OFFSET_2;
			}
			else {
				mainStatPaint.setTextSize(MAIN_STAT_TEXT_SIZE_1);
				mainStatHorizontalOffset = MAIN_STAT_HORIZONTAL_OFFSET_1;
			}

			if (DEBUG) Log.d(TAG, "maxValue invalidate maxStat : " + currentStat + " max : " + val);
			invalidate();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		//int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		//int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		if (DEBUG) {
			//Log.d(TAG, "onMeasure called : measured : " + measured);
			Log.d(TAG, "widthSize " + widthSize + "heightSize : " + heightSize);
		}

		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		Log.d(TAG, "onVisibilityChanged called");
	}

	private Rect mainStatBounds = new Rect();
	float shape_offset = SHAPE_RING_THICKNESS/2+BLUR_RADIUS;
	
	@Override
	protected void onDraw(Canvas canvas) {
		// uncomment this if needed, it will only work API+11
		//setLayerType(LAYER_TYPE_HARDWARE, shapeBackPaint);

		getDrawingRect(drawRect);
		
		shapeRectF.left = drawRect.left + shape_offset;
		shapeRectF.right = drawRect.right - shape_offset;
		shapeRectF.top = drawRect.top + shape_offset;
		shapeRectF.bottom = drawRect.bottom - shape_offset + SLIDE_DOWN_OFFSET;

		String speedText = "--";
		if (currentStat > -1) { // No GPS fix yet
			speedText = (int)currentStat + "";
		}

		float mainStatWidth = mainStatPaint.measureText(speedText);
		mainStatPaint.getTextBounds(speedText, 0, speedText.length(), mainStatBounds);
		//float mainStatBoundWidth = mainStatBounds.width();
		
		canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, SHAPE_SWEEP_OFFSET, false, shapeBackPaint);
		canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, currentSweep, false, shapePaint);
		canvas.drawText("SPEED",  shapeRectF.width()/2 + STAT_TYPE_HORIZONTAL_OFFSET, shapeRectF.centerY() + STAT_TYPE_VERTICAL_OFFSET , statTypePaint);
		canvas.drawText(speedText, shapeRectF.width()/2 - (int)(mainStatWidth/2.7) + mainStatHorizontalOffset, shapeRectF.centerY() + MAIN_STAT_VERTICAL_OFFSET , mainStatPaint);
		canvas.drawLine(shapeRectF.width() * 0.25f, shapeRectF.height() * 0.7f + LINE_VERTICAL_OFFSET, shapeRectF.width() * 0.92f, shapeRectF.height() * 0.7f + LINE_VERTICAL_OFFSET, statTypePaint);

		if (maxStatSet) {
			final String maxStr = "MAX";
			final String maxStatValueStr = (int)(maxStat+0.5f) + "";
			int halfOffset = getHalfOffset((int)statTypePaint.measureText(maxStr), 2*MAIN_MAX_GAP_OFFSET, (int)statMaxPaint.measureText(maxStatValueStr));
			canvas.drawText(maxStr, shapeRectF.width()/2 - halfOffset , shapeRectF.centerY() + MAIN_MAX_VERTICAL_OFFSET, statTypePaint);
			canvas.drawText(maxStatValueStr, shapeRectF.width()/2 - halfOffset + statTypePaint.measureText(maxStr) + MAIN_MAX_GAP_OFFSET, shapeRectF.centerY() + MAIN_MAX_VERTICAL_OFFSET, statMaxPaint);
			statTypePaint.setTextSize(MAX_STAT_UNIT_SIZE);
			canvas.drawText(statUnit, shapeRectF.width()/2 - halfOffset + statTypePaint.measureText(maxStr) + 2*MAIN_MAX_GAP_OFFSET + statMaxPaint.measureText(maxStatValueStr), shapeRectF.centerY() + MAIN_MAX_VERTICAL_OFFSET, statTypePaint);
			statTypePaint.setTextSize(MAX_STAT_TEXT_SIZE);
		}

		float sweepRatio = currentSweep / SHAPE_SWEEP_OFFSET;
		int shapeFastPaintAlpha = shapeFastPaint.getAlpha();
		int shapeFasterPaintAlpha = shapeFasterPaint.getAlpha();
		int shapeFastestPaintAlpha = shapeFastestPaint.getAlpha();

		if (sweepRatio <= RATIO_FAST) {
			if (shapeFastPaintAlpha > 0) {
				shapeFastPaint.setAlpha(0);
			}
			if (shapeFasterPaintAlpha > 0) {
				shapeFasterPaint.setAlpha(0);
			}
			if (shapeFastestPaintAlpha > 0) {
				shapeFastestPaint.setAlpha(0);
			}
		} else if (RATIO_FAST < sweepRatio && sweepRatio <= RATIO_FASTER) {

			shapeFastPaint.setAlpha((int)((sweepRatio - RATIO_FAST) / (RATIO_FASTER - RATIO_FAST) * 255));

			if (shapeFasterPaintAlpha > 0) {
				shapeFasterPaint.setAlpha(0);
			}
			if (shapeFastestPaintAlpha > 0) {
				shapeFastestPaint.setAlpha(0);
			}
			drawOverlayScreen(canvas);
		} else if (RATIO_FASTER < sweepRatio && sweepRatio <= RATIO_FASTEST) {

			shapeFastPaint.setAlpha(255 - (int)((sweepRatio - RATIO_FASTER) / (RATIO_FASTEST - RATIO_FASTER) * 255));
			shapeFasterPaint.setAlpha((int)((sweepRatio - RATIO_FASTER) / (RATIO_FASTEST - RATIO_FASTER) * 255));

			if (shapeFastestPaintAlpha > 0) {
				shapeFastestPaint.setAlpha(0);
			}
			drawOverlayScreen(canvas);
		} else if (RATIO_FASTEST < sweepRatio) {

			shapeFasterPaint.setAlpha(255 - (int)((sweepRatio - RATIO_FASTEST) / (1.0f - RATIO_FASTEST) * 255));
			shapeFastestPaint.setAlpha((int)((sweepRatio - RATIO_FASTEST) / (1.0f - RATIO_FASTEST) * 255));

			if (shapeFastPaintAlpha > 0) {
				shapeFastPaint.setAlpha(0);
			}
			drawOverlayScreen(canvas);
		}

		/**
		 * increments/decrements the currentSweep and request to redraw it 
		 */
		float newSweep = SHAPE_SWEEP_OFFSET * Math.min(1.0f, Math.max(currentStat, 0)/Math.max(maxStat , maxLowerLimit));
		float incrementsRatio = Math.abs(newSweep-currentSweep)/ SHAPE_SWEEP_CHANGE_OFFSET;
		float increments = incrementsRatio >= 1.0f ? SHAPE_SWEEP_CHANGE_OFFSET : (incrementsRatio * SHAPE_SWEEP_CHANGE_OFFSET);

		if (increments > SHAPE_CHANGE_THRESHHOLD) {
			if(newSweep>currentSweep){
				currentSweep += increments;
				if (DEBUG) Log.d(TAG, "currentSweepInvalidate");
				invalidate();
			} else {
				currentSweep -= increments;
				if (DEBUG) Log.d(TAG, "currentSweepInvalidate");
				invalidate();
			}
		}
	}
	
	private int getHalfOffset(int... values) {
		int total = 0;
		for (int value : values) {
			total += value;
		}
		return (total/2);
	}

	private void drawOverlayScreen(Canvas canvas) {
		canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, currentSweep, false, shapeFastPaint);
		canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, currentSweep, false, shapeFasterPaint);
		canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, currentSweep, false, shapeFastestPaint);
	}

}
