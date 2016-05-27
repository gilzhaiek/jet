/*
 * Copyright (c) 2007-2011 Rx Networks, Inc. All rights reserved.
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
 * $LastChangedDate: 2009-06-25 13:19:37 -0700 (Thu, 25 Jun 2009) $
 * $Revision: 11690 $
 *************************************************************************
 *
 * This file contains implementation of common APIs exposed within the RXN_CL.h file.
 * 
 */

#include "RXN_CL_Common.h"  /* Internal common declarations. */
#include "RXN_CL_RINEX.h"   /* RINEX - specific declarations. */

static const U08 numOfPRNs[2] =
{
    RXN_CONSTANT_NUM_GPS_PRNS,
    RXN_CONSTANT_NUM_GLONASS_PRNS
};

U16 RXN_CL_RINEX_Get_API_Version(char version[RXN_CONSTANT_VERSION_STRING_LENGTH])
{
  sprintf(version, "%s", RXN_CL_VER);
  
  return RXN_SUCCESS;
}

U16 RXN_CL_RINEX_Initialize(const char config[RXN_CL_CONFIG_MAX_STR_LEN], U16* phandle)
{
  U16 result = RXN_SUCCESS;               /* Fcn call result store. */

  /* Globals below - see RXN_CL_Common.c */
  extern U08 gCL_Instance_Cnt;
  extern CL_instance_t gCL_Instances[];
  extern U16 gCL_Next_Instance_Handle;

  /* If this is the first initialization (i.e. the first CL instance),
   * clear the instances array. */
  if(gCL_Instance_Cnt == 0)
  {
    memset(&gCL_Instances, 0, (sizeof(CL_instance_t) * CL_MAX_INSTANCES));
  }

  /* Validate that another instance is ok. */
  if(gCL_Instance_Cnt >= CL_MAX_INSTANCES)
  {
    return RXN_CL_TOO_MANY_INSTANCES_ERR;
  }

  /* Add a new instance to gCL_Instances. */
  gCL_Instances[gCL_Instance_Cnt].CL_Handle = gCL_Next_Instance_Handle;

  /* Set the handle. */
  *phandle = gCL_Next_Instance_Handle;

  /* Call the chipset specific implementation. */
  result = CL_RINEX_Initialize(&(gCL_Instances[gCL_Instance_Cnt].CL_Attrib), config);

  /* Increment the counter and the next handle. */
  gCL_Next_Instance_Handle++;
  gCL_Instance_Cnt++;

  /* If the call to Initialize() wasn't successful - un-init. */
  if(result != RXN_SUCCESS)
  {
    /* Un-Init for cleanup. */
    RXN_CL_RINEX_Uninitialize(*phandle);
  }

  return result;
}

U16 RXN_CL_RINEX_Uninitialize(U16 handle)
{
  U08 y;                  /* Counter. */
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* RXN_CL_Uninitialize result. */
  
  /* Globals below - see RXN_CL_Common.c */
  extern U08 gCL_Instance_Cnt;
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Call the chipset specific implementation. */
  result = CL_RINEX_Uninitialize(&(gCL_Instances[handleIdx].CL_Attrib));

  /*  Shuffle down any instances that follow in the
      array to replace this instance. */
  for(y=handleIdx+1; y<CL_MAX_INSTANCES; y++)
  {
    gCL_Instances[y-1] = gCL_Instances[y];
  }

  /* Reduce the instance count. */
  gCL_Instance_Cnt--;

  /* Return the result. */
  return result;
}

U16 RXN_CL_RINEX_ReadEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  
  /* Globals below - see RXN_CL_Common.c */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Verify that applicable storage is available. */
  if (pNavDataList->numEntries == 0 || pNavDataList->numEntries > numOfPRNs[constel])
  {
    return RXN_CL_ALLOCATION_ERROR;
  }

  /* Call the chipset specific implementation. */
  if(constel == RXN_GPS_CONSTEL)
  {
      result = CL_RINEX_ReadEphemeris(&(gCL_Instances[handleIdx].CL_Attrib), &pNavDataList->ephList.gps[0], pNavDataList->numEntries);
  }
  else if(constel == RXN_GLO_CONSTEL)
  {
    result = CL_RINEX_ReadEphemerisGLO(&(gCL_Instances[handleIdx].CL_Attrib), &pNavDataList->ephList.glo[0], pNavDataList->numEntries);
  }
  return result;
}

U16 RXN_CL_RINEX_WriteEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  
  /* Globals below - see RXN_CL_Common.c */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Verify that applicable storage is available. */
  if (pNavDataList->numEntries == 0 || pNavDataList->numEntries > numOfPRNs[constel])
  {
    return RXN_CL_ALLOCATION_ERROR;
  }

  /* Call the chipset specific implementation. */
  if(constel == RXN_GPS_CONSTEL)
  {
      result = CL_RINEX_WriteEphemeris(&(gCL_Instances[handleIdx].CL_Attrib), &pNavDataList->ephList.gps[0], pNavDataList->numEntries);
  }
  else if(constel == RXN_GLO_CONSTEL)
  {
    result = CL_RINEX_WriteEphemerisGLO(&(gCL_Instances[handleIdx].CL_Attrib), &pNavDataList->ephList.glo[0], pNavDataList->numEntries);
  }
  return result;
}


U16 RXN_CL_RINEX_Work(U16 handle)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  
  /* Globals below - see RXN_CL_Common.c */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Call the implementation. */
  CL_RINEX_Work();

  return RXN_SUCCESS;
}

void RXN_CL_RINEX_RXNToFull( RXN_ephem_t* pRXN, RXN_FullEph_t* pFull, U08 CAOrPOnL2,
                U16 iodc, U08 L2PData, S08 TGD, U08 AODO)
{
	/* Map pRXN to pFull and set additional params within pFull. */
	pFull->prn = pRXN->prn;
	pFull->gps_week = pRXN->gps_week;
	pFull->CAOrPOnL2 = CAOrPOnL2;
	pFull->ura = pRXN->ura;
	pFull->health = pRXN->health;
	pFull->iodc = iodc;
	pFull->L2PData = L2PData;
	pFull->TGD = TGD;
	pFull->toc = pRXN->toc;
	pFull->af2 = pRXN->af2;
	pFull->af1 = pRXN->af1;
	pFull->af0 = pRXN->af0;
	pFull->crs = pRXN->crs;
	pFull->delta_n = pRXN->delta_n;
	pFull->m0 = pRXN->m0;
	pFull->cuc = pRXN->cuc;
	pFull->e = pRXN->e;
	pFull->cus = pRXN->cus;
	pFull->sqrt_a = pRXN->sqrt_a;
	pFull->toe = pRXN->toe;
	pFull->ephem_fit = pRXN->ephem_fit;
    pFull->ure = pRXN->ure;
	pFull->AODO = AODO;
	pFull->cic = pRXN->cic;
	pFull->omega0 = pRXN->omega0;
	pFull->cis = pRXN->cis;
	pFull->i0 = pRXN->i0;
	pFull->crc = pRXN->crc;
	pFull->w = pRXN->w;
	pFull->omega_dot = pRXN->omega_dot;
	pFull->iode = pRXN->iode;
	pFull->i_dot = pRXN->i_dot;

	return;
}

void RXN_CL_RINEX_RXNToFull_GLO(RXN_glonass_ephem_t* pRXN, RXN_FullEph_GLO_t* pFull)
{
	//memcpy(pFull, pRXN,sizeof(RXN_FullEph_GLO_t));

	pFull->slot			= pRXN->slot;
	pFull->FT			= pRXN->FT;
	pFull->freqChannel	= pRXN->freqChannel;
	pFull->M			= pRXN->M;
	pFull->Bn			= pRXN->Bn;
	pFull->utc_offset	= pRXN->utc_offset;
	pFull->gamma		= pRXN->gamma;
	pFull->tauN			= pRXN->tauN;
	pFull->gloSec		= pRXN->gloSec;
	pFull->x			= pRXN->x;
	pFull->y			= pRXN->y;
	pFull->z			= pRXN->z;
	pFull->vx			= pRXN->vx;
	pFull->vy			= pRXN->vy;
	pFull->vz			= pRXN->vz;
	pFull->lsx			= pRXN->lsx;
	pFull->lsy			= pRXN->lsy;
	pFull->lsz			= pRXN->lsz;

	return;
}

void RXN_CL_RINEX_FullToRXN(RXN_FullEph_t* pFull, RXN_ephem_t* pRXN)
{
	/* Map pFull to pRXN. */
	pRXN->prn = pFull->prn;
	pRXN->gps_week = pFull->gps_week;
	pRXN->ura = pFull->ura;
	pRXN->health = pFull->health;
	pRXN->toc = pFull->toc;
	pRXN->af2 = pFull->af2;
	pRXN->af1 = pFull->af1;
	pRXN->af0 = pFull->af0;
	pRXN->crs = pFull->crs;
	pRXN->delta_n = pFull->delta_n;
	pRXN->m0 = pFull->m0;
	pRXN->cuc = pFull->cuc;
	pRXN->e = pFull->e;
	pRXN->cus = pFull->cus;
	pRXN->sqrt_a = pFull->sqrt_a;
	pRXN->toe = pFull->toe;
	pRXN->ephem_fit = pFull->ephem_fit;
    pRXN->ure = pFull->ure;
	pRXN->cic = pFull->cic;
	pRXN->omega0 = pFull->omega0;
	pRXN->cis = pFull->cis;
	pRXN->i0 = pFull->i0;
	pRXN->crc = pFull->crc;
	pRXN->w = pFull->w;
	pRXN->omega_dot = pFull->omega_dot;
	pRXN->iode = pFull->iode;
	pRXN->i_dot = pFull->i_dot;

	return;
}

void RXN_CL_RINEX_FullToRXN_GLO(RXN_FullEph_GLO_t* pFull,RXN_glonass_ephem_t* pRXN)
{
	//memcpy(pFull, pRXN,sizeof(RXN_FullEph_GLO_t));

	pRXN->slot			= pFull->slot;
	pRXN->FT			= pFull->FT;
	pRXN->freqChannel	= pFull->freqChannel;
	pRXN->M				= pFull->M;
	pRXN->Bn			= pFull->Bn;
	pRXN->utc_offset	= pFull->utc_offset;
	pRXN->gamma			= pFull->gamma;
	pRXN->tauN			= pFull->tauN;
	pRXN->gloSec		= pFull->gloSec;
	pRXN->x				= pFull->x;
	pRXN->y				= pFull->y;
	pRXN->z				= pFull->z;
	pRXN->vx			= pFull->vx;
	pRXN->vy			= pFull->vy;
	pRXN->vz			= pFull->vz;
	pRXN->lsx			= pFull->lsx;
	pRXN->lsy			= pFull->lsy;
	pRXN->lsz			= pFull->lsz;

	return;
}
