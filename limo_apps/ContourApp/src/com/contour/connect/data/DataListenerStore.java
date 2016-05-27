package com.contour.connect.data;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import android.util.Log;

public class DataListenerStore
{
    public static final String TAG = "DataListenerStore";

    
    static class ListenerProfile
    {
        final DataListener mListener;
        
        public ListenerProfile(DataListener listener)
        {
            mListener = listener;
        }
    }
    
    private final HashMap<DataListener,ListenerProfile> mCurrentListeners = new HashMap<DataListener,ListenerProfile>();
    private final WeakHashMap<DataListener,ListenerProfile> mDeadListeners = new WeakHashMap<DataListener,ListenerProfile>();
    
    private final Map<DataManager.DataType, Set<DataListener>> mDataListenerTypeMap = new EnumMap<DataManager.DataType, Set<DataListener>>(DataManager.DataType.class);

    public DataListenerStore()
    {
        for (DataManager.DataType type : DataManager.DataType.values())
        {
            mDataListenerTypeMap.put(type, new LinkedHashSet<DataListener>());
        }
    }
    
    public ListenerProfile registerListener(DataListener regListener, EnumSet<DataManager.DataType> listenerDataTypes)
    {
        if(mCurrentListeners.containsKey(regListener))
        {
            throw new IllegalArgumentException("Attempt to register a listener that already exists");
        }
        
        ListenerProfile listenerProfile = mDeadListeners.remove(regListener);
        if(listenerProfile == null)
        {
            listenerProfile = new ListenerProfile(regListener);
        }
        mCurrentListeners.put(regListener, listenerProfile);
        
        for (DataManager.DataType type : listenerDataTypes) 
        {
            Set<DataListener> typeSet = mDataListenerTypeMap.get(type);
            typeSet.add(regListener);
          }
        
        return listenerProfile;
    }
    
    public void unregisterListener(DataListener unregListener)
    {
        ListenerProfile listenerProfile = mCurrentListeners.remove(unregListener);
        if(listenerProfile == null)
        {
           Log.w(TAG,"Attempt to remove listener that was never registered");
           return;
        }
        mDeadListeners.put(unregListener, listenerProfile);
    }
    
    public Set<DataListener> getListenersForType(DataManager.DataType type)
    {
        return mDataListenerTypeMap.get(type);
    }
}
