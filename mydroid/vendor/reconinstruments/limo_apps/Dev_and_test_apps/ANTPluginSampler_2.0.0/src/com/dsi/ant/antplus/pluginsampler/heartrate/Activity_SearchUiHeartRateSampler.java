/*
This software is subject to the license described in the License.txt file 
included with this software distribution. You may not use this file except in compliance 
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
*/

package com.dsi.ant.antplus.pluginsampler.heartrate;

import android.os.Bundle;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;

/**
 * Requests access to the heart rate using the plugin automatic search activity.
 */
public class Activity_SearchUiHeartRateSampler extends Activity_HeartRateDisplayBase
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        showDataDisplay("Connecting...");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void requestAccessToPcc()
    {
        AntPlusHeartRatePcc.requestAccess(this, this, base_IPluginAccessResultReceiver, base_IDeviceStateChangeReceiver);
    }
}
