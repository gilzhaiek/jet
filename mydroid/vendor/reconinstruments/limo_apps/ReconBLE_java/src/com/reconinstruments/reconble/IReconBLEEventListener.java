package com.reconinstruments.reconble;
import android.os.Bundle;
/* Recon BLE is asynchronous by nature. Client (Java APK) implements these
 * callbacks (interfaces) in order to be notified of events */
public interface IReconBLEEventListener
{
	 // Connection (Pairing Status) changes. Client is interested in:
	 //  -- ReconBLE.BLEStatus.STATUS_CONNECTED    : Master-Slave Pair established
	 //  -- ReconBLE.BLEStatus.STATUS_DISCONNECTED : Master-Slave Pair broken
	 //  -- ReconBLE.BLEStatus.STATUS_REMOTE_LIST  : Remote List Ready. Retrieve, then Bond with desired MAC address
	 public abstract void onPairChanged(int status);
	 
	 // Asynchronous Outgoing (Tx) Transaction status updates.
	 public abstract void onSendUpdate    (int bytes);          // "tick" update -- Total # of bytes sent so far passed
    public abstract void onSendCompleted (int status, Bundle info);         // status of completed transaction (ERR_SUCCESS or FAIL)
	                                                            // Tx Thread automatically terminates
	 
	 // Asynchronous Incoming (Rx) Transaction status updates
	 public abstract void onReceiveUpdate    (int bytes);       // "tick" update --total # of bytes for this transaction
    public abstract void onReceiveCompleted (int status, Bundle info);      // Status of completed incoming transaction (ERR_SUCCESS or fail)
	                                                            // Rx Thread does NOT automatically terminate
    public abstract void onXmlReceived (String xml);
    public abstract void onMiscData (byte [] miscData);
}
