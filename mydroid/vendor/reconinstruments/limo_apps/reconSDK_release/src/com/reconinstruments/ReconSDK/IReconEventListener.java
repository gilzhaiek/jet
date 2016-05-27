package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;

/** Recon Event Notifier Interface is similar to  
 * <a href="http://developer.android.com/reference/android/hardware/SensorEventListener.html">
 * Android SensorEventListener</a>. Since Recon Events are asynchronous by nature, the Client is required to implement
 * this interface in order to be notified of MOD Server Data Broadcasts. <p>
 * <p>
 * Clients can register for Notification callbacks using the services
 * of {@link ReconSDKManager#registerListener(IReconEventListener, int)} class. 
 */
public interface IReconEventListener
{
	/**
	 * Callback that will be invoked when registered Data Event has been received.
	 *
	 * @param event Recieved ReconEvent. Client code will typically cast it to desired type
	 * @param m     Java Reflection type - class Method that will return value of most interest
	 *
	 * <p>
	 * Example of receiving maximum temperature broadcast Event:
	   <pre>
	   <code>
	 * public void onDataChanged(ReconEvent event, Method m)
	 * {	
	 *    if ( (event.getType() == ReconEvent.TYPE_TEMPERATURE) &&
	 *         (m.getName().equals("GetMaxTemperature") == true) )
	 *    {
     *       Toast toast = Toast.makeText(this, 
     *             String.format("Maximum Temperature Broadcast: [%d Celsius]",
     *             (Integer)m.invoke(event) ), Toast.LENGTH_LONG);
     *       toast.show();
	 *    }
     * }
	   </code>
	   </pre>
	 * <p>
	 * @see ReconSDKManager#registerListener
	 */
    public abstract void onDataChanged(ReconEvent event, Method m);
}
