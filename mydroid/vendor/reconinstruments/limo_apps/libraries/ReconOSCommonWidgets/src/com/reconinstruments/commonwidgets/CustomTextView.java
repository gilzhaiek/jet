package com.reconinstruments.commonwidgets;
import android.widget.TextView;
import android.util.AttributeSet;
import android.util.Log;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.content.Context;
import android.graphics.Typeface;


public class CustomTextView extends TextView
{

    private static String TAG = "CustomTextView";

    private ColorStateList mShadowColors;
    private float mShadowDx;
    private float mShadowDy;
    private float mShadowRadius;
    
    String regularFontPath = "fonts/SourceSansPro-Regular.otf";
    String semiBoldFontPath = "fonts/SourceSansPro-Semibold.otf";
    String boldFontPath = "fonts/SourceSansPro-Bold.otf";

    public CustomTextView(Context context)
    {
        super(context);
        setViewTypeface(context , "regular");
    }


    public CustomTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    	init(context, attrs);
//   	setViewTypeface(context , "regular");
    }


    public CustomTextView(Context context, AttributeSet attrs, int defStyle)
    {
       super(context, attrs, defStyle);

       init(context, attrs);
 //      setViewTypeface(context , "regular");

    }


    /**
     * Initialization process
     * 
     * @param context
     * @param attrs
     * @param defStyle
     */
    private void init(Context context, AttributeSet attrs)
    {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);

        final int attributeCount = a.getIndexCount();
        for (int i = 0; i < attributeCount; i++) {
            int curAttr = a.getIndex(i);

            if (curAttr == R.styleable.CustomTextView_shadowColors) {
				mShadowColors = a.getColorStateList(curAttr);
			} else if (curAttr == R.styleable.CustomTextView_android_shadowDx) {
				mShadowDx = a.getFloat(curAttr, 0);
			} else if (curAttr == R.styleable.CustomTextView_android_shadowDy) {
				mShadowDy = a.getFloat(curAttr, 0);
			} else if (curAttr == R.styleable.CustomTextView_android_shadowRadius) {
				mShadowRadius = a.getFloat(curAttr, 0);
			} else {
			}
    	for (int j = 0; j < attrs.getAttributeCount(); j++) {
    		if("textStyle".equals(attrs.getAttributeName(j))){
    			setViewTypeface(context, attrs.getAttributeValue(j));
    			break;
    		}
    	}
   }

        a.recycle();

        updateShadowColor();
    }

    private void updateShadowColor()
    {

        if (mShadowColors != null) {
            setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, mShadowColors.getColorForState(getDrawableState(), 0));
            invalidate();
        }
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();
        updateShadowColor();
    }
    

    public void setViewTypeface(Context c, String type){
    	Typeface tf = null;
    	if(type.equals("0x1"))//bold
    		tf = Typeface.createFromAsset(c.getAssets(), boldFontPath);
    	else if(type.equals("semi-bold")){
    		tf = Typeface.createFromAsset(c.getAssets(), semiBoldFontPath);
    	}
    	else
    		tf = Typeface.createFromAsset(c.getAssets(), regularFontPath);
		this.setTypeface(tf);
    }

    
}