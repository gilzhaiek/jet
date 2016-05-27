package com.contour.connect.debug;

import android.util.Log;

public class CLog
{    
    public static final boolean D = true;

    public static final String TAG = "CONTOUR";

    public static void out(Object... objs)
    {       
         Log.d(TAG, CLog.msg(objs));
    }

    
    public static void e(Exception e,Object... objs)
    {        
        Log.e(TAG, CLog.msg(objs), e);
    }
    
    private static String msg(Object... objs)
    {       
        StringBuilder sb = new StringBuilder();
        sb.append(Thread.currentThread().getName()).append(" :: ");
        int n = objs.length;
        for(int i = 0; i < n; i++)
        {
            sb.append(objs[i]);
            if(i < n-1) sb.append(',').append(' ');
        }   
   
         return sb.toString();
    }
}
