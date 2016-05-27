package com.contour.connect.data;

import java.util.EnumSet;
import java.util.Set;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.contour.connect.data.DataController.DataControllerListener;
import com.contour.utils.LocationUtils;


public class DataManager
{
    public static final String TAG = "DataManager";
   
    private final Context                mContext;
    private final DataListenerStore     mDataListenerStore;

    private static DataManager           sInstance;
    
    private HandlerThread                mHandlerThread;
    private Handler                      mHandler;

    private DataController               mDataController;
    private DataControllerListener       mDataControllerListener = new ManagerDataControllerListener();


    private Location mLastGoodLocationFix;
    

    public static enum DataType
    {
        COMPASS,
        LOCATION,
        PREFERENCES;
        
        public static EnumSet<DataType> basicListeners()
        {
            EnumSet<DataType> set = EnumSet.allOf(DataType.class);
            return set;
        }
    }
    
    public synchronized static DataManager newInstance(Context context)
    {
        context = context.getApplicationContext();
        DataListenerStore dataAuscultator = new DataListenerStore();
        return new DataManager(context,dataAuscultator);
    }
    
    public static DataManager getCurrentInstance()
    {
        if(!isStarted())
        {
            throw new IllegalStateException("Attempt to acquire DataManager before calling start()");
        }
        return sInstance;
    }
    
    private DataManager(Context pContext, DataListenerStore pDataStore)
    {
        mContext = pContext;
        mDataListenerStore = pDataStore;
    }
    
    public void start()
    {
        if(isStarted())
        {
            throw new IllegalArgumentException("attempt to start DataManager on running instance!");
        }
        
        sInstance = this;
        mHandlerThread = new HandlerThread("DataManagerListenerHandler");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        
        mDataController = new DataController(mContext,mDataControllerListener);
        mDataController.updateListeners(DataType.basicListeners());
        
    }
    
    public void stop()
    {
        if(!isStarted())
        {
            Log.w(TAG, "attempt to stop DataManager before calling start()");
            return;
        }


        mDataController.unregisterAllListeners();
        sInstance = null;
        
        mDataController = null;
        
        mHandlerThread.getLooper().quit();
        mHandler = null;
        mHandlerThread = null;
    }
    
    public void registerListener(DataListener listener, EnumSet<DataManager.DataType> listenerTypes)
    {
        synchronized(mDataListenerStore)
        {
           
//            ListenerProfile listenerProfile = 
            
            //returned value will be used to pre=populate the listener with cached data
            mDataListenerStore.registerListener(listener,listenerTypes);
            if(!isStarted()) return; 
            mDataController.updateListeners(DataType.basicListeners());
        }
    }
    
    
    public void unregisterListener(DataListener listener)
    {
        synchronized(mDataListenerStore)
        {
//            ListenerProfile listenerProfile = 
            mDataListenerStore.unregisterListener(listener);
            if(!isStarted()) return;
            mDataController.updateListeners(DataType.basicListeners());
        }
    }
    
    
    void runInListenerThread(Runnable runnable)
    {
        if (mHandler == null)
        {
            Log.e(TAG, "Tried to use listener thread before start()", new Throwable());
            return;
        }

        mHandler.post(runnable);
    }

    private Set<DataListener> getListenersFor(DataManager.DataType type)
    {
        synchronized (mDataListenerStore)
        {
            return mDataListenerStore.getListenersForType(type);
        }
    }
    
    private static boolean isStarted()
    {
        return sInstance != null;
    }
    
    void notifyLocationChanged(Location location, final Set<DataListener> listeners)
    {       
        if(LocationUtils.isLocationValid(location) == false)
        {
            return;
        }
        
        boolean isFresh;
        boolean isAccurate;
        
        if (LocationUtils.isLocationValid(mLastGoodLocationFix) == false)
        {
            isFresh = true;
            isAccurate = true;
        } else
        {
            isAccurate = LocationUtils.locationAccuracyImproved(location,mLastGoodLocationFix);
            
            if (LocationUtils.isLocationFromGPS(location))
            {
                isFresh = true;

            } else
            {
                isFresh = LocationUtils.locationAgeImproved(location, mLastGoodLocationFix) == false;
            }
        }
        
        if(isAccurate && isFresh)
        {
            mLastGoodLocationFix = location;
        }
        
        final Location loc = mLastGoodLocationFix;
        runInListenerThread(new Runnable()
        {
            @Override
            public void run()
            {
                for (DataListener listener : listeners)
                {
                    listener.onCurrentLocationUpdate(loc);
                }
            }
        });
    }
    
    void notifyAzimuthChanged(float pAzimuth, final Set<DataListener> listeners)
    {
        final float azimuth = pAzimuth;
        runInListenerThread(new Runnable()
        {
            @Override
            public void run()
            {
                for (DataListener listener : listeners)
                {
                    listener.onAzimuthUpdate(azimuth);
                }
            }
        });
    }
    
    class ManagerDataControllerListener implements DataControllerListener
    {

        @Override
        public void sharedPreferenceChanged(String key)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void savedPointsChanged()
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void locationProviderAvailable(boolean b)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void locationProviderEnabled(boolean b)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void locationChanged(Location location)
        {
            DataManager.this.notifyLocationChanged(location, getListenersFor(DataManager.DataType.LOCATION));
            
        }

        @Override
        public void azimuthChanged(float azimuth)
        {
            DataManager.this.notifyAzimuthChanged(azimuth, getListenersFor(DataManager.DataType.COMPASS));
            
        }

    }
}
