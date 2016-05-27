/*
 * Copyright (C) 2014 Recon Instruments
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
#ifndef __APDS_H
#define __APDS_H

#define APDS_ATIME_MIN                      0xFF
#define APDS_ATIME_50MS                     0xED        //reduce 50/60Hz ripple
#define APDS_ATIME_100MS                    0xDB        // Application note uses this for calibration
#define APDS_ATIME_MAX_COUNT                0xC0        // Full scale integration time

//WTIME register settings, 2's compliment number
//Dependent on WLONG bit, if set 12x longer
//Reg Value	Wall Time   Time(WLONG=0)   Time(WLONG=1)
// 0xFF     1       2.72 ms     0.32 s
// 0xB6     74      201.29 ms   2.37 s
// 0x00     256     696.32 ms   8.19 s
#define POLLING_CALIBRATION_WTIME           0xF7        //experimental polling configutation
#define APDS_WTIME_25MS                     0xF7        //EE is 20.4 Hz	 F9 is 19.04  ms , F7 is 24.48
#define APDS_WTIME_19MS                     0xF9        //EE is 20.4 Hz	 F9 is 19.04  ms , F7 is 24.48
#define APDS_WTIME_1S                       0xE0        //E0 is ~1s when WLONG = 1, used for face detection early testing
#define APDS_WTIME_4S                       0x82        //82 is ~4s when WLONG = 1, used for face detection
#define APDS_WTIME_PICTURES                 0xFF        //Set minimum wait time for pictures

#define APDS_PTIME                          0xFF        //reserved

// IRLED pulse counts per cycle for different states
#define APDS_PPCOUNT_MIN                    0x08        //Min PPCOUNT for calibration
#define APDS_PPCOUNT_MAX                    0x42        //Max PPCOUNT 65 for calibration
#define APDS_PPCOUNT_FACE_DARK              0x23        //PPCOUNT for face detection, was 0x12, 0x23 for dark lens
#define APDS_PPCOUNT_FACE_POLARIZED         0x12        //PPCOUNT for face detection on polarized lens
#define APDS_PPCOUNT_FACE_POLARIZED_LIGHT   0x0A        //PPCOUNT for face detection on polarized light coloured lens
#define APDS_PPCOUNT_PICTURES               0xFF        //PPCOUNT for images of the face

//IRLED drive current
#define APDS_PDRIVE_100                     0x00        //0x00 = 100mA
#define APDS_PDRIVE_50                      0x40        //0x40 = 50 mA
#define APDS_PDRIVE_25                      0x80        //0x80 = 25 mA
#define APDS_PDRIVE_12_5                    0xC0        //0xC0 = 12.5mA

#define APDS_PDIODE                         0x20        //reserved
#define APDS_PGAIN                          0x00        //reserved

//ALS gains
#define APDS_AGAIN_1X                       0x00
#define APDS_AGAIN_8X                       0x01
#define APDS_AGAIN_16X                      0x02
#define APDS_AGAIN_120X                     0x03

//Wait time type, if WLONG set then the wait time is 12x longer than set in the WTIME register
#define APDS_WLONG                          0x02
#define APDS_WSHORT                         0x00

//Enable register values to enable/disable various aspects
//Proximity Interupt enable bit
#define APDS_PIEN_ON                        0x20
#define APDS_PIEN_OFF                       0x00
//ALS Interupt enable bit
#define APDS_AIEN_ON                        0x10
#define APDS_AIEN_OFF                       0x00
//Wait enable bit
#define APDS_WEN_ON                         0x08
#define APDS_WEN_OFF                        0x00
//Proximity enable bit
#define APDS_PEN_ON                         0x04
#define APDS_PEN_OFF                        0x00
//ALS enable bit
#define APDS_AEN_ON                         0x02
#define APDS_AEN_OFF                        0x00
//Active chip bit
#define APDS_PON                            0x01
#define APDS_POFF                           0x00

//Initial values of the proximity interupt thresholds and persistance for various states
#define APDS_PILTL_INIT                     0x00
#define APDS_PILTH_INIT                     0x00
#define APDS_PIHTL_INIT                     0xF4
#define APDS_PIHTH_INIT                     0x01
#define APDS_PERS_INIT                      0x80

// APDS REGISTER Defines
#define APDS_ENABLE_REG                     0x00
#define APDS_ATIME_REG                      0x01
#define APDS_PTIME_REG                      0x02
#define APDS_WTIME_REG                      0x03
#define APDS_AILTL_REG                      0x04
#define APDS_AILTH_REG                      0x05
#define APDS_AIHTL_REG                      0x06
#define APDS_AIHTH_REG                      0x07
#define APDS_PILTL_REG                      0x08
#define APDS_PILTH_REG                      0x09
#define APDS_PIHTL_REG                      0x0A
#define APDS_PIHTH_REG                      0x0B
#define APDS_PERS_REG                       0x0C
#define APDS_CONFIG_REG                     0x0D
#define APDS_PPCOUNT_REG                    0x0E
#define APDS_CONTROL_REG                    0x0F
#define APDS_REV_REG                        0x11
#define APDS_ID_REG                         0x12
#define APDS_STATUS_REG                     0x13
#define APDS_CDATAL_REG                     0x14
#define APDS_CDATAH_REG                     0x15
#define APDS_IRDATAL_REG                    0x16
#define APDS_IRDATAH_REG                    0x17
#define APDS_PDATAL_REG                     0x18
#define APDS_PDATAH_REG                     0x19

// APDS Special Funcitons
#define APDS_SF_NORMAL                      0x00        //No special function
#define APDS_PS_CLR                         0x05        //Proximity Interupt Clear
#define APDS_ALS_CLR                        0x06        //ALS Interupt Clear
#define APDS_PS_ALS_CLR                     0x07        //Prox and ALS Interupt Clear

#endif
