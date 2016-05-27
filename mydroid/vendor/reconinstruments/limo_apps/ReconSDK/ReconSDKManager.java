package com.reconinstruments.ReconSDK;

import android.content.Context;


/* SDK Provider of Recon Data Objects. Instance of this class is Transcend Service Proxy, but this
 * fact is hidden from the client. Instead standard minimalistic public interface is used to:
 * 
 * 1) Register for system wide notifications (Recon Events)
 * 2) Retrieve full data history for any specific Type (ReconEvent.TYPE_XXX enumeration)
 * 
 * Internally we use Singleton Pattern -- app needs only 1 instance during its lifetime. In addition
 * "Initialize" constructor allows us perform any other initialization that can fail
 */
public class ReconSDKManager 
{
	 @SuppressWarnings("unused")
	 private static final String TAG = ReconSDKManager.class.getSimpleName(); 
	 
	 // status codes. TODO: Fill with full array; pull into separate class
 	 public static final int STATUS_OK          =  0;    // status ok
 	 public static final int GENERIC_FAILURE    = -1;    // generic failure
 	 public static final int ERR_DATA_FORMAT    = -2;    // Client-Server Data Format Error
 	 public static final int ERR_NOT_SUPPORTED  = -3;    // Feature not supported
 	 
	 /* Singleton Pattern */
	 private ReconSDKManager(){}
	 private static ReconSDKManager mInstance = null;
	 
	 public  static ReconSDKManager Initialize (Context c)
	 {
		 if (ReconSDKManager.mInstance == null)
		 {
			 // allocate us
			 ReconSDKManager.mInstance = new ReconSDKManager();
			 
			 // remember context
			 ReconSDKManager.mInstance.mContext      = c;
			 
			 // instatiate event and data managers
			 ReconSDKManager.mInstance.mEventManager = new EventManager();
			 ReconSDKManager.mInstance.mDataManager  = new DataManager();
			 
			 // do not connect Transcend service, because client might be interested
			 // in in broadcasts only. Service connection will happen on first data request

		 }
		 
		 return mInstance;
	 }
	 
    /* Subscriber for specific data change notification. Received broadcast
     *  (internal transcend implementation) calls registered onDataChanged callback 
	  
	  For instance:
	  ReconDataManager rdm = new ReconDataManager();
	  ReconEventListener list = new ReconEventListener()
	  {
	      public void onDataChanged () {....};
	  };
	  
	  rdm.registerListener (list, ReconEvent.TYPE_JUMP | ReconEvent.TYPE_TEMPERATURE);   // new jump and temperature notifications
    */
    public void registerListener (IReconEventListener listener, int dataType) throws InstantiationException
    {
       mEventManager.registerListener (mContext, listener, dataType);	
    }
 
    public void unregisterListener (int dataType)
    {
       mEventManager.unregisterListener (mContext, dataType);
    }
    
    /* Retrieve full history for specific ReconEvent. Completes asynchronously, so callback must be registered
 
    For instance:
	     ReconDataManager rdm = new ReconDataManager ();
		 rdm.receiveData (this, ReconEvent.TYPE_ADVANCED_JUMP);    // calling object implements ReconDataReceiver interface
		 
		 To Retrieve Full Update, simply call with ReconEvent.TYPE_FULL, i.e.
		 rdm.receiveData (this, ReconEvent.TYPE_FULL);
		 
    */
    public void receiveData (IReconDataReceiver completion, int dataType)
    {
    	mDataManager.QueueRequest(mContext, completion, dataType);
    }
 
    /* Convenience method for Full Data Update */
    public void receiveFullUpdate (IReconDataReceiver completion)
    {
    	mDataManager.QueueRequest(mContext,  completion, ReconEvent.TYPE_FULL);
    }
   
    /* Private Implementation */
	private EventManager mEventManager;    // Broadcast dispatcher
	private DataManager  mDataManager;     // IPC Processing
	
	private Context      mContext;         // Client context
  
}
