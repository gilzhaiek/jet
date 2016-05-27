package com.reconinstruments.ReconSDK;

import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.os.Bundle;

/** Generic Data Buffer encapsulating the results of ReconEvent Data Receive Request. Internally
 *  composed as C-style structure (data members are public).
 * <p>
 * For the convenience of App Developers this class also supports serialization to/from
 * a <a href="http://developer.android.com/reference/android/os/Bundle.html">
 * Bundle</a> (main Android Transport Mechanism for passing Data between Activities).
 * This is useful when received Data Buffer must be passed between different components of
 *  the application without expensive round trip calls to the MOD Server */
public final class ReconDataResult
{
	private static final String COUNTER_KEY = "BUNDLE_DATA_COUNTER";
	private static final String TYPE_KEY    = "BUNDLE_DATA_TYPE";
	private static final String DATA_KEY    = "BUNDLE_DATA_VALUE";
	
	/** Data Item "serial" number, managed by MOD Server; not same as physical size of Items Container */
	public int itemCounter;     
	
	/** Data Container Result. In case of non-aggregate items (i.e. ReconTemperature)
	 *  contains only single entry.  <p>
	 *  Client Code will always cast to desired concrete Type, which can always be determined
	 *  by calling {@link ReconEvent#getType()}
	 */
    public  ArrayList<ReconEvent> arrItems;  
    
    /** Public Constructor */
    public ReconDataResult()
    {
    	itemCounter = 0;
    	arrItems = new ArrayList<ReconEvent> ();
    }
    
    /** Human readable Identification for Diagnostic Purposes */
    @Override
    public String toString()
    {
    	String strType = ReconEvent.TAG;
    	if (arrItems.size() > 0)
    	   strType = arrItems.get(0).toString();
    	
    	return strType;
    }
    
    /** Serialization to a Bundle
     * <p>
     *  @return Android Bundle that contains serialized ReconDataResult
     *  <p> For Instance if Viewer of received Data is implemented in different Activity:
     *  <pre>
     *  <code>
     *  Intent i = new Intent(ReceivingActivity.this, ProcessingActivity.class)
     *  i.putExtra("My Identification String", result.toBundle() );
     *  startActivity (i);
     *  </code>
     *  </pre>
     */
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
    
    /** Virtual Constructor - Serialization from a Bundle. If Bundle is invalid, throws an Exception<p>
     * @param  b Android Bundle, typically obtained with {@link ReconDataResult#toBundle()}
     * @return assembled ReconDataResult ("this") object.<p>
     * Typically receiving Activity will re-assemble ReconEvent Data Buffer from passed Bundle using 
     * this Method. For instance:
     * <pre>
     * <code>
     * Bundle bdata = getIntent().getBundleExtra("My Identification String");
	 * ReconDataResult result = ReconDataResult.fromBundle(bdata);
	 * .... now process data normally ...
     * </code>
     * </pre>
     * @exception
     */
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
