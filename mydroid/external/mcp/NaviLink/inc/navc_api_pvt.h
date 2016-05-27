#ifndef __NAVC_API_PVT_H__
#define __NAVC_API_PVT_H__

#include "mcp_hal_types.h"
#include "mcpf_defs.h"
#include "navc_ini_file_def.h"	

#define GPSC_RAW_MAX_SV_REPORT         (0x11)


/***********************************************************/
/* NAVC_CMD_PLT command parameters                         */
/***********************************************************/
/*
 * enum to Variable req_type
 * Specify the which production line test is requested
 */
typedef enum
{
  GPSC_USERDEF_CWTEST_REQ        = 0xc6,          /* User Defined CW Test           */
  GPSC_SIGACQ_TEST_REQ           = 0xca,          /* Signal Acquisition Test        */
  GPSC_GPSOSC_TEST_REQ           = 0xcc,          /* GPS Oscillator test            */
  GPSC_RTC_TEST_REQ              = 0xcd,          /* RTC Offset Test                */
  GPSC_SYNC_TEST_REQ             = 0xce,          /* Sync Test                      */
  GPSC_PREDEF_CWTEST_REQ         = 0xcf           /* Predefined CW Test             */
}T_GPSC_req_type;

/*
 * cw test request  parameters
 */
typedef struct
{
  McpU16   test_request;             /*                                                    */
  McpU16   start_delay;              /* Range 0 to 65535 msec                              */
  McpU8   zzz_align0;               /* alignment                                          */
  McpU8   zzz_align1;               /* alignment                                          */
  McpS32  wideband_centfreq;        /* Range -8.184 MHz to 8.184 - 16.368/224 MHz         */
  McpS32  narrowband_centfreq;      /* Range -8.184 MHz to 8.184 - 16.368/224 MHz         */
  McpU8   wideband_peaks;           /* Range is  0 to 10                                  */
  McpU8   wideband_adj_samples;     /* Range is  0 to 255                                 */
  McpU8   narrowband_peaks;         /* Range is  0 to 10                                  */
  McpU8   narrowband_adj_samples;   /* Range is  0 to 255                                 */
  McpU8   rawiqsample_rawfft_capture; 
  McpU8   nf_correction_factor;
  McpU16  input_tone;
  McpU8   test_mode;
  McpU8   narrowband_decimation;
  McpU16  reserved;    
} T_GPSC_cw_test_params;

/*
 * gpio test request  parameters
 * CCDGEN:WriteStruct_Count==47
 */
typedef struct
{
  McpU16                  write_value;              /* GPIO write value- bit field                        */
  McpU16                  write_mask;               /* GPIO write mask                                    */
  McpU16                  status_mask;              /* GPIO read status mask                              */
  McpU8                   zzz_align0;               /* alignment                                          */
  McpU8                   zzz_align1;               /* alignment                                          */
} T_GPSC_gpio_test_params;

typedef struct
{
  McpU32                  req_type;                 /* T_GPSC_req_type Specify the which production line test is requested */
  McpU32                  timeout;                  /* In milliseconds                                    */
  McpU8                   svid;                     /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
  McpU8                   cw_test_ver;              /*                                                    */
  McpU8                   termination_event;        /*                                                    */
  McpU8                   zzz_align0;               /* alignment                                          */
  T_GPSC_cw_test_params   cw_test_params;           /* cw test request  parameters                        */
  T_GPSC_gpio_test_params gpio_test_params;         /* gpio test request  parameters                      */
} TNAVC_plt;



/***********************************************************/
/* NAVC_EVT_PLT_RESPONSE event parameters                  */
/***********************************************************/
/*
 * enum to UnionController prodtest_response_union 
 */
typedef enum
{
  GPSC_RESULT_RTC_OSCTEST        = 0x0,           
  GPSC_RESULT_GPS_OSCTEST        = 0x1,           
  GPSC_RESULT_SIGACQ_TEST        = 0x2,           
  GPSC_RESULT_CW_TEST            = 0x3,           
  GPSC_RESULT_GPIO_TEST          = 0x4,           
  GPSC_RESULT_SYNC_TEST          = 0x5,           
  GPSC_RESULT_SELF_TEST          = 0x6,           
  GPSC_RESULT_RAM_CHECKSUM_TEST  = 0x7            
}T_GPSC_ctrl_prodtest_response_union;

/*
 * RTC oscillator offset response
 */
typedef struct
{
  McpS16   rtcoscoffset;   /* Range: -32.768 Hz to 32.767 Hz                     */
  McpU8    zzz_align0;     /* alignment                                          */
  McpU8    zzz_align1;     /* alignment                                          */
} T_GPSC_rtc_osctest;

/*
 * GPS oscillator offset response
 */
typedef struct
{
  McpS16   gpsoscoffset;             /* Range: -32.768 Hz to 32.767 Hz                     */
  McpU8    zzz_align0;               /* alignment                                          */
  McpU8    zzz_align1;               /* alignment                                          */
} T_GPSC_gps_osctest;


/*
 * Signal Acquisition Test response
 */
typedef struct
{
  McpU8    numofsv;                  /* Range: 0 to 12                                     */
  McpU8    svprn[12];                /* Range: 1 to 32                                     */
  McpU8    zzz_align0;               /* alignment                                          */
  McpU16   svcno[12];                /* Range: 0 to 65535 dB                               */
  McpU8    zzz_align1;               /* alignment                                          */
  McpU8    zzz_align2;               /* alignment                                          */
} T_GPSC_sigacq_test;

/*
 * enum to Variable cwresponsetype
 * Specify the Response type for CW test
 */
typedef enum
{
  GPSC_IQ_SAMPLES_1MHZ           = 0x0,           /* CW Test IQ Samples for 1 MHz   */
  GPSC_IQ_SAMPLES_2MHZ           = 0x1,           /* CW Test IQ Samples for 2 MHz   */
  GPSC_WIDEBAND_FFT              = 0x2,           /* CW Test Wideband FFT Result    */
  GPSC_NARROWBAND_FFT            = 0x3,           /* CW Test Narrowband FFT Result  */
  GPSC_CWSTATUS                  = 0x4            /* CW Test Status                 */
}T_GPSC_cwresponsetype;

/*
 * Continuous Wave Test response
 */

typedef struct
{
	McpU16						 wideband_peak_index; /* Range: 0 to 4096			 */
	McpU16						 wideband_peak_snr;   /* Range: 0 to 65535 dB		 */
} T_GPSC_cwtest_widebandpeakinfo;

typedef struct
{
McpU16						 narrowband_peak_index;  /* Range: 0 to 4096									*/
McpU16						 narrowband_peak_snr; /* Range: 0 to 65535 dB								 */
} T_GPSC_cwtest_narrowbandpeakinfo;

typedef struct
{
McpS16 isamplepacketnumber;
McpS16 qsamplepacketnumber;
} T_GPSC_cwtest_iqsamples_packetbody;


typedef struct
{
  T_GPSC_cwresponsetype		   cwresponsetype;           /* (enum=32bit)<->T_GPSC_cwresponsetype Specify the Response type for CW test */
  McpU16                       totalpacket;              /* Range: 0 to 32                                     */
  McpU16                       packetnumber;             /* Range: 0 to 32                                     */
  McpU16                       can1bin_peak;             /* Range: 0 to 65535 dB                               */
  McpU16                       can2to3bin_peak;          /* Range: 0 to 65535 dB                               */
  McpU16                       can4to7bin_peak;          /* Range: 0 to 65535 dB                               */
  McpU16                       can8to15bin_peak;         /* Range: 0 to 65535 dB                               */
  McpU16                       can16to31bin_peak;        /* Range: 0 to 65535 dB                               */
  McpU16                       can32to63bin_peak;        /* Range: 0 to 65535 dB                               */
  McpU16                       can64to127bin_peak;       /* Range: 0 to 65535 dB                               */
  McpU16                       can128to255bin_peak;      /* Range: 0 to 65535 dB                               */
  McpU16                       can256to511bin_peak;      /* Range: 0 to 65535 dB                               */
  McpU8                        noise_figure;
  McpS32                        tcxo_offset;
  T_GPSC_cwtest_widebandpeakinfo 		wideband_peakinfo[128];
  McpU8						 num_wideband_peak; 	   /* Range: 0 to 255									 */
  McpS32					 wideband_centfreq; 	   /* Range -8.184 MHz to 8.184 - 16.368/224 MHz		 */
  T_GPSC_cwtest_narrowbandpeakinfo 		narrowband_peakinfo[128];
  McpU8						 num_narrowband_peak;	   /* Range: 0 to 255									 */
  McpS32					 narrowband_centfreq;	   /* Range -8.184 MHz to 8.184 - 16.368/224 MHz		 */
  T_GPSC_cwtest_iqsamples_packetbody 	iqsamples_packetbody[128];
  
} T_GPSC_cw_test;

/*
 * gpio test request  parameters
 */
typedef struct
{
  McpU16     status_value;             /* GPIO status value                                  */
  McpU8      zzz_align0;               /* alignment                                          */
  McpU8      zzz_align1;               /* alignment                                          */
} T_GPSC_gpio_test;

/*
 * Sync Test response
 */
typedef struct
{
  McpU8      state;                    /* state value                                        */
  McpU8      zzz_align0;               /* alignment                                          */
  McpU8      zzz_align1;               /* alignment                                          */
  McpU8      zzz_align2;               /* alignment                                          */
  McpU32     on_time;                  /* State On Time value                                */
  McpU32     start_time;               /* State Start Time value                             */
  McpU32     code_sync_time;           /* Code Sync Time value                               */
  McpU32     bit_sync_time;            /* Bit Sync Time value                                */
  McpU32     frame_sync_time;          /* Frame Sync Time value                              */
} T_GPSC_sync_test;

/*
 * Self Test response
 */
typedef struct
{
  McpU8      self_test_result;         /* Self test result value                             */
  McpU8      zzz_align0;               /* alignment                                          */
  McpU8      zzz_align1;               /* alignment                                          */
  McpU8      zzz_align2;               /* alignment                                          */
} T_GPSC_self_test;

/*
 * Ram checksum test response
 */
typedef struct
{
  McpU8      checksumresult;           /* Checksum Result                                    */
  McpU8      zzz_align0;               /* alignment                                          */
  McpU8      zzz_align1;               /* alignment                                          */
  McpU8      zzz_align2;               /* alignment                                          */
} T_GPSC_ram_checksum_test;

/*
 * Production line test response union
 */
typedef union
{
  T_GPSC_rtc_osctest        rtc_osctest;       /* RTC oscillator offset response                     */
  T_GPSC_gps_osctest        gps_osctest;       /* GPS oscillator offset response                     */
  T_GPSC_sigacq_test        sigacq_test;       /* Signal Acquisition Test response                   */
  T_GPSC_cw_test            cw_test;           /* Continuous Wave Test response                      */
  T_GPSC_gpio_test          gpio_test;         /* gpio test request  parameters                      */
  T_GPSC_sync_test          sync_test;         /* Sync Test response                                 */
  T_GPSC_self_test          self_test;         /* Self Test response                                 */
  T_GPSC_ram_checksum_test  ram_checksum_test; /* Ram checksum test response                         */
} T_GPSC_prodtest_response_union;


typedef union
{
  TNAVC_plt tPltParams;
}TNAVC_pltUnion;

/*
 * Production line test  response
 */
typedef struct
{
  McpU32                         ctrl_prodtest_response_union; /* T_GPSC_ctrl_prodtest_response_union */
  T_GPSC_prodtest_response_union prodtest_response_union; /*<  4:2084> Production line test response union                */
} T_GPSC_prodtest_response;
typedef T_GPSC_prodtest_response TNAVC_pltResponse;


/*
 * Raw measurement
 */
typedef struct
{
  McpU8                        svid;                     /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
  McpU8                        zzz_align0;               /* alignment                                          */
  McpU8                        zzz_align1;               /* alignment                                          */
  McpU8                        zzz_align2;               /* alignment                                          */
  McpU32                       time_tag_info;            /* (enum=32bit)<->T_GPSC_time_tag_info Time tag information */
  McpU16                       snr;                      /* SV signal to noise ratio, unsigned, 0.1 dB per bit */
  McpU16                       cno_tenths;               /* SV C/No measurement, unsigned, 0.1 dB per bit      */
  McpS16                       latency_ms;               /* SV latency, signed, 1 msec per bit                 */
  McpU8                        pre_int;                  /* Pre int, unsigned, 1 msec per bit                  */
  McpU8                        zzz_align3;               /* alignment                                          */
  McpU16                       post_int;                 /* Post int, unsigned, 1 msec per bit                 */
  McpU8                        zzz_align4;               /* alignment                                          */
  McpU8                        zzz_align5;               /* alignment                                          */
  McpU32                       msec;                     /* SV time millisecond, unsigned, 1 msec per bit      */
  McpU32                       sub_msec;                 /* SV time sub-millisecond, unsigned, 1/2^24 msec per bit */
  McpU16                       sv_time_uncertainty;      /* SV time uncertainty, unsigned, 1 nsec per bit, mantissa in bits 15:5 and exponent in bit 4:0 */
  McpU8                        zzz_align6;               /* alignment                                          */
  McpU8                        zzz_align7;               /* alignment                                          */
  McpS32                       sv_speed;                 /* SV speed, signed, 0.01 meters/sec per bit          */
  McpU32                       sv_speed_uncertainty;     /* SV speed uncertainty, unsigned, 0.1 meters/sec per bit */
  McpU16                       meas_status_bitmap;       /* T_GPSC_meas_status_bitmap,  Measurement status bitmap */
  McpU8                        channel_meas_state;       /* channel measurement state. 1 byte                  */
  McpU8                        zzz_align8;               /* alignment                                          */
  McpS32                       accum_carrier_phase;      /* accumulated carrier phase. signed, -262144 m to 262144 m */
  McpS32                       carrier_vel;              /* carrier velocity. signed, -131072m/sec to 131072m/sec */
  McpS16                       carrier_acc;              /* carrier acceleration. signed, -128m/s^2,+128m/s^2  */
  McpU8                        loss_lock_ind;            /* loss of lock indication. 0-255                     */
  McpU8                        good_obv_cnt;             /* good observation. unsigned, 0 to 255               */
  McpU8                        total_obv_cnt;            /* totalobservation. unsigned, 0 to 255               */
  McpS8                        elevation;              /*< 57:  1> alignment                                          */
  McpU8                        azimuth;              /*< 58:  1> alignment                                          */
  McpU8                        zzz_align11;              /* alignment                                          */
} T_GPSC_measurement;


typedef struct
{
  McpU16    gps_week;                 /*<  0:  2> GPS week number                                    */
  McpU8     zzz_align0;               /*<  2:  1> alignment                                          */
  McpU8     zzz_align1;               /*<  3:  1> alignment                                          */
  McpU32    gps_msec;                 /*<  4:  4> GPS milliseconds                                   */
  McpFLT                       sub_ms;                   /*<  8:  4> GPS sub-milliseconds - 1 bit = 1 microsecond       */
  McpFLT 			tUnc;
  McpFLT 			fbias;
  McpFLT 			fUnc;
  McpU32			TimerCount;
  McpU8				u_valid;

} T_GPSC_pvt_toa;


/*
 * Raw (native GPS baseband format) measurement response
 */
typedef struct
{
  McpU16                       report_num;               /*<  0:  2> Report number                                      */
  McpU16                       num_requested_reports;    /*<  2:  2> Number of requested reports                        */
  T_GPSC_pvt_toa                toa;                      /*<  4: 12> Time of applicability for reported data            */
  McpU8                        zzz_align0;               /*< 16:  1> alignment                                          */
  McpU8                        zzz_align1;               /*< 17:  1> alignment                                          */
  McpU8                        zzz_align2;               /*< 18:  1> alignment                                          */
  McpU8                        c_measurement;            /*< 19:  1> counter                                            */
  T_GPSC_measurement        measurement[GPSC_RAW_MAX_SV_REPORT]; /*< 20:1020> Raw measurement                                    */
  McpU16 assist_availability_flags;
  McpU32 eph_availability_flags;
  McpU32 pred_eph_availability_flags;
  McpU32 alm_availability_flags;
  McpU8 u_IsValidMeas;
  McpU32 q_GoodStatSvIdBitMap;
  McpDBL d_PosUnc;
} T_GPSC_raw_measurement;

#endif // #ifndef __NAVC_API_PVT_H__
