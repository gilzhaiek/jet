package com.reconinstruments.ReconSDK;

import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.os.Bundle;

/* Simple C-style structure encapsulating the Result of Remote Data Receive operation */
public final class ReconDataResult
{
	private static final String COUNTER_KEY = "BUNDLE_DATA_COUNTER";
	private static final String TYPE_KEY    = "BUNDLE_DATA_TYPE";
	private static final String DATA_KEY    = "BUNDLE_DATA_VALUE";
	
	public int itemCounter;                   // Data Item "serial" number; not same as physical container size
    public  ArrayList<ReconEvent> arrItems;   // result Data Container. In case of non-aggregate items 
                                              // (i.e. temperature) contains only 1 item.
    
    public ReconDataResult()
    {
    	itemCounter = 0;
    	arrItems = new ArrayList<ReconEvent> ();
    }
    
    @Override
    public String toString()
    {
    	String strType = ReconEvent.TAG;
    	if (arrItems.size() > 0)
    	   strType = arrItems.get(0).toString();
    	
    	return strType;
    }
    
    // Serialization to/from Bundle -- main Android transport mechanism for passing data between activities
    public Bundle toBundle()
    {
    	Bundle b = new Bundle();
    	
    	// put item counter
    	b.putInt(ReconDataResult.COUNTER_KEY, itemCounter);
    	
    	// put type and data itmes
    	if (arrItems.size() > 0)
    	{
    		b.putInt(ReconDataResult.TYPE_KEY, arrItems.get(0).getType() );
    	
	    	// put ReconEvents
	    	ArrayList<Bundle> al = new ArrayList<Bundle>();
	    	for (int i = 0; i < arrItems.size(); i++)
	    	{
	    	    al.add(arrItems.get(i).generateBundle() );
	    	}
	
	    	b.putParcelableArrayList(ReconDataResult.DATA_KEY, al);
    	}
    	
    	return b;
    }
    
    public static ReconDataResult fromBundle (Bundle b) throws InstantiationException, DataFormatException
    {
    	ReconDataResult result = new ReconDataResult();
    	
    	// get item counter
    	result.itemCounter = b.getInt(ReconDataResult.COUNTER_KEY);
    	
    	// get ReconEvents
    	ArrayList<Bundle> al = b.getParcelableArrayList(ReconDataResult.DATA_KEY);
    	if (al.size() > 0)
    	{
    		// get type first
    		int typeID = b.getInt(ReconDataResult.TYPE_KEY);
	    	for (int i = 0; i < al.size(); i++)
	    	{
	    		ReconEvent evt = ReconEvent.Factory(typeID );
	    		evt.fromBundle(al.get(i) );
	    		
	    		result.arrItems.add(evt);
	    	}
    	}
    	
    	return result;
    }
}
