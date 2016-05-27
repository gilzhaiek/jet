package com.reconinstruments.agps;

/** Interface for async completion of GPS Events.
 * 
 * This is independant of client type (regular, or agps) or client package (service or UI)
 * There are 4 types of async events Recon GPS Framework is reporting:
 * 
 *    1) Gps Events: Unsolicited status messages arriving at random times, as condition of
 *       of firmware changes. For full enumeration of possible status codes see ReconAGPS.java
 *       
 *    2) Command Completion: Results of commands issued agains the firmware
 *    
 *    3) NMEA Data:  NMEA Sentences (reported as strings) emited by chip, regardless of lock state
 *    
 *    4) Location Report: Position Fix, when there is lock. 
 *    
 * In order to utilize Recon GPS Framework, client must implement this interface. 
 * Register with ReconAgps.java for events you are interested in provide corresponding interface
 * methods implementation; leave empty methods you are not interested in
 */
public interface IReconGpsEventListener
{
    public abstract void onCommandCompleted(int command, int result);
    public abstract void onStatusReceived(int status);
    public abstract void onNmeaData(String strNmea);
    public abstract void onPositionReport(ReconAGPS.LocationFix fix);
	
}
