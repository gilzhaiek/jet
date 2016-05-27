package com.contour.connect.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.contour.connect.R;

public class ContourToast {
    private static int sYOffset = 0;
    
    @SuppressLint("NewApi")
    public static void showText (Activity context, CharSequence text, int duration) {
        
        LayoutInflater inflater = context.getLayoutInflater();
        ViewGroup root = (ViewGroup) context.findViewById(R.id.toast_layout_root);
        View layout = inflater.inflate(R.layout.contour_toast,root);

        TextView tv = (TextView) layout.findViewById(R.id.text);
        tv.setText(text);
        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(duration);
        toast.setView(layout);
        toast.setGravity(Gravity.CENTER, 0, sYOffset);
        toast.show();    
        
        int heightPixels = context.getResources().getDisplayMetrics().heightPixels; 
        int toastHeight = heightPixels/6;

        if(sYOffset >= (heightPixels/2 - toastHeight))
            sYOffset = -(heightPixels/2)+toastHeight; 
        else
            sYOffset += toastHeight;
    }
    
    public static void showText (Activity context, int textId, int duration) {
        ContourToast.showText(context, context.getResources().getString(textId), duration);   
    }
}
