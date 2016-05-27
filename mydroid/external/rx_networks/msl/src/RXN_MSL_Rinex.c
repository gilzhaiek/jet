/*
* Copyright (c) 2011 Rx Networks, Inc. All rights reserved.
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
* $LastChangedDate: 2009-04-22 17:33:16 -0700 (Wed, 22 Apr 2009) $
* $Revision: 10292 $
*************************************************************************
*
*/

#include <stdio.h>    /* Used for file I/O. */
#include <string.h>   /* Used for memset */
#include <stdlib.h>   /* Used for atoi */
#include <math.h>
#include "RXN_MSL.h"
#include "RXN_MSL_Common.h"
#include "RXN_MSL_Platform.h"
#include "RXN_MSL_Time.h"

#define MAX_RINEX_ENTRY_LEN             500
#define MAX_RINEX_TOKEN_LEN             32
#define END_OF_RINEX_HEADER             "END OF HEADER"
#define MAX_RINEX_HEADER_LINES_GPS      8
#define MAX_RINEX_HEADER_LINES_GLO      6
#define PI                              3.1415926535898

static int URAIdxFromSVAcc(float SVacc)
{
    if(SVacc < 0)
    {
        return 15;
    }
    else if(SVacc <= 2.4)
    {
        return 0;
    }
    else if(SVacc <= 3.4)
    {
        return 1;
    }
    else if(SVacc <= 4.85)
    {
        return 2;
    }
    else if(SVacc <= 6.85)
    {
        return 3;
    }
    else if(SVacc <= 9.65)
    {
        return 4;
    }
    else if(SVacc <= 13.65)
    {
        return 5;
    }
    else if(SVacc <= 24)
    {
        return 6;
    }
    else if(SVacc <= 48)
    {
        return 7;
    }
    else if(SVacc <= 96)
    {
        return 8;
    }
    else if(SVacc <= 192)
    {
        return 9;
    }
    else if(SVacc <= 384)
    {
        return 10;
    }
    else if(SVacc <= 768)
    {
        return 11;
    }
    else if(SVacc <= 1536)
    {
        return 12;
    }
    else if(SVacc <= 3072)
    {
        return 13;
    }
    else if(SVacc <= 6144)
    {
        return 14;
    }

    return 15;
}

static S32 roundS(const double x)
{
    double  ans = (x > 0) ? (x+ 0.5) :  (x - 0.5) ;
    S32 sans = (S32) ans;
    return sans;
}

static U32 roundU(const double x)
{
    double  ans = (x > 0) ? (x+ 0.5) :  (x - 0.5) ;
    U32 uans = (U32) ans;
    return uans;
}

static void ConvertToEFormat(char* pChar)
{
    /* IMPORTANT: atof() cannot be relied upon to convert strings of the "D" format
    * e.g. atof("-0.164542347193D-03") returns -1.645423e-004 in Windows
    *                                  returns -1.645423e-001 in Android.
    * We must use the "E" format i.e "-0.164542347193E-03".
    * Therefore convert every instance of 'D' to 'E' in the line.
    */
    while(*pChar)
    {
        if(*pChar == 'D' || *pChar == 'd')
        {
            *pChar = 'E';
        }
        pChar++;
    }
}

static FILE* ProcessRinexHeader(const char rinexFile[RXN_MSL_MAX_PATH], U08 maxRinexHeaderLines)
{
    FILE* pFile;
    char csvLine[MAX_RINEX_ENTRY_LEN];          /* Storage for each line read. */
    int headerLine;                             /* Line number in header. */

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "ProcessRinexHeader: Processing file: %s.", rinexFile);

    pFile = fopen(rinexFile, "r");

    if(!pFile)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "ProcessRinexHeader: Failed open %s.", rinexFile);
        return NULL;
    }

    /* Assume there is a header, simply read through these lines. */
    for(headerLine = 0; headerLine < maxRinexHeaderLines; headerLine++)
    {
        if(fgets(csvLine, MAX_RINEX_ENTRY_LEN - 1, pFile) == NULL)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "ProcessRinexHeader: Bad header.");
            fclose(pFile);
            return NULL;
        }

        if (strstr(csvLine, END_OF_RINEX_HEADER) != NULL)
        {
            break;      // Find the end of header
        }
    }

    if(headerLine == maxRinexHeaderLines)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "ProcessRinexHeader: No header found.");
        fclose(pFile);
        return NULL;
    }

    return pFile;
}

U08 MSL_GPS_ProcessRinexFile(const char rinexFile[RXN_MSL_MAX_PATH])
{
    FILE* pFile;
    char csvLine[MAX_RINEX_ENTRY_LEN];          /* Storage for each line read. */
    char token[MAX_RINEX_TOKEN_LEN];            /* Storage for each element or token. */
    int cntInRec = 0;                           /* Count lines for each sv. */
    float SVAcc = 0;                            /* Will need the SV acc for URA convert. */
    RXN_FullEph_t curEph;                       /* Current ephemeris work struct. */
    RXN_FullEph_t readBCEArr[RXN_CONSTANT_NUM_PRNS];
    RXN_FullEphem_u writeBCEArr;

    memset(&curEph, 0, sizeof(curEph));
    memset((void*) readBCEArr, 0, sizeof(RXN_FullEph_t) * RXN_CONSTANT_NUM_PRNS);

    if((pFile = ProcessRinexHeader(rinexFile, MAX_RINEX_HEADER_LINES_GPS)) == NULL)
    {
        return RXN_FAIL;            
    }

    /* Process SVs until we hit the end of the file. */
    while(fgets(csvLine, MAX_RINEX_ENTRY_LEN - 1, pFile) != NULL)
    {
        ConvertToEFormat(csvLine);

        /* Process depending on which line within a record we are looking at. */
        switch(cntInRec)
        {
        case 0:
            memcpy(token,&csvLine[0], 2);
            token[2] = 0;
            curEph.prn = (U08) atoi(token);
            memcpy(token,&csvLine[22], 19);
            token[19] = 0;
            curEph.af0 = (S32) roundS(atof(token) /  pow((double) 2, (double) -31));
            memcpy(token,&csvLine[41], 19);
            curEph.af1 = (S16) roundS(atof(token) / pow((double) 2, (double) -43));
            memcpy(token,&csvLine[60], 19);
            curEph.af2 = (S08) roundS(atof(token) / pow((double) 2, (double) -55));
            cntInRec++;
            break;

        case 1:
            memcpy(token,&csvLine[0], 22);
            token[22] = 0;
            curEph.iode = (U16) atof(token);
            memcpy(token,&csvLine[22], 19);
            token[19] = 0;
            curEph.crs = (S16) roundS(atof(token) / pow((double) 2, (double) -5));
            memcpy(token,&csvLine[41], 19);
            curEph.delta_n = (S16) roundS(atof(token) / (pow((double) 2,(double) -43) * PI));
            memcpy(token,&csvLine[60], 19);
            curEph.m0 = (S32) roundS(atof(token) / (pow((double) 2, (double) -31) * PI));
            cntInRec++;
            break;

        case 2:
            memcpy(token, &csvLine[0], 22);
            token[22] = 0;
            curEph.cuc = (S16) roundS(atof(token) / pow((double) 2, (double) -29));
            memcpy(token, &csvLine[22], 19);
            token[19] = 0;
            curEph.e = (U32) roundU(atof(token) / pow((double) 2,(double) -33));
            memcpy(token, &csvLine[41], 19);
            curEph.cus = (S16) roundS(atof(token) / pow((double) 2, (double) -29));
            memcpy(token, &csvLine[60], 19);
            curEph.sqrt_a = (U32) roundU(atof(token) / pow((double) 2, (double) -19));
            cntInRec++;
            break;

        case 3:
            memcpy(token, &csvLine[0], 22);
            token[22] = 0;
            curEph.toe = (U16) roundU(atof(token) / pow((double) 2, (double) 4));
            curEph.toc = curEph.toe; //assume toc and toe are same as prev eg.
            memcpy(token, &csvLine[22], 19);
            token[19] = 0;
            curEph.cic = (S16) roundS(atof(token) / pow((double) 2,(double) -29));
            memcpy(token, &csvLine[41], 19);
            curEph.omega0 = (S32) roundS(atof(token) / (pow((double) 2, (double) -31) * PI));
            memcpy(token, &csvLine[60], 19);
            curEph.cis = (S16) roundS(atof(token) / pow((double) 2,(double) -29));
            cntInRec++;
            break;

        case 4:
            memcpy(token, &csvLine[0], 22);
            token[22] = 0;
            curEph.i0 = (S32) roundS(atof(token) / (pow((double) 2,(double) -31) * PI));
            memcpy(token, &csvLine[22], 19);
            token[19] = 0;
            curEph.crc = (S16) roundS(atof(token) / pow((double) 2,(double) -5));
            memcpy(token, &csvLine[41], 19);
            curEph.w = (S32) roundS(atof(token) / (pow((double) 2, (double) -31) * PI));
            memcpy(token, &csvLine[60], 19);
            curEph.omega_dot = (S32) roundS(atof(token) / (pow((double) 2, (double) -43) * PI));
            cntInRec++;
            break;

        case 5:
            memcpy(token, &csvLine[0], 22);
            token[22] = 0;
            curEph.i_dot = (S16) roundS(atof(token) / (pow((double) 2,(double) -43) * PI));
            memcpy(token, &csvLine[22], 19);
            token[19] = 0;
            curEph.CAOrPOnL2 = (U08) (atof(token));
            memcpy(token, &csvLine[41], 19);
            curEph.gps_week = (U16) atof(token);
            memcpy(token, &csvLine[60], 19);
            curEph.L2PData = (U08) (atof(token));
            cntInRec++;
            break;

        case 6:
            memcpy(token, &csvLine[0], 22);
            SVAcc = (float) atof(token);
            token[22] = 0;
            curEph.ura = (U08)URAIdxFromSVAcc(SVAcc);
            memcpy(token, &csvLine[22], 19);
            token[19] = 0;
            curEph.health = (U08) atof(token);
            memcpy(token, &csvLine[41], 19);
            curEph.TGD = (S08) roundS(atof(token) / pow((double) 2,(double) -31));
            memcpy(token, &csvLine[60], 19);
            curEph.iodc = (U16) atof(token);
            cntInRec++;
            break;

        case 7:
            memcpy(token, &csvLine[0], 22);
            token[22] = 0;
            /* Don't don anything with transmission time at present. */
            memcpy(token, &csvLine[22], 19);
            token[19] = 0;
            curEph.ephem_fit = (S08) atof(token);
            memcpy(token, &csvLine[41], 19);
            /* spare1 */
            memcpy(token, &csvLine[60], 19);
            /* spare2 */

            /* Reset cntInRec as the next line will contine the first entry for another SV. */
            cntInRec = 0;

            if((curEph.prn > 0) && (curEph.prn <= RXN_CONSTANT_NUM_PRNS))
            {
                readBCEArr[curEph.prn - 1] = curEph;
            }
            else
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                    "MSL_GPS_ProcessRinexFile: Invalid PRN: %1.0f.", (double) curEph.prn);
            }

            /* Ensure that curEph is clear. */
            memset(&curEph, 0, sizeof(curEph));

            /* Process the next SV. */
            break;

        default:
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "MSL_GPS_ProcessRinexFile: Parsing error.");
            fclose(pFile);
            /* We should never get here unless cntInRec is mismanaged. */
            return RXN_FAIL;
        }
    }

    fclose(pFile);

    /* Now we pass the ephemeris array to MSL for progagation.
    * The array may be sparsely populated depending on file content.
    */

    writeBCEArr.gpsPRNArr = readBCEArr;
    RXN_MSL_WriteEphemeris(writeBCEArr, RXN_CONSTANT_NUM_PRNS, RXN_GPS_CONSTEL);

    return RXN_SUCCESS;
}

U08 MSL_GLO_ProcessRinexFile(const char rinexFile[RXN_MSL_MAX_PATH])
{
    FILE* pFile;
    char csvLine[MAX_RINEX_ENTRY_LEN];          /* Storage for each line read. */
    char token[MAX_RINEX_TOKEN_LEN];            /* Storage for each element or token. */
    int cntInRec = 0;                           /* Count lines for each sv. */
    RXN_FullEph_GLO_t curEph;                   /* Current ephemeris work struct. */
    RXN_FullEph_GLO_t readBCEArr[RXN_CONSTANT_NUM_GLONASS_PRNS];
    RXN_FullEphem_u writeBCEArr;
    MSL_time_t ephUTCTime;                      /* UTC or GMT time struct. */
    time_t tTime;

    memset(&curEph, 0, sizeof(curEph));
    memset((void*) readBCEArr, 0, sizeof(RXN_FullEph_GLO_t) * RXN_CONSTANT_NUM_GLONASS_PRNS);

    if((pFile = ProcessRinexHeader(rinexFile, MAX_RINEX_HEADER_LINES_GLO)) == NULL)
    {
        return RXN_FAIL;            
    }

    /* Process SVs until we hit the end of the file. */
    while(fgets(csvLine, MAX_RINEX_ENTRY_LEN - 1, pFile) != NULL)
    {
        ConvertToEFormat(csvLine);

        /* Process depending on which line within a record we are looking at. */
        switch(cntInRec)
        {
        case 0:
            // Get line 1 parameters.  
            memcpy(token,&csvLine[0], 2);
            token[2] = 0;
            curEph.slot = (U08) atoi(token);		// SV No.
            memcpy(token,&csvLine[3], 2);
            ephUTCTime.MSL_Year = atoi(token) + 2000;
            memcpy(token,&csvLine[6], 2);
            ephUTCTime.MSL_Month = atoi(token);
            memcpy(token,&csvLine[9], 2);
            ephUTCTime.MSL_Day = atoi(token);
            memcpy(token,&csvLine[12], 2);
            ephUTCTime.MSL_Hour = atoi(token);
            memcpy(token,&csvLine[15], 2);
            ephUTCTime.MSL_Min = atoi(token);
            memcpy(token,&csvLine[17], 5);
            token[5] = 0;
            ephUTCTime.MSL_Sec = atoi(token);
            ephUTCTime.MSL_mSec = 0;
            tTime = MSL_ConvertTimeStructToSeconds(&ephUTCTime);
            curEph.gloSec = MSL_ConvertUTCToGLOTime(tTime);
            memcpy(token,&csvLine[22], 19);			// -TauN
            token[19] = 0;
            curEph.tauN = (S32) (atof(token) /  pow((double) 2, (double) -30));
            memcpy(token,&csvLine[41], 19);			// GammanN
            curEph.gamma = (S16) (atof(token) / pow((double) 2, (double) -40));
            memcpy(token,&csvLine[60], 19);			// tk, Not used
            // Increment cntInRec so that the next line type is handled next time.
            cntInRec++;
            break;

        case 1:
            // Get line 2 parameters.				// X
            memcpy(token,&csvLine[0], 22);			
            token[22] = 0;
            curEph.x = (S32) (atof(token) / pow((double) 2, (double) -11));
            memcpy(token,&csvLine[22], 19);
            token[19] = 0;
            curEph.vx = (S32) (atof(token) / pow((double) 2, (double) -20));
            memcpy(token,&csvLine[41], 19);
            curEph.lsx = (S16) (atof(token) / pow((double) 2,(double) -30));
            memcpy(token,&csvLine[60], 19);
            curEph.Bn = (U08) (atof(token) / pow((double) 2, (double) 0));
            // Increment cntInRec so that the next line type is handled next time.
            cntInRec++;
            break;

        case 2:
            // Get line 3 parameters.				//Y
            memcpy(token,&csvLine[0], 22);			
            token[22] = 0;
            curEph.y = (S32) (atof(token) / pow((double) 2, (double) -11));
            memcpy(token,&csvLine[22], 19);
            token[19] = 0;
            curEph.vy = (S32) (atof(token) / pow((double) 2, (double) -20));
            memcpy(token,&csvLine[41], 19);
            curEph.lsy = (S16) (atof(token) / pow((double) 2,(double) -30));
            memcpy(token,&csvLine[60], 19);
            curEph.freqChannel = (S08) (atof(token) / pow((double) 2, (double) 0));
            // Increment cntInRec so that the next line type is handled next time.
            cntInRec++;
            break;

        case 3:
            // Get line 4 parameters. last line		//Z
            memcpy(token,&csvLine[0], 22);			
            token[22] = 0;
            curEph.z = (S32) (atof(token) / pow((double) 2, (double) -11));
            memcpy(token,&csvLine[22], 19);
            token[19] = 0;
            curEph.vz = (S32) (atof(token) / pow((double) 2, (double) -20));
            memcpy(token,&csvLine[41], 19);
            curEph.lsz = (S16) (atof(token) / pow((double) 2,(double) -30));
            memcpy(token,&csvLine[60], 19);			// age of operation, Not in used

            if((curEph.slot > 0) && (curEph.slot <= RXN_CONSTANT_NUM_GLONASS_PRNS))
            {
                // Add curEph to readBCEArr.
                readBCEArr[curEph.slot-1] = curEph;
            }
            else
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                    "MSL_GLO_ProcessRinexFile: Invalid PRN: %1.0f.", (double) curEph.slot);
            }

            // Ensure that curEph is clear.
            memset(&curEph, 0, sizeof(curEph));

            // Reset cntInRec as the next line will contine the first entry for another SV.
            cntInRec = 0;
            // Process the next SV.
            break;

        default:
            // Close the file.
            fclose(pFile);

            // We should never get here unless cntInRec is mismanaged.
            return RXN_FAIL;

            break;
        }
    }
    // Close the file.
    fclose(pFile);

    writeBCEArr.gloPRNArr = readBCEArr;
    RXN_MSL_WriteEphemeris(writeBCEArr, RXN_CONSTANT_NUM_GLONASS_PRNS, RXN_GLO_CONSTEL);

    return RXN_SUCCESS;
}
