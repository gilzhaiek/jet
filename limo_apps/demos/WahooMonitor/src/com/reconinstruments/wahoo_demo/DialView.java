package com.reconinstruments.wahoo_demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class DialView extends View {
	public static final String TAG = "DialView";


	public static final int SHAPE_START_DEGREE			= 135;
	public static final int SHAPE_MAX_SWEEP				= 270;
	
	/** Decrease this to increase the thickness */
	public static final float SHAPE_RING_THICKNESS		= 16.0f;
	public static final float BLUR_RADIUS		= 10.0f;

	private float currentStat;
	private float maxStat;
	

	private Paint shapePaint = null;
	private Paint glowPaint = null;
	
	private float currentSweep = 0;

	private Rect drawRect = null;
	private RectF shapeRectF = null;

	public DialView(Context context) {
		super(context);
		initShape();
	}

	public DialView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initShape();
	}
	public DialView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		initShape();
	}
	private void initShape(){
		
		shapePaint = new Paint();
		shapePaint.setAntiAlias(true);
		shapePaint.setDither(true);
		shapePaint.setStyle(Style.STROKE);
		shapePaint.setStrokeWidth(SHAPE_RING_THICKNESS);
		
		shapePaint.setStrokeCap(Cap.ROUND);
		
		//shapePaint.setShadowLayer(10.0f, 0.0f, 0.0f, 0xFF00FFFF);

		int[] colours = new int[]{0xFFFF0000,0xFFFF0000,0xFF00FFFF,0xFF00FFFF,0xFF00FF00,0xFFFFFF00,0xFFFF0000};//,0xFFFFFF00,0xFF00FFFF};
		float[] positions = new float[]{0.0f,0.25f,0.25f,0.4f,0.7f,0.8f,1.0f};
		
		Shader shader = new SweepGradient(95.0f,95.0f,colours,positions);
		shapePaint.setShader(shader);
		
		glowPaint = new Paint();
		glowPaint.setAntiAlias(true);
		glowPaint.setDither(true);
		glowPaint.setStyle(Style.STROKE); 
		glowPaint.setStrokeWidth(SHAPE_RING_THICKNESS);
		glowPaint.setShadowLayer(10.0f, 0.0f, 0.0f, 0xFF00FFFF);
		glowPaint.setColor(0xFF00FFFF);
		glowPaint.setStrokeCap(Cap.ROUND);
		
		shapeRectF = new RectF();
		drawRect = new Rect();
	}

	public void setMaxVal(float max){
		maxStat = max;
		invalidate();
	}
	public void setCurrentVal(float val){
		currentStat = val;
		invalidate();
	}
	
	boolean measured = false;
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		
		if(!measured){
			getDrawingRect(drawRect);
			float shape_offset = SHAPE_RING_THICKNESS/2+BLUR_RADIUS;
			shapeRectF.left = drawRect.left + shape_offset;
			shapeRectF.right = drawRect.right - shape_offset;
			shapeRectF.top = drawRect.top + shape_offset;
			shapeRectF.bottom = drawRect.bottom - shape_offset;
			
			measured = (drawRect.width()>0);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		//setLayerType(LAYER_TYPE_HARDWARE, shapeBackPaint);


		//shapePaint.setStrokeWidth(SHAPE_RING_THICKNESS);
		canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, currentSweep, false, glowPaint);
		canvas.drawArc(shapeRectF, SHAPE_START_DEGREE, currentSweep, false, shapePaint);

		//shapePaint.setStrokeWidth(1);
		//canvas.drawRect(shapeRectF, shapePaint);
		
		float ratio = Math.min(1.0f, currentStat/maxStat); 
		float newSweep = ratio*SHAPE_MAX_SWEEP;

		float speed = 40.0f*Math.abs(newSweep-currentSweep)/SHAPE_MAX_SWEEP;
		
		if(newSweep>currentSweep){
			currentSweep += speed;
			invalidate();
		} else {
			currentSweep -= speed;
			invalidate();
		}
	}

}
