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

/** 
 *  \file   bmtrace.h 
 *  \brief  Event trace tool
 * 
 * This file contains trace tool function specification
 * 
 *  \see    bmtrace.c
 */


#ifndef __BM_TRACER_H
#define __BM_TRACER_H

#define CL_TRACE_CONTEXT_TASK	"Task"
#define CL_TRACE_CONTEXT_ISR	"ISR"
#define CL_TRACE_CONTEXT_SYSTEM	"Sys"

#define CL_TRACE_START() \
	unsigned long in_ts = bm_act_trace_in();

#define CL_TRACE_RESTART_Lx() \
	in_ts = bm_act_trace_in();

#ifdef TI_BMTRACE
	#define CL_TRACE_INIT(hMcpf)    bm_init(hMcpf)
	#define CL_TRACE_DEINIT()     	bm_deinit()
	#define CL_TRACE_ENABLE()   	bm_enable()
	#define CL_TRACE_DISABLE()  	bm_disable()
	#define CL_TRACE_PRINT(buf) 	bm_print_out_buffer(buf)
	#define CL_TRACE_SAVE()			bm_save_to_file()
	#define CL_TRACE_START_L1() 	CL_TRACE_START()
	#define CL_TRACE_RESTART()  	CL_TRACE_RESTART_Lx()
	#define CL_TRACE_END_L1(mod, cntxt, grp, sufx) \
		{ \
			static int loc; \
			if (unlikely(loc == 0)) \
				loc = bm_act_register_event((mod), (cntxt), (grp), 1, (char*)__FUNCTION__, (sufx), 0); \
			bm_act_trace_out(loc, in_ts); \
		}
	#define CL_TRACE_TASK_DEF() 	McpU32	in_ts; McpS32  loc=0;
	#define CL_TRACE_TASK_START() 	in_ts = bm_act_trace_in();
	#define CL_TRACE_TASK_END(mod, cntxt, grp, sufx) \
			if (unlikely(loc == 0)) \
				loc = bm_act_register_event((mod), (cntxt), (grp), 1, (char*)__FUNCTION__, (sufx), 0); \
			bm_act_trace_out(loc, in_ts);
#else
    #define CL_TRACE_INIT(hMcpf)   
    #define CL_TRACE_DEINIT()
    #define CL_TRACE_ENABLE() 
    #define CL_TRACE_DISABLE()
    #define CL_TRACE_RESTART() 
    #define CL_TRACE_PRINT(buf)  
	#define CL_TRACE_SAVE()
	#define CL_TRACE_START_L1() 
	#define CL_TRACE_END_L1(mod, cntxt, grp, sufx) 
	#define CL_TRACE_TASK_DEF()
	#define CL_TRACE_TASK_START()
	#define CL_TRACE_TASK_END(mod, cntxt, grp, sufx)
#endif


/** 
 * \fn     bm_init
 * \brief  Tracer initialization
 * 
 * This function initializes the tracer object
 * 
 * \note
 * \param	hMcpf - handle to MCPF
 * \return 	void
 * \sa     	bm_deinit
 */
void 		bm_init (handle_t hMcpf);

/** 
 * \fn     bm_deinit
 * \brief  Tracer de-initialization
 * 
 * This function de-initializes the tracer object
 * 
 * \note
 * \return 	void
 * \sa     	bm_deinit
 */
void		bm_deinit (void);

/** 
 * \fn     bm_enable
 * \brief  Enable tracing
 * 
 * This function enable logging of events into the trace buffer
 * 
 * \note
 * \return 	void
 * \sa     	bm_disable
 */
void		bm_enable (void);

/** 
 * \fn     bm_disable
 * \brief  Disable tracing
 * 
 * This function disables logging of events into the buffer
 * 
 * \note
 * \return 	void
 * \sa     	bm_enable
 */
void		bm_disable(void);

/** 
 * \fn     bm_act_trace_in
 * \brief  Start of event logging
 * 
 * This function starts specific event logging and returns time stamp
 * 
 * \note
 * \return 	void
 * \sa     	bm_act_trace_out
 */
unsigned long   bm_act_trace_in (void);

/** 
 * \fn     bm_act_trace_out
 * \brief  Endx of event logging
 * 
 * This function starts specific event logging
 * 
 * \note
 * \param	event_id - event ID
 * \param	in_ts    - timestamp of event start
 * \return 	void
 * \sa     	bm_act_trace_in
 */
void            bm_act_trace_out (int event_id, unsigned long in_ts);

/** 
 * \fn     bm_act_register_event
 * \brief  Register event
 * 
 * This function registeres event in tracer and adds event description
 * 
 * \note
 * \param	module  - module name of event
 * \param	context - context name of event
 * \param	group   - group name of event
 * \param	level   - event level number
 * \param	name    - event name
 * \param	suffix  - event name suffix
 * \param	is_param  - is parameter is used instead of end timestamp
 * \return 	event id of registered event
 * \sa     	bm_act_trace_in
 */
int             bm_act_register_event (char* module, 
									   char* context, 
									   char* group, 
									   unsigned char level, 
									   char* name, 
									   char* suffix, 
									   int is_param);

/** 
 * \fn     bm_print_out_buffer
 * \brief  Output trace buffer
 * 
 * This function produces formated text output of trace buffer to specified buffer
 * 
 * \note
 * \param	pBuf - output buffer
 * \sa     	bm_save_to_file
 */
int             bm_print_out_buffer (char *pBuf);

/** 
 * \fn     bm_save_to_file
 * \brief  Save trace buffer to file
 * 
 * This function saves the trace buffer to text file
 * 
 * \note
 * \sa     	bm_print_out_buffer
 */
void            bm_save_to_file ( void );


#endif /* _TRACER_H */
