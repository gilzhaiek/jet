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
import com.reconinstruments.commonwidgets.GradientGaugeView;

public class DialView extends GradientGaugeView {
    public static final String TAG = "DialView";

    public static final int SHAPE_START_DEGREE			= 135;
    public static final int SHAPE_MAX_SWEEP				= 270;
    public DialView(Context context) {
	super(context);
    }
    public DialView(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
    }
    public DialView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
	//setLayerType(View.LAYER_TYPE_HARDWARE, shapeBackPaint);


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
