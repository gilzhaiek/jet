/*

 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   Navc_priority_handler.c
 *
 * Description      :
 *
 * Author           :   Ashish Dhiman
 * Date             :   01-august-2011
 *
 ******************************************************************************
 */


#include "gpsc_data.h"

#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "navc_api.h"
#include "navc_defs.h"
#include "navc_time.h"
#include "navc_cmdHandler.h"
#include "navc_sm.h"
#include "gpsc_app_api.h"
#include "gpsc_msg.h"
#include "gpsc_mgp_cfg.h"


Navc_PriorityHashTable* PriorityHashTable;
U32 CP_SessionId;
U8 SUPL_NI_Pending = FALSE;
U8 Loc_request_pending = FALSE;
Loc_Fix_request *LocFixRequest = NULL;




Navc_PriorityHashTable* Navc_PriorityTable_Search(NAVC_OngoingSession Ongoing_session,NAVC_IncomingSession Incoming_session){

STATUSMSG("Navc_PriorityTable_Search \n");
 Navc_PriorityHashTable* np = PriorityHashTable;
 for(;np!=NULL;np=np->Next_node){
 	if((np->OngoingSession) == Ongoing_session)
   		if((np->IncomingSession) == Incoming_session)
	 	return np;
}
return NULL;
}


McpU8 Navc_PriorityTable_GetResult(NAVC_OngoingSession Ongoing_session,NAVC_IncomingSession Incoming_session){

  STATUSMSG("Navc_PriorityTable_GetResult \n");
  Navc_PriorityHashTable* n=Navc_PriorityTable_Search(Ongoing_session,Incoming_session);
  if(n==NULL)
    return NULL;
  else {
    return n->Action;
        }
}

McpU8 Navc_Set_Priority(NAVC_OngoingSession Ongoing_session,NAVC_IncomingSession Incoming_session,NAVC_PriorityAction Priority_Action){

  Navc_PriorityHashTable* np = NULL;
  STATUSMSG("Navc_Set_Priority \n");
if((np=Navc_PriorityTable_Search(Ongoing_session,Incoming_session))==NULL){

	np = (Navc_PriorityHashTable *)mcpf_mem_alloc(NULL,sizeof(Navc_PriorityHashTable));
	if(np==NULL)
		return GPSC_FAIL;
	np->OngoingSession=Ongoing_session;
	np->IncomingSession=Incoming_session;
	if(np->OngoingSession==NULL)
		return 0;
	if(np->IncomingSession==NULL)
		return 0;
	np->Next_node=PriorityHashTable;
	PriorityHashTable=np;
}
  //else {
  	np->Action=Priority_Action;
 // }
  if(np->OngoingSession==NULL)
  	return GPSC_FAIL;
  if(np->IncomingSession==NULL)
  	return GPSC_FAIL;

  return GPSC_SUCCESS;
}

T_GPSC_result Navc_Priority_Handler(TNAVC_reqLocFix* loc_fix_req_params) 
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;
	gpsc_sess_cfg_type*	p_zSessConfig;
	gpsc_cfg_type*	p_zGPSCConfig;
   gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;

	gpsc_event_cfg_type * p_zEventCfg;
   T_GPSC_result gpscRes = GPSC_SUCCESS;
	McpU8 Result;

	SAPENTER(Navc_Priority_Handler);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;
	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	p_zEventCfg   = p_zGPSCControl->p_zEventCfg;
	handle_t	 pMcpf  = (handle_t *)p_zGPSCControl->p_zSysHandlers->hMcpf;

	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	static TNAVC_reqLocFix  *pReqLocFix = NULL;
	pReqLocFix = (TNAVC_reqLocFix *)mcpf_mem_alloc(NULL,sizeof(TNAVC_reqLocFix));
	pReqLocFix->loc_fix_mode = loc_fix_req_params->loc_fix_mode;
	pReqLocFix->loc_fix_result_type_bitmap = loc_fix_req_params->loc_fix_result_type_bitmap;
	pReqLocFix->loc_fix_num_reports = loc_fix_req_params->loc_fix_num_reports;
	pReqLocFix->loc_fix_period = loc_fix_req_params->loc_fix_period;
	pReqLocFix->loc_fix_max_ttff = loc_fix_req_params->loc_fix_max_ttff;
	pReqLocFix->loc_fix_session_id = loc_fix_req_params->loc_fix_session_id;
	pReqLocFix->loc_fix_qop = loc_fix_req_params->loc_fix_qop;
	pReqLocFix->loc_fix_velocity = loc_fix_req_params->loc_fix_velocity;
	pReqLocFix->loc_sess_type = loc_fix_req_params->loc_sess_type;
	pReqLocFix->loc_fix_pos_type_bitmap = loc_fix_req_params->loc_fix_pos_type_bitmap;
	pReqLocFix->call_type = loc_fix_req_params->call_type;

	/* if no node present - no session running */
	if (p_zSessSpecificCfg == NULL) 
   {
		 gpscRes = gpsc_app_loc_fix_req(pReqLocFix, 0);
		 if (gpscRes < GPSC_SUCCESS)
			return gpscRes;
	}

	else 
   { /* session is running */
		while (p_zSessSpecificCfg != NULL) 
      {
			 Result = Navc_PriorityTable_GetResult(p_zSessSpecificCfg->call_type, loc_fix_req_params->call_type);

			 switch (Result)
    			{
				case NAVC_ACCEPTINCOMING_STOPONGOING: /* stop previous session and start new session */

				switch (p_zSessSpecificCfg->call_type) /* check for ongoing session*/
				{
					/* in case of ongoing SUPL session stop the supl session and start another session */
					case NAVC_SUPL_MO:

						if (Stop_Supl_Session()== GPSC_SUCCESS) /* stop the current supl session but not the NAVC session */
						{
				 		   gpscRes = gpsc_app_loc_fix_req(pReqLocFix,0);
						   gpsc_app_priority_override_ind(); 
						}

		 		      if (gpscRes < GPSC_SUCCESS)
                     return gpscRes;

					break;

					case NAVC_SUPL_MT_POSITIONING:
						Loc_request_pending = TRUE;

				 		LocFixRequest = (Loc_Fix_request *)mcpf_mem_alloc(NULL,sizeof(Loc_Fix_request));
				 		mcpf_mem_copy(pMcpf, LocFixRequest, loc_fix_req_params, sizeof(TNAVC_reqLocFix));

						Stop_Supl_Session();
						if (gpsc_app_loc_fix_stop_req(p_zSessSpecificCfg->w_SessionTagId) == GPSC_SUCCESS) 
						{
						   gpsc_app_priority_override_ind(); /* Sent noti to app about priority */
						}

					break;

				}

				break;

				case NAVC_IGNOREINCOMING: /*Ignore the incoming request and dont stop the previous session */

				STATUSMSG("NAVC_IGNOREINCOMING :Ignore the incoming request \n");
				 gpsc_app_priority_override_ind(); /* Sent noti to app about priority */
				break;

				case NAVC_HOLD:

					if (p_zSessSpecificCfg->call_type == NAVC_CP_NILR) 
               {
					   CP_SessionId= p_zSessSpecificCfg->w_SessionTagId;
					
						if (loc_fix_req_params->call_type == NAVC_SUPL_MT_POSITIONING) 
						   SUPL_NI_Pending= TRUE;
					}

				break;

				case NAVC_NOEFFECT: /* dont stop any session (no effect on previous session)*/
            {
				   return gpsc_app_loc_fix_req(pReqLocFix, 0);
            }
				break;

				default: /* dont stop any session (no priority set)*/
            {
				   return gpsc_app_loc_fix_req(pReqLocFix, 0);
            }
				break;

			 }
			 p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
		}

	}
	mcpf_mem_free(NULL,pReqLocFix);
	return gpscRes;
}

T_GPSC_result Stop_Supl_Session() 
{

T_GPSC_result	res = GPSC_FAIL;
gpsc_ctrl_type*    p_zGPSCControl;
p_zGPSCControl = gp_zGPSCControl;
gpsc_smlc_assist_type*	p_zSmlcAssist	= p_zGPSCControl->p_zSmlcAssist;

SAPENTER(Stop_Supl_Session);


if(p_zSmlcAssist->u_NumAssistAttempt)
	{
		STATUSMSG("Stop_Supl_Session : pirority override, Stopping SUPL session");
		res = gpsc_app_stop_request_ind();
	  	 if(res == GPSC_FAIL)
		{
			STATUSMSG("gpsc_app_stop_request_ind : Stop SUPL Session Failed");
		}
		p_zSmlcAssist->u_NumAssistAttempt=0;
	}
	else
	{
		STATUSMSG("Stop_Supl_Session : Assistance data not requested or Nw session already stopped");
	}

return res;


}



/*callback send from gpsc if position is available */
void gpsc_send_position_to_pirority_handler(gpsc_ctrl_type* p_zGPSCControl,T_GPSC_prot_position* p_prot_position)
{

gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
gpsc_sess_specific_cfg_type* p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
T_GPSC_result	res = GPSC_FAIL;
gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
gpsc_db_pos_type *p_DBPos = &p_zGPSCDatabase->z_DBPos;
TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

SAPENTER(gpsc_send_position_to_pirority_handler);



if(p_zSessSpecificCfg->w_SessionTagId == CP_SessionId) {
	STATUSMSG("gpsc_send_position_to_pirority_handler : position Recieved for Control Plane session \n");
	if(SUPL_NI_Pending == TRUE) {
		STATUSMSG("gpsc_send_position_to_pirority_handler : SUPL NI Session is Pending, So send the latest position to SUPL \n");
		//	res = Send_Position_to_SUPL(p_zGPSCControl,p_prot_position);
		SUPL_NI_Pending = FALSE;
		supl_qop qop_to_supl;
		U16 assist_bitmap_mandatory = (U16)(0);
		U16 assist_type_optional = (U16)(0);
		T_GPSC_nav_model_req_params *p_nav_model = NULL;


		STATUSMSG("Send_Position_to_SUPL : Sending postion to SUPL for Pending NI session \n");


		memset(&qop_to_supl,0x00,sizeof(supl_qop));

		GPS_Position gpsPosInfo;
		memset(&gpsPosInfo,0x00,sizeof(GPS_Position));

		gpsPosInfo.pos_opt_bitmap=0x01;
		gpsPosInfo.latitude=p_prot_position->ellip_alt_unc_ellip.latitude;
		gpsPosInfo.latitude_sign=p_prot_position->ellip_alt_unc_ellip.latitude_sign;
		gpsPosInfo.longtitude=p_prot_position->ellip_alt_unc_ellip.longitude;
		gpsPosInfo.altitude=p_prot_position->ellip_alt_unc_ellip.altitude;
		gpsPosInfo.altitudeDirection=p_prot_position->ellip_alt_unc_ellip.altitude_sign;
		gpsPosInfo.altUncertainty=p_prot_position->ellip_alt_unc_ellip.unc_alt;
		gpsPosInfo.uncertaintySemiMinor=p_prot_position->ellip_alt_unc_ellip.unc_minor;
		gpsPosInfo.uncertaintySemiMajor=p_prot_position->ellip_alt_unc_ellip.unc_major;

		//to do - UTC time to be added

		memset(gpsPosInfo.UTCTimeStamp, 0x00, 20);


		STATUSMSG("gpsc_send_position_to_pirority_handler:gps time : %f",p_DBPos->d_ToaSec);
		McpU8 * p_utctime = convert_gps2utc(p_DBPos->d_ToaSec, &gpsPosInfo.UTCTimeStampNumByte);;
		gpsPosInfo.UTCTimeStampNumByte = 13;
		memcpy(gpsPosInfo.UTCTimeStamp,p_utctime,gpsPosInfo.UTCTimeStampNumByte);

		qop_to_supl.delay = 0;
		qop_to_supl.horr_acc = 100;
		qop_to_supl.max_loc_age = 0;
		qop_to_supl.max_response_time = 0;
		qop_to_supl.qop_optional_bitmap = 0;
		qop_to_supl.ver_acc = 0;


		if((pNavc->tAssistSrcDb[SUPL_PROVIDER].uAssistSrcType == SUPL_PROVIDER) && 
			(pNavc->tAssistSrcDb[SUPL_PROVIDER].uAssistSrcPriority != 0))
		{
			res = gpsc_app_assist_request_ind (assist_bitmap_mandatory,assist_type_optional, p_nav_model,&gpsPosInfo ,&qop_to_supl, NULL,
								pNavc->tAssistSrcDb[SUPL_PROVIDER].uAssistProviderTaskId,
								pNavc->tAssistSrcDb[SUPL_PROVIDER].uAssistProviderQueueId) ;
			if(res == GPSC_FAIL){
				STATUSMSG("gpsc_send_position_to_pirority_handler : Send position to SUPL Failed");
			}
		}
	    }
	}
}


T_GPSC_result gpsc_send_noti_to_priority_handler(gpsc_ctrl_type* p_zGPSCControl) {

SAPENTER(gpsc_send_noti_to_priority_handler);
T_GPSC_result	res = GPSC_FAIL;
if (p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_IDLE)
	{
	STATUSMSG("gpsc_send_noti_to_priority_handler : RECIEVER IDLE \n");
	if(Loc_request_pending)
		{
			STATUSMSG("gpsc_send_noti_to_priority_handler : loc request is pending so sending loc fix request \n");
			 res = gpsc_app_loc_fix_req(LocFixRequest,0);
		 		if (res < GPSC_SUCCESS)
        			{
			 		STATUSMSG("ERROR.gpsc_app_loc_fix_req failed \n");
					return res;
        			}
        			Loc_request_pending = FALSE;

		}
	}

	mcpf_mem_free(NULL,LocFixRequest);
	return res;

}


McpDBL convert_gps2julianday( McpU32 gpsTime)
{

    //Constants
    McpDBL SEC_PER_DAY     = 86400;
    //McpDBL NUM_GPS_WEEKS   = 1024;  //% Number of GPS weeks before a week rollover  /* Klocwork Changes */

   //Julian day number of the birthday of GPS (Midnight of January 5, 1980)
   //Can compute using January 6, 1980, HMS = 00:00:00
   McpDBL JD_GPS = 2444244.5;

   //Compute the (decimal) number of elapsed days since the start of GPS
   McpDBL gpsDays = (gpsTime / SEC_PER_DAY)  + 0.5;

    //Compute the current (decimal) Julian Day
    //JD = JD_GPS + gpsDays - 0.5
    return (JD_GPS + gpsDays - 0.5);
}

McpU16 *convert_julianday2caldate(McpDBL  JD)
{

    static McpU16 cal_time[6] = {0,0,0,0,0,0};
    //Constants
    McpDBL SEC_PER_DAY  = 86400;

    // Most of the calculations do not need the decimal portion
    McpDBL JDN = floor(JD);

    //Compute Seconds After Midnight (SAM) - note that days in the Julian
    //date system start at 12:00pm, so must adjust the SAM accordingly
    McpDBL partialDay = JD - JDN;
    McpDBL sam = 0; /* Klocwork changes */

    if ( (partialDay >= 0) && (partialDay < 0.5) )
        sam = (partialDay + 0.5) * SEC_PER_DAY;

    else if( (partialDay >= 0.5) && (partialDay < 1.0) )
        sam = (partialDay - 0.5) * SEC_PER_DAY;

    //else
        //LOGD("SAM requires partial days, i.e. less than 86400 seconds!\n");

    //Compute YMD
    McpDBL Z,W,X,A,B,C,D,E,F;

    Z = JD + 0.5 ;
    W = floor( (Z - 1867216.25) / 36524.25 );
    X = floor( W / 4 );
    A = Z + 1 + W - X;
    B = A + 1524;
    C = floor( (B - 122.1) / 365.25 );
    D = floor( 365.25 * C );
    E = floor( (B - D) / 30.6001 );
    F = floor( 30.6001 * E );

    //Compute day of the month
    //day = floor(B - D - F);
    cal_time[2] = floor(B - D - F);

    //Compute month of the year
    if( (E - 1) <= 12 )
        cal_time[1] =  E - 1;    //month = E - 1;
    else if( (E-13) <= 12 )
          cal_time[1] = E - 13;  //month = E - 13;
    else
        //LOGD("Can''t get number less than 12 in gpsc_app_convert_julianday2caldate \n");

    //Compute year
    if ( cal_time[1] < 3 )        // month < 3
        cal_time[0] = C - 4715;   // year = C - 4715;
    else
        cal_time[0] = C - 4716;   //year = C - 4716;


       McpDBL TOL = 0.000001;  //% floor function is sensitive to near-zero values
       cal_time[3] = floor ((sam / 3600) + TOL);
       cal_time[4] = floor( ((sam - (cal_time[3] * 3600) ) / 60 ) + TOL );
       cal_time[5] = ceil( sam - cal_time[3] * 3600 - cal_time[4] * 60);

    //LOGD("In gpsc_app_convert_julianday2caldate\n\n");
    //LOGD("year = %d month = %d day = %d hh = %d mm = %d ss = %d \n",cal_time[0],cal_time[1],cal_time[2],cal_time[3],cal_time[4],cal_time[5]);


    return cal_time;

}


McpU16 * convert_gps2cal( McpU32 gpsTime)
 {
     McpDBL JD = convert_gps2julianday( gpsTime);
     return convert_julianday2caldate(JD);

 }

McpU8 * convert_gps2utc(McpU32 gpsTime, McpU8 *p_len )
{
    McpU16 *cal_time;
    cal_time = convert_gps2cal(gpsTime); //747437400

    static McpU8 utcTs[50];
    McpU8 buff[3];

   /**  Convert unsigned short to unsigned char  **/

    if (cal_time[0] > 2000)
    {
        cal_time[0] = cal_time[0] - 2000;
        if (cal_time[0] < 10)
        {
            MCP_HAL_STRING_ItoA (0,utcTs);
            MCP_HAL_STRING_ItoA (cal_time[0],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);

        }else
        {
            MCP_HAL_STRING_ItoA (cal_time[0],utcTs);
        }
    }
    else
    {
        MCP_HAL_STRING_ItoA (cal_time[0],utcTs);
    }

    if (cal_time[1] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[1],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[1],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[2] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[2],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);

    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[2],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[3] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[3],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[3],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[4] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[4],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[4],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[5] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[5],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[5],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }

    MCP_HAL_STRING_StrCat(utcTs,"Z");
    *p_len = MCP_HAL_STRING_StrLen(utcTs);

    return utcTs;

}









