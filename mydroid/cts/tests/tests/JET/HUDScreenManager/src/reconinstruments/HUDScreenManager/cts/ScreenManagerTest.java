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

package reconinstruments.HUDScreenManager.cts;

import com.reconinstruments.os.hardware.screen.HUDScreenManager;

import android.test.AndroidTestCase;

/**
 * Junit / Instrumentation test case for HUDScreenManager API
 *
 * Tests and validate the behaviour of the HUDScreenManager library
 */
public class ScreenManagerTest extends AndroidTestCase {
    // Control Constant defining the loop amount of each functionality test
    private static final int CONTROL = 50;

    // HUDScreenManager instance
    private HUDScreenManager mHUDScreenManager;

    /**
     * The very first function called in the CTS Test
     *
     * Obtained a single HUDScreenManager instance for setup purposes
     */
    public void setUp() throws Exception 
    {
        super.setUp();

        mHUDScreenManager = HUDScreenManager.getInstance();
    }

    /**
     * Tests the getInstance() method
     *
     * Checks for null and type validity
     */
    public void test_getInstance() 
    {
        assertNotNull(mHUDScreenManager);
        assertTrue(mHUDScreenManager instanceof HUDScreenManager);
        for(int i = 0; i < CONTROL; i++)
        {
            mHUDScreenManager = HUDScreenManager.getInstance();
            assertNotNull(mHUDScreenManager);
            assertTrue(mHUDScreenManager instanceof HUDScreenManager);
        }
    }

    /**
     * Tests the constant variables
     *
     * Checks for value validity
     */
    public void test_variables()
    {
        if (mHUDScreenManager != null)
        {
            assertTrue("var SCREEN_STATE_BL_OFF != 0, Fail!", mHUDScreenManager.SCREEN_STATE_BL_OFF == 0);
            assertTrue("var SCREEN_STATE_FADING_OFF != 3, Fail!", mHUDScreenManager.SCREEN_STATE_FADING_OFF == 3);
            assertTrue("var SCREEN_STATE_FORCED_ON != 5, Fail!", mHUDScreenManager.SCREEN_STATE_FORCED_ON == 5);
            assertTrue("var SCREEN_STATE_FORCED_STAY_ON != 6, Fail!", mHUDScreenManager.SCREEN_STATE_FORCED_STAY_ON == 6);
            assertTrue("var SCREEN_STATE_ON != 4, Fail!", mHUDScreenManager.SCREEN_STATE_ON == 4);
            assertTrue("var SCREEN_STATE_PENDING_OFF != 2, Fail!", mHUDScreenManager.SCREEN_STATE_PENDING_OFF == 2);
            assertTrue("var SCREEN_STATE_POWER_OFF != 1, Fail!", mHUDScreenManager.SCREEN_STATE_POWER_OFF == 1);
        }
    }

    /**
     * Tests the registerToGlance() & unregisterToGlance() method
     *
     * Checks for behaviour validity:
     *      unregister with no listener registered
     *      repeated listener register/unregister
     */
    public void test_GlanceRegristration()
    {
        if (mHUDScreenManager != null)
        {
            assertTrue("unregisterToGlance() == -1, Fail!", mHUDScreenManager.unregisterToGlance() == -1);

            assertFalse("registerToGlance() == -1, Fail!", mHUDScreenManager.registerToGlance() == -1);
            for (int i = 0; i < CONTROL; i++)
            {
                assertTrue("registerToGlance() == -1, Fail!", mHUDScreenManager.registerToGlance() == -1);
            }

            assertFalse("unregisterToGlance() == -1, Fail!", mHUDScreenManager.unregisterToGlance() == -1);
            for (int i = 0; i < CONTROL; i++)
            {
                assertFalse("registerToGlance() == -1, Fail!", mHUDScreenManager.unregisterToGlance() == -1);
            }
        }
    }

    /**
     * Tests the getScreenState() method
     *
     * Checks for value validity:
     *      Range: 0 to 6
     */
    public void test_getScreenState()
    {
        if (mHUDScreenManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                int mScreenState = mHUDScreenManager.getScreenState();
                assertTrue("getScreenState() out of range, Fail!", mScreenState == mHUDScreenManager.SCREEN_STATE_BL_OFF || 
                        mScreenState == mHUDScreenManager.SCREEN_STATE_POWER_OFF || mScreenState == mHUDScreenManager.SCREEN_STATE_PENDING_OFF || 
                        mScreenState == mHUDScreenManager.SCREEN_STATE_FADING_OFF || mScreenState == mHUDScreenManager.SCREEN_STATE_ON || 
                        mScreenState == mHUDScreenManager.SCREEN_STATE_FORCED_ON ||  mScreenState == mHUDScreenManager.SCREEN_STATE_FORCED_STAY_ON);
            }
        }
    }

    /**
     * Tests the forceScreenOn() & setScreenOffDelay() method
     *
     * Check for behaviour validity:
     *      Valid argument: positive input
     *      Invalid argument: negative input
     *      null type input: won't compile pass
     */
    public void test_ScreenOnOff()
    {
        if (mHUDScreenManager != null)
        {
            try
            {
                assertTrue("forceScreenOn() != 1, Fail!", mHUDScreenManager.forceScreenOn(3000, false) == 1);
                assertTrue("setScreenOffDelay() == -1, Fail!", mHUDScreenManager.setScreenOffDelay(3000) == 1);
                assertTrue("forceScreenOn() != 1, Fail!", mHUDScreenManager.forceScreenOn(3000, false) == 1);

                for(int i = 0; i < CONTROL; i++)
                {
                    assertTrue("setScreenOffDelay() == -1, Fail!", mHUDScreenManager.setScreenOffDelay(i) == 1);
                }
                for(int i = 0; i < CONTROL; i++)
                {
                    assertTrue("forceScreenOn() != 1, Fail!", mHUDScreenManager.forceScreenOn(i, false) == 1);
                }
                for(int i = 0; i < CONTROL; i++)
                {
                    assertTrue("setScreenOffDelay() == -1, Fail!", mHUDScreenManager.setScreenOffDelay(i) == 1);
                    assertTrue("forceScreenOn() != 1, Fail!", mHUDScreenManager.forceScreenOn(i, true) == 1);
                }

                for(int i = -1; i > -CONTROL; i--)
                {
                    assertFalse("setScreenOffDelay() invalid arguemnt (negative) not handled, Fail!", mHUDScreenManager.setScreenOffDelay(i) == 1);
                }
                for(int i = -1; i > -CONTROL; i--)
                {
                    assertFalse("forceScreenOn(invalid) == 1, Fail!", mHUDScreenManager.forceScreenOn(i, false) == 1);
                    assertFalse("forceScreenOn(invalid) == 1, Fail!", mHUDScreenManager.forceScreenOn(i, true) == 1);
                }
            }
            catch(RuntimeException e)
            {
                fail("forceScreenOn() thrown exception: " + e.getMessage());
            }
        }
    }
}
