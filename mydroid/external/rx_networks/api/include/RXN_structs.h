/*
 * Copyright (c) 2011 Rx Networks, Inc. All rights reserved.
 *
 * Property of Rx Networks
 * Proprietary and Confidential
 * Do NOT distribute
 * 
 * Any use, distribution, or copying of this document requires a 
 * license agreement with Rx Networks. 
 * Any product development based on the contents of this document 
 * requires a license agreement with Rx Networks. 
 * If you have received this document in error, please notify the 
 * sender immediately by telephone and email, delete the original 
 * document from your electronic files, and destroy any printed 
 * versions.
 *
 * This file contains sample code only and the code carries no 
 * warranty whatsoever.
 * Sample code is used at your own risk and may not be functional. 
 * It is not intended for commercial use.   
 *
 * Example code to illustrate how to integrate Rx Networks PGPS 
 * System into a client application.
 *
 * The emphasis is in trying to explain what goes on in the software,
 * and how to the various API functions relate to each other, 
 * rather than providing a fully optimized implementation.
 *
 *************************************************************************
 * $LastChangedDate: 2012-12-07 15:20:20 -0800 (Fri, 07 Dec 2012) $
 * $Revision: 104460 $
 *************************************************************************
 *
 */

/**
* \file
* \brief
* Defines the assistance data structures.
*
* \since
* 1.1.0
*/

#ifndef RXN_STRUCTS_H
#define RXN_STRUCTS_H

#include "RXN_data_types.h"
#include "RXN_constants.h"

#ifdef __cplusplus
extern "C" {
#endif

  /**
   * \brief
   * The maximum number of internal sub-states in the state machine used
   * in the Non-Blocking API functions.
   *
   * \internal
   *
   * \since
   * 6.0.0
   */  
  #define MAX_SUB_STATES  8
 
  /**
   * \brief
   * Buffer size needed to store the internal state. 
   * Create a buffer of MAX_STATE_BUF_SIZE in the heap or stack and assign this memory
   * to RXN_state.buffer_ptr to be used in a Non-Blocking API functions.
   *
   * \internal
   *
   * \since
   * 6.0.0
   */
  #define MAX_STATE_BUF_SIZE   (17*1024)

  /**
   * \brief
   * An enumeration of the different states for the RXN_state_t element status.
   *
   * \since
   * 6.0.0
   */    
  typedef enum RXN_state_status
  {  
    RXN_STATUS_START,		/*!< Start executing a Non-Blocking API call.*/
    RXN_STATUS_DONE,		/*!< The Non-Blocking API call has finished completely. */
    RXN_STATUS_PENDING, /*!< The Non-Blocking API call has has finish the current slice, but the API function has not finished completely. */
    RXN_STATUS_ABORT,   /*!< Abort the Non-Blocking API call.*/
    RXN_STATUS_ABORTED, /*!< The Non-Blocking API call aborted.*/
  } RXN_state_status_t;

  /**
   * \brief
   * Enumeration used to select the constellation from which to draw or 
   * generate assistance.
   * 
   * \since
   * 4.0.0
   */
  typedef enum RXN_constel 
  {
    RXN_GPS_CONSTEL = 0, /*!< NAVSTAR constellation.*/
    RXN_GLO_CONSTEL      /*!< GLONASS constellation.*/
  } RXN_constel_t;

  /**
   * \brief
   * Enumeration used to select the seed type to retieve information.
   * 
   * \since
   * 6.2.0
   */
  typedef enum RXN_seed_type 
  {
    RXN_ANY_SEED_TYPE = 0,      /*!< Any seed type. (Used to retrieve any seed type information.)*/
    RXN_PGPS_SEED_TYPE,         /*!< PGPS seed type.*/
    RXN_SAGPS_SEED_TYPE         /*!< SAGPS seed type.*/
  } RXN_seed_type_t;

  /**
   * \brief
   * Enumberation used to specific the number of segements to use for SAGPS.
   *
   * \details
   * This enumeration provides a set of flags that describe the number of 
   * segement(s) to be used when generating SAGPS seeds. Each segment 
   * corresponds to a single broadcast ephemeris observation. Using multiple
   * segments (observations) to generate the predict increases the accuracy
   * at the expense of the processing time.
   *
   * It is recommended to use the default value and let the library
   * select the most appropriate choice. However, if necessary, one or more 
   * flags can be combined using the bitwise operator to specify customize 
   * the behavior.
   * 
   * \since
   * 7.0.0
   */
  typedef enum RXN_SAGPS_segment_flag
  {
    DISABLE_ALL_SEGMENTS =      0x0,   /*!< Disable all the use of any segment. (ie. disables SAGPS) */
    ENABLE_1_SEGMENT     =      0x1,   
    ENABLE_2_SEGMENTS    =      0x2,
    DEFAULT_SEGMENTS     =   0xFFFF    /*!< Enable any combination of segment(s) to be used. */
  } RXN_SAGPS_segment_flag_t;

  /**
   * \brief
   * A structure containing the information for the current processing state.
   * Create a buffer of MAX_BUF_SIZE in the heap or stack and assign this memory
   * to RXN_state.buffer_ptr to be used in a Non-Blocking API function.
   *
   * \note
   * The state object needs to be initialized properly before first use. 
   * Please see the given example.
   * 
   * <b>Example Usage:</b>
   * \code
   * // Instantiate the state object
   * RXN_state_t state;
   *
   * // Initialize the initiate state
   * memset(&state, 0, sizeof(state));
   * state.buffer_ptr = &state.buffer[0];
   * \endcode
   *
   * \since
   * 6.0.0
   */
  typedef struct RXN_state
  {
    RXN_state_status_t status; 		        /*!< Status of the API call.*/
    U32 sub_state_index;			            /*!< Internal use only.*/
    U32 sub_state[MAX_SUB_STATES];	      /*!< Internal use only.*/
    void* dynamic_memory_allocation_list; /*!< Internal use only. Pointer to list of dynamic memory allocated.*/
    U08 buffer[MAX_STATE_BUF_SIZE];	      /*!< Internal use only. Buffer to store internal state.*/
    U08* buffer_ptr;				              /*!< Internal use only.*/
    U32 buffer_size;				              /*!< Internal use only.*/
  } RXN_state_t;

  /**
   * \brief
   * A structure for overriding configuration defaults / getting information
   * from the server regarding leap seconds or GPS/Glonass clock offsets
   *
   * Note: the library does not use tau_gps or tt_tau_gps but provides it. 
   *
   * The library *does* need leap_secs for Glonass and will use gps_time_next_leap
   * if not zero. The user is therefore advised not zero either of these out when 
   * calling RXN_Set_Config(). Note that PGPS seeds are synced to GPS time so calling
   * RXN_Get_Ephemeris() is fine if PGPS seeds are available. For SA Glonass seeds, the value 
   * of leap_secs must be correct.
   *
   * Note also the time-tag for tau_gps is provided; it varies slowly ~ 2m /week
   *
   * \details
   * For more information, please refer to the integrator's manual.
   * 
   * \since
   * 4.0.0
   */
  typedef struct RXN_config
  {
    S32  toe_buffer_offset;    /*!< \brief Time offset [sec] between prediction buffer and last toe [default 0 sec].*/
    S32  chip_expiry_from_TOE; /*!< \brief Nominal chipset ephemeris expiration after toe [default 7200 sec].*/
    U32  tt_tau_gps;           /*!< \brief Time tag for tauGPS in absolute GPS sec. */
    S32  tau_gps;              /*!< \brief Fine GPS/UTC time corr scale factor 2^-30 [sec]: T_GPS = T_GLO + utc_offset + tauGPS + (t - tt_tau_gps) * tau_gps_dot.*/
    S32  tau_gps_dot;          /*!< \brief Fine GPS/UTC time corr drift factor 2^-49 [sec]: T_GPS = T_GLO + utc_offset + tauGPS + (t - tt_tau_gps) * tau_gps_dot.*/
    U32  gps_time_next_leap;   /*!< \brief GPS time [sec] of the next leap if known; otherwise the value is set to 0. Based on International Earth Rotation and Reference Systems Service (IERS) bulletin.*/
    U16  ephemeris_duration;   /*!< \brief Duration of predicted Keplarian ephemeris [default 300 sec].*/
    U08  logging_level;        /*!< \brief 0 no logging, 1 error, 2 info, 3 debug, 4 full dump (including eph store) [default 1].*/
    U08  leap_secs;            /*!< \brief Current leap seconds between UTC and GPS.*/
    U32  min_seed_threshold;   /*!< \brief Minimum time in seconds before a new SAGPS seed will be create. [default 50400 sec = 14hrs].*/
    RXN_SAGPS_segment_flag_t sagps_orbit_segment_mask_gps;  /*!< \brief The number of orbit segments to use when create an SAGPS seed for GPS [default all supported segments].*/
    RXN_SAGPS_segment_flag_t sagps_orbit_segment_mask_glo;  /*!< \brief The number of orbit segments to use when create an SAGPS seed for GLONASS [default all supported segments].*/
  } RXN_config_t;

  /**
   * \brief
   * A structure containing ephemeris data elements supported by GPStream products.
   *
   * \attention Both GPS and Glonass assistance will be delivered in this format.
   *
   * Please refer to the ICD-GPS-200 Rev C or IS-GPS-200 Rev D for more information
   * on the elements included within this structure.
   * Assume a scale factor of 1.0 if not specified otherwise.
   *
   * \see
   * RXN_Set_Ephemeris()
   *
   * \see
   * RXN_Get_Ephemeris()
   * 
   * \since
   * 1.1.0
   */
  typedef struct RXN_ephem
  {
    U08   prn;        /*!< \brief Ephemeris PRN or SV. Range 1-32.*/
    U08   ura;        /*!< \brief User Range Accuracy index. Actual URA from broadcast ephemeris or estimated URA from predicted ephemeris. See IS-GPS-200 Rev D for index values.*/
    U08   health;     /*!< \brief Corresponds to the SV health value. 6 bits as described within IS-GPS-200 Rev D.*/
    S08   af2;        /*!< \brief Clock Drift Rate Correction Coefficient. Scale: 2^-55. Units: sec/sec^2. */
    S08   ephem_fit;  /*!< \brief Fit interval relating to the fit interval flag. Typically 4 hrs. */
    U08   ure;        /*!< \brief User Range Error. Provides an indication of EE accuracy. Always a GPStream output. Units: meters.*/
    U16   gps_week;   /*!< \brief Extended week number (i.e. > 1024, e.g.1486). */
    U16   iode;       /*!< \brief Issue Of Data (Ephemeris). */
    U16   toc;        /*!< \brief Time Of Clock or time of week. Scale: 2^4. Units: seconds. */
    U16   toe;        /*!< \brief Time Of Ephemeris. Scale: 2^4. Units: seconds. */
    S16   af1;        /*!< \brief Clock Drift Correction Coefficient. Scale: 2^-43. Units: sec/sec. */
    S16   i_dot;      /*!< \brief Rate of Inclination Angle. Scale: 2^-43. Units: semi-circles/sec. */
    S16   delta_n;    /*!< \brief Mean Motion Difference from Computed Value. Scale: 2^-43. Units: semi-circles/sec. */
    S16   cuc;        /*!< \brief Amplitude of the Cos Harmonic Correction Term to the Arguement of Latitude. Scale: 2^-29. Units: radians. */
    S16   cus;        /*!< \brief Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude. Scale: 2^-29. Units: radians. */
    S16   cic;        /*!< \brief Amplitude of the Cos Harmonic Correction Term to the Angle of Inclination. Scale:  2^-29. Units: radians. */
    S16   cis;        /*!< \brief Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination. Scale: 2^-29. Units: radians. */
    S16   crc;        /*!< \brief Amplitude of the Cos Harmonic Correction Term to the Orbit Radius. Scale: 2^-5. Units: meters. */
    S16   crs;        /*!< \brief Amplitude of the Sine Harmonic Correction Term to the Orbit Radius. Scale: 2^-5. Units: meters. */
    S32   af0;        /*!< \brief Clock Bias Correction Coefficient. Scale: 2^-31. Units: sec. */
    S32   m0;         /*!< \brief Mean Anomaly at Reference Time. Scale: 2^-31. Units: semi-circles. */
    U32   e;          /*!< \brief Eccentricity. Scale: 2^-33. Units: N/A - Dimensionless. */
    U32   sqrt_a;     /*!< \brief Square Root of the Semi-Major Axis. Scale: 2^-19. Units: square root of meters. */
    S32   omega0;     /*!< \brief Longitude of Ascending Node of Orbit Plane and Weekly Epoch. Scale: 2^-31. Units: semi-circles. */
    S32   i0;         /*!< \brief Inclination Angle at Reference Time. Scale: 2^-31. Units: semi-circles. */
    S32   w;          /*!< \brief Argument of Perigee. Scale: 2^-31. Units: semi-circles. */
    S32   omega_dot;  /*!< \brief Rate of Right Ascension. Scale: 2^-43. Units: semi-circles/sec. */
  } RXN_ephem_t;

  /**
   * \brief
   * A structure containing Glonass ephemeris data elements supported by GPStream products.
   *
   * \details
   * Please refer to the Glonass ICD 5.1 
   * for the elements included within this structure.
   * Assume a scale factor of 1.0 if not otherwise specified.
   *
   * \attention 
   * To represent the current Glonass time in seconds.
   *   gloSec = [(N4-1)*1461 + (NT-1)]*86400 + tb*900 [sec]
   *
   * Using params as in Table 4.9 Glonass ICD 5.1:
   *   N4: range bf1, ..., 31    [4 year interval] 5 bits 
   *   NT: range bf1, ..., 1461  [days] 11 bits
   *   tb: range 15,  ..., 1425  [units of 15 minutes] 7 bits with scale factor 15 min
   *
   * \attention 
   * If utc_offset != 0 it is assumed valid both going in and out of the API.
   *
   * \note
   * Glonass-M sats (currently the whole constellation) have a parameter NT, which
   * replaces NA. Formerly one had to use gloSec=[(N4-1)*1461 + (NA-1)]*86400 + tb*900
   *
   * \note
   * That to convert gloSec to gpsSec (sec since start of GPS) one has
   *   gpsSec = gloSec + GPS_GLO_OFFSET + leap_sec.
   *
   * \see
   * RXN_Set_Glonass_Ephemeris()
   *
   * \since
   * 4.0.0
   */
  typedef struct RXN_glonass_ephem
  {
    U08 slot;         /*!< \brief Ephemeris Id for SV. Range 1-24.*/
    U08 FT;           /*!< \brief User Range Accuracy index.  P32 ICD Glonass for value of Ft.*/
    S08 freqChannel;  /*!< \brief Freq slot: -7 to +13 incl. */
    U08 M;            /*!< \brief Glonass vehicle type. M=1 means type M*/
    U08 Bn;           /*!< \brief Bn SV health see p30 ICD Glonass. */
    U08 utc_offset;   /*!< \brief Current GPS-UTC leap seconds [sec]; 0 if unknown. */
    S16 gamma;        /*!< \brief SV clock frequency error ratio scale factor 2^-40 [seconds / second] */
    S32 tauN;         /*!< \brief SV clock bias scale factor 2^-30 [seconds]. */
    U32 gloSec;       /*!< \brief gloSec=[(N4-1)*1461 + (NT-1)]*86400 + tb*900. [sec] ie sec since Jan 1st 1996 <b>see caution note in struct details description</b> */
    S32 x;            /*!< \brief x position at toe scale factor 2^-11 Km PZ90 datum. */
    S32 y;            /*!< \brief y position at toe scale factor 2^-11 Km PZ90 datum. */
    S32 z;            /*!< \brief z position at toe scale factor 2^-11 Km PZ90 datum. */
    S32 vx;           /*!< \brief x velocity at toe scale factor 2^-20 Km/s PZ90 datum. */
    S32 vy;           /*!< \brief y velocity at toe scale factor 2^-20 Km/s PZ90 datum. */
    S32 vz;           /*!< \brief z velocity at toe scale factor 2^-20 Km/s PZ90 datum. */
    S16 lsx;          /*!< \brief x luni solar accel scale factor 2^-30 Km/s^2 PZ90 datum. */ 
    S16 lsy;          /*!< \brief y luni solar accel scale factor 2^-30 Km/s^2 PZ90 datum. */ 
    S16 lsz;          /*!< \brief z luni solar accel scale factor 2^-30 Km/s^2 PZ90 datum. */ 
  } RXN_glonass_ephem_t;

  /**
   * \brief
   * A structure containing seed information supported by GPStream products.
   *
   * \see
   * RXN_Get_Seed_Information()
   *
   * \since
   * 1.2.0
   */
  typedef struct RXN_seed_information
  {
    U08 number_of_ephemeris_used; /*!< \brief 0 - PGPS seed; 1 or 2 - the number of ephmerides used in the creation of an SAGPS seed. */
    U32 seed_creation_time;       /*!< \brief Start time of the seed created from the server [sec]. */
    U32 poly_start_time;          /*!< \brief Start time of this seed's prediction buffer [sec].*/
    U32 poly_curr_time;           /*!< \brief Current time of this seed's prediction buffer [sec].*/
    U32 poly_expire_time;         /*!< \brief Expiry time of this seed's prediction buffer [sec]. */
    U32 clock_ref_time; 		      /*!< \brief Clock reference time [sec] (used to determine if need clock update in PG) */
  } RXN_seed_information_t;

  /**
   * \brief
   * A structure containing auxiliary information for GLONASS PRNs
   *
   * \see
   * 
   *
   * \since
   * 7.1.0
   */
  typedef struct RXN_glonass_aux_info
  {
    U08 prn;                      /*!< \brief Ephemeris Id for SV. Range 1-24.*/
    S08 frequency_channel;        /*!< \brief Frequency channels of the GLONASS PRN. -7 to +13 incl. */
    U08 signals_available;        /*!< \brief Signals Available */
  } RXN_glonass_aux_info_t;

  /**
   * \brief
   * A structure containing block type information
   *
   * \see
   * 
   *
   * \since
   * 7.1.0
   */
  typedef struct RXN_block_type
  {
    U08 prn;                      /*!< \brief PRN. Range 0-63.*/
    U08 block_type;               /*!< \brief GPS: IIA=1, IIR=2, IIRM=3, IIF=4 and III=5
                                              GLONASS: GS=1, GSM=2 and GSK=3 */
                                  
  } RXN_block_type_t;

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_STRUCTS_H
