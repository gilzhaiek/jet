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

package reconinstruments.HUDActivityMotionManager.cts;

import com.reconinstruments.os.hardware.motion.HUDActivityMotionManager;
import com.reconinstruments.os.hardware.motion.ActivityMotionDetectionListener;
import com.reconinstruments.os.HUDOS;

import android.test.AndroidTestCase;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.ConditionVariable;
import android.util.Log;

/**
 * Junit / Instrumentation test case for HUDActivityMotionManager
 *
 * Test and validate the behaviour of the HUDActivityMotionManager library
 */
public class ActivityMotionManagerTest extends AndroidTestCase implements ActivityMotionDetectionListener {
    // Debug variables
    private static final String TAG = "ActivityMotionManagerTest";
    private static final boolean DEBUG = false;
    private static final long TIMEOUT = 30000;

    // Lock for ListenerTestThrd and the MainThrd
    private static final ConditionVariable thrdLock = new ConditionVariable(true);

    // Handler to communicate with ListenerTestThrd
    Handler mHandler = null;

    // Control Constant defining the loop amount of each functionality test
    private static final int CONTROL = 50;

    // Constant what value to kill test_listener thread
    private static final int KILL_SIG = -1;

    // HUDActivityMotionManager instance
    private HUDActivityMotionManager mHUDActivityMotionManager = null;

    // ActivityMotionDetectionListener instance to be tested throughout the CTS test
    private ActivityMotionDetectionListener MockActivityMotionDetectionListener = new ActivityMotionDetectionListener() {
        public void onDetectEvent(boolean inMotion, int type) {
            if (DEBUG) Log.d(TAG, "ActivityMotionDetectionListener Invoked...");

            Log.d(TAG, "type = "+type);
            assertTrue("ActivityMotionDetectionListener->type out of bound, Fail!",
                    type == mHUDActivityMotionManager.MOTION_DETECT_CYCLING ||
                    type == mHUDActivityMotionManager.MOTION_DETECT_RUNNING ||
                    type == mHUDActivityMotionManager.MOTION_DETECT_SNOW);
            synchronized(thrdLock)
            {
                unblockThrd();
            }
        }
    };

    /**
     * The very first function called in the CTS Test
     *
     * Obtained a single HUDActivityMotionManager instance for setup purposes
     */
    public void setUp() throws Exception 
    {
        super.setUp();

        mHUDActivityMotionManager = (HUDActivityMotionManager) HUDOS.getHUDService(HUDOS.HUD_ACTIVITY_MOTION_SERVICE);
    }

    /**
     * Tests the invoked ActivityMotionDetectionListener
     *
     * Test for value validity 
     *      Correct type
     *      type range: MOTION_DETECT_SNOW, MOTION_DETECT_CYCLING, MOTION_DETECT_RUNNING 
     */
    public void test_Listener()
    {
        if (mHUDActivityMotionManager != null && thrdLock != null)
        {
            synchronized(thrdLock)
            {
                ListenerTestThrd listenerTest = new ListenerTestThrd();
                listenerTest.start();
                blockThrd();
            }

            Message msg = mHandler.obtainMessage(KILL_SIG);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Tests the getHUDService(HUDOS.HUD_ACTIVITY_MOTION_SERVICE) method
     *
     * Checks for null and type validity
     */
    public void test_getHUDService() 
    {
        assertNotNull(mHUDActivityMotionManager);
        assertTrue(mHUDActivityMotionManager instanceof HUDActivityMotionManager);
        for(int i = 0; i < CONTROL; i++)
        {
            mHUDActivityMotionManager = (HUDActivityMotionManager) HUDOS.getHUDService(HUDOS.HUD_ACTIVITY_MOTION_SERVICE);
            assertNotNull(mHUDActivityMotionManager);
            assertTrue(mHUDActivityMotionManager instanceof HUDActivityMotionManager);

            mHUDActivityMotionManager = (HUDActivityMotionManager) HUDOS.getHUDService(-i);
            assertNull(mHUDActivityMotionManager);
        }
    }

    /**
     * Tests the constant variables
     *
     * Checks for value validity
     */
    public void test_variables()
    {
        if (mHUDActivityMotionManager != null)
        {
            assertTrue("var EVENT_IN_MOTION != 1, Fail!", mHUDActivityMotionManager.EVENT_IN_MOTION == 1);
            assertTrue("var EVENT_INVALID != -1, Fail!", mHUDActivityMotionManager.EVENT_INVALID == -1);
            assertTrue("var EVENT_STATIONARY != 0, Fail!", mHUDActivityMotionManager.EVENT_STATIONARY == 0);
            assertTrue("var MOTION_DETECT_CYCLING != 1, Fail!", mHUDActivityMotionManager.MOTION_DETECT_CYCLING == 1);
            assertTrue("var MOTION_DETECT_RUNNING != 2, Fail!", mHUDActivityMotionManager.MOTION_DETECT_RUNNING == 2);
            assertTrue("var MOTION_DETECT_NOT_SUPPORTED != -1, Fail!", mHUDActivityMotionManager.MOTION_DETECT_NOT_SUPPORTED == -1);
            assertTrue("var MOTION_DETECT_SNOW != -1, Fail!", mHUDActivityMotionManager.MOTION_DETECT_SNOW == 0);
        }
    }

    /**
     * Tests the registerActivityMotionDetection() & unregisterActivityMotionDetection() method
     *
     * Checks for behaviour validity:
     *      unregister with no listener registered
     *      repeated listener register/unregister
     *      multiple listener register/unregister
     *      mismatch listener type register
     *      invalid type register/unregister
     *      null type register/unregister
     */
    public void test_ActivityMotionListenerRegistration()
    {
        if (mHUDActivityMotionManager != null)
        {
            try
            {
                mHUDActivityMotionManager.unregisterActivityMotionDetection(this);
                mHUDActivityMotionManager.unregisterActivityMotionDetection(MockActivityMotionDetectionListener);

                assertFalse("registerActivityMotionDetection == 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (MockActivityMotionDetectionListener, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);
                assertFalse("registerActivityMotionDetection second listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (this, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);

                for(int i = 0; i < CONTROL; i++)
                {
                    assertTrue("registerActivityMotionDetection repeat listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (MockActivityMotionDetectionListener, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);
                    assertTrue("registerActivityMotionDetection second listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (this, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);
                    assertTrue("registerActivityMotionDetection repeat listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (MockActivityMotionDetectionListener, mHUDActivityMotionManager.MOTION_DETECT_CYCLING) == 0);
                    assertTrue("registerActivityMotionDetection second listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (this, mHUDActivityMotionManager.MOTION_DETECT_CYCLING) == 0);
                }

                mHUDActivityMotionManager.unregisterActivityMotionDetection(MockActivityMotionDetectionListener);
                mHUDActivityMotionManager.unregisterActivityMotionDetection(this);

                assertFalse("registerActivityMotionDetection == 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (MockActivityMotionDetectionListener, mHUDActivityMotionManager.MOTION_DETECT_CYCLING) == 0);
                assertFalse("registerActivityMotionDetection second listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (this, mHUDActivityMotionManager.MOTION_DETECT_CYCLING) == 0);

                for(int i = 0; i < CONTROL; i++)
                {
                    assertTrue("registerActivityMotionDetection repeat listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (MockActivityMotionDetectionListener, mHUDActivityMotionManager.MOTION_DETECT_CYCLING) == 0);
                    assertTrue("registerActivityMotionDetection second listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (this, mHUDActivityMotionManager.MOTION_DETECT_CYCLING) == 0);
                    assertTrue("registerActivityMotionDetection repeat listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (MockActivityMotionDetectionListener, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);
                    assertTrue("registerActivityMotionDetection second listener != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                            (this, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);
                }
                mHUDActivityMotionManager.unregisterActivityMotionDetection(MockActivityMotionDetectionListener);
                mHUDActivityMotionManager.unregisterActivityMotionDetection(this);

                for(int i = 0; i < CONTROL; i++)
                {
                    mHUDActivityMotionManager.unregisterActivityMotionDetection(MockActivityMotionDetectionListener);
                    mHUDActivityMotionManager.unregisterActivityMotionDetection(this);
                    mHUDActivityMotionManager.unregisterActivityMotionDetection(new ActivityMotionDetectionListener() {
                        public void onDetectEvent(boolean inMotion, int type) {

                        }
                    });
                }

                assertTrue("registerActivityMotionDetection null != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (null, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);
                mHUDActivityMotionManager.unregisterActivityMotionDetection(null);

                assertTrue("registerActivityMotionDetection invalid activity type != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (this, mHUDActivityMotionManager.MOTION_DETECT_SNOW) == 0);
                assertTrue("registerActivityMotionDetection invalid activity type != 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (this, mHUDActivityMotionManager.MOTION_DETECT_NOT_SUPPORTED) == 0);
            }
            catch(RuntimeException e)
            {
                fail("RuntimeException thrown: " + e.getMessage() + ", Fail!");
            }
        }
    }

    /**
     * Tests the getActivityMotionDetectedEvent() method
     *
     * Checks for value validity
     *      Range: -1 to 1
     */
    public void test_getActivityMotionDetectedEvent()
    {
        if (mHUDActivityMotionManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                int lastEvent = mHUDActivityMotionManager.getActivityMotionDetectedEvent();
                assertTrue("getActivityMotionDetectedEvent out of bounds, Fail!", lastEvent == mHUDActivityMotionManager.EVENT_INVALID || 
                        lastEvent == mHUDActivityMotionManager.EVENT_STATIONARY || lastEvent == mHUDActivityMotionManager.EVENT_IN_MOTION);
            }
        }
    }

    @Override
    // ActivityMotionDetectionListener to be tested throughout the CTS Test
    public void onDetectEvent(boolean inMotion, int type)
    {
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

    // Thread to register the ActivityMotionDetectionListener to test listener
    class ListenerTestThrd extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            try {
                assertFalse("registerActivityMotionDetection == 0, Fail!", mHUDActivityMotionManager.registerActivityMotionDetection
                        (MockActivityMotionDetectionListener, mHUDActivityMotionManager.MOTION_DETECT_RUNNING) == 0);

                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
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
