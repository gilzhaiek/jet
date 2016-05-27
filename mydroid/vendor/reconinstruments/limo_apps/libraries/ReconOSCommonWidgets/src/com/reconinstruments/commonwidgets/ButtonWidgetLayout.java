package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.reconinstruments.utils.DeviceUtils;

/**
 * <code>ButtonWidgetLayout</code> sets the default layout for buttons.
 * Requires buttonText in xml to be set and has capability for custom icons.
 */

public class ButtonWidgetLayout extends LinearLayout {

    public ButtonWidgetLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray resArray = context.obtainStyledAttributes(attrs,
                R.styleable.ButtonWidgetStyle, 0, 0);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.button_widget, this, true);

        ImageView buttonImage = (ImageView) getChildAt(0);
        TextView buttonText = (TextView) getChildAt(1);

        String mButtonText = resArray.getString(R.styleable.ButtonWidgetStyle_buttonText);
        buttonText.setText(mButtonText);

        int mButtonImage = resArray.getResourceId(R.styleable.ButtonWidgetStyle_buttonImage, -1);

        //check if they have set their own image
        if(mButtonImage != -1){
            buttonImage.setImageResource(mButtonImage);
        }else{
            if(mButtonText.equals("SELECT") || mButtonText.equals("OK")){
                buttonImage.setImageResource((DeviceUtils.isSun()) ? R.drawable.jet_select : R.drawable.select);
            }else if(mButtonText.equals("BACK")){
                buttonImage.setImageResource(R.drawable.back);
            }else if(mButtonText.equals("DOWN")){
                buttonImage.setImageResource(R.drawable.down_arrow);
            }
        }

        resArray.recycle();
    }

}
