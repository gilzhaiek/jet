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

package reconinstruments.HUDPowerManager.cts;

import com.reconinstruments.os.hardware.power.HUDPowerManager;
import com.reconinstruments.os.HUDOS;

import android.test.AndroidTestCase;

/**
 * Junit / Instrumentation test case for HUDPowerManager API
 *
 * Tests and validate the behaviour of the HUDPowerManager library
 */
public class PowerManagerTest extends AndroidTestCase {
    // Control Constant defining the loop amount of each functionality test
    private static final int CONTROL = 50;

    // HUDPowerManager instance
    private HUDPowerManager mHUDPowerManager = null;

    /**
     * The very first function called in the CTS test
     *
     * Obtained a single HUDPowerManager instance for setup purposes
     */
    public void setUp() throws Exception 
    {
        super.setUp();

        mHUDPowerManager = (HUDPowerManager) HUDOS.getHUDService(HUDOS.HUD_POWER_SERVICE);
    }

    /**
     * Tests the getInstance() method
     *
     * Checks for null and type validity
     */
    public void test_getInstance() 
    {
        assertNotNull(mHUDPowerManager);
        assertTrue(mHUDPowerManager instanceof HUDPowerManager);

        for(int i = 0; i < CONTROL; i++)
        {
            mHUDPowerManager = (HUDPowerManager) HUDOS.getHUDService(HUDOS.HUD_POWER_SERVICE);
            assertNotNull(mHUDPowerManager);
            assertTrue(mHUDPowerManager instanceof HUDPowerManager);

            mHUDPowerManager = (HUDPowerManager) HUDOS.getHUDService(-i);
            assertNull(mHUDPowerManager);
        }
    }

    /**
     * Tests the constant variables
     *
     * Checks for value validity
     *
     * Might have to be changed after Javadoc hidden update
     */
    public void test_const_vars()
    {
        assertEquals("var EXTRA_REASON != reason", mHUDPowerManager.EXTRA_REASON, "reason");
        assertEquals("var EXTRA_REASON_STR != reason_str", mHUDPowerManager.EXTRA_REASON_STR, "reason_str");
        assertEquals("var SHUTDOWN_GRACEFUL != 0", mHUDPowerManager.SHUTDOWN_GRACEFUL, 0);
        assertEquals("var SHOTDOWN_ABRUPT != 1", mHUDPowerManager.SHUTDOWN_ABRUPT, 1);
        assertEquals("var SHUTDOWN_BATT_REMOVED != 2", mHUDPowerManager.SHUTDOWN_BATT_REMOVED, 2);
        assertEquals("var INSTANT_CURRENT_UPDATE_INTERVAL != 0", mHUDPowerManager.INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC, 0);
        assertEquals("var AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC != 1", mHUDPowerManager.AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC, 1);
        assertEquals("var BAD_VALUE != -2147483648", mHUDPowerManager.BAD_VALUE, -2147483648);
        assertEquals("var FREQ_SCALING_GOV_HOTPLUG != 0", mHUDPowerManager.FREQ_SCALING_GOV_HOTPLUG, 0);
        assertEquals("var FREQ_SCALING_GOV_INTERACTIVE != 1", mHUDPowerManager.FREQ_SCALING_GOV_INTERACTIVE, 1);
        assertEquals("var FREQ_SCALING_GOV_CONSERVATIVE != 2", mHUDPowerManager.FREQ_SCALING_GOV_CONSERVATIVE, 2);
        assertEquals("var FREQ_SCALING_GOV_USERSPACE != 3", mHUDPowerManager.FREQ_SCALING_GOV_USERSPACE, 3);
        assertEquals("var FREQ_SCALING_GOV_POWERSAVE != 4", mHUDPowerManager.FREQ_SCALING_GOV_POWERSAVE, 4);
        assertEquals("var FREQ_SCALING_GOV_ONDEMAND != 5", mHUDPowerManager.FREQ_SCALING_GOV_ONDEMAND, 5);
        assertEquals("var FREQ_SCALING_GOV_PERFORMANCE != 6", mHUDPowerManager.FREQ_SCALING_GOV_PERFORMANCE, 6);
    }

    /**
     * Tests the getBatteryPercentage() method
     *
     * Checks for value validity:
     *      Range: 0% to 100%
     */
    public void test_getBatteryPercentage() 
    {
        if (mHUDPowerManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                int battery = mHUDPowerManager.getBatteryPercentage();
                assertTrue("Battery Percentage out of bound: " + battery, battery >= 0 && battery <= 100);
            }
        }
    }

    /**
     * Tests the getBatteryTemperature_C() method
     *
     * Checks for value validity:
     *      Range: 15C to 85C
     */
    public void test_getBatteryTemperature_C()
    {
        if (mHUDPowerManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                int tempC = mHUDPowerManager.getBatteryTemperature_C();
                assertTrue("Battery TemperatureC out of bound: " + tempC, tempC >= 15 && tempC <= 85);
            }
        }
    }

    /**
     * Tests the getLastShutdownReason() method
     *
     * Checks for value validity:
     *      Range: 0 to 2
     */
    public void test_getLastShutdownReason()
    {
        if (mHUDPowerManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                int reason = mHUDPowerManager.getLastShutdownReason();
                assertTrue("Shutdown Reason invalid return: " + reason, reason == mHUDPowerManager.SHUTDOWN_GRACEFUL || 
                        reason == mHUDPowerManager.SHUTDOWN_ABRUPT || reason == mHUDPowerManager.SHUTDOWN_BATT_REMOVED);
            }
        }
    }

    /**
     * Tests the setCompassTemperature() method
     *
     * Checks for value validity:
     *      Returns 1 at all times
     */
    public void test_setCompassTemperature()
    {
        if (mHUDPowerManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                assertTrue("setCompassTemperature() not hidden", mHUDPowerManager.setCompassTemperature(true) == 1);
                assertTrue("setCompassTemperature() not hidden", mHUDPowerManager.setCompassTemperature(false) == 1);
            }
        }
    }

    /**
     * Tests the getCompassTemperature() method
     *
     * Checks for value validity:
     *      Returns !BAD_VALUE at all times
     */
    public void test_getCompassTemperature()
    {
        if (mHUDPowerManager != null)
        {
            if (mHUDPowerManager.setCompassTemperature(true) == 1)
            {
                for(int i = 0; i < CONTROL; i++)
                {
                    int compassTempC = mHUDPowerManager.getCompassTemperature();
                    assertFalse("getCompassTemperature not hidden", compassTempC == mHUDPowerManager.BAD_VALUE || compassTempC == mHUDPowerManager.BAD_VALUE+1);
                }
            }
        }
    }

    /**
     * Tests the getBoardTemperature() method
     *
     * Checks for value validity:
     *      Range: 35C to 85C
     */
    public void test_getBoardTemperature()
    {
        if (mHUDPowerManager != null)
        {
            for(int i = 0; i < CONTROL; i++)
            {
                int boardTempC = mHUDPowerManager.getBoardTemperature();
                assertTrue("Board Temperature out of bound: " + boardTempC, boardTempC >= 35 && boardTempC <= 85);
            }
        }
    }

    /**
     * Tests the setFreqScalingGovernor() method
     *
     * Checks for value validity:
     *      Returns -1 at all times
     *
     * Might have to be changed after Javadoc hidden update
     */
    public void test_setFreqScalingGovernor()
    {
        if (mHUDPowerManager != null)
        {
            assertFalse("setFreqScalingGovernor(HOTPLUG) not hidden", mHUDPowerManager.setFreqScalingGovernor(mHUDPowerManager.FREQ_SCALING_GOV_HOTPLUG) == 1);
            assertFalse("setFreqScalingGovernor(INTERACTIVE) not hidden", mHUDPowerManager.setFreqScalingGovernor(mHUDPowerManager.FREQ_SCALING_GOV_INTERACTIVE) == 1);
            assertFalse("setFreqScalingGovernor(CONSERVATIVE) not hidden", mHUDPowerManager.setFreqScalingGovernor(mHUDPowerManager.FREQ_SCALING_GOV_CONSERVATIVE) == 1);
            assertFalse("setFreqScalingGovernor(USERSPACE) not hidden", mHUDPowerManager.setFreqScalingGovernor(mHUDPowerManager.FREQ_SCALING_GOV_USERSPACE) == 1);
            assertFalse("setFreqScalingGovernor(POWERSAVE) not hidden", mHUDPowerManager.setFreqScalingGovernor(mHUDPowerManager.FREQ_SCALING_GOV_POWERSAVE) == 1);
            assertFalse("setFreqScalingGovernor(ONDEMAND) not hidden", mHUDPowerManager.setFreqScalingGovernor(mHUDPowerManager.FREQ_SCALING_GOV_ONDEMAND) == 1);
            assertFalse("setFreqScalingGovernor(PERFORMANCE) not hidden", mHUDPowerManager.setFreqScalingGovernor(mHUDPowerManager.FREQ_SCALING_GOV_PERFORMANCE) == 1);
        }

        for(int i = 500; i < 500+CONTROL; i++)
        {
            assertFalse("setFreqScalingGovernor invalid input passed: " + i, mHUDPowerManager.setFreqScalingGovernor(i) == 1);
        }
        for(int i = -500; i > -500-CONTROL; i--)
        {
            assertFalse("setFreqScalingGovernor invalid input passed: " + i, mHUDPowerManager.setFreqScalingGovernor(i) == 1);
        }
    }

    /**
     * Tests the getCurrent() method
     *
     * Checks for value validity:
     *      Range: -40mA to 350mA
     *      Invalid Input: INTMIN or INTMIN+1
     */
    public void test_getCurrent()
    {
        if (mHUDPowerManager != null)
        {
            int avgCurrent = mHUDPowerManager.getCurrent(mHUDPowerManager.AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC);
            assertTrue("Average Current out of bound: " + avgCurrent, avgCurrent >= -40 && avgCurrent <= 350);
            for(int i = 0; i < CONTROL; i++)
            {
                assertTrue("Average Current out of bound: " + avgCurrent, avgCurrent >= -40 && avgCurrent <= 350);
            }

            int instantCurrent = mHUDPowerManager.getCurrent(mHUDPowerManager.INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC);
            assertTrue("Instant Current out of bound: " + instantCurrent, instantCurrent >= -40 && avgCurrent <= 350);
            for(int i = 0; i < CONTROL; i++)
            {
                assertTrue("Instant Current out of bound: " + instantCurrent, instantCurrent >= -40 && avgCurrent <= 350);
            }

            int invalidTest = mHUDPowerManager.getCurrent(10011);
            assertTrue("getCurrent(105) invalid argument not handled: " + invalidTest, invalidTest == mHUDPowerManager.BAD_VALUE || invalidTest == mHUDPowerManager.BAD_VALUE+1);
            invalidTest = mHUDPowerManager.getCurrent(-5001);
            assertTrue("getCurrent(-5001) invalid argument not handled: " + invalidTest, invalidTest == mHUDPowerManager.BAD_VALUE || invalidTest == mHUDPowerManager.BAD_VALUE+1);

            for(int i = 2; i < 2+CONTROL; i++)
            {
                invalidTest = mHUDPowerManager.getCurrent(i);
                assertTrue("getCurrent(" + i + ")->invalide argument not handled", invalidTest == mHUDPowerManager.BAD_VALUE || invalidTest == mHUDPowerManager.BAD_VALUE + 1);
            }
            for(int i = -1; i > -1-CONTROL; i--)
            {
                invalidTest = mHUDPowerManager.getCurrent(i);
                assertTrue("getCurrent(" + i + ")->invalide argument not handled", invalidTest == mHUDPowerManager.BAD_VALUE || invalidTest == mHUDPowerManager.BAD_VALUE + 1);
            }
        }
    }

    /**
     * Tests the getBatteryVoltage() method
     *
     * Checks for value validity:
     *      Range: 3.2V to 4.2V
     */
    public void test_getBatteryVoltage()
    {
        if (mHUDPowerManager != null)
        {
            int batteryVolt = mHUDPowerManager.getBatteryVoltage();
            assertTrue("Battery Voltage out of bound: " + batteryVolt + "mV", batteryVolt >= 3200 && batteryVolt <= 4200);
        }
    }
}
