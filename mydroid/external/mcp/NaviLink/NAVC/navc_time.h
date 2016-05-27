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

/** \file   navc_time.h 
 *  \brief  NAVC time module interface specification
 * 
 * This file contains functions to manage GPS coarse and fine time
 * 
 *  \see    navc_time.c
 */

#ifndef _NAVC_TIME_H
#define _NAVC_TIME_H

#include "navc_defs.h"


/************************************************************************
 * Defines
 ************************************************************************/


/************************************************************************
 * Types
 ************************************************************************/

/** 
 * \fn     navcTime_init
 * \brief  Initialize GPS time using host system timer
 * 
 * The function sets initial GPS time from host system time:
 * GPS time of week, GPS msec, time uncertainty 
 * 
 * \note
 * \param	pNavc 	  - pointer to NAVC object
 * \param	uTimeUnc  - initial time uncertainty
 * \return	void
 * \sa
 */
void navcTime_init (TNAVCD_Ctrl	*pNavc, McpU32 uTimeUnc, McpBool bPulseReqEnable);


/** 
 * \fn     navcTime_setGpsTime
 * \brief  Set GPS Time of NAVC object
 * 
 * The function sets GPS time estimation: GPS time of week, GPS msec, time uncertainty, 
 * of system time relating to the GPS time etc. 
 * 
 * \note
 * \param	pNavc 	  - pointer to NAVC object
 * \param	pGpsTime  - pointer to GPS time structure
 * \param	eTimeMode - time mode: time now or time of pulse used for fine time injection
 * \return	void
 * \sa
 */
void navcTime_setGpsTime (TNAVCD_Ctrl 		*pNavc, 
						  TNAVCD_GpsTime 	*pGpsTime,
						  ENAVCD_TimeMode 	eTimeMode);



#endif /* _NAVC_TIME_H */


