package com.reconinstruments.ReconSDK;

import java.util.ArrayList;

/** Client Implemented Recon Data Receiver Callback <p>
 * 
 * Notifications for all data changes are not sent by the system; For instance MOD Service
 * Broadcasts only Max/Min Temperature, but not every temperature change for ReconEvent.TYPE_TEMPERATURE Data Type <p>
 * 
 * SDK Clients can at any time request the full history for any supported Recon Event Data Type.
 * Since data retrieval (service communication) is asynchronous by nature, the callback must be registered
 * during API Invocation. IReconDataReceiver interface encapsulates this callback
 *
 */
public interface IReconDataReceiver 
{
	/** Callback for Full Update of all supported ReconEvent Data Types
	 *  <p>
	 *  @param status  Request Status: {@link ReconSDKManager#STATUS_OK} on success, error code on failure
	 *  @param results Received Data Buffer filled on success. Each array slot contains Data Buffer entry
	 *  for unique ReconEvent Data Type
	 *  <p>
	 *  @see ReconDataResult
	 */
	public abstract void onFullUpdateCompleted
	(
			int status,                         // Request Status: 0 - OK, otherwise Error Code
			ArrayList<ReconDataResult> results  // Array of Data Results; one entry per data type
    );
	
	/** Callback for single ReconEvent Data Type
	 *  <p>
	 *  @param status Request Status: {@link ReconSDKManager#STATUS_OK} on success, error code on failure
	 *  @param result Received Buffer filled on success
	 *  <p>
	 *  Example of ReconTemperature Data Receiver:
	 *  <pre>
	 *  <code>
	 *  public void onReceiveCompleted(int status, ReconDataResult result)
	 *  {
 	 *     if (status != ReconSDKManager.STATUS_OK) { .... handle error ... }
 	 *     
  	 *     ReconTemperature temp = (ReconTemperature)result.arrItems.get(0);
     *     String strText = String.format("Received Temperature: %d", temp.GetTemperature() );
     *     Toast toast = Toast.makeText(this, strText, Toast.LENGTH_LONG);
     *     toast.show();
     *  }
	 *  </code>
	 *  </pre>
	 *  @see ReconDataResult
	 */
	public abstract void onReceiveCompleted
	(
			int status,                        // Request Status: 0 - OK, otherwise Error Code
			ReconDataResult result             // Data Fetch Result structure
    );
	

}
