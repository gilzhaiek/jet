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

package reconinstruments.HUDGlanceManager.cts;

import com.reconinstruments.os.hardware.glance.HUDGlanceManager;
import com.reconinstruments.os.hardware.glance.GlanceCalibrationListener;
import com.reconinstruments.os.hardware.glance.GlanceDetectionListener;
import com.reconinstruments.os.HUDOS;

import android.test.AndroidTestCase;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.ConditionVariable;
import android.util.Log;

/**
 * Junit / Instrumentation test case for HUDGlanceManger API
 *
 * Test and validate the behaviour of the HUDGlanceManager library
 */ 
public class GlanceManagerTest extends AndroidTestCase {
    // Debug variables
    private static final String TAG = "GlanceManagerTest";
    private static final boolean DEBUG = false;
    private static final long TIMEOUT = 30000;

    // Enable to test HUDGlanceManager Listener manually
    private static final boolean TEST_LISTENER_MANUAL = false;

    // Lock for ListenerTestThrd and the MainThrd
    private static final ConditionVariable thrdLock = new ConditionVariable(true);

    // Handler to communicate with ListenerTestThrd
    Handler mHandler = null;

    // Control Constant defining the loop amount of each functionality test
    private static final int CONTROL = 50;

    // Constant what value to kill test_listener thread
    private static final int KILL_SIG = -1;

    // HUDGlanceManager instance
    private HUDGlanceManager mHUDGlanceManager = null;

    // GlanceCalibrationListener instance to be tested throughout the CTS test
    private GlanceCalibrationListener MockGlanceCalibrationListener = new GlanceCalibrationListener() {
        public void onCalibrationEvent(boolean atDisplay) {
        }
    }; 

    // GlanceDetectionListener instance to be tested throughout the CTS test
    private GlanceDetectionListener MockGlanceDetectionListener = new GlanceDetectionListener() {
        public void onDetectEvent(boolean atDisplay) {
            if (DEBUG) Log.d(TAG, "GlanceDetectionListener->onDetectEvent Invoked...SUCCESS");

            synchronized (thrdLock)
            {
                unblockThrd();
            }
        }
        public void onRemovalEvent(boolean removed) {
            if (DEBUG) Log.d(TAG, "GlanceDetectionListener->onRemovalEvent Invoked...SUCCESS");

            synchronized (thrdLock)
            {
                unblockThrd();
            }
        }
    };

    // GlanceDetectionListener instnace to be tested throughout the CTS test
    private GlanceDetectionListener MockGlanceDetectionListenerNew = new GlanceDetectionListener() {
        public void onDetectEvent(boolean atDisplay) {
        }
        public void onRemovalEvent(boolean removed) {
        }
    };

    /**
     * The very first function called in the CTS test
     *
     * Obtained a single HUDGlanceManager instance for setup purposes
     */
    public void setUp() throws Exception 
    {
        super.setUp();

        mHUDGlanceManager = (HUDGlanceManager) HUDOS.getHUDService(HUDOS.HUD_GLANCE_SERVICE);
    }

    /**
     * Manually tests the invoked GlanceDetectionListener
     *      Have to manually invoke glnace detection event
     *
     *
     * Uncomment to Test
     */
    public void test_Listener()
    {
        if (TEST_LISTENER_MANUAL)
        {
            if (mHUDGlanceManager != null && thrdLock != null)
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
    }

    /**
     * Tests the getHUDService(HUDOS.HUD_GLANCE_SERVICE) method
     *
     * Checks for null and type validity
     */
    public void test_getHUDService() 
    {
        assertNotNull(mHUDGlanceManager);
        assertTrue(mHUDGlanceManager instanceof HUDGlanceManager);
        for(int i = 0; i < CONTROL; i++)
        {
            mHUDGlanceManager = (HUDGlanceManager) HUDOS.getHUDService(HUDOS.HUD_GLANCE_SERVICE);
            assertNotNull(mHUDGlanceManager);
            assertTrue(mHUDGlanceManager instanceof HUDGlanceManager);

            mHUDGlanceManager = (HUDGlanceManager) HUDOS.getHUDService(-i);
            assertNull(mHUDGlanceManager);
        }
    }

    /**
     * Tests the constant variables
     *
     * Checks for value validity
     */
    public void test_variables()
    {
        if (mHUDGlanceManager != null)
        {
            assertTrue("var EVENT_AHEAD_CALIBRATED != 0, Fail!", mHUDGlanceManager.EVENT_AHEAD_CALIBRATED == 0);
            assertTrue("var EVENT_DISPLAY_CALIBRATED != 1, Fail!", mHUDGlanceManager.EVENT_DISPLAY_CALIBRATED == 1);
            assertTrue("var EVENT_GLANCE_AHEAD != 2, Fail!", mHUDGlanceManager.EVENT_GLANCE_AHEAD == 2);
            assertTrue("var EVENT_GLANCE_DISPLAY != 3, Fail!", mHUDGlanceManager.EVENT_GLANCE_DISPLAY == 3);
            assertTrue("var EVENT_GLANCE_STOPPED != 4, Fail!", mHUDGlanceManager.EVENT_GLANCE_STOPPED == 4);
            assertTrue("var EVENT_REMOVED != 5, Fail!", mHUDGlanceManager.EVENT_REMOVED == 5);
            assertTrue("var EVENT_UNKNOWN != -1, Fail!", mHUDGlanceManager.EVENT_UNKNOWN == -1);
        }
    }

    /**
     * Tests the registerGlanceCalibration() method
     *
     * Checks for permission validity:
     *      SecurityException thrown
     *
     * Might have to be changed after Javadoc hidden update
     */
    public void test_registerGlanceCalibration()
    {
        if (mHUDGlanceManager != null)
        {
            try
            {
                for(int i = 0; i < CONTROL; i++)
                {
                    mHUDGlanceManager.registerGlanceCalibration(MockGlanceCalibrationListener);
                    mHUDGlanceManager.registerGlanceCalibration(null);
                }
            }
            catch(SecurityException e)
            {
                assertEquals("registerGlanceCalibration did not throw correct message, Fail!" + e.getMessage(), e.getMessage(), "Requires GLANCE_CALIBRATION permission");
            }
        }
    }

    /**
     * Tests the unregisterGlanceCalibration() method
     *
     * Checks for permission validity:
     *      SecurityException thrown
     *
     * Might have to be changed after Javadoc hidden update
     */
    public void test_unregisterGlanceCalibration()
    {
        if (mHUDGlanceManager != null)
        {
            try
            {
                for(int i = 0; i < CONTROL; i++)
                {
                    mHUDGlanceManager.unregisterGlanceCalibration(MockGlanceCalibrationListener);
                    mHUDGlanceManager.unregisterGlanceCalibration(null);
                }

            }
            catch(SecurityException e)
            {
                assertEquals("registerGlanceCalibration did not throw correct message, Fail!" + e.getMessage(), e.getMessage(), "Requires GLANCE_CALIBRATION permission");
            }
        }
    }

    /**
     * Tests the aheadCalibration() method
     *
     * Checks for runtime exception
     *
     * Might have to be changed after Javadoc hidden update
     */
    public void test_aheadCalibration()
    {
        if (mHUDGlanceManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                try
                {
                    mHUDGlanceManager.aheadCalibration();
                    fail("aheadCalibration did not throw exception");
                }
                catch(RuntimeException e)
                {
                    assertEquals("registerGlanceCalibration did not throw correct message, Fail!" + e.getMessage(), e.getMessage(), "Failed to initiate ahead calibration");
                }
            }
        }
    }

    /**
     * Tests the displayCalibration() method
     *
     * Checks for runtime exception
     *
     * Might have to be changed after Javadoc hidden update
     */
    public void test_displayCalibration()
    {
        if (mHUDGlanceManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                try
                {
                    mHUDGlanceManager.displayCalibration();
                    fail("displayCalibration did not throw exception");
                }
                catch(RuntimeException e)
                {
                    assertEquals("registerGlanceCalibration did not throw correct message, Fail!" + e.getMessage(), e.getMessage(), "Failed to initiate display calibration");
                }
            }
        }
    }

    /**
     * Tests the registerGlanceDetection() & unregisterGlanceDetection() method
     *
     * Checks for behaviour validity:
     *      unregister with no listener registered
     *      repeated listener register/unregister
     *      multiple listener register/unregister
     *      invalid type register/unregister
     *      null type register/unregister
     */
    public void test_GlanceDetectionRegistration()
    {
        if (mHUDGlanceManager != null)
        {
            try
            {
                mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListener);
                mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListenerNew);
            }
            catch(RuntimeException e)
            {
                fail("unregisterGlanceDetection threw exception: " + e.getMessage() + ", Fail!");
            }

            assertFalse("registerGlanceDetection == -1, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListener) == -1);
            assertFalse("registerGlanceDetection == -1, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListenerNew) == -1);
            try
            {
                mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListener);
                mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListenerNew);
            }
            catch(RuntimeException e)
            {
                fail("unregisterGlanceDetection threw exception: " + e.getMessage() + ", Fail!");
            }

            assertFalse("registerGlanceDetection == -1, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListener) == -1);
            assertFalse("registerGlanceDetection == -1, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListenerNew) == -1);
            for(int i = 0; i < CONTROL; i++)
            {
                assertTrue("registerGlanceDetection != -1 when duplicate, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListener) == -1);
                assertTrue("registerGlanceDetection != -1 when duplicate, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListenerNew) == -1);
            }
            for(int i = 0; i < CONTROL; i++)
            {
                try
                {
                    mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListener);
                    mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListenerNew);
                }
                catch(RuntimeException e)
                {
                    fail("unregisterGlanceDetection threw exception: " + e.getMessage() + ", Fail!");
                }
            }

            for(int i = 0; i < CONTROL; i++)
            {
                assertFalse("registerGlanceDetection == -1, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListener) == -1);
                assertFalse("registerGlanceDetection == -1, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListenerNew) == -1);
                try
                {
                    mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListener);
                    mHUDGlanceManager.unregisterGlanceDetection(MockGlanceDetectionListenerNew);
                }
                catch(RuntimeException e)
                {
                    fail("unregisterGlanceDetection threw exception: " + e.getMessage() + ", Fail!");
                }
            }
            for(int i = 0; i < CONTROL; i++)
            {
                assertTrue("registerGlanceDetection(null) != -1, Fail!", mHUDGlanceManager.registerGlanceDetection(null) == -1);
                try
                {
                    mHUDGlanceManager.unregisterGlanceDetection(null);
                }
                catch(RuntimeException e)
                {
                    fail("unregisterGlanceDetection threw exception: " + e.getMessage() + ", Fail!");
                }
            }
        }
    }

    private void blockThrd()
    {
        thrdLock.close();
        if (!thrdLock.block(TIMEOUT))
            fail("");
    }

    private void unblockThrd()
    {
        thrdLock.open();
    }

    // Test Thread to register the GlanceDetectionListener
    class ListenerTestThrd extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            try {
                assertFalse("registerGlanceDetection == -1, Fail!", mHUDGlanceManager.registerGlanceDetection(MockGlanceDetectionListener) == -1);

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
