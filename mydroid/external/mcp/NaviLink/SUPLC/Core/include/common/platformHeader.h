/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   platformHeader.h
 *
 * Description      :   Structure in namespace platform.
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#ifndef __PLATFORM_HEADER_H__
#define __PLATFORM_HEADER_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "codec/Frequencyinfo.h"
#include "codec/Measuredresultslist.h"

//Requested assited data
#define ALMANAC_REQ_MASK                0x001
#define UTC_MODE_REQ_MASK               0x002
#define IONOSPHERIC_MODE_REQ_MASK       0x004
#define DGPS_CORRECTIONS_REQ_MASK       0x008
#define REFERENCE_LOCATION_REQ_MASK     0x010
#define REFERENCE_TIME_REQ_MASK         0x020
#define ACQUI_ASSIST_REQ_MASK           0x040
#define REAL_TIME_INT_REQ_MASK          0x080
#define NAVIGATION_MODEL_REQ_MASK       0x100
#define EXTENDED_EPHEMERIS_REQ_MASK     0x200 //rsuvorov extended ephemeris improvement

#define ALMANAC_REQ_OFFSET              0x0
#define UTC_MODE_REQ_OFFSET             0x1
#define IONOSPHERIC_MODE_REQ_OFFSET     0x2
#define DGPS_CORRECTIONS_REQ_OFFSET     0x3
#define REFERENCE_LOCATION_REQ_OFFSET   0x4
#define REFERENCE_TIME_REQ_OFFSET       0x5
#define ACQUI_ASSIST_REQ_OFFSET         0x6
#define REAL_TIME_INT_REQ_OFFSET        0x7
#define NAVIGATION_MODEL_REQ_OFFSET     0x8
#define EXTENDED_EPHEMERIS_REQ_OFFSET   0x9 //rsuvorov extended ephemeris improvement


/*
Bit 0- Almanac Requested
Bit 1- UTC Model Request
Bit 2- Ionospheric Model Request
Bit 3- DGPS Corrections Requested
Bit 4- Reference Location Requested
Bit 5- Reference Time Requested
Bit 6- Acquisition Assistance Requested
Bit 7- Real Time Integrity Requested
Bit 8- Navigation Model Requested
*/

namespace Platform {

    /*
    Strutures below describes incoming data from gps
    */

#if defined(WINXP)
    struct Imsi_t
    {
        Engine::uint8_t         imsi_buf[16];
        Engine::uint8_t         imsi_size;
    };
#endif
    /************************************************************************/
    /* Preffered method                                                     */
    /************************************************************************/
    enum PrefMethod_t
    {
        AGPS_SET_ASSISTED = 0,
        AGPS_SET_BASED = 1,
        AGPS_NO_PREF = 2
    };

    /************************************************************************/
    /*                                                                      */
    /* This structure contains parameters that define the SET capabilities  */
    /* in terms of supported positioning technologies and protocols         */
    /************************************************************************/
    struct SetCapabilities_t
    {
        Engine::uint8_t       pos_technology_bitmap;
        Engine::uint8_t       pos_protocol_bitmap;
        PrefMethod_t          pref_method;
    };


    enum // pos_technology_bitmap bit meaning
    {
        SET_ASSISTED_AGPS_OFFSET    = 0x1,
        SET_BASED_AGPS_OFFSET       = 0x2,
        AUTONOMUS_GPS_OFFSET        = 0x4,
        AFLT_OFFSET                 = 0x8,
        E_CID_OFFSET                = 0x10,
        E_OTD_OFFSET                = 0x20,
        OTDOA_OFFSET                = 0x40
    };


    enum // pos_protocol_bitmap bit meaning
    {
        TIA_801_MASK                = 0x1,
        RRLP_MASK                   = 0x2,
        RRC_MASK                    = 0x4
    };

    /************************************************************************/
    /*  Network Measurement report                                         */
    /************************************************************************/

    struct GsmNMR_t
    {
        Engine::uint16_t    arfcn;
        Engine::uint8_t     bsic;
        Engine::uint8_t     rxlev;
    };


    /************************************************************************/
    /* Cell Information                                                     */
    /************************************************************************/

#define MEASUREMENT_REPORT_BIT 0x01
#define TA_BIT                 0x02

    struct CellInfo_t
    {
        enum CELL_TYPE
        {
        	UNKNOWN,
            GSM,
            WCDMA,
            CDMA
        } cell_type;

        struct CellInfoGSM_t
        {
            Engine::uint16_t gsm_mcc;       // MCC
            Engine::uint16_t gsm_mnc;       // MNC
            Engine::uint16_t gsm_lac;       // LAC
            Engine::uint16_t gsm_ci;        // CI
            Engine::uint8_t  gsm_opt_param;
            Engine::uint8_t  gsm_ta;        // timing advance
            Engine::uint8_t  nmr_quantity;  // count of NMR's
            GsmNMR_t         gsm_nmr[15];   // NMR's array
        } cell_info_gsm;
		struct CellInfoWCDMA_t
		{
			Engine::uint64_t wcdma_mcc;								//MCC
			Engine::uint64_t wcdma_mnc;								//MNC
			Engine::uint64_t wcdma_ci;								//CI
			Engine::uint8_t  wcdma_opt_param;
			FrequencyInfo_t *frequencyInfo;				//Frequency Info
			Engine::int64_t *primaryScramblingCode;			//Primary Scrambling Code
			MeasuredResultsList_t *measuredResultsList;	//Measured Results List
		}cell_info_wcdma;
    };

    /************************************************************************/
    /* Cell info status                                                     */
    /* Describes whether or not the cell info is Not Current last known     */
    /* cell info, Current, the present cell info, Unknown (i.e. not known   */
    /* whether the cell id is current or not current)                       */
    /************************************************************************/

    enum CellInfoStatus_t
    {
        NO_CELL_INFO = 0,
        PREVIOUS_CELL_INFO = 1,
        PRESENT_CELL_INFO = 2,
        UNKNOWN_CELL_INFO = 3
    };

    /************************************************************************/
    /* Location ID                                                          */
    /* This structure contains parameters that define the globally unique   */
    /* cell identification of the most  current serving cell.               */
    /************************************************************************/

    struct LocationID_t
    {
        CellInfo_t          cell_info;
        CellInfoStatus_t    cell_info_status;
    };

    /************************************************************************/
    /* Quality of Position                                                  */
    /************************************************************************/

#define VER_ACC     0x01
#define MAX_LOC_AGE 0x02
#define DELAY       0x04

    struct QoP_t
    {
        Engine::uint8_t     horr_acc;
        Engine::uint8_t     qop_optional_bitmap;
        Engine::uint8_t     ver_acc;
        Engine::uint16_t    max_loc_age;
        Engine::uint8_t     delay;
    };


    /************************************************************************/
    /* Velocity                                                             */
    /************************************************************************/

    /* Velocity Opts*/
#define VERDIRECT           0x01
#define BEARING             0x02
#define HORSPEED            0x04
#define VERSPEED            0x08
#define HORUNCERTSPEED      0x10
#define VERUNCERTSPEED      0x20

    struct Velocity_t
    {
        Engine::uint8_t     velocity_flag;
        Engine::uint16_t    verdirect;
        Engine::uint16_t    bearing;
        Engine::uint16_t    horspeed;
        Engine::uint16_t    verspeed;
        Engine::uint16_t    horuncertspeed;
        Engine::uint16_t    veruncertspeed;

    };

    /************************************************************************/
    /* Position                                                             */
    /* This structure contains parameters defining the position of the SET. */
    /* The parameter also contains a timestamp and optionally the velocity. */
    /************************************************************************/
    struct Position_t
    {
        Engine::uint8_t     UTCTimeStampNumByte;
        Engine::uint8_t     UTCTimeStamp[20];
        Engine::uint8_t     latitude_sign;
        Engine::uint8_t     pos_opt_bitmap;
        Engine::uint8_t     altitudeDirection;
        Engine::uint8_t     altUncertainty;
        Engine::uint8_t     uncertaintySemiMajor;
        Engine::uint8_t     uncertaintySemiMinor;
        Engine::uint8_t     orientationMajorAxis;
        Engine::uint8_t     confidence;
        Engine::uint32_t    latitude;
        Engine::int32_t     longtitude;
        Engine::uint32_t    altitude;
        Velocity_t          velocity;
    };

    enum //pos_opt_bitmap bits meaning
    {
        POS_ALTITUDE            = 0x01,
        POS_UNCERTAINTY         = 0x02,
        POS_CONFIDENCE          = 0x04,
        POS_VELOCITY            = 0x08
    };


    /************************************************************************/
    /* Satellite info element                                               */
    /************************************************************************/
    struct SatInfo_t
    {
        Engine::uint8_t     satelliteID;
        Engine::uint8_t     iode;
    };

    struct GPSTime_t//rsuvorov extended ephemeris
    {
        Engine::uint16_t week;
        Engine::uint8_t HourOfWeek;
    };

    struct EEData_t //rsuvorov extended ephemeris
    {
        Engine::uint16_t validity;
        GPSTime_t StartCurExt; //SET keep extension from StartCurExt
        GPSTime_t EndCurExt;   //SET keep extension to EndCurExt
    };
    /************************************************************************/
    /* Requested Assist Data                                                */
    /* This structure contains parameters that define the requested GPS     */
    /* assistance data. The presence of this element indicates that the SET */
    /* wants to obtain specific GPS assistance data from the SLP. The SET   */
    /* might use this element in any combination of A-GPS SET assisted or   */
    /* A-GPS SET based and  Network initiated or SET initiated positioning. */
    /************************************************************************/

    struct ReqAssistedData_t
    {
        Engine::uint16_t    assitance_req_bitmap;
        Engine::uint16_t    gpsWeak;
        Engine::uint8_t     gpsToe;
        Engine::uint8_t     nSAT;
        Engine::uint8_t     toeLimit;
        Engine::uint8_t     num_sat_info;
        SatInfo_t           sat_info_elemnet[31];
        EEData_t            ee_info; //rsuvorov extended ephemeris
    };


    struct ControlPDU
    {
        Engine::uint16_t  ctrl_pdu_len;
        Engine::uint8_t   ctrl_pdu[8192];
    };
    /************************************************************************/
    /* Positioning Playload                                                */
    /************************************************************************/

    struct SUPLPosPayload_t{

        enum PayloadType
        {
            RRC_PAYLOAD=0,
            RRLP_PAYLOAD,
            TIA_801_PAYLOAD
        } type;

        ControlPDU  payload;
    };


    /************************************************************************/
    /* Status Code                                                          */
    /************************************************************************/
    enum StatusCode
    {
        SUCCESS = 0,
        FAILURE = 1
    };

    /* Commands and data primitives */

    /************************************************************************/
    /* UPL_START_REQ                                                        */
    /************************************************************************/

#define REQUEST_ASSISTANCE 0x01
#define POSITION           0x02
#define QOP                0x04

    struct StartReqData
    {
#if defined(WINXP)
        Imsi_t                  imsi;
#endif
        Engine::uint8_t         start_opt_param;
        
        // session supl pos init
        ReqAssistedData_t       a_data;     //Optional
        

        // session. uid
        Engine::uint16_t        app_id;
        
        // supl global
        SetCapabilities_t       set;
        
        // location id
        LocationID_t            lac;
        
        // session (supl start)
        QoP_t                   qop;
        
        // session supl pos
        Position_t              position;   //Optional
        

    };


    /************************************************************************/
    /* UPL_DATA_REQ                                                         */
    /************************************************************************/
    struct DataReqData
    {
        SUPLPosPayload_t     pos_payload;
        Engine::uint16_t     app_id;
        Engine::uint8_t      last_rrlp_packet;   //used for indication of last rrlp packet in session
    };


    /************************************************************************/
    /* UPL_STOP_REQ                                                         */
    /************************************************************************/

    struct StopReqData
    {
        StatusCode              gps_stoped;
        Engine::uint16_t        app_id;
    };

    /************************************************************************/
    /* UPL_DATA_IND                                                         */
    /************************************************************************/
    struct DataIndData
    {
        SUPLPosPayload_t     pos_payload;
        Velocity_t           vel;
        Engine::uint16_t     app_id;
    };

    /************************************************************************/
    /* UPL_POS_IND                                                          */
    /************************************************************************/
    struct PosIndData
    {
        Position_t           position;
        Engine::uint16_t     app_id;
    };

    /************************************************************************/
    /* UPL_STOP_IND                                                         */
    /************************************************************************/
    struct StopIndData{
        Engine::uint16_t        app_id;
        StatusCode              gps_stoped;
    };


    /************************************************************************/
    /* UPL_START_LOC_IND                                                        */
    /************************************************************************/
/* resp_opt_bitmap*/
#define START_LOC_QOP_BIT       0x01
#define LOC_FIX_MODE            0x02
#define LOC_FIX_RESULT_TYPE     0x04
#define LOC_FIX_PERIOD          0x08
#define LOC_FIX_NUM_REP         0x10

    /* loc_fix_mode */
    enum
    {
        AUTONOMOUS          = 0x00,
        MSBASED             = 0x01,
        MSASSISTED          = 0x02,
        AFLT                = 0x03,
        E_CID               = 0x04,
        E_OTD               = 0x05,
        OTDOA               = 0x06,
        NO_POSITIONING      = 0x07
    };
    struct StartLocIndData
    {
        Engine::uint16_t        app_id;
        Engine::uint8_t         resp_opt_bitmap;
        Engine::uint8_t         loc_fix_mode;
        Engine::uint16_t        loc_fix_result_type_bitmap;
        Engine::uint16_t        loc_fix_period;
        Engine::uint16_t        loc_fix_num_reports;
        QoP_t                   qop;
    };

    /************************************************************************/
    /* UPL_NOTIFY_IND                                                        */
    /************************************************************************/
/*  notify_opt_param    */
#define CLIENT_NAME_TYPE        0x01
#define CLIENT_NAME_PRESENT     0x02
#define REQUESTER_ID_TYPE       0x04
#define REQUESTER_ID_PRESENT    0x08
#define ENCODING_TYPE           0x10
    /* notification_type */
    enum NotificationType
    {
        NO_NOTIFY_NO_VERIFY     = 0,
        NOTIFY_ONLY             = 1,
        NOTIFY_VERIFY_ALLOW_NA  = 2,
        NOTIFY_VERIFY_DENY_NA   = 3,
        PRIVACY_OVERRIDE        = 4
    };

    enum EncodingType
    {
        UCS2            = 0,
        GSM_DEFAULT     = 1,
        UTF8            = 2
    };

    enum RequesterIDType
    {
        REQUESTOR_LOGICAL_NAME          = 0,
        REQUESTOR_E_MAIL_ADDRESS        = 1,
        REQUESTOR_MSISDN                = 2,
        REQUESTOR_URL                   = 3,
        REQUESTOR_SIP_URL               = 4,
        REQUESTOR_MIN                   = 5,
        REQUESTOR_MDN                   = 6,
        REQUESTOR_IMS_PUBLIC_IDENTITY   = 7
    };

    enum ClientNameType
    {
        CLIENT_LOGICAL_NAME             = 0,
        CLIENT_E_MAIL_ADDRESS           = 1,
        CLIENT_MSISDN                   = 2,
        CLIENT_URL                      = 3,
        CLIENT_SIP_URL                  = 4,
        CLIENT_MIN                      = 5,
        CLIENT_MDN                      = 6,
        CLIENT_IMS_PUBLIC_IDENTITY      = 7
    };

    struct NotifyIndData
    {
        Engine::uint16_t        app_id;
        Engine::uint8_t         notify_opt_param;
        NotificationType        notification_type;
        EncodingType            encoding_type;
        Engine::uint8_t         requestor_id[50];
        RequesterIDType         requestor_id_type;
        Engine::uint8_t         client_name[50];
        ClientNameType          client_name_type;
    };

    /************************************************************************/
    /* UPL_NOTIFY_RESP                                                        */
    /************************************************************************/
    enum NotificationStatus
    {
        ACCEPT  = 0,
        DENIED  = 1
    };

    struct NotifyRespData
    {
        Engine::uint16_t        app_id;
        NotificationStatus      notification_status;
    };
};

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __PLATFORM_HEADER_H__ */
