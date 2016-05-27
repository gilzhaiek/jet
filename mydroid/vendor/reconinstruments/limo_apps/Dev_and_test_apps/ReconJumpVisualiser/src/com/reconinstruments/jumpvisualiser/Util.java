package com.reconinstruments.jumpvisualiser;

import java.io.File;
import java.io.FileOutputStream;

import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.dto.message.JumpDataMessage;
import com.reconinstruments.modlivemobile.dto.message.JumpDataMessage.JumpData;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

public class Util
{
	// Pathname & filename used for the saved screen-shot
	public final static String PATH = "/ReconApps/";
	public final static String NAME = "screen_shot.jpeg";
	
	// Saves a bitmap object as a jpeg to the file system
	public static File saveBitmapToFile(Bitmap bitmap)
    {
        File path = Environment.getExternalStorageDirectory();
        File file = new File(path, PATH + NAME);
        try
        {
        	file.createNewFile();
			FileOutputStream ostream = new FileOutputStream(file);
			bitmap.compress(CompressFormat.PNG, 100, ostream);
			ostream.close();
			Log.i("UTIL", "Screen-Shot.jpeg written successfully.");
			
			return file;
        }
        catch (Exception e){ e.printStackTrace(); }
        return null;  
    }
	
	// Saves a snapshot of the displayed jump to the file system and 
	// requests a file transfer of the file from ReconHQ to be posted to facebook.
	public static void uploadJumpImageToFB(Jump jump, Context context)
	{
		try
		{
			String xml = JumpDataMessage.compose(new JumpData(PATH + NAME, jump.mAir, jump.mDistance, jump.mHeight, jump.mDrop));
			
			// Broadcast Intent Message
			BTCommon.broadcastMessage(context, xml);
		}
		catch(Exception e){ e.printStackTrace(); }
	}

	// -- Convenience functions -- //
	
	// Draw text onto canvas, centered at specified [x,y] coordinates
	public static void drawCenteredText(String text, float x, float y, Canvas canvas, Paint p)
	{
		canvas.drawText(text, x, y + p.getTextSize()/2, p);
	}
	
	// Return float rounded to 1 decimal place
	public static float roundTo1D(float n)
	{
		return Math.round(n*10.0f)/10.0f;
	}
	
	// -- Physics functions -- //
	
	public static float getTime(float accel, float dist)
	{
		return getVelocity(accel, dist) / accel; 
	}

	public static float getVelocity(float accel, float dist)
	{
		return (float) Math.sqrt(2 * accel * dist);
	}
	
	public static float getDistance(float accel, float time)
	{
		return (accel * time * time)/2.0f;
	}
}
