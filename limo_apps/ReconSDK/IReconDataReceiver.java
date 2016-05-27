package com.reconinstruments.ReconSDK;

import java.util.ArrayList;

/* Recon Data Receiver Interface 
 * 
 * Notifications for all data changes are not sent by the system; For instance Transcend service
 * Broadcasts only Max/Min Temperature, but not every temperature change for ReconEvent.TYPE_TEMPERATURE
 * 
 * SDK Client can request at any time full history for all published Recon Event Type
 * The fact that this data is managed / persisted by Transcend service is irrelevant; Client interfaces
 * only with ReconDataManager Proxy. 
 * 
 * Since data retrieval (service communication) is asynchronous by nature, callback must be registered
 * during API Invocation. ReconDataReceiver interface encapsulates this callback
 *
 *    
 */
public interface IReconDataReceiver 
{
	// callback for single data type
	public abstract void onReceiveCompleted
	(
			int status,                        // Request Status: 0 - OK, otherwise Error Code
			ReconDataResult result             // Data Fetch Result structure
    );
	
	// full update
	public abstract void onFullUpdateCompleted
	(
			int status,                         // Request Status: 0 - OK, otherwise Error Code
			ArrayList<ReconDataResult> results  // Array of Data Results; one entry per data type
    );
}
