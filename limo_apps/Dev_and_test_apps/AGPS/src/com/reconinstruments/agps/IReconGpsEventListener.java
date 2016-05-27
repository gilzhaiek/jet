package com.reconinstruments.agps;

/* Interface for async completion of GPS Events.
 * 
 * This is independant of client type (regular, or agps) or client package (service or UI)
 * There are 2 types of async events Recon GPS Framework is reporting:
 * 
 *    1) Gps Events: Unsolicited status messages arriving at random times, as condition of
 *       of firmware changes. For full enumeration of possible status codes see ReconAGPS.java
 *       
 *    2) Command Completion: Results of commands issued agains the firmware
 *    
 *  NOTE that NMEA sentences and Location Fix are reported usual Android way
 *    
 * In order to utilize Recon GPS Framework, client must implement this interface. 
 * Register with ReconAgps.java for events you are interested in provide corresponding interface
 * methods implementation; leave empty methods you are not interested in
 */
public interface IReconGpsEventListener
{
    public abstract void onCommandCompleted(int command, int result);
    public abstract void onStatusReceived(int status, int extra);
	
}
