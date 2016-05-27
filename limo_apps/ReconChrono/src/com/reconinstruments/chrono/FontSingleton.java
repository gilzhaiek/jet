package com.reconinstruments.chrono;

import android.content.Context;
import android.graphics.Typeface;

public class FontSingleton  {
	
	private static Typeface tf = null;
	
	private static FontSingleton INSTANCE = new FontSingleton();
	 
    // Private constructor prevents instantiation from other classes
    private FontSingleton() 
    {
    	tf = null;
    }
    
    private FontSingleton(Context c) 
    {
    	tf = Typeface.createFromAsset(c.getResources().getAssets(), "fonts/Eurostib_1.TTF");
    }
 
    public static FontSingleton getInstance(Context c) 
    {
    	if (tf == null)
    		INSTANCE = new FontSingleton(c);
        
        return INSTANCE;
    }
    
    public Typeface getTypeface() {
    	return tf;
    }
}
