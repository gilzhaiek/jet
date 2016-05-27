package com.reconinstruments.symptomchecker.Tools;

public class TimeOut {
	
	private long mStartTime;
	private int mTestPeriod;
	
	/**
	 * @param testPeriod set test period in second
	 */
	public TimeOut(int testPeriod){
		mTestPeriod = testPeriod;
	}
		
	/**
	 * Set the start time
	 */
	public void Start(){
		mStartTime = System.currentTimeMillis();
	}
	
	/**
	 * Check if test exceed test period
	 * @return true if exceed test period else false
	 */
	public boolean IsTimeOut(){
		return ((System.currentTimeMillis() - mStartTime)/ (mTestPeriod * 1000)) > 1;
	}

}
