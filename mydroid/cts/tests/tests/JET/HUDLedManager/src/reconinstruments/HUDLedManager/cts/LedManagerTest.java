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

package reconinstruments.HUDLedManager.cts;

import com.reconinstruments.os.hardware.led.HUDLedManager;
import com.reconinstruments.os.HUDOS;

import android.test.AndroidTestCase;
import android.os.SystemClock;
import android.os.Build;

/**
 * Junit / Instrumentation test case for HUDLedManager API
 *
 * Tests and validate the behaviour of the HUDLedManager library
 */
public class LedManagerTest extends AndroidTestCase {
    // Control Constant defining the loop amount of each functionality test
    private static final int CONTROL = 50;

    // HUDLedManager instance
    private HUDLedManager mHUDLedManager;

    /**
     * The very first function called in the CTS test
     *
     * Obtained a single HUDLedManager instance for setup purposes
     */
    public void setUp() throws Exception 
    {
        super.setUp();

        mHUDLedManager = (HUDLedManager) HUDOS.getHUDService(HUDOS.HUD_LED_SERVICE);
    }

    /**
     * Tests the getHUDService(HUDOS.HUD_LED_SERVICE) method
     *
     * Checks for null and type validity
     */
    public void test_getHUDService() 
    {
        assertNotNull(mHUDLedManager);
        assertTrue(mHUDLedManager instanceof HUDLedManager);
        for(int i = 0; i < CONTROL; i++)
        {
            mHUDLedManager = (HUDLedManager) HUDOS.getHUDService(HUDOS.HUD_LED_SERVICE);
            assertNotNull(mHUDLedManager);
            assertTrue(mHUDLedManager instanceof HUDLedManager);

            mHUDLedManager = (HUDLedManager) HUDOS.getHUDService(-i);
            assertNull(mHUDLedManager);
        }
    }

    /**
     * Tests the constant variables
     *
     * Checks for value validity
     */
    public void test_variables()
    {
        if (mHUDLedManager != null)
        {
            if (Build.MODEL.equalsIgnoreCase("JET"))
            {
                assertTrue("Build: JET, var brightness_low != 2, Fail!", mHUDLedManager.BRIGHTNESS_LOW == 2);
                assertTrue("Build: JET, var BRIGHTNESS_NORMAL != 26, Fail!", mHUDLedManager.BRIGHTNESS_NORMAL == 26);
            }
            else
            {
                assertTrue("Build: SNOW, var brightness_low != 20, Fail!", mHUDLedManager.BRIGHTNESS_LOW == 20);
                assertTrue("Build: SNOW, var BRIGHTNESS_NORMAL != 62, Fail!", mHUDLedManager.BRIGHTNESS_NORMAL == 62);
            }

            assertTrue("var BRIGHTNESS_HIGH != 255, Fail!", mHUDLedManager.BRIGHTNESS_HIGH == 255);
        }
    }

    /**
     * Tests the blinkPowerLED() method
     *
     * Check for behaviour validity:
     *      Valid argument
     *      Invalide argument
     *
     * Can manually check to see if LED is functioning correctly
     */
    public void test_blinkPowerLED()
    {
        if (mHUDLedManager != null)
        {
            assertTrue("blinkPowerLED(128, [5000,2500]) Fail", mHUDLedManager.blinkPowerLED(128, new int[]{5000,2500}) == 1);
            SystemClock.sleep(10000);
            for(int i = 0; i < 5000; i += 500)
            {
                assertTrue("blinkPowerLED(BRIGHTNESS_NORMAL, [" + i + ", " + i/2 + "]) Fail", mHUDLedManager.blinkPowerLED(mHUDLedManager.BRIGHTNESS_NORMAL, new int[]{i, i/2}) == 1);
            }

            SystemClock.sleep(20000);
            assertFalse("blinkPower(1000, [1000,1000])->invalid argument not handled, Fail!", mHUDLedManager.blinkPowerLED(1000, new int[]{1000,1000}) == 1);
            for(int i = 256; i < 2560; i += 256)
            {
                assertFalse("blinkPower(" + i + ", [1000,1000]), Fail!", mHUDLedManager.blinkPowerLED(i, new int[]{1000,1000}) == 1);
            }
            for(int i = -1; i > -2560; i -= 256)
            {
                assertFalse("blinkPower(" + i + ", [1000,1000]), Fail!", mHUDLedManager.blinkPowerLED(i, new int[]{1000,1000}) == 1);
            }

            assertFalse("blinkPowerLED(BRIGHTNESS_NORMAL, [-500, -2000])->invalid argument not handled, Fail!", mHUDLedManager.blinkPowerLED(mHUDLedManager.BRIGHTNESS_NORMAL, new int[]{-500, -2000}) == 1);
            for(int i = -1; i > -5000; i -= 500)
            {
                assertFalse("blinkPowerLED(BRIGHTNESS_NORMAL, [" + i + ", " + i + "]), Fail!", mHUDLedManager.blinkPowerLED(mHUDLedManager.BRIGHTNESS_HIGH, new int[]{i, i}) == 1);
            }
        }
    }

    /**
     * Tests the contBlinkPowerLED() method
     *
     * Checks for behaviour validity:
     *      Valid argument
     *      Invalid argument
     *
     * Can manually check to see if LED is functioning correctly
     */
    public void test_contBlinkPowerLED()
    {
        if (mHUDLedManager != null)
        {
            assertTrue("contBlinkPowerLED(BRIGHTNESS NORMAL, 500, 500, true) failed", mHUDLedManager.contBlinkPowerLED(mHUDLedManager.BRIGHTNESS_NORMAL, 500, 500, true) == 1);
            SystemClock.sleep(5000);
            assertTrue("contBlinkPowerLED(0, 0, 0, false) failed", mHUDLedManager.contBlinkPowerLED(0, 0, 0, false) == 1);

            for(int i = 1; i < 5000; i += 100)
            {
                assertTrue("contBlinkPowerLED(BRIGHTNESS NORMAL, "+i+", "+i+", true) failed", mHUDLedManager.contBlinkPowerLED(mHUDLedManager.BRIGHTNESS_NORMAL, i, i, true) == 1);
                assertTrue("contBlinkPowerLED(BRIGHTNESS NORMAL, "+i+", "+i+", false) failed", mHUDLedManager.contBlinkPowerLED(mHUDLedManager.BRIGHTNESS_NORMAL, i, i, false) == 1);
            }
            for(int i = 1; i < 5000; i += 100)
            {
                assertTrue("contBlinkPowerLED(BRIGHTNESS NORMAL, "+i+", "+i+", true) failed", mHUDLedManager.contBlinkPowerLED(mHUDLedManager.BRIGHTNESS_NORMAL, i, i, true) == 1);
            }
            for(int i = 1; i < 5000; i += 100)
            {
                assertTrue("contBlinkPowerLED(BRIGHTNESS NORMAL, "+i+", "+i+", false) failed", mHUDLedManager.contBlinkPowerLED(mHUDLedManager.BRIGHTNESS_NORMAL, i, i, false) == 1);
            }

            assertFalse("contBlinkPowerLED(BRIGHTNESS_LOW, -500, -500, true)->invalid argument not handled", mHUDLedManager.contBlinkPowerLED(mHUDLedManager.BRIGHTNESS_LOW, -500, -500, true) == 1);
            for( int i = -1; i > -10000; i -= 1000)
            {
                assertFalse("contBlinkPowerLED(BRIGHTNESS_HIGH, " + i + ", " + i + ", true)->invalid argument not handled", mHUDLedManager.contBlinkPowerLED(mHUDLedManager.BRIGHTNESS_HIGH, i, i, true) == 1);
            }

            assertFalse("contBlinkPowerLED(1000, 1000, 1000, true)->invalid argument not handled", mHUDLedManager.contBlinkPowerLED(1000, 1000, 1000, true) == 1);
            for(int i = 256; i < 10256; i += 1000)
            {
                assertFalse("contBlinkPowerLED(" + i + ", 1000, 1000, true)->invalid argument not handled", mHUDLedManager.contBlinkPowerLED(i, 1000, 1000, true) == 1);
            }
            for(int i = -1; i > -10000; i -= 1000)
            {
                assertFalse("contBlinkPowerLED(" + i + ", 1000, 1000, true)->invalid argument not handled", mHUDLedManager.contBlinkPowerLED(i, 1000, 1000, true) == 1);
            }
        }
    }

    /**
     * Tests the setPowerLEDBrightness() method
     *
     * Checks behaviour validity
     *      3 provided levels: BRIGHTNESS_LOW, BRIGHTNESS_NORMAL, BRIGHTNESS_HIGH 
     *      Valid argument
     *      Invalid argument
     *
     * Can manually check to see if LED is functioning correctly
     */
    public void test_setPowerLEDBrightness()
    {
        if (mHUDLedManager != null)
        {
            assertTrue("setPowerLEDBrightness(BRIGHTNESS_LOW) failed", mHUDLedManager.setPowerLEDBrightness(mHUDLedManager.BRIGHTNESS_LOW) == 1);
            SystemClock.sleep(2000);
            assertTrue("setPowerLEDBrightness(BRIGHTNESS_HIGH) failed", mHUDLedManager.setPowerLEDBrightness(mHUDLedManager.BRIGHTNESS_HIGH) == 1);
            SystemClock.sleep(2000);
            assertTrue("setPowerLEDBrightness(BRIGHTNESS_NORMAL) failed", mHUDLedManager.setPowerLEDBrightness(mHUDLedManager.BRIGHTNESS_NORMAL) == 1);
            SystemClock.sleep(2000);

            for(int i = 0; i < 10; i++)
            {
                assertTrue("setPowerLEDBrightness(BRIGHTNESS_LOW) failed", mHUDLedManager.setPowerLEDBrightness(mHUDLedManager.BRIGHTNESS_LOW) == 1);
                assertTrue("setPowerLEDBrightness(BRIGHTNESS_HIGH) failed", mHUDLedManager.setPowerLEDBrightness(mHUDLedManager.BRIGHTNESS_HIGH) == 1);
                assertTrue("setPowerLEDBrightness(BRIGHTNESS_NORMAL) failed", mHUDLedManager.setPowerLEDBrightness(mHUDLedManager.BRIGHTNESS_NORMAL) == 1);
            }
            for(int i =0; i < 255; i++)
            {
                assertTrue("setPowerLEDBrightness(" + i + ")->invalid argument not handled", mHUDLedManager.setPowerLEDBrightness(i) == 1);
            }

            assertFalse("setPowerLEDBrightness(4598)->invalid argument not handled", mHUDLedManager.setPowerLEDBrightness(4598) == 1);
            for(int i = 256; i < 50256; i += 5000)
            {
                assertFalse("setPowerLEDBrightness(" + i + ")->invalid argument not handled", mHUDLedManager.setPowerLEDBrightness(i) == 1);
            }
            for(int i = -1; i > -50256; i -= 5000)
            {
                assertFalse("setPowerLEDBrightness(" + i + ")->invalid argument not handled", mHUDLedManager.setPowerLEDBrightness(i) == 1);
            }
        }
    }
}
