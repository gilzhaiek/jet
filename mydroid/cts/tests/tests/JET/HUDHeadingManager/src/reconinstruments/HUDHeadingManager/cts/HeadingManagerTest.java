/*
 * Copyright (C) 2015 Recon Instruments
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reconinstruments.HUDHeadingManager.cts;

import com.reconinstruments.os.hardware.sensors.HUDHeadingManager;
import com.reconinstruments.os.hardware.sensors.HeadLocationListener;
import com.reconinstruments.os.HUDOS;

import android.test.AndroidTestCase;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.ConditionVariable;
import android.util.Log;

/**
 * Junit / Instrumentation test case for HUDHeadingManager API
 *
 * Tests and validate the behaviour of the HUDHeadingManager library
 */
public class HeadingManagerTest extends AndroidTestCase implements HeadLocationListener {
    // Debug variables
    private static final String TAG = "HeadingManagerTest";
    private static final boolean DEBUG = false;
    private static final long TIMEOUT = 30000;

    // Lock for ListenerTestThrd and the MainThrd
    private static final ConditionVariable thrdLock = new ConditionVariable(true);

    // Handler to communicate with ListenerTestThrd
    Handler mHandler = null;

    // Control Constant defining the loop amount of each functionality test
    private static final int CONTROL = 50;

    // Control Constant defining the loop amount of each functionality test
    private static final int KILL_SIG = -1;

    // HUDHeadingManager instance
    private HUDHeadingManager mHUDHeadingManager;

    // HeadLocationListener instance to be tested throughtout the CTS test
    private HeadLocationListener MockHeadLocationListener = new HeadLocationListener() {
        public void onHeadLocation(float yaw, float pitch, float roll) {
            if (DEBUG) Log.d(TAG, "yaw: "+yaw+", pitch: "+pitch+", roll: "+roll);

            assertTrue("yaw out of bound, Fail!", yaw >= 0 && yaw <= 360);
            assertTrue("pitch out of bound, Fail!", pitch >= -90 && pitch <= 90);
            assertTrue("roll out of bound, Fail!", roll >= -180 && roll <= 180);

            synchronized(thrdLock)
            {
                unblockThrd();
            }
        }
    };

    /**
     * The very first function called in the CTS test
     *
     * Obtained a single HUDHeadingManager instance for setup purposes
     */
    public void setUp() throws Exception 
    {
        super.setUp();

        mHUDHeadingManager = (HUDHeadingManager) HUDOS.getHUDService(HUDOS.HUD_HEADING_SERVICE);
    }

    /**
     * Tests the invoked HeadLocationListener
     *
     * Test for value validity
     *      Correct type
     *      Range: yaw -> 0 to 360, pitch -> -90 to 90, roll -> -180 to 180
     */
    public void test_Listener()
    {
        if (mHUDHeadingManager != null && thrdLock != null)
        {
            synchronized(thrdLock) {
                ListenerTestThrd listenerTest = new ListenerTestThrd();
                listenerTest.start();

                blockThrd();
            }
            Message msg = mHandler.obtainMessage(KILL_SIG);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Tests the getHUDService(HUDOS.HUD_HEADING_SERVICE) method
     *
     * Checks for null and type validity
     */
    public void test_getHUDService() 
    {
        assertNotNull(mHUDHeadingManager);
        assertTrue(mHUDHeadingManager instanceof HUDHeadingManager);
        for(int i = 0; i < CONTROL; i++)
        {
            mHUDHeadingManager = (HUDHeadingManager) HUDOS.getHUDService(HUDOS.HUD_HEADING_SERVICE);
            assertNotNull(mHUDHeadingManager);
            assertTrue(mHUDHeadingManager instanceof HUDHeadingManager);

            mHUDHeadingManager = (HUDHeadingManager) HUDOS.getHUDService(-i);
            assertNull(mHUDHeadingManager);
        }
    }

    /**
     * Tests the register() & unregister() method
     *
     * Checks for behaviour validity:
     *      unregister with no listener registered
     *      repeated listener register/unregister
     *      mulitple listener register/unregister
     *      null type register/unregister
     */
    public void test_Registration()
    {
        if (mHUDHeadingManager != null)
        {
            try
            {
                mHUDHeadingManager.unregister(MockHeadLocationListener);
                mHUDHeadingManager.unregister(this);
                mHUDHeadingManager.register(MockHeadLocationListener);
                mHUDHeadingManager.register(this);

                for (int i = 0; i < CONTROL; i++)
                {
                    mHUDHeadingManager.register(MockHeadLocationListener);
                    mHUDHeadingManager.register(this);
                    mHUDHeadingManager.register(null);
                }

                mHUDHeadingManager.unregister(MockHeadLocationListener);
                mHUDHeadingManager.unregister(this);
                mHUDHeadingManager.unregister(null);
                for(int i = 0; i < CONTROL; i++)
                {
                    mHUDHeadingManager.unregister(MockHeadLocationListener);
                    mHUDHeadingManager.unregister(this);
                    mHUDHeadingManager.unregister(null);
                }

                for(int i = 0; i < CONTROL; i++)
                {
                    mHUDHeadingManager.register(MockHeadLocationListener);
                    mHUDHeadingManager.register(this);
                    mHUDHeadingManager.register(null);

                    mHUDHeadingManager.unregister(MockHeadLocationListener);
                    mHUDHeadingManager.unregister(this);
                    mHUDHeadingManager.unregister(null);
                }
            }
            catch(RuntimeException e)
            {
                fail("register() / unregister() failed: " + e.getMessage());
            }
        }
    }

    @Override
    // HeadLocationListener instance to be tested throughtout the CTS test
    public void onHeadLocation(float yaw, float pitch, float roll) {
    }

    private void blockThrd()
    {
        thrdLock.close();
        if (!thrdLock.block(TIMEOUT))
            fail("test listener timed out... FAIL!");
    }

    private void unblockThrd()
    {
        thrdLock.open();
    }

    // Test Thread to register HeadLocationListener
    class ListenerTestThrd extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            try {
                if (DEBUG) Log.d(TAG, "Thread: " + Thread.currentThread().getId() + ", is going to register MockIheadLocationListener and wake up main thread when listener invoked!!!");
                mHUDHeadingManager.register(MockHeadLocationListener);

                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        switch(msg.what) {
                            case KILL_SIG:
                                if (DEBUG) Log.d(TAG, "Stopping ListenerTestThrd Looper!");

                                Looper mLooper = Looper.myLooper();
                                if (mLooper != null)
                                    mLooper.quit();
                                break;
                            default:
                                break;
                        }
                    }
                };
            } catch (Exception e) {
                Log.d(TAG, "Listener Thread Exception Thrown: " + e.getMessage() + ", Fail!");
            }
            Looper.loop();
        }
    }
}
