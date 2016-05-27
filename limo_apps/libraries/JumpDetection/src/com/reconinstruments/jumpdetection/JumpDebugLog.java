//package com.reconinstruments.applauncher.transcend;
package com.reconinstruments.jumpdetection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import android.os.Environment;

// this class is internal helper to log some stuff during
// jump air time processing;  key reason is to move code
// away from main JumpAnalyzer & make it a bit cleaner
public class JumpDebugLog
{
    private static final String Jump_LOG_FILE_NAME = "jump.txt";   // jump log
    
    private File f;
    private FileWriter os;
  
    void startLog (long jumpStartDate, float pressure, long lastUpdateTime)
    {
       Calendar now = Calendar.getInstance();
	   f = new File(Environment.getExternalStorageDirectory(), JumpDebugLog.Jump_LOG_FILE_NAME);
       try
       {
          os = new FileWriter (f, true);
        
          os.write(
        		jumpStartDate + "," +
                pressure + "," + lastUpdateTime + "," +
                now.get(Calendar.HOUR_OF_DAY)  + "," +
                now.get(Calendar.MINUTE)  + "," +
                now.get(Calendar.SECOND) + 
                ",B\r\n");
        
        //+k_factor+",minimumJumpAir:"+mMinimumJumpAir
        // +mJumpStartDate.getHours()+","
        // +mJumpStartDate.getMinutes()+","
        // +mJumpStartDate.getSeconds()
      } 
      catch (IOException e)
      {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
    }

    void logCheckpoint(long timestamp, float pressure, float [] accelerometer, float P_avg)
    {
    	try 
        {
    		os.write("Checkpoint. Timestamp: " + timestamp + ","
    	    + " Pressure: " + pressure + ","
    	    + " Accel X: "  + accelerometer[0] + ","
    	    + " Accel Y: "  + accelerometer[1] + ","
    	    + " Accel Z: "  + accelerometer[2] + ","
            + " P_avg: "    + P_avg + "," + "\r\n"); //","+P_sum+
        } 
    	catch (IOException e)
    	{
    		// TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    void logJump(ReconJump jump, float P0, float P1, float Ptemp, float Ptemp1)
    {
    	try 
    	{
    		os.write("Jump Detected. Drop: " + jump.mDrop + ","
            + "Height: "  + jump.mHeight + ","
            + "Air: "     + jump.mAir + ","
            + "mP0: "     + P0 + "," 
            + "mP1: "     + P1 + "," 
            + "mPTemp: "  + Ptemp + "," 
            + "mPtemp1: " + Ptemp1 + ",E\r\n");
        } 
    	catch (IOException e) 
    	{
           e.printStackTrace();
        }
    }
    void logJumpEndTime (long recheck, long time)
    {
    	try 
    	{
    		os.write("Recheck Time: " + recheck + " End Time: " + time + ",E\r\n");
        } 
    	catch (IOException e) 
    	{
           e.printStackTrace();
        }
    }
    
    void endLog()
    {
    	try 
    	{
            os.close();
        } 
    	catch (IOException e) 
    	{
           e.printStackTrace();
        }
    }
}
