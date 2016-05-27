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

public class HorizontalGradientGaugeView extends GradientGaugeView {
    public static final String TAG = "DialView";

    public static final int SHAPE_START_DEGREE			= 135;
    public static final int SHAPE_MAX_SWEEP				= 270;
    public HorizontalGradientGaugeView(Context context) {
	super(context);
    }
    public HorizontalGradientGaugeView(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
    }
    public HorizontalGradientGaugeView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
	float x0, x1, y0,y1;
	Rect r = canvas.getClipBounds();
	x0 = r.left;
	x1 = r.right;
	y0 = (r.top + r.bottom)/2;
	y1 = y0;
	float ratio = Math.min(1.0f, currentStat/maxStat); 
	canvas.drawLine(x0,y0,(x1-x0)*ratio,y1,glowPaint);
	canvas.drawLine(x0,y0,(x1-x0)*ratio,y1,shapePaint);
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
