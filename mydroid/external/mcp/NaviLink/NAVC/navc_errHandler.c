#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_data.h"
#include "gpsc_drv_api.h"
#include "gpsc_comm.h"
#include "gpsc_mgp.h"
#include "gpsc_msg.h"
#include "gpsc_state.h"
#include "gpsc_timer_api.h"
#include <stdio.h>
#include "navc_errHandler.h"

//#define RE_TRANSMIT 01

extern gpsc_comm_tx_buff_type  g_gpsc_comm_tx_buff;
extern U8 g_ack;
extern U8 g_retransmitFlag;
extern U8 g_databuffer[2048];
extern U16 g_NumBytes;
U8 g_retransmitCount =0;

  
  
U16 gpsc_err_handler(gpsc_ctrl_type*  p_zGPSCControl,U8 code)
{
	U16 ret =0;
  
  switch(code)
  {
    case RE_TRANSMIT : // this is the case for retransmit
    {
      g_retransmitFlag =1; //set the flag so that we indicate that we are in the re-transmit mode
      g_retransmitCount ++; //increment the count
      if(g_retransmitCount >2)
      {
      
        gpsc_sess_cfg_type *p_zSessConfig;
        p_zSessConfig = p_zGPSCControl->p_zSessConfig;

        g_retransmitFlag=0;

        
        //go back to idle state and close all the session...
         STATUSMSG("retransmission count excceded count limit so going to idle state, del all sessions");
         
         p_zGPSCControl->p_zGPSCCommTxBuff->u_AI2AckPending =0; // so that message for idle goes out...

         gpsc_sess_end(p_zGPSCControl); //close all the session and go to idle state
   
         /* Turn off all async/periodic events */
	     //gpsc_mgp_cfg_evt_rep_init ( p_zGPSCControl );

#ifdef LARGE_UPDATE_RATE
	     /* q_SleepDuration = 0 - if in case session sleep when Rp > 30 seconds */
	     p_zSessConfig->q_SleepDuration = 0;
#endif
	     p_zSessConfig->u_ReConfigFlag = FALSE;
	     p_zSessConfig->w_TotalNumSession = 0;

				//Send protocol select command
				gpsc_inject_comm_protocol (p_zGPSCControl);


				return ret;

      }
			else
			{
      gpsc_transmit_data(&g_databuffer[0], g_NumBytes);
      g_retransmitFlag =0;
    }
    }
    break;
		
    default :
    break;
  }

return ret;
}
