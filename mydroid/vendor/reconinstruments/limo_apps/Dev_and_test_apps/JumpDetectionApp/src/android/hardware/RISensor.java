/*
 * Copyright (C) 2008 The Android Open Source Project
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


package android.hardware;

/* RECON Specific Sensor Type Definition */
public class RISensor
{
   /** 
     * A constant describing RECON Jump Sensor Type
     */
    public static final int RI_SENSOR_TYPE_JUMP = 17;

   /**
     * A constant describing RECON Synchronized Sensor Type
     */
    public static final int RI_SENSOR_TYPE_SYNCHRONIZED = 18; 

    /* Synchronized Event Offsets */
    public static final int RI_SYNCHRONIZED_OFFSET_ACCEL_X           = 0;
    public static final int RI_SYNCHRONIZED_OFFSET_ACCEL_Y           = 1;
    public static final int RI_SYNCHRONIZED_OFFSET_ACCEL_Z           = 2;
    
    public static final int RI_SYNCHRONIZED_OFFSET_MAGNETIC_X        = 3;
    public static final int RI_SYNCHRONIZED_OFFSET_MAGNETIC_Y        = 4;
    public static final int RI_SYNCHRONIZED_OFFSET_MAGNETIC_Z        = 5;
    
    public static final int RI_SYNCHRONIZED_OFFSET_ORIENTATION_X     = 6;
    public static final int RI_SYNCHRONIZED_OFFSET_ORIENTATION_Y     = 7;
    public static final int RI_SYNCHRONIZED_OFFSET_ORIENTATION_Z     = 8;
    
    public static final int RI_SYNCHRONIZED_OFFSET_GYROSCOPE_X       = 9;
    public static final int RI_SYNCHRONIZED_OFFSET_GYROSCOPE_Y       = 10;
    public static final int RI_SYNCHRONIZED_OFFSET_GYROSCOPE_Z       = 11;
    
    public static final int RI_SYNCHRONIZED_OFFSET_LIGHT             = 12;
    public static final int RI_SYNCHRONIZED_OFFSET_PRESSURE          = 13;
    public static final int RI_SYNCHRONIZED_OFFSET_TEMPERATURE       = 14;
    public static final int RI_SYNCHRONIZED_OFFSET_PROXIMITY         = 15;
}