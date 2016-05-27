/*
* Copyright (c) 2007-2012 Rx Networks, Inc. All rights reserved.
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
* $LastChangedDate: 2009-04-17 13:53:14 -0700 (Fri, 17 Apr 2009) $
* $Revision: 10192 $
*************************************************************************
*
* This file contains implementation of XYBRID functionality.
* 
*/

#include <stdio.h>              /* Required for string manipulation. */
#include <stdlib.h>             /* Required for some string manipulation functions. */
#include <assert.h>

#include "RXN_MSL_Xybrid.h"     /* Contains declarations for fcns within. */
#include "RXN_MSL_Common.h"     /* Internal common declarations. */
#include "RXN_MSL_PGPS.h"
#include "RXN_MSL_Time.h"

#define MSL_XYBRID_MESSAGE_SIZE 512

static RXN_RefLocation_t mXybridPosition;

static BOOL mBCEFiltered;
static char mDeviceID[MSL_MAX_DEVICE_STR_LEN]; 

static U32 mAssistanceMask = 0;

/* Internal prototypes */
static void BuildPositionRequest(U08* msg, const char* requestName);
static void ResetXybridCoarsePosition();
static U16 DecodeAndStoreBCE(const U08* data, U32 len);
static U16 GetNavModelFieldWithSize(const U08* chr_p, const U32 cur_bit_ptr, const U08 field_len, const U08 count, U32* field);

/* Externals */
extern MSL_Config_t gConfig;

extern BOOL MSL_SendBytes(U08* pBytes, int bytesToSend);
extern void MSL_SendLocationToObservers(RXN_RefLocation_t* position);
extern void MSL_SendBCEToObservers(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel);



/* The length of each field in the navigation model received from the Xybrid server.
   see details in 3GPP TS 44.031bV7.3.0 (2006-1") Page 25 */
static int sat_nav_mdl_len[] =          {9,4,6,1,2,2,4,6,10,1,87,8,16,8,16,22,16,16,32,16,32,16,32,16,1,5,16,32,16,32,16,32,24,14};
/* some fields are [0..max]; others are [-min..max]; the real value needs to justified to -min */
static int sat_nav_mdl_justify_flag[] = {0,0,0,0,0,0,0,0,0 ,0,0, 1,0, 1,1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0,0,1, 1, 1, 1, 1, 1, 1, 1};

/* IS-GPS-200D spec */
static int ura_to_ure[] = {2, 3, 4, 6, 9, 13, 24, 48, 96, 192, 384, 768, 1536, 3072, 6144, 6144};


void MSL_SetBCEFiltered(BOOL filtered)
{
    MSL_EnterBlock(RXN_MSL_CS_Xybrid);
    mBCEFiltered = filtered;
    MSL_LeaveBlock(RXN_MSL_CS_Xybrid);
}

U32 MSL_GetAssistanceMask()
{
    return mAssistanceMask;
}

/* Note:  The caller is responsible for locking the RXN_MSL_CS_Xybrid critical section */
void RemoveFromAssistanceMask(U32 assistance)
{
    mAssistanceMask &= ~assistance;
}

U08 MSL_RequestXybridAssistance(U32 assistanceMask)
{
    if(RXN_MSL_GetDataAccess() != MSL_DATA_ACCESS_ENABLED)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_RequestXybridAssistance: Network access not available or allowed.");
         
        return RXN_FAIL;
    }

    MSL_EnterBlock(RXN_MSL_CS_Xybrid);
    ResetXybridCoarsePosition();
    mAssistanceMask = assistanceMask;
    MSL_LeaveBlock(RXN_MSL_CS_Xybrid);

    RXN_MSL_GetDeviceID(mDeviceID);

    /* Send command to messaging thread */
    if (MSL_CmdRequest(MSL_XYBRID_MESSAGE_EVENT) != RXN_SUCCESS)
    {
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

/* This is called by the messaging thread */
BOOL MSL_SendXybridMessage()
{
    U08 buffer[MSL_XYBRID_MESSAGE_SIZE];
    memset(buffer, 0, sizeof(buffer));

    BuildPositionRequest(buffer, "get_xybrid_response"); 

    return MSL_SendBytes(buffer, MSL_XYBRID_MESSAGE_SIZE);
}

static void BuildPositionRequest(U08* msg, const char* requestName)
{
    const U08* password = MSL_GenPW(gConfig.sec.vendorId, gConfig.sec.modelId, mDeviceID);
    if (!password)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE06, "Error generating password");
        return;
    }

    /* Construct a string that describes what BCE is required */
    const char* bceString = "none";
    if (mAssistanceMask & RXN_MSL_ASSISTANCE_XBD_BCE)
    {
        bceString = mBCEFiltered ? "filtered" : "composite";
    }

    const char* formatString = 
        "<RXN_Command>\n"
        "<name>%s</name>\n"
        "<data>\n"
        "<include_WiFi>%s</include_WiFi>\n"
        "<include_Cell>%s</include_Cell>\n"
        "<Donate>%s</Donate>\n"
        "<rt>%s</rt>\n"
        "<bce>%s</bce>\n"
        "<synchro>%s</synchro>\n"
        "<Host>%s</Host>\n"
        "<MaxIndex>%d</MaxIndex>\n"
        "<Port>%d</Port>\n"
        "<VendorID>%s</VendorID>\n"
        "<VendorSalt>%s</VendorSalt>\n"
        "<GPSWeek>%d</GPSWeek>\n"
        "</data>\n"
        "</RXN_Command>\n"; 

    sprintf((char*)msg, 
            formatString,
            requestName,
            (gConfig.xybrid.flags & RXN_MSL_XBD_CONFIG_WIFI_LOOKUP_ENABLED) ? "true" : "false", 
            (gConfig.xybrid.flags & RXN_MSL_XBD_CONFIG_CELL_LOOKUP_ENABLED) ? "true" : "false", 
            (gConfig.xybrid.flags & RXN_MSL_XBD_CONFIG_DONATE_ENABLED) ? "true" : "false",
            (mAssistanceMask & RXN_MSL_ASSISTANCE_XBD_RT) ? "true" : "false",
            bceString,
            (mAssistanceMask & RXN_MSL_ASSISTANCE_SYNCHRO) ? "true" : "false",
            gConfig.xybrid.host, 
            gConfig.xybrid.hostMaxIdx,
            gConfig.xybrid.hostPort, 
            gConfig.xybrid.vendorId,
            gConfig.xybrid.vendorSalt,
            MSL_GetBestGPSTime(NULL) / 604800);
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09, "SendPositionRequest() XYBRID position request. %s", (char*)msg);
}

void MSL_ProcessXybridErrorRsp()
{
    MSL_EnterBlock(RXN_MSL_CS_Xybrid);
    RemoveFromAssistanceMask(RXN_MSL_ASSISTANCE_XBD_BCE | 
            RXN_MSL_ASSISTANCE_XBD_RT | 
            RXN_MSL_ASSISTANCE_SYNCHRO);
    ResetXybridCoarsePosition();
    MSL_LeaveBlock(RXN_MSL_CS_Xybrid);
}

void MSL_ProcessXybridCoarsePostionRsp(R32 lat, R32 lon, R32 alt, U32 uncert)
{
    MSL_EnterBlock(RXN_MSL_CS_Xybrid);
    RemoveFromAssistanceMask(RXN_MSL_ASSISTANCE_XBD_RT | RXN_MSL_ASSISTANCE_SYNCHRO);
    mXybridPosition.type = RXN_LOC_LLA;
    mXybridPosition.LLA.Lat = lat;
    mXybridPosition.LLA.Lon = lon;
    mXybridPosition.LLA.Alt = alt;	
    mXybridPosition.uncertSemiMajor = uncert * 100;

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09, "Sending location to observers");

    MSL_SendLocationToObservers(&mXybridPosition);
    MSL_LeaveBlock(RXN_MSL_CS_Xybrid);
}

void MSL_ProcessXybridBCEResponse(const char* data, U16 dataSize)
{
    MSL_EnterBlock(RXN_MSL_CS_Xybrid);
    RemoveFromAssistanceMask(RXN_MSL_ASSISTANCE_XBD_BCE);
    MSL_LeaveBlock(RXN_MSL_CS_Xybrid);

    U32 result = DecodeAndStoreBCE((const U08*)data, dataSize);
    if (result != RXN_SUCCESS)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09, "Failed to decode Xybrid BCE: %1.0f", result);
    }
}

void MSL_GetXybridPosition(RXN_RefLocation_t* pPosition)
{
    assert(pPosition);

    MSL_EnterBlock(RXN_MSL_CS_Xybrid);
    memcpy(pPosition, &mXybridPosition, sizeof(RXN_RefLocation_t));
    MSL_LeaveBlock(RXN_MSL_CS_Xybrid);
}

static void ResetXybridCoarsePosition()
{
    mXybridPosition.type = RXN_LOC_UNSET;
    mXybridPosition.LLA.Lat = 0;
    mXybridPosition.LLA.Lon = 0;
    mXybridPosition.LLA.Alt = 0;	
}

static U16 GetNavModelField(const U08* chr_p, const U32 cur_bit_ptr, const U08 count, U32* field)
{
	U32 field_len = sat_nav_mdl_len[count];
    return GetNavModelFieldWithSize(chr_p, cur_bit_ptr, field_len, count, field);
}

static U16 GetNavModelFieldWithSize(const U08* chr_p, const U32 cur_bit_ptr, const U08 field_len, const U08 count, U32* field)
{
	const U08* byte_ptr = NULL;
	U32  leading_bits = 0;

	/* It would be too expensive to manipulate 64 bits DDWORD on 32 bits system. */
	/* If the target system is 64 bits, the tmp_32buf part can be removed. */

	/* use 32 bits DWORD to process anything it can hold. */
	U32  tmp_buf32 = 0;
	/* use 64 bits DDWORD to process anyhing 32bits can't hold. */
	U64  tmp_buf64 = 0;
	U32 offset = sat_nav_mdl_justify_flag[count];

	if (chr_p == NULL)
	{
		return RXN_FAIL;
	}

	byte_ptr = chr_p + (cur_bit_ptr / 8);
	leading_bits = cur_bit_ptr % 8;

	if ((leading_bits + field_len) <= 32)
	{
		/* byte manipulation works for both big and little edian system. */
		/* big endian system can use memcpy instead to improve performance. */
		tmp_buf32 = (*byte_ptr << 24) + (*(byte_ptr + 1) << 16) + (*(byte_ptr + 2) << 8) + *(byte_ptr + 3);
		tmp_buf32 <<= leading_bits;
		tmp_buf32 >>= (32- field_len);
		*field = tmp_buf32;
	}
	else
	{
		/* byte manipulation works for both big and little edian system. */
		/* big endian system can use memcpy instead to improve performance. */
		tmp_buf64 = (*byte_ptr << 24)+ (*(byte_ptr + 1) << 16) + (*(byte_ptr + 2) << 8) + *(byte_ptr + 3);
		tmp_buf64 <<= 32;
		tmp_buf32 =  (*(byte_ptr + 4) << 24) + (*(byte_ptr + 5) << 16) + (*(byte_ptr + 6) << 8) + *(byte_ptr + 7);
		tmp_buf64 += (U64) tmp_buf32;
		tmp_buf64 <<= leading_bits;
		tmp_buf64 >>= (64 - field_len);
		*field = (U32)tmp_buf64;
	}

	*field -= (offset << (field_len - 1));

	return RXN_SUCCESS;
}

static U16 DecodeAndStoreBCE(const U08* data, U32 len)
{
    RXN_FullEph_t* eph;
    U32	ctl_hdr = 0;
    U32 cur_bit_ptr = 0;
    U32 sat_num = 0;
    U32 field = 0;
    U08 i = 0;

#ifdef RXN_MSL_USE_XBD_BCE_FOR_SAGPS
    RXN_Ephemeris_u ephemeris; 
    RXN_FullEphem_u fullEphemeris; 
    U16 error = 0;
#endif

    /* no data */
    if ((data == NULL) || (len < 32))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
            "DecodeAndStoreBCE() aborting.. data chunk <32 bytes");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    /* Read 9 bit control header... */
    if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 0, &ctl_hdr))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
            "DecodeAndStoreBCE():  Error getting control header");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    cur_bit_ptr += sat_nav_mdl_len[0];

    /* The number of sv's is a 4 bit field for the filtered message and 5 bits for composite */
    if (RXN_SUCCESS != GetNavModelFieldWithSize(data, cur_bit_ptr, mBCEFiltered ? 4 : 5, 1, &sat_num))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
            "DecodeAndStoreBCE(): Unable to read number of records in navigation model");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09,
        "DecodeAndStoreBCE() Number of records = %1.0f", sat_num+1);

    cur_bit_ptr += mBCEFiltered ? 4 : 5;

    /* sat number wrong */
    if ((sat_num <= 0) || (sat_num > 32))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
            "DecodeAndStoreBCE() Invalid number of records in navigation model");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    if ( (sat_num*69)>len )
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
            "DecodeAndStoreBCE() Insufficient data available to decode");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    U32 gpsWeek = (RXN_MSL_GetBestGPSTime() / 604800);

    RXN_MSL_NavDataList_t navList;
    memset(&navList, 0, sizeof(navList));
    navList.numEntries = sat_num + 1;

    for (i = 0; i <= sat_num; i++)
    {
        eph = &navList.ephList.gps[i];
        memset(eph, 0, sizeof(RXN_ephem_t));

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 2, &field))
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
                "DecodeAndStoreBCE() Error decoding field 2 (PRN)");
            break;
        }

        /*Add one for correct PRN #*/
        eph->prn = (U08)field+1;
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09,
            "DecodeAndStoreBCE() Processing prn %1.0f", eph->prn);


        /* ignore the following 2 fields */
        cur_bit_ptr += (sat_nav_mdl_len[2] + sat_nav_mdl_len[3] + sat_nav_mdl_len[4]);


        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 5, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[5];
        eph->CAOrPOnL2 = (U08)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 6, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[6];
        eph->ura = (U08)field;
        eph->ure = ura_to_ure[eph->ura]; 

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 7, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[7];
        eph->health = (U08)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 8, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[8];
        eph->iodc = (U16)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 9, &field))
        {
            break;
        }

        cur_bit_ptr += (sat_nav_mdl_len[9] + sat_nav_mdl_len[10]);
        eph->L2PData = (U08)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 11, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[11];
        eph->TGD = (S08)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 12, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[12];
        eph->toc = (U16)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 13, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[13];
        eph->af2 = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 14, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[14];
        eph->af1 = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 15, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[15];
        eph->af0 = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 16, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[16];
        eph->crs = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 17, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[17];
        eph->delta_n = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 18, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[18];
        eph->m0 = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 19, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[19];
        eph->cuc = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 20, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[20];
        eph->e = (U32)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 21, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[21];
        eph->cus = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 22, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[22];
        eph->sqrt_a = (U32)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 23, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[23];
        eph->toe = (U16)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 24, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[24];
        eph->ephem_fit = (S08)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 25, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[25];
        eph->AODO = (U08)field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 26, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[26];
        eph->cic = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 27, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[27];
        eph->omega0 = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 28, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[28];
        eph->cis = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 29, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[29];
        eph->i0 = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 30, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[30];
        eph->crc = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 31, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[31];
        eph->w = field;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 32, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[32];
        eph->omega_dot = field ;

        if (RXN_SUCCESS != GetNavModelField(data, cur_bit_ptr, 33, &field))
        {
            break;
        }

        cur_bit_ptr += sat_nav_mdl_len[33];
        eph->i_dot = field ;

        eph->gps_week = gpsWeek;

        /* IODE is the 8 LSB of IODC (according to ICD-200) */
        eph->iode = (U08)eph->iodc;

#ifdef RXN_MSL_USE_XBD_BCE_FOR_SAGPS
        fullEphemeris.gpsPRNArr = navList.ephList.gps;
        MSL_FullToRXN(fullEphemeris, i, &ephemeris, RXN_GPS_CONSTEL);
        error = RXN_Set_Ephemeris(&ephemeris.gpsEphemeris);

        if (error != RXN_SUCCESS )
        {
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
                "DecodeAndStoreBCE(): RXN_MSL_WriteSingleEphemeris returned %1.0f", error);
        }
#endif
    }

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09, "Sending BCE to observers");

    MSL_SendBCEToObservers(&navList, RXN_GPS_CONSTEL);

    return RXN_SUCCESS;
}

