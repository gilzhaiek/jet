

/*********************************************************************************

                                   TI GPS Confidential

*********************************************************************************/
/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	gpsc_geofence.h
 *
 * Description     	:
 * 
 *
 *
 * Author         	: 	Makbul Siram - makbul@ti.com
 *
 *
 ******************************************************************************
 */



#ifndef _GPSC_GEOFENCE_H
#define _GPSC_GEOFENCE_H

#include "gpsc_data.h"
#include "gpsc_types.h"

U8 gpsc_check_geofence_features(T_GPSC_geo_fence_config_params *p_zGeoFenceConfig,
                                S32 lat, S32 lan, S16 alt, S16 *p_Velocity);
U8 mapToXY(S32 *p_DestnX , S32 *p_DestnY, S32 latitude, S32 longitude);
U8 gpsc_geofence_check_line(T_GPSC_geo_fence_config_params *p_zGeoFenceConfig);
U8 gpsc_geofence_sort_valid_vertices(T_GPSC_geo_fence_config_params *p_zGeoFenceConfig);
#endif