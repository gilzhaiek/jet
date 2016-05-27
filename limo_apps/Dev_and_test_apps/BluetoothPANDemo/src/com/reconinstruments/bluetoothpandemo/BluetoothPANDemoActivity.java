package com.reconinstruments.bluetoothpandemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.Log;
import java.util.Set;
import android.view.View;

import dalvik.system.DexClassLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import  java.lang.reflect.InvocationTargetException;

public class BluetoothPANDemoActivity extends Activity
{
    private static final String TAG = "BluetoothPANDemo";
    public static final boolean DEBUG = false;//true;
 
    private Context mContext = null;
    private Set<BluetoothDevice> mDevices;
    private Object mObject;

    private static boolean mConnected = false;

    private final class PanServiceListener
            implements BluetoothProfile.ServiceListener {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (DEBUG) Log.d(TAG, "onServiceConnected: " + proxy);
            mObject = (Object)proxy;
        }
        public void onServiceDisconnected(int profile) {
            mObject = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.main);

        // Retrieve the BT adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.getProfileProxy(mContext, new PanServiceListener(), 5);

        // Get the connected devices
        mDevices = adapter.getBondedDevices();
    }

    public void enableBluetoothPAN(View view) {
        if (!mDevices.isEmpty() && mObject != null && !mConnected) {
            DexClassLoader loader;
            Class BluetoothPANClass = null;
            String frameworkJarLocation = "/system/framework/framework.jar";
            loader = new DexClassLoader(frameworkJarLocation,
                new ContextWrapper(mContext).getCacheDir().getAbsolutePath(),
                null, ClassLoader.getSystemClassLoader());
            try {
                BluetoothPANClass = loader.loadClass("android.bluetooth.BluetoothPan");
                Method m = BluetoothPANClass.getMethod("connect", new Class[] {BluetoothDevice.class});
                Object res = m.invoke(mObject, mDevices.iterator().next());
                mConnected = (Boolean)res;
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Failed to find BluetoothPan");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Illegal access to BluetoothPan");
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "No such method");
            } catch (InvocationTargetException e) {
                Log.e(TAG, "Invoke exception: " + e.getCause());
            } catch (Exception e) {
                Log.e(TAG, "EXCEPTION: " + e);
            }
        } else {
            Log.e(TAG, "Cannot enable BT PAN!");
        }
    }

    public void disableBluetoothPAN(View view) {
        if (!mDevices.isEmpty() && mObject != null && mConnected) {
            DexClassLoader loader;
            Class BluetoothPANClass = null;
            String frameworkJarLocation = "/system/framework/framework.jar";
            loader = new DexClassLoader(frameworkJarLocation,
                new ContextWrapper(mContext).getCacheDir().getAbsolutePath(),
                null, ClassLoader.getSystemClassLoader());
            try {
                BluetoothPANClass = loader.loadClass("android.bluetooth.BluetoothPan");
                Method m = BluetoothPANClass.getMethod("disconnect", new Class[] {BluetoothDevice.class});
                m.invoke(mObject, mDevices.iterator().next());
                mConnected = false;
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Failed to find BluetoothPan");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Illegal access to BluetoothPan");
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "No such method");
            } catch (InvocationTargetException e) {
                Log.e(TAG, "Invoke exception: " + e.getCause());
            } catch (Exception e) {
                Log.e(TAG, "EXCEPTION: " + e);
            }
        } else {
            Log.e(TAG, "Cannot enable BT PAN!");
        }
    }
}
