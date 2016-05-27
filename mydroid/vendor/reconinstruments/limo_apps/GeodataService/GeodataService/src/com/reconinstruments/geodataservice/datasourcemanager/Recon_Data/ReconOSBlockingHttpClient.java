package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.database.MergeCursor;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.webapi.IReconHttpCallback;
import com.reconinstruments.webapi.IReconHttpCallback.ERROR_TYPE;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;
import com.reconinstruments.webapi.ReconOSHttpClient;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.ThreadAgent;


public class ReconOSBlockingHttpClient extends ReconOSHttpClient {	// a blocking wrapper on ReconOSHTTPClient
	private final static String TAG = "ReconOSBlockingHTTPClient";
	public static ERROR_TYPE mRequestError = ERROR_TYPE.UNKNOWN;
    static TreeMap<Integer, ThreadAgent> mSuspendedThreads = new  TreeMap<Integer, ThreadAgent>();
    
    public ReconOSBlockingHttpClient(Context context) {
    	super(context, mWebRequestCallback);
    }
    
    public ReconHttpResponse sendBlockingRequest(ReconHttpRequest request) {
    	ThreadAgent threadAgent = new ThreadAgent();
    	
    	int requestId = super.sendRequest(request);
	    if (requestId == -1 ) {  // error case
		    return null;
		}
	    
		mSuspendedThreads.put(requestId, threadAgent);
		
        synchronized (threadAgent) {
            try { 
            	threadAgent.wait();		// wait until request has completed, see continueThread() in mWebRequestCallback definition
            } 
            catch (InterruptedException e) { 
            	// fail request
            }
		}
        return threadAgent.mRequestResponse;
    }

    public ERROR_TYPE GetErrorType() {
    	return mRequestError;
    }
    private static IReconHttpCallback mWebRequestCallback = new IReconHttpCallback() {
	    
    	@Override
	    public void onReceive(int requestId, ReconHttpResponse response) {
//			Log.e(TAG, "Request succeeded" );	// note, HTTP 404 and the like are still considered successes
			continueThread(requestId, response);
    	}

	    // something went wrong with request... possibly timed out
	    @Override
	    public void onError(int requestId, ERROR_TYPE type, String message) {
//			Log.e(TAG, "Request failed" );
			continueThread(requestId, null);
			mRequestError = type;
	    }

	    private void continueThread(int requestId, ReconHttpResponse response) {
	    	ThreadAgent threadAgent = mSuspendedThreads.get(requestId);
		
			if(threadAgent != null) {
				mSuspendedThreads.remove(requestId);
			    synchronized (threadAgent) {
			        try { 
			        	threadAgent.mRequestResponse = response;	// pass back response to thread through agent
			        	threadAgent.notify();	// restart thread
			        } 
			        catch (Exception e) { 
			        	// fail request
			        }
				}
			}
		}

    };
	
	
}
