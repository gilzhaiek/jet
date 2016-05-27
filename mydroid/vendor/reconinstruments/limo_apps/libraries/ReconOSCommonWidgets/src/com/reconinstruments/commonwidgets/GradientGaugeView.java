package com.reconinstruments.commonwidgets;

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
public class GradientGaugeView extends View {
    /** Decrease this to increase the thickness */
    public static float SHAPE_RING_THICKNESS		= 16.0f;
    public static float BLUR_RADIUS		= 10.0f;

    protected float currentStat;
    protected float maxStat;
	

    protected Paint shapePaint = null;
    protected Paint glowPaint = null;
	
    protected float currentSweep = 0;

    protected Rect drawRect = null;
    protected RectF shapeRectF = null;

    public GradientGaugeView(Context context) {
	super(context);
	initShape();
    }

    public GradientGaugeView(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	initShape();
    }
    public GradientGaugeView(Context context, AttributeSet attrs) {
	super(context, attrs);
		
	initShape();
    }
    protected void initShape(){
		
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

}