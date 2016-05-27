package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;

/* Concrete Implementation of ReconEvent.TYPE_ADVANCED_JUMP Data Type */
public class ReconAdvancedJump extends ReconJump
{
	// constants 
	@SuppressWarnings("unused")
	private static final String TAG = ReconAdvancedJump.class.getSimpleName();
	
	// public c-tor
    public ReconAdvancedJump() 
    {
    //	mType = ReconEvent.TYPE_ADVANCED_JUMP;
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_ADVJUMP_ACTION_STRING;
    }
    
    // public data accessors
    
    // identification
    @Override
    public String toString()
    {
    	return ReconAdvancedJump.TAG;
    }
    
    // IPC Serialization
	@Override
	protected Bundle generateBundle() 
	{
		// call base class to insert its stuff first
		Bundle b = super.generateBundle();
		
		// now insert our own additions
		
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		// call base class first
		Method m = super.changedField(strName);
		
		// how our own stuff if still not set
		if (m == null)
		{
		}
		
		return m;
	}
	
	@Override
	protected void fromBundle(Bundle b) throws DataFormatException
	{
        // call base class to extract its stuff rist
		super.fromBundle(b);
		
		// now add our own stuff
	}

}
