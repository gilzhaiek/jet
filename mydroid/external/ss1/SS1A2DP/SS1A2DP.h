/*****< SS1A2DP.h >************************************************************/
/*      Copyright 2000 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1A2DP - Stonestreet One A2DP interface for Android AudioFlinger Type    */
/*            Definitions, Constants, and Prototypes                          */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   09/15/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __SS1A2DP__
#define __SS1A2DP__

#include "SS1UTIL.h"

typedef void *A2DP_Handle_t;

#ifdef __cplusplus
extern "C"
{
#endif

int  SS1API a2dp_init(int Rate, int Channels, A2DP_Handle_t *HandlePtr);

void SS1API a2dp_set_sink(A2DP_Handle_t Handle, const char *Address);

int  SS1API a2dp_write(A2DP_Handle_t Handle, const void *Buffer, int BufferLength);

int  SS1API a2dp_stop(A2DP_Handle_t Handle);

void SS1API a2dp_cleanup(A2DP_Handle_t Handle);

#ifdef __cplusplus
}
#endif

#endif /* __SS1A2DP__ */
