package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;

/* Recon Event Notifier Interface -- similar to android.hardware.SensorEventListener.
 * 
 * ReconEvents are asynchronous by nature. In order to be notified SDK clients are required
 * to register callbacks, by implementing this interface
 * 
 * Transcend service publishes System-wide event change (such as new Jump, new maximum Temperature, etc.)
 * via standard Android broadcast mechanism. It is not necessary to expose this
 * transport mechanism to the clients. Instead they simply register callback for events of particular
 * type of interest, without need of knowledge how the notification will be propagated to them
 * 
 * Second argument is Java Reflection type -- class Method that will return value of most interest
 * (i.e. Maximum Temperature, etc)
 *    
 */
public interface IReconEventListener
{
    public abstract void onDataChanged(ReconEvent event, Method m);
}
