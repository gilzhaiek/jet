package com.reconinstruments.jumpdetection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.RISensor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;



public class JumpActivity extends Activity
{
	 private static final String TAG = "HelloLIMO";
	 private SensorManager mSensorManager;
	 
	 //Timer jumpTimer = new Timer();
	 //TimerTask jumpTask;
	 
	 public static final String Jump_LOG_FILE_NAME = "/mnt/storage/jump.txt";//jump log
	 File f;
	 FileWriter os;
	 public static final boolean DEBUG_MODE = false;//true;
	 
	 
	 
	 private Sensor        mJumpSensor;
	 private Sensor        mSyncSensor;
	 private Date mjumpStart;
	 private Date mjumpEnd;
	 //1g=9.80665f
	 static final float jump_land_threshold_sq=311.6f;//(1.8g)^2
	 static final float jump_land_soft1_threshold_sq=96.17f;//(1g)^2
	 static final float jump_land_soft2_threshold_sq=34.62f;//(0.6g)^2
	 static final float k_factor=0.5f;
	 private float P_Prev=0;
	 private float Ptemp=0;//Ptemp store the highest point of jump track
	 
	 /*****
	 temp1 store the highest point of the track starting when 
	 freefall happens and ending at the point when the landing condition is detected
	 *****/
	 private float Ptemp1=0;
	 private float P0=0;
	 private float P1=0;
	 private int end_indicator=0;
	 private int iJumpIndicator = 0;
	 private float acc_sum=0;
	 private int soft_land_time=0;
	 
	 private final short[] Pressure_Delimiter={1000,1130,1300,1500,1730,2000,2300,2650,//above the Mount Everest
             3000,3350,3700,4100,4500,5000,5500,6000,
             6500,7100,7800,8500,9200,9700,10300,11000};//24 members

	 private final short[]  AltCoefficient_i={12256,10758,9329,8085,7001,6069,5360,4816,
             4371,4020,3702,3420,3158,2908,2699,2523,
             2359,2188,2033,1905,1802,1720,1638};//23 members
	 private final short[]  AltCoefficient_j={16212,15434,14541,13630,12722,11799,10910,9994,
             9171,8424,7737,7014,6346,5575,4865,4206,
             3590,2899,2151,1456,805,365,-139};  //23 members 0.1mbar
	 
	 private Context me = null;
	 
	 
	 
	 private TextView cntview;
	 private TextView tsview;
	 /*
	 private TextView accxView;
	 private TextView accyView;
	 private TextView acczView;
	 private TextView pressureView;
	 */
	 private TextView airView;
	 private TextView dropView;
	 private TextView heightView;
	 private TextView jumpView;
	 private int calculate_altitude(float pressure) 
	 {
		 int h;
		 int altitude_decimal_bits;
		 char i;
		 int p=(int)(pressure*100);
		 for(i=22;i>0;i--)//22 ,Pressure_Delimiter[i]=10300
		{
				h=(int)(Pressure_Delimiter[i]*10);
				if(p>=h)
					break;
				//if(i==0)
				//  break;//reach bottom
				//i--;
		}
		h= (int)(AltCoefficient_j[i]*10);
		altitude_decimal_bits=(p-(int)Pressure_Delimiter[i]*10)*(int)AltCoefficient_i[i];
		h =h -(altitude_decimal_bits>>11);
		
		return h;//0.1m
	 }
	 
	 private final SensorEventListener mJumpListener = new SensorEventListener() 
	 {
		 public void onAccuracyChanged(Sensor sensor, int accuracy)
		 {
		    // TODO Auto-generated method stub				
	     }
		 
		 public void onSensorChanged(SensorEvent event)
		 {
			 // li wants to reactivate sensor each time; simulate with
			 // unregister at start of processing & register at end (?)
			 iJumpIndicator=0;
			 mSensorManager.unregisterListener((SensorEventListener)this, mJumpSensor);
			 
			 // for whatever reason Google geeks propagate event time in [nanoseconds], but everyone else
			 // works with miliseconds, so convert back 
			 mjumpStart = new Date ( event.timestamp / 1000000 );
			 /*
			 // curious about time diff between jump detection in Kernel and Java trigger
			 Date now = new Date();
			 
			 long diff = now.getTime() - jumpStart.getTime();
			 String strLog = String.format("Number of miliseconds between Jump Detection in Kernel and current time in Java: [%d]", diff);
		        
		     Log.i(TAG, strLog);
			 */
			 //cntview.setText(Integer.toString(iJumpIndicator) ); 
			 tsview.setText(mjumpStart.toString() );

			 // register sync listener which will get disabled after timer expiry
			 mSensorManager.registerListener((SensorEventListener) 
					   m_SyncListener,	
		    		   mSyncSensor, 
		    		   0);
			 /*
			 // start synchronized timer. This is poor man's end of jump simulation for unit test purposes
			 // when timer expires, jump has "finished" and re-start detection again
			 jumpTask = new TimerTask()
			 {
			    public void run()
			    {
			    	 // deregister from synchronized sensor
			    	 mSensorManager.unregisterListener(m_SyncListener, mSyncSensor);
			    	 
			    	 // reactivate jump
					 mSensorManager.registerListener((SensorEventListener) mJumpListener,	
					    		   mJumpSensor, 0); 
			    }
			 };
			 

			 jumpTimer.schedule(jumpTask, (long)10000);
			 */
		 }
	 };
	 private final SensorEventListener m_SyncListener = new SensorEventListener()
	 {
			 public void onAccuracyChanged(Sensor sensor, int accuracy)
			 {
			    // TODO Auto-generated method stub				
		     }
			 
			 public void onSensorChanged(SensorEvent event)
			 {
				 String strLog;
				 
				 if(iJumpIndicator==2)
				 {
					 strLog = "discard old data\n";			        
				     Log.i(TAG, strLog);
					 return;
				 }
					 
				 // check pressure----------------------------------
				 if(iJumpIndicator==0)
				 {
					 /*
					 accxView.setText(Float.toString(event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_X]) );
					 accyView.setText(Float.toString(event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]) );
					 acczView.setText(Float.toString(event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Z]) );
					 pressureView.setText(Float.toString(event.values[RISensor.RI_SYNCHRONIZED_OFFSET_PRESSURE]));
					 */
					 P0=event.values[RISensor.RI_SYNCHRONIZED_OFFSET_PRESSURE];
					 Ptemp=P0;
		             Ptemp1=P0;
		             P_Prev=P0; 
		             iJumpIndicator=1;
		             //jump log	
		             if(DEBUG_MODE)
		             {	 
			             f= new File(Jump_LOG_FILE_NAME);
			             try {
							os = new FileWriter(f,true);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		             }
				 }
				 else
				 {
					//find the heightest point in the jump track
					//Pass low pass filter first 
					 
					 P_Prev=P_Prev*(1-k_factor)+k_factor*
							event.values[RISensor.RI_SYNCHRONIZED_OFFSET_PRESSURE];
					 					 
	                //update Ptemp1
					 if(P_Prev<Ptemp1)
		             {
		                    Ptemp1=P_Prev;
		                    //update Ptemp
		                    if(end_indicator==0)
		                        Ptemp=Ptemp1;
		              }          
				 }
				 strLog = String.format("P%f,%f\n",event.values[RISensor.RI_SYNCHRONIZED_OFFSET_PRESSURE],P_Prev);			        
			     Log.i(TAG, strLog);
			     if(DEBUG_MODE)
			     {
				     		
				     		try {
								os.write(event.values[RISensor.RI_SYNCHRONIZED_OFFSET_PRESSURE]+","
								+event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_X]+","
								+event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]+","
								+event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Z]+"\r\n");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

			     }
			     
				 //check acc---------------------------------
				 acc_sum=event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_X]*event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_X]
						 +event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]*event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]
						 +event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Z]*event.values[RISensor.RI_SYNCHRONIZED_OFFSET_ACCEL_Z];
				 if(acc_sum>=jump_land_threshold_sq)//1800mg,hard landing
				 {
					 if(end_indicator==0)
			                end_indicator=1;// possible landing point in the jump track
					 iJumpIndicator=2;
				 }
				 else if(acc_sum>=jump_land_soft1_threshold_sq)//1000mg, soft landing
				 {
					 soft_land_time+=25;//> 1g for 200ms (500/25=20points=200ms @100hz)
			            if(end_indicator==0)
			                end_indicator=1;// possible landing point in the jump track
				 }
				 else if(acc_sum>=jump_land_soft2_threshold_sq)//600mg
			     {
			            soft_land_time+=10;//   > 600mg for 500ms (500/10=50points=500ms @ 100hz)
			            if(end_indicator==0)
			                end_indicator=1;// possible landing point in the jump track
			     }
				 else
				 {
					 end_indicator=0;
			         soft_land_time=0; 
				 }

				 //check if landing founded-----------------
				 if(end_indicator==1)
			     {
			            end_indicator=2;
			            mjumpEnd=new Date();
			            P1=P_Prev;  
			     }
				 if(soft_land_time>=500)    
					 iJumpIndicator=2;//detect landing
				 
				 //jump landing detect end---------------
				 if(iJumpIndicator==2)
				 {
					//Check air time------------
					 long diff = mjumpEnd.getTime() - mjumpStart.getTime(); 
					 diff=diff+100;//roughly 100ms duration time before freefall interrupt
					 airView.setText(Long.toString(diff));
					 cntview.setText(mjumpEnd.toString() ); 
					 if(diff>500 && diff <9000)
					 {
						 
						//check drop,height
						 int h0,htemp,h1;
						 h0=calculate_altitude(P0);//0.1m
						 htemp=calculate_altitude(Ptemp);//0.1m
						 h1=calculate_altitude(P1);//0.1m
						 strLog = String.format("%d,%d,%d,%f,%f,%f\n", h0,htemp,h1,P0,Ptemp,P1);					    						 
					     Log.v(TAG, strLog);
					     
					     h1=htemp-h1;
					     h0=htemp-h0;
						 
						 if(DEBUG_MODE)
					     {
							 try {
								os.write("End"+diff+","+h1+","+h0+"\r\n");
								 
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							 try {
								os.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					     }
						 
						 
						 dropView.setText(Integer.toString(h1) );
						 heightView.setText(Integer.toString(h0) );						 
						 
						 
						 
						 jumpView.setText("TRUE"); 
					 }
					 else
					 {	 
						 jumpView.setText("FALSE"); 

						 try {
							os.write("No\r\n");
							 
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						 try {
							os.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					 }
					 
					 
					
					// tsview.setText(mjumpStart.toString() );
					//deregister from synchronized sensor
			    	 mSensorManager.unregisterListener(m_SyncListener, mSyncSensor);
			    	 
			    	 //reactivate jump			    
			         mSensorManager.registerListener((SensorEventListener) mJumpListener,	
			      		   mJumpSensor, 
			      		   0);
					 
				 }
				
			 }
	 
	 };
	 
	 
	 
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        me = this;
        
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        
        mJumpSensor = mSensorManager.getDefaultSensor(RISensor.RI_SENSOR_TYPE_JUMP);
        
        String strLog = String.format("Jump Sensor: Name [%s] Vendor [%s] Version [%d] Range [%f] Resolution [%f] Power [%f] Min Delay [%d]",
        		mJumpSensor.getName(), mJumpSensor.getVendor(), mJumpSensor.getVersion(), mJumpSensor.getMaximumRange(), 
        		mJumpSensor.getResolution(), mJumpSensor.getPower(), mJumpSensor.getMinDelay() );
        
        Log.i(TAG, strLog);
        
        mSyncSensor  = mSensorManager.getDefaultSensor(RISensor.RI_SENSOR_TYPE_SYNCHRONIZED);
        
        setContentView(R.layout.main);
        
        cntview = (TextView) findViewById(R.id.jcount);
        cntview.setTextColor(Color.BLUE);
        cntview.setText(Integer.toString(iJumpIndicator) );
        
        tsview = (TextView) findViewById(R.id.jtime);
        tsview.setTextColor(Color.RED);
        tsview.setText("");
        
        airView = (TextView) findViewById(R.id.air);
        airView.setText("0");
        
        dropView = (TextView) findViewById(R.id.drop);
        dropView.setText("0");
        
        heightView = (TextView) findViewById(R.id.height);
        heightView.setText("0");
        
        jumpView = (TextView) findViewById(R.id.jump);
        jumpView.setText("0");

    }
    
    @Override
   	protected void onResume()
    {
       super.onResume();  
       
       // register jump listener
       mSensorManager.registerListener((SensorEventListener) mJumpListener,	
    		   mJumpSensor, 
    		   SensorManager.SENSOR_DELAY_GAME);
       
       mSensorManager.unregisterListener(m_SyncListener, mSyncSensor);
       
    }

}