package com.reconinstruments.widgets;

import android.content.Context;
import android.graphics.Typeface;

public class Util
{
	static  Typeface MENU_TYPE_FONT = null;
	
	static public Typeface getMenuFont( Context context )
	{
		if( MENU_TYPE_FONT == null )
		{			
			MENU_TYPE_FONT = Typeface.createFromAsset(context.getAssets(), "fonts/Eurostib.ttf" );
		}
		
		return MENU_TYPE_FONT;
	}

}