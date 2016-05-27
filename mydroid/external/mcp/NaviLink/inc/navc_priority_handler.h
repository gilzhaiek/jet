#ifndef __NAVC_PRIORITY_HANDLER_H__
#define __NAVC_PRIORITY_HANDLER_H__

#include "mcp_hal_types.h"
#include "mcpf_defs.h"
#include "navc_ini_file_def.h"
#include "navc_api.h"


#define HASHSIZE 101








typedef enum
{
	NAVC_SUPL_MO = 1,
	NAVC_SUPL_MT_POSITIONING,
	NAVC_SUPL_MT_NO_POSITIONING,
	NAVC_E911,
	NAVC_CP_MTLR,
	NAVC_CP_NILR,
	NAVC_STANDLONE 

} T_GPSC_call_type;




typedef struct
{
  McpU8     velocity_flag;            /* This flag indicates the velocity with a universal Geographical Area Description when both are applied to a common entity at a common time. */
  McpU8     zzz_align0;               /* alignment */
  McpU16    vertical_direction;       /* Vertical speed direction is encoded using 1 bit: a bit value of 0 indicates upward speed; a bit value of 1 indicates downward speed. */
  McpU16    Bearing;                  /* Bearing is encoded in increments of 1 degree measured clockwise from North using a 9 bit binary coded number N. */
  McpU16    horizontal_speed;         /* Horizontal speed is encoded in increments of 1 kilometre per hour using a 16 bit binary coded number N. */
  McpU16    vertical_speed;           /* Vertical speed is encoded in increments of 1 kilometre per hour using 8 bits giving a number N between 0 and 28-1. */
  McpU16    horizontal_speed_uncertainity; /* Uncertainty speed is encoded in increments of 1 kilometre per hour using an 8 bit binary coded number N. The value of N gives the uncertainty speed except for N=255 which indicates that the uncertainty is not specified. */
  McpU16    vertical_speed_uncertainity;   /* Uncertainty speed is encoded in increments of 1 kilometre per hour using an 8 bit binary coded number N. The value of N gives the uncertainty speed except for N=255 which indicates that the uncertainty is not specified. */
  McpU8     zzz_align1[2];            /* alignment */
} EGPSC_loc_fix_velocity;

typedef struct
{
  McpU8     qop_flag;             /* Presently not being used ,This flag indicates if a set of attributes associated with a request for the geographic position of SET is made. */
  McpU8     delay_n;              /* Presently not being used ,Delay N. 2^N, N from (0..7), unit is seconds       */
  McpU16     horizontal_accuracy;  /* Horizontal Accuracy. This describes the uncertainty for latitude and longitude, set OxFF if not in use*/
  McpU16    vertical_accuracy;    /* Presently not being used Vertical Accuracy.This describes the uncertainity in the altitude. */
  McpU16    max_response_time;     /* Maximum response time set by the network. Units in seconds from 0 to 65535. set OXFFFF if not being used */
  McpU8     max_loc_age; 		/* Location Age..Sent by Network in Qop field */
} EGPSC_loc_fix_qop;



typedef struct
{
    McpU8   horr_acc;
    McpU8   qop_optional_bitmap;
    McpU8   ver_acc;
    McpU16  max_loc_age;
    McpU8   delay;
        McpU8   max_response_time;
}supl_qop;





typedef T_GPSC_call_type NAVC_OngoingSession;
typedef T_GPSC_call_type NAVC_IncomingSession;


typedef enum
{
	NAVC_ACCEPTINCOMING_STOPONGOING = 1,
	NAVC_IGNOREINCOMING,
	NAVC_HOLD,
	NAVC_NOEFFECT,

} NAVC_PriorityAction;


struct Navc_PriorityHashTable{
	
  NAVC_OngoingSession  OngoingSession;
  NAVC_IncomingSession  IncomingSession;
  NAVC_PriorityAction  Action;
  struct Navc_PriorityHashTable *Next_node;
  
};
typedef struct Navc_PriorityHashTable Navc_PriorityHashTable;

typedef struct Gpsvelocity
	{
		McpU8	velocity_flag;
		McpU16	verdirect;
		McpU16	bearing;
		McpU16	horspeed;
		McpU16	verspeed;
		McpU16	horuncertspeed;
		McpU16	veruncertspeed;
	}GPS_Velocity;


typedef struct Gpsposition
	{
		McpU8		UTCTimeStampNumByte;
		McpU8		UTCTimeStamp[20];
		McpU8		latitude_sign;
		McpU8		pos_opt_bitmap;
		McpU8		altitudeDirection;
		McpU8		altUncertainty;
		McpU8		uncertaintySemiMajor;
		McpU8		uncertaintySemiMinor;
		McpU8		orientationMajorAxis;
		McpU8		confidence;
		McpU32		latitude;
		McpU32		longtitude;
		McpU32		altitude;
		GPS_Velocity  velocity;
	}GPS_Position;


 typedef struct
{
EGPSC_LocFixMode          loc_fix_mode;              /*  Location Fix Mode (T_GPSC_loc_fix_mode) */
  McpU16                    loc_fix_result_type_bitmap;/*  Location Fix Result Type Bitmap */
  McpU16                    loc_fix_num_reports;       /*  Number of reports in loc fix request */
  McpU16                    loc_fix_period;            /*  Location fix period */
  McpU16                    loc_fix_max_ttff;          /*  Location fix max TTFF, seconds, 0xFFFF = no max TTFF, 0x0000 = unknown */
  McpU32			        loc_fix_session_id;        /*  The session tag ID */
  EGPSC_call_type		    call_type;
  T_GPSC_loc_fix_qop        loc_fix_qop;               /*  Location fix Quality of Position. A set of attributes associated with a request for the geographic position of SET. The attributes include the required horizontal accuracy, vertical accuracy, max location age, and response time of the SET position. */
  T_GPSC_loc_fix_velocity   loc_fix_velocity;          /*  Velocity may be associated with a universal Geographical Area Description when both are applied to a common entity at a common time. */

  McpU8                     loc_fix_pos_type_bitmap;   /* Location fix position type ; last 3 bit indicate the type of position is requested, any combination can be used
                                                                                                    if bleneded position is needed
                                                                                                    000GPS Only (Default)
                                                                                                    001 GPS Only (Default)
                                                                                                    010 Wifi Only
                                                                                                    100 Sensor Only
                                                                                                    011 Blended wifi+gps*/
  T_GPSC_sess_type          loc_sess_type;
  McpU8                     zzz_align1[3];            /* alignment */


} Loc_Fix_request; /* Replaces  T_GPSC_loc_fix_req_params */



McpU8 Navc_PriorityTable_GetResult(NAVC_OngoingSession Ongoing_session,NAVC_IncomingSession Incoming_session);

Navc_PriorityHashTable* Navc_PriorityTable_Search(NAVC_OngoingSession Ongoing_session,NAVC_IncomingSession Incoming_session);

McpU8 Navc_Set_Priority(NAVC_OngoingSession Ongoing_session,NAVC_IncomingSession Incoming_session,NAVC_PriorityAction Priority_Action);



McpDBL convert_gps2julianday( McpU32 gpsTime);

McpU16 *convert_julianday2caldate(McpDBL  JD);

McpU16 * convert_gps2cal( McpU32 gpsTime);


McpU8 * convert_gps2utc(McpU32 gpsTime, McpU8 *p_len );











#endif

