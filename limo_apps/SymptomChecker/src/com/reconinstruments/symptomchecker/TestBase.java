package com.reconinstruments.symptomchecker;

import android.app.Activity;
import android.content.Context;

public abstract class TestBase {

	private boolean mIsSelected = true;
	private boolean mIsInProgress = false;
	private boolean mTestResult = false;
	private String mTestComments = "";
	private int mTestPeriod = 5;
	private int mTimeoutPeriod = 10;
	private Thread mTestThread = null;
	
	private String mTestName = null;
	private Activity mParentActivity;
	
	public TestBase(String testName, Activity activity){
		mTestName = testName;
		mParentActivity = activity;
	}
		
	/**
	 * Get Test Name
	 * @return test Name
	 */
	public String GetTestName() {
		return mTestName;
	}
	
	/**
	 * Get Application Parent Activity
	 * @return application activity
	 */
	public Activity GetParentActivity(){
		return mParentActivity;
	}
	
	/**
	 * Get Application Context
	 * @return application context
	 */
	public Context GetContext(){
		return mParentActivity.getApplicationContext();
	}
	
	/**
	 * Set the test flag
	 * @param selected true to run test otherwise false to skip test
	 */
	public void SetIsSelectedFlag(boolean selected) {
		mIsSelected = selected;
	}
	
	public int GetTimeOutPeriod(){
		return mTimeoutPeriod;
	}
	
	public void SetTimeOutPeriod(int timeOutPeriod){
		mTimeoutPeriod = timeOutPeriod;
	}
	
	/**
	 * Get how long the test will last in second
	 * @return test period in second
	 */
	public int GetTestPeriod(){
		return mTestPeriod;
	}
	
	/**
	 * Set how long the test will last in second
	 * @param testPeriod period in second
	 */
	public void SetTestPeriod(int testPeriod){
		mTestPeriod = testPeriod;
	}

	/**
	 * If test was selected for testing
	 * @return true if selected for testing otherwise false
	 */
	public boolean IsSelected() {
		return mIsSelected;
	}

	/**
	 * If test is under progress
	 * @return true if test is still running otherwise false
	 */
	public boolean IsInProgress() {
		return mIsInProgress;
	}

	public void StartTest() {
		mTestResult = false;
		mIsInProgress = true;
		mTestThread = GetNewTestThread();
		mTestThread.start();
	}
	
	public Thread GetTestThread(){
		return mTestThread;
	}
	
	public abstract Thread GetNewTestThread();	
	
	public abstract void ForceStop();
	
	public void EndTest() {
		mIsInProgress = false;
	}

	/**
	 * Get the result of the test
	 * @return true if test passed otherwise false
	 */
	public boolean GetTestResult() {
		return mTestResult;
	}
	
	/**
	 * Set the result of the test
	 * @param result true if test passed otherwise false
	 */
	public void SetTestResult(boolean result){
		mTestResult = result;
	}
	
	public void SetTestComments(String comment) {
		if (mTestComments == null || mTestComments.isEmpty()){
			mTestComments = comment;
		}
		else {
			mTestComments += ", " + comment; 
		}
	}

	public String GetTestComments() {
		return mTestComments;
	}	
}