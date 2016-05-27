/******************************************************************************\
##                                                                            *
## Unpublished Proprietary and Confidential Information of Texas Instruments  *
## Israel Ltd. Do Not Disclose.                                               *
## Copyright 2008 Texas Instruments Israel Ltd.                               *
## All rights reserved. All unpublished rights reserved.                      *
##                                                                            *
## No part of this work may be used or reproduced in any form or by any       *
## means, or stored in a database or retrieval system, without prior written  *
## permission of Texas Instruments Israel Ltd. or its parent company Texas    *
## Instruments Incorporated.                                                  *
## Use of this work is subject to a license from Texas Instruments Israel     *
## Ltd. or its parent company Texas Instruments Incorporated.                 *
##                                                                            *
## This work contains Texas Instruments Israel Ltd. confidential and          *
## proprietary information which is protected by copyright, trade secret,     *
## trademark and other intellectual property rights.                          *
##                                                                            *
## The United States, Israel  and other countries maintain controls on the    *
## export and/or import of cryptographic items and technology. Unless prior   *
## authorization is obtained from the U.S. Department of Commerce and the     *
## Israeli Government, you shall not export, reexport, or release, directly   *
## or indirectly, any technology, software, or software source code received  *
## from Texas Instruments Incorporated (TI) or Texas Instruments Israel,      *
## or export, directly or indirectly, any direct product of such technology,  *
## software, or software source code to any destination or country to which   *
## the export, reexport or release of the technology, software, software      *
## source code, or direct product is prohibited by the EAR. The subject items *
## are classified as encryption items under Part 740.17 of the Commerce       *
## Control List (“CCL”). The assurances provided for herein are furnished in  *
## compliance with the specific encryption controls set forth in Part 740.17  *
## of the EAR -Encryption Commodities and Software (ENC).                     *
##                                                                            *
## NOTE: THE TRANSFER OF THE TECHNICAL INFORMATION IS BEING MADE UNDER AN     *
## EXPORT LICENSE ISSUED BY THE ISRAELI GOVERNMENT AND THE APPLICABLE EXPORT  *
## LICENSE DOES NOT ALLOW THE TECHNICAL INFORMATION TO BE USED FOR THE        *
## MODIFICATION OF THE BT ENCRYPTION OR THE DEVELOPMENT OF ANY NEW ENCRYPTION.*
## UNDER THE ISRAELI GOVERNMENT'S EXPORT LICENSE, THE INFORMATION CAN BE USED *
## FOR THE INTERNAL DESIGN AND MANUFACTURE OF TI PRODUCTS THAT WILL CONTAIN   *
## THE BT IC.                                                                 *
##                                                                            *
\******************************************************************************/


/** \file   Navc_ini_file_def.h
 *  \brief  NAVC API file specifies NAVC commnads and events supported by
 *   NAVC stack.
 *
 *
 */

#ifndef __NAVC_INI_FILE_H__
#define __NAVC_INI_FILE_H__


/************************************************************************
 * Defines
 ************************************************************************/
#define GPSC_MAX_COMPATIBLE_VERSIONS   (0x4)


/************************************************************************
 * Types
 ************************************************************************/
/*
 * GPSC configuration file contents
 * CCDGEN:WriteStruct_Count==28
 */
typedef enum
{
  GPSC_FALSE                     = 0x0,           /* GPSC False                     */
  GPSC_TRUE                      = 0x1,           /* GPSC True                      */
  GPSC_ROM_PATCH                 = 0x2,           /* For ROM down load              */
  GPSC_STACKRAM_PATCH            = 0x3            /* For Stack RAM down load        */
}T_GPSC_patch_available;

/*
 * Comm config UART
 */
typedef enum
{
  GPSC_BAUD_1200                 = 0x0,           /* Baud Rate 1200 bits/sec        */
  GPSC_BAUD_2400                 = 0x1,           /* Baud Rate 2400 bits/sec        */
  GPSC_BAUD_4800                 = 0x2,           /* Baud Rate 4800 bits/sec        */
  GPSC_BAUD_9600                 = 0x3,           /* Baud Rate 9600 bits/sec        */
  GPSC_BAUD_14400                = 0x4,           /* Baud Rate 14400 bits/sec       */
  GPSC_BAUD_19600                = 0x5,           /* Baud Rate 19600 bits/sec       */
  GPSC_BAUD_38400                = 0x6,           /* Baud Rate 38400 bits/sec       */
  GPSC_BAUD_57600                = 0x7,           /* Baud Rate 57600 bits/sec       */
  GPSC_BAUD_115200               = 0x8,           /* Baud Rate 115200 bits/sec      */
  GPSC_BAUD_230400               = 0x9            /* Baud Rate 230400 bits/sec      */
}T_GPSC_uart_baud_rate;

typedef struct
{
  McpU32  uart_baud_rate;   /* T_GPSC_uart_baud_rate UART baud rate */
} T_GPSC_comm_config_uart;

/*
 * Comm config I2C
 */
typedef enum
{
  GPSC_I2C_RATE_100KHZ           = 0x0,           /* I2C data transfer rate 100KHz  */
  GPSC_I2C_RATE_400KHZ           = 0x1            /* I2C data transfer rate 400KHz  */
}T_GPSC_i2c_data_rate;

typedef enum
{
  GPSC_I2C_7BIT_ADDRESS          = 0x0,           /* 7-Bit addressing mode          */
  GPSC_I2C_10BIT_ADDRESS         = 0x1            /* 10-Bit addressing mode         */
}T_GPSC_i2c_ce_address_mode;
typedef T_GPSC_i2c_ce_address_mode T_GPSC_i2c_gps_address_mode;


typedef struct
{

  McpU32  i2c_data_rate;            /*<  0:  4> (enum=32bit)<->T_GPSC_i2c_data_rate I2C transmission rate */
  McpU8   i2c_logicalid;            /*<  4:  1> I2C logical device ID                              */
  McpU8   zzz_align0;               /*<  5:  1> alignment                                          */
  McpU8   zzz_align1;               /*<  6:  1> alignment                                          */
  McpU8   zzz_align2;               /*<  7:  1> alignment                                          */
  McpU32  i2c_ce_address_mode;     /*<  8:  4> (enum=32bit)<->T_GPSC_i2c_ce_address_mode I2C CE addressing mode */
  McpU16  i2c_ce_address;           /*< 12:  2> I2C CE address                                     */
  McpU8   zzz_align3;               /*< 14:  1> alignment                                          */
  McpU8   zzz_align4;               /*< 15:  1> alignment                                          */
  McpU32  i2c_gps_address_mode;   /*< 16:  4> (enum=32bit)<->T_GPSC_i2c_gps_address_mode I2C GPS addressing mode */
  McpU16  i2c_gps_address;          /*< 20:  2> I2C CE address                                     */
  McpU8   zzz_align5;               /*< 22:  1> alignment                                          */
  McpU8   zzz_align6;               /*< 23:  1> alignment                                          */
  McpU8  i2c_mode_gps_i2c_addr;
  McpU8  i2c_gps_opr_mode;
  McpU8  i2c_slave_transfer;
  McpU8	  i2c_max_msg_len;
} T_GPSC_comm_config_i2c;


/*
 * Comm config_union
 */
typedef union
{
  T_GPSC_comm_config_uart   comm_config_uart;         /*<  0:  4> Comm config UART                                   */
  T_GPSC_comm_config_i2c    comm_config_i2c;          /*<  0: 24> Comm config I2C                                    */
} T_GPSC_comm_config_union;

/*
 * Communication configuration
 */
typedef enum
{
  GPSC_COMM_MODE_UART            = 0x0,
  GPSC_COMM_MODE_I2C             = 0x1
}T_GPSC_ctrl_comm_config_union;

typedef struct
{
  McpU32                    ctrl_comm_config_union;	 /* controller for union  */
  T_GPSC_comm_config_union  comm_config_union;       /* Comm config_union     */
} T_GPSC_comm_config;

/*
 * enum to Variable altitude_hold_mode
 * Altitude hold mode
 */
typedef enum
{
  GPSC_ALT_HOLD_MANUAL_3D        = 0x0,           /* Manual 3D                      */
  GPSC_ALT_HOLD_MANUAL_2D        = 0x1,           /* Manual 2D                      */
  GPSC_ALT_HOLD_AUTO             = 0x2,           /* Auto                           */
  GPSC_ALT_HOLD_FILTERED         = 0x3,           /* Filtered                       */
  GPSC_ALT_HOLD_ALLOW_2D         = 0x4            /* Allow 2D                       */
}T_GPSC_altitude_hold_mode;

/*
 * enum to Variable timestamp_edge
 * Timestamp signal registration edge
 */
typedef enum
{
  GPSC_NEGATIVE_EDGE             = 0x0,           /* Use negative edge of Timestamp signal */
  GPSC_POSITIVE_EDGE             = 0x1            /* Use positive edge of Timestamp signal */
}T_GPSC_timestamp_edge;

/*
 * enum to Variable pa_blank_polarity
 * PA blanking signal active polarity
 */
typedef enum
{
  GPSC_ACTIVE_HIGH               = 0x0,           /* PA blanking signal active high */
  GPSC_ACTIVE_LOW                = 0x1            /* PA blanking signal active low  */
}T_GPSC_pa_blank_polarity;

/*
 * enum to Variable apm_control
 * Advance Power Management Feature Activation
 */
typedef enum
{
  GPSC_APM_DISABLE               = 0x0,           /* Advance Power Management feature option Disabled (Default) */
  GPSC_APM_ENABLE                = 0x1            /* Advance Power Management feature option Enabled */
}T_GPSC_apm_control;

/*
 * enum to Variable search_mode
 * Advance Power Management search mode Activation
 */
typedef enum
{
  GPSC_APM_SEARCH_MODE_CONT_TRACKING = 0x0,       /* APM Search Mode Continuous Tracking. Recommended for use when update rates are less than 10 sec. */
  GPSC_APM_SEARCH_MODE_RAPID_REACQ = 0x1,         /* APM Search Mode Rapid Reaquisition. Recommended for longer update rates. */
  GPSC_APM_SEARCH_MODE_AUTO      = 0x2            /* APM Search Mode Auto. Receiver automatically selects between continuous tracking mode and rapid reacquisition. */
}T_GPSC_search_mode;

/*
 * enum to Variable saving_options
 * Advance Power Management saving option. Power save state to enter while no active acquisition is ongoing.
 */
typedef enum
{
  GPSC_APM_SAVING_OPT_IDLE_EQ_STATE = 0x0,        /* APM Saving option idle equivalent state. (Default).(For clock configuration-1,2,3,4) */
  GPSC_APM_SAVING_OPT_SLEEP_EQ_STATE = 0x1        /* APM Saving option sleep equivalent state. (For Clock configuration-1, 2, 3) */
}T_GPSC_saving_options;


/*
 * enum to Variable sbas_control
 * SBAS Activation.
 */
typedef enum
{
  GPSC_SBAS_DISABLE              = 0x0,           /* Sbas  Disable (Default)        */
  GPSC_SBAS_ENABLE               = 0x1            /* Sbas  Enable                   */
}T_GPSC_sbas_control;

enum
{
	GPSC_CFG_INJECTED_TIME		= 0x0,	/* use injected time */
	GPSC_CFG_SYSTEM_TIME 		=0x1		/* use system time */
};

typedef struct
{
  McpU32                    driver_tx_response_required; /* T_GPSC_patch_available Driver transmission response required Boolean values */
  McpU32                    ai2_ack_required;            /* T_GPSC_patch_available AI2 acknowledge required Boolean values */
  McpU32                    auto_power_save_enable;      /* T_GPSC_patch_available Automatic GPS powersave request Boolean values */
  McpU32                    auto_power_ready_enable;     /* T_GPSC_patch_available Automatic GPS ready request Boolean values */
  McpU32                    power_mgmt_enable;           /* T_GPSC_patch_available Enable power management */
  McpU32                    driver_tx_timeout;           /* Driver transmission response required, msec        */
  McpU32                    ai2_comm_timeout;            /* AI2 acknowledge required, msec                     */
  McpU32                    auto_power_save_timeout;     /* Automatic GPS powersave request, msec              */
  McpU32                    internal_1;                  /* Internal Variable, should be set as TBD            */
  McpU32                    internal_2;                  /* Internal Variable, should be set as TBD            */
  McpU32                    internal_3;                  /* Internal Variable, should be set as TBD            */
  McpU32                    internal_4;                  /* Internal Variable, should be set as TBD            */
  McpU32                    smlc_comm_timeout;           /* Automatic GPS ready request, msec                  */
  McpU32                    sleep_entry_delay_timeout;   /* Sleep entry delay, msec                            */
  McpU32                    default_max_ttff;            /* GSPC default internal max TTFF, msec               */
  McpU32                    patch_available;             /* T_GPSC_patch_available Patch available Boolean values */
  T_GPSC_comm_config        comm_config;                 /* Communication configuration                        */
  McpU16                    assist_bitmap_msbased_mandatory_mask;    /* GPSC configuration for mandatory assistance data for msbased mode */
  McpU16                    assist_bitmap_msassisted_mandatory_mask; /* GPSC configuration for mandatory assistance data for msassisted mode */
  McpU32                    compatible_gps_versions[GPSC_MAX_COMPATIBLE_VERSIONS]; /* GPSC/GPS compatibility                             */
  McpU32                    ref_clock_frequency;      /* Reference clock frequency0 to 4294967295 Hz, resolution 1 Hz per bit. */
  McpU16                    ref_clock_quality;        /* Reference clock quality, 0.00 to 655.35 ppm, resolution 0.01 ppm per bit. */
  McpU16                    max_clock_acceleration;   /* Maximum reference clock acceleration, unsigned, 1 mm/sec^2 per bit, 0 to 65535 mm/sec^2 */
  McpU16                    max_user_acceleration;    /* Unsigned, 1 mm/sec^2 per bit, 0 to 65535 mm/sec^2  */
  McpU8                     zzz_align0;               /* alignment                                          */
  McpU8                     zzz_align1;               /* alignment                                          */
  McpU32                    altitude_hold_mode;       /* T_GPSC_altitude_hold_mode Altitude hold mode */
  McpS8                     elevation_mask;           /* Elevation mask, signed, 90/2^7 degrees per bit, range -90 to 90 - 90/2^7 degrees */
  McpU8                     pdop_mask;                /* PDOP mask                                          */
  McpU8                     zzz_align2;               /* alignment                                          */
  McpU8                     zzz_align3;               /* alignment                                          */
  McpU32                    timestamp_edge;           /* T_GPSC_timestamp_edge Timestamp signal registration edge */
  McpU32                    pa_blank_enable;          /* T_GPSC_patch_available PA blanking enable Boolean values */
  McpU32                    pa_blank_polarity;        /* T_GPSC_pa_blank_polarity PA blanking signal active polarity */
  McpU16                    gps_minimum_week;         /* GPS minimum week                                   */
  McpU8                     utc_leap_seconds;         /* UTC leap seconds                                   */
  McpU8                     zzz_align4;               /* alignment                                          */
  McpU16                    diag_report_control;      /* Diagnostic Report Control                          */
  McpU8                     front_end_loss;           /* RF Front End Loss .1dB per bit                     */
  McpU8                     kalman_feat_control;      /* Kalman feature control enable or disable bit.0 : Enable Kalman filter feature, 1: Disable Kalman filter feature. */
  McpU8                     checksum;                 /* Config file checksum                               */
  McpU8                     zzz_align5;               /* alignment                                          */
  McpU8                     zzz_align6;               /* alignment                                          */
  McpU8                     zzz_align7;               /* alignment                                          */
  McpU32                    apm_control;              /* T_GPSC_apm_control Advance Power Management Feature Activation */
  McpU32                    search_mode;              /* T_GPSC_search_mode Advance Power Management search mode Activation */
  McpU32                    saving_options;           /* T_GPSC_saving_options Advance Power Management saving option. Power save state to enter while no active acquisition is ongoing. */
  McpU8                     power_save_qc;            /* Unsigned: 10 msec per bit, Range: 100ms default to 900ms maximum */
  McpU8                     zzz_align8;               /* alignment                                          */
  McpU8                     zzz_align9;               /* alignment                                          */
  McpU8                     zzz_align10;              /* alignment                                          */
  McpU32                    sbas_control;             /* T_GPSC_sbas_control SBAS Activation. */
  McpU32                    sbas_prn;                 /* SBAS prn mask.Bit field 0 - 18 corresponds to SBAS SVs 120 to 138. Bit field 19-31 is reserved and set to 0. By default all bits are set to 0 which corresponds to searching all PRN. */
  McpU8                     sbas_mode;                /* SBAS mode. Set to 0.                               */
  McpU8                     sbas_flags;               /* SBAS flags. Set to 0.                              */
  McpU8                     block_almanac;            /* Block the injection of Almanac to the sensor while doing 3GPP test. Set as  0 to No Block (Default),1 to Block. */
  McpU8                     rx_opmode;                /* Select the receiver operation  mode as accuracy  - 0,combo - 1,speed  - 2 */
  McpU8                     lna_crystal;              /* LNA GPS Crystal control.This is used to configure presence of external LNA and crystal. On power-up GPS engine comes up with EXTN_LDO and crystal core turned ON. Depending on the system configuration (TCXO v/s Crystal, Internal LNA v/s External LNA), CE could use this message to optimize power. Only on receiving this message packet, GPS engine will turn-off unwanted blocks. Bit 0 : Crystal, TCXO control . 1 : TCXO connected. 0 : Crystal connected. Bit 1: LNA configuration. 0 : Internal LNA (Default). 1 : External LNA.Bit 2-7: Reserve */
  McpU8                     enable_timeout;           /* To enable or disable Timeout and CEP info;Bit1: Enable, Bit0 : Disable */
  McpU16                    timeout1;                 /* Unsigned, Scale: 0.5s per bit;In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 20. */
  McpU16                    timeout2;                 /* Unsigned, Scale: 0.5s per bit , In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 12. */
  McpU16                    accuracy1;                /* Unsigned, Scale: 1m per bit , 2D position accuracy */
  McpU16                    accuracy2;                /* Unsigned, Scale: 1m per bit , 2D position accuracy */
  McpU8                     autonomous_test_flag;     /* Defines the autonomous test flag.Value0:Cold Start,Value1:Hot Start,Value2:Warm Start. */
  McpU8                     rti_enable;               /* Enable and Disable RTI feature                     */
  McpU8                     data_wipe;                /* Data wipe feature enable or disable bit 0 ; Data wipe off feature disabled (Default), 1: Data wipe off feature enabled */
  McpU8                     dll;                      /* 0: Disable DLL based tracking (Default in NL5500 PG-1.0)1: Enable DLL based tracking */
  McpU8                     carrier_phase;            /* 0: Disable carrier phase measurement (Default in NL5500 PG-1.0)1: Enable carrier phase measurement */
  McpU8                     hw_req_opt;               /* Specifies host request signaling options for communication.Bit0: 0: Host request feature disabled 1: Host request feature enabled (Default);Bit 7:1: Reserved */
  McpU16                    hw_assert_delay;          /* Minimum delay between host request signal (GPS_IRQ) assertion to commencement of transmission from GE. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms) */
  McpU16                    hw_reassert_delay;        /* Minimum delay between host request (GPS_IRQ) signal de-assertion to re-assertion. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms) */
  McpU8                     hw_ref_clk_req_opt;       /* Specifies host reference clock request signaling options for calibration purposes. 0: Disable,1: GPS HW signal controlled calibration,2: GPS SW message controlled calibration */
  McpU8                     hw_ref_clk_req_sig_sel;   /* Selects signal to be used for REF_CLK request. Applicable only for GPS HW signal controlled calibration option.0: Use GPS_IRQ pin for making reference clock requests,1: Use REF_CLK_REQ pin for making reference clock request */
  McpU16                    hw_ref_clk_assert_dly;    /* Minimum delay between host reference clock request signal assertion to availability of clock. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 ssec(Default : 5 ms) */
  McpU16                    hw_ref_clk_reassert_dly;  /* Minimum delay between host reference clock request signal de-assertion to re-assertion for next clock request. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec (Default : 91.5 usec) */
  McpU8                     hw_sigout_type_ctrl;      /* Bit 1: Output type control for Host REF_CLK Request0: Open source signal (Default)1: Push/Pull signal.Bit2: Output type control for TCXO_CLK_REQ signal0: Open source signal (Default)1: Push/Pull signal */
  McpU8                     zzz_align11;              /* alignment                                          */
  McpU16                    tcxo_startup_time;        /* Unsigned, Scale: 1/32768S (=30.517uS) per bit Range - 0 to 65535 Interpreted Value - 30.517uS to (2S-30.517uS)Values outside the range are considered as invalid. Default value is 5ms. */
  McpU8                     gps_sleep_ctrl;           /* Bit 0: Auto Sleep Control Enable Disable,Bit1: Wakeup Source Control */
  McpU8                     geofence_enable;          /* Enable and Disable Geofence feature                */
  McpU16                    recv_min_rep_period;      /* Minimum Report Period to enable and disable  fast TTFF Default: 1000 ms Disable fast TTFF and 500 ms - Enable Fast TTFF */
  McpU8                     count_invalid_fix;        /* Invalid fix count for re-request assistance in case of timeouts */
  McpU8                     low_power_state;          /* low power state of GPSC. ,Bit 0:Sleep state (Default), Bit 1:Idle state */
  McpU16                    calib_period;             /* Calibration Period: Unsigned, 1ms per bit,Range: 0 to 4000 ms */
  McpU16                    period_uncertainity;      /* Period Uncertainty: Unsigned, 1ns per bit,Range: 0 to 65.535 us */
  McpU16                    max_gps_clk_unc;          /* To provide GPS maximum clock uncertainty           */
  McpU8                     zzz_align12;              /* alignment                                          */
  McpU8                     zzz_align13;              /* alignment                                          */
  McpFLT						shrt_term_gps_clk_unc;    /* To provide GPS short term clock uncertainty        */
  //McpU32					shrt_term_gps_clk_unc;    /* To provide GPS short term clock uncertainty        */
  McpU32                    system_time_unc;          /* To provide the System time uncertainty             */
  McpU8                     enable_manualrefclk;      /* To switch between Manual Reference clock and pre-configured Reference clock */
  McpU8                     enable_finetime;          /* To enable fine time injection                      */
  McpS16                    altitude_estimate;        /* Altitude Estimate Scale: 0.5s per bit , range 16384 to 16383.5 meters */
  McpU16                    altitude_unc;             /* Altitude Uncertainty Scale: 0.5s per bit , range 0 to 32767.5 meters */
  McpU8                     ref_clock_calib_type;     /* Reference Clock Calibration Type: 0-xx as defined in T_GPSC_calib_type  */
  McpU8                     zzz_align15;              /* alignment                                          */
  McpU8                     priority_sagps;             /* priority for sagps */
  McpU8                     priority_pgps;             /* priority for pgps */
  McpU8                     priority_supl;              /* priority for supl */
  McpU8                     priority_cplane;            /* priority for cplane */
  McpU8                     priority_custom_agps_provider1;  /* priority for custom provider1 */
  McpU8                     priority_custom_agps_provider2;   /* priority for custom provider2 */
  McpU8			     priority_custom_agps_provider3;   /* priority for custom provider3 */
  McpU16                    pgps_sagps_timeout1;                 /* Unsigned, Scale: 0.5s per bit;In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 20. */
  McpU16                    pgps_sagps_timeout2;                 /* Unsigned, Scale: 0.5s per bit , In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 12. */
  McpU16                    pgps_sagps_accuracy1;                /* Unsigned, Scale: 1m per bit , 2D position accuracy */
  McpU16                    pgps_sagps_accuracy2;                /* Unsigned, Scale: 1m per bit , 2D position accuracy */
  McpU8			     alt_2d_pdop;				/* To provide ALT 2D PDOP        */
  McpFLT			     tcxo_unc_longterm;			/* To provide TCXO Long term clock uncertainty        */
  McpFLT			     tcxo_aging;					/* To provide TCXO Aging        */

  /* New Variables Added*/

  McpU32                    sys_time_sync;
  McpU32                    unc_threshold;
  McpU16                    pos_velocty;
  McpU16                    pos_unc_threshold;
  McpU16                    hours_thrshold;
  McpU16                    week_threshold;
  McpU16					rtc_quality;
  McpU8						rtc_calibration;
  McpU32					prm_adjustment;

  McpU8                     pdop_mask_time_out;                /* PDOP mask Time Out */

  McpU32                    pps_output;
  McpU32					pps_polarity;
  McpU32					time_injection;

  McpU8						enable_rtc_time_injection;
  
  McpU8						time_inj_check;
  McpU8						time_validate_check;

  McpU8						pos_inject_check;
  McpU8						pos_validate_check;

  McpU8						eph_inject_check;
  McpU8						eph_validate_check;

  McpU8						alm_inject_check;
  McpU8						alm_validate_check;

  McpU8						tcxo_inject_check;
  McpU8						tcxo_validate_check;

  McpU8						utc_inject_check;
  McpU8						utc_validate_check;

  McpU8						ion_inject_check;
  McpU8						ion_validate_check;

  McpU8						health_inject_check;
  McpU8						health_validate_check;

  McpU8						systemtime_or_injectedtime;

  McpS16					tone_power;
  McpS8						nf_correction_factor;
  McpU16					fft_average_number;

} T_GPSC_config_file;

#endif /*__NAVC_INI_FILE_H__*/

