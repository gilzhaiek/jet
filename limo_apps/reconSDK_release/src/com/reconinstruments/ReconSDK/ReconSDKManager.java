package com.reconinstruments.ReconSDK;

import android.content.Context;

/** SDK Provider of Recon Data Objects. Instance of this class serves as a MOD Service Proxy, but this
 *  fact is abstracted from the client. The standard minimalistic public interface is used to:
 * <p>
 * <ul>
 * <li> Register for system wide notifications of ReconEvents
 * <li> Retrieve full data history for any specific Event Type - see {@link ReconEvent} enumeration.
 * </ul>
 * <p>
 *  Internally the Singleton Pattern is used as back-end MOD Server is a system-wide Service.
 */
public class ReconSDKManager 
{
	 @SuppressWarnings("unused")
	 private static final String TAG = ReconSDKManager.class.getSimpleName(); 
	 
	 /** Request Status: OK (No error) */
 	 public static final int STATUS_OK          =  0;   
 	 
 	 /** Request Status: Error (Generic Failure) */
 	 public static final int GENERIC_FAILURE    = -1;    // generic failure
 	 
 	 /** Request Status: Error (Internal -- Data Format) */
 	 public static final int ERR_DATA_FORMAT    = -2;    // Client-Server Data Format Error
 	 
 	 /** Request Status: Error (Feature not supported) */
 	 public static final int ERR_NOT_SUPPORTED  = -3;    // Feature not supported
 	 
	 /* Singleton Pattern */
	 private ReconSDKManager(){}
	 private static ReconSDKManager mInstance = null;
	 
	 /**
	  * Class constructor: Singleton Pattern
	  * <p>
	  * @param c Context 
	  *  - typically <a href="http://developer.android.com/reference/android/app/Activity.html">
	  * Application Activity</a>
	  * @return   Singleton Instance of ReconSDKManager
	  * <p>
	  * Concept of lazy evaluation is used: Does not immediately connect with back-end MOD Service.
	  * Direct server link is required only for Data Retrieval and Client might be interested in
	  * broadcasts only. Once first "<i>fetch</i>" request is issued, Server Link will be established 
	  * and consequently kept alive during Object lifetime.
	  */
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

		 }
		 
		 return mInstance;
	 }
	 
    /** Subscriber for specific data change notification. Received broadcast
     *  invokes registered onDataChanged callback. 
     *  If registration for unsupported ReconEvent type is passed, throws an Exception <p>
     *   
     *  @param listener  Client implemented Callback Interface.
	 *  @param dataType  ReconEvent type notification is being registered for. 
     *  
     *  <p>
     *  For instance:
     *  <pre>
     *  <code>
	 *  ReconDataManager   rdm  = new ReconDataManager();
	 *  ReconEventListener list = new ReconEventListener()
	 *  {
	 *     public void onDataChanged () {....};
	 *  };
	 *
	 *  // register for temperature event broadcasts
	 *  rdm.registerListener (list, ReconEvent.TYPE_TEMPERATURE); 
	 * </code>
	 * </pre>
	 * <p>
	 * For convenience, a client app can register for multiple broadcasts by using bitmasks:
	 * <pre><code>
	 * rdm.registerListener (list, ReconEvent.TYPE_TEMPERATURE | ReconEvent.TYPE_ALTITUDE); 
	 * </code></pre>
	 * @see ReconEvent
	 * @see IReconEventListener
	 * @exception 
	 * 
     */
    public void registerListener (IReconEventListener listener, int dataType) throws InstantiationException
    {
       mEventManager.registerListener (mContext, listener, dataType);	
    }
 
    /**
     * Cancel specified Data Change Notification previously registered with <code>registerListener()</code>
     * <p>
     * @param dataType  ReconEvent type notification that subscription to Event Broadcasts has been cancelled. See {@link ReconEvent}
     * <p>
     * Event bitmasks are supported the same as registration. This method always succeeds;
     * Any mismatch (i.e. cancelling subscription that has not been registered) is ignored.
     */
    public void unregisterListener (int dataType)
    {
       mEventManager.unregisterListener (mContext, dataType);
    }
    
    /** Data Retrieval of any supported Recon Event. <p>
     * @param completion Client implemented Callback Interface. 
     * @param dataType   ReconEvent Data Type
     * <p>
     *  Since communication with MOD Server is asynchronous, completion callback must be provided.
     *   For instance:
     *   <pre>
     *   <code>
     *   ReconSDKManager rdm    = ReconSDKManager.Initialize(this);
     *   IReconDataReceiver cbk = new MyDataReceiver();
     *   rdm.receiveData (cbk, ReconEvent.TYPE_ADVANCED_JUMP);    
     *   </code>
     *   </pre>
     *   To Retrieve Full Update, simply call with ReconEvent.TYPE_FULL:
     *   <pre><code>
     *    rdm.receiveData (cbk, ReconEvent.TYPE_FULL);
     *    </code></pre>
     *    Note that Data Type Bitmask is not supported, i.e
     *    <pre><code>
     *    rdm.receiveData (cbk, ReconEvent.TYPE_TEMPERATURE | ReconEvent.TYPE_ALTITUDE);
     *    </pre></code>
     *    would result with {@link IReconDataReceiver#onReceiveCompleted(int, ReconDataResult)}
     *    with result set to ERR_DATA_FORMAT.
     *   @see IReconDataReceiver
     */
    public void receiveData (IReconDataReceiver completion, int dataType)
    {
    	mDataManager.QueueRequest(mContext, completion, dataType);
    }
 
    /** Convenience method for Full Data Update 
     * <p>
     * @param completion Client implemented Callback Interface. See {@link IReconDataReceiver} 
     * */
    public void receiveFullUpdate (IReconDataReceiver completion)
    {
    	mDataManager.QueueRequest(mContext,  completion, ReconEvent.TYPE_FULL);
    }
   
    /* Private Implementation */
	private EventManager mEventManager;    // Broadcast dispatcher
	private DataManager  mDataManager;     // IPC Processing
	
	private Context      mContext;         // Client context
  
}
