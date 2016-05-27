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
* $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
* $Revision: 9396 $
*************************************************************************
*
* This file contains implementation of APIs exposed within the RXN_CL_Protocol.h file.
* 
*/


#include "RXN_CL_Common.h"  /* Includes declarations for fcns within and common declarations. */

U16 CL_WriteMsg(CL_Chipset_Attrib_t* pAttrib, U08* Msg, U16 MsgLen)
{
	U16 Result = RXN_FAIL;  /* Results storage. */

	/* Don't do anything if the port has not been properly opened
	* Windows (CL_Port_Hdl != 0), Linux (CL_FD >= 0). */
	if((pAttrib->CL_Port_Hdl == 0) && (pAttrib->CL_FD < 0))
	{
		/* Handle the error and return error code. */
		CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_WriteMsg: Port not opened properly before msg write attempted.");

		return RXN_CL_OPEN_OR_CLOSE_ERR;
	}

	/* Write the msg. */
	Result = CL_WriteBytes(pAttrib, Msg, (U32) MsgLen);
	if(Result != RXN_SUCCESS)
	{
		/* Handle the error and return an error code. */
		CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, 
			"CL_WriteMsg: Error writing a msg. CL_WriteBytes error: %1.0f", (double) Result);

		return RXN_CL_CHIPSET_WRITE_ERR;
	}

	return RXN_SUCCESS;
}

U16 CL_ReadMsg(CL_Chipset_Attrib_t* pAttrib, BOOL BufClear, U16 MSecTimeout, U08* Prefix, U16 PrefixLen, U08* Msg,
    U08* Suffix, U16 SuffixLen, U16 BytesAfterSuffix, U16* pMsgLen, U16 MsgLenMSBOffset, U16 MsgLenLSBOffset,
    U16 WrapBytes)
{
	U64 TickAtTimeout = 0;                  /* Storage for tickcount at timeout. */
	U16 MsgBytesToGet = 0;                  /* Number of msg bytes to get. */
	U32 i = 0;                              /* Counter. */
	BOOL PrefixJustFound = FALSE;           /* Flag indicating if the prefix was found. */
	U32 BytesRead = 0;                      /* Storage for num bytes read. */
	U08* PrefixInTempBuf = NULL;            /* Ptr to prefix in temp buf. */
	U16 Result = RXN_FAIL;                  /* Result storage. */
	S32 CmpResult = 1;                      /* memcmp result storage. */
	U08* MsgEnd = NULL;                     /* Ptr to the end of the msg. */
	U16 BytesAfterPrefix = 0;               /* Stores the num bytes after the prefix. */
	U08 TempBuf[CL_MAX_MSG_BUF_LEN];           /* Incoming data store. */
	static U08 RemainBuf[CL_MAX_MSG_BUF_LEN];  /* Pointer to the remaining data buf. */
	static U16 MsgBufLen = 0;               /* Length of the msg buf (valid contents not allocation). */
	static U16 RemainBufLen = 0;            /* Length of the remaining data buf (valid contents not allocation). */
	S16 MsgLenInMsg = 0;                    /* Length of the msg as stipulated within the msg. */
	char logStr[CL_MAX_LOG_ENTRY];          /* Temp store for log msgs. */

	/* Don't do anything if the port has not been properly opened
	* Windows (CL_Port_Hdl != 0), Linux (CL_FD >= 0). */
	if((pAttrib->CL_Port_Hdl == 0) && (pAttrib->CL_FD < 0))
	{
		/* Handle the error and return error code. */
		CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_ReadMsg: Port not opened properly before msg read attempted.");

		return RXN_CL_OPEN_OR_CLOSE_ERR;
	}

	/* If specified, clear out the RemainBuf. */
	if(BufClear != 0)
	{
		memset((void*) RemainBuf, 0, sizeof(U08) * CL_MAX_MSG_BUF_LEN);
		RemainBufLen = 0;
	}

	/* Clear out the temp buffer. */
	memset((void*) TempBuf, 0, CL_MAX_MSG_BUF_LEN);

	/* Setup a var that will contain the tick count that corresponds to a timeout. */
	TickAtTimeout = CL_GetTickCount() + MSecTimeout;

	/* When msg length is specified, keep track of the number of msg bytes remaining. */
	if(*pMsgLen != 0)
	{
		MsgBytesToGet = *pMsgLen;
	}

	/* Loop through multiple reads as required until the prefix can be found
	 * within incoming bytes. */
	while(TRUE)
	{
		/* Check for a timeout before each read.
		 * Timeout if the msg cannot be read before the tickcount grows past TickAtTimeout. */
		if(CL_GetTickCount() >= TickAtTimeout)
		{
			/* Handle the error and return an error code. */
			CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_ReadMsg: Timeout waiting for a msg.");

			return RXN_CL_CHIPSET_READ_TIMEOUT;
		}

		/* Check if any bytes were remaining from a previous read.
		 * If so, preload the buf with these bytes.
		 * Note that this is req'd to support reading msgs that are adjacent to
		 * one another where the first bytes from the second msg might be discarded
		 * when the first message is read. */
		if(RemainBufLen > 0)
		{
			/* Clear the temp buffer then copy any bytes remaining from a previous read into this buffer. */
			memset((void*) TempBuf, 0, CL_MAX_MSG_BUF_LEN);
			memcpy(TempBuf, RemainBuf, RemainBufLen);

			/* Read data into the buffer following the bytes that remained from a previous read. */
			Result = CL_ReadBytes(pAttrib, TempBuf + RemainBufLen,
					CL_MAX_MSG_BUF_LEN - RemainBufLen, &BytesRead);

		    /* Lines below ONLY for protocol troubleshooting. */
			sprintf(logStr, "Remaining %d bytes to TempBuf. Additional %d bytes read, TempBuf length is %d",
                      RemainBufLen, BytesRead, BytesRead + RemainBufLen);
			CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);

			/* Increase BytesRead to account for bytes remaining from the prev read. */
			BytesRead = BytesRead + RemainBufLen;

			/* Clear RemainBufLen so this only occurs once for the
			 * first read within CL_ReadMsg. */
			RemainBufLen = 0;
			memset((void*) RemainBuf, 0, sizeof(U08) * CL_MAX_MSG_BUF_LEN);

		}
		else
		{
			/* Clear the temp buffer then read incoming bytes into this buffer. */
			memset((void*) TempBuf, 0, sizeof(U08) * CL_MAX_MSG_BUF_LEN);
			Result = CL_ReadBytes(pAttrib, TempBuf, CL_MAX_MSG_BUF_LEN, &BytesRead);

			/* Lines below ONLY for protocol troubleshooting. */
			sprintf(logStr, "No remain. Read %d bytes to TempBuf.", BytesRead);
			CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);
		}

		/* Handle errors. */
		if(Result != RXN_SUCCESS)
		{
			/* Handle the error and return an error code. */
			CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
				"CL_ReadMsg: Error reading from the port. CL_ReadBytes error: %1.0f", (double) Result);

			return RXN_CL_CHIPSET_READ_ERR;
		}

		/* If no bytes could be read, simply continue. */
		if(BytesRead == 0)
		{
			/* Try another read. */
			continue;
		}

    /******************************************************************
     * LOOK FOR A PREFIX **********************************************
     ******************************************************************/

		/* If not found, look for the prefix within incoming data. */
		if(PrefixInTempBuf == NULL)
		{
			/* Look for the prefix. */
			if (PrefixLen == 0)
			{
				/* Pitfall: Because the following uses strstr() for pattern matching, 
				* it will terminate as soon as it encounters a string terminator
				* (0x00 or '\0'), therefore if the either TempBuf or Prefix
				* contains this byte, the rest of the data ignored. */
			  PrefixInTempBuf = (U08*) strstr((char*) TempBuf, (char*) Prefix);
			}
			else
			{
				/* Don't bother looking if BytesRead <= PrefixLen. Need more
				* incoming bytes before processing. */
				if(BytesRead > PrefixLen)
				{
					/* Use alternative search method -- byte comparison. */
					for (i=0; i<BytesRead - PrefixLen; i++)
					{
						CmpResult = memcmp(&TempBuf[i], Prefix, PrefixLen);

						if (CmpResult == 0)
						{
							/* match */
							PrefixInTempBuf = (U08*) &TempBuf[i];
							break;
						}
					}
				}
			}
			
			/* If found set the flag that indicates that it was just found. */
			if(PrefixInTempBuf != NULL)
			{
				PrefixJustFound = TRUE;
			}
			else
			{
				/* Copy up to 64 bytes of incoming data into the RemainBuf as this
				* data may include part of a prefix and therefore must be inserted before subsequent
				* incoming bytes. Can't copy the entire incoming data as the amount of incoming data
				* can (potentially) fill TempBuf and prevent any addition data from coming in. */
				if(BytesRead > 64)
				{
					/* Copy the last 64 bytes read into RemainBuf. */
					memcpy(RemainBuf, TempBuf + (BytesRead - 64), 64);
					RemainBufLen = 64;
				}
				else
				{
					/* Copy all remaining bytes. */
					memcpy(RemainBuf, TempBuf, BytesRead);
					RemainBufLen = (U16) BytesRead;
				}

				/* Try another read. */
				continue;
			}
		}
		else
		{
			/* Ensure that the flag is cleared indicating that the prefix was NOT just found. */
			PrefixJustFound = FALSE;
		}

		/* Lines below ONLY for protocol troubleshooting. */
		sprintf(logStr, "PrefixInTempBuf just found: %d. Spec'd prefix len: %d",
			PrefixJustFound, PrefixLen);
		CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);

    /**********************************************************************
     * LOOK FOR A MSG END (Look for suffix or the number of bytes spec'd) *
     **********************************************************************/

		if(PrefixJustFound != 0)
		{
			/* Calc the number of bytes after the prefix pointer that are available within the
			 * incoming bytes. */
			BytesAfterPrefix = BytesRead - (PrefixInTempBuf - TempBuf);
      
			/* Lines below ONLY for protocol troubleshooting. */
			sprintf(logStr, "Will process %d bytes after the newly found prefix.", BytesAfterPrefix);
			CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);

			/* Msg length within msg. */
			if(MsgLenLSBOffset != 0) 
			{
				/* Get the payload length from within the msg. */
				MsgLenInMsg = GetMsgLen(PrefixInTempBuf, BytesAfterPrefix,
                              MsgLenMSBOffset, MsgLenLSBOffset);

				/* If not enough bytes read yet to read the msg length from within 
				* the msg.... */
				if(MsgLenInMsg == -1)
				{
					/* Set both *pMsgLen and MsgBytesToGet to 0. */
					*pMsgLen = 0;
					MsgBytesToGet = 0;
				}
				else
				{
					/* Set *pMsgLen and MsgBytesToGet to include the entire msg.
					* Must add WrapBytes to the msg length provided within the msg. */
					*pMsgLen = MsgLenInMsg + WrapBytes;
					MsgBytesToGet = *pMsgLen; 
				}

				/* Lines below ONLY for protocol troubleshooting. */
				sprintf(logStr, "Msg length from within msg with prefix %d. Total msg: %d",
				  MsgLenInMsg, MsgBytesToGet);
				CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);
			}

			/* If a length was specified, determine if we have all the bytes. */
			if(*pMsgLen != 0)
			{
				/* If we have all the bytes */
				if(BytesAfterPrefix >= *pMsgLen)
				{
					/* Set MsgEnd to point just past the end of the msg.
					 * (i.e. point at the next byte). */
					MsgEnd = PrefixInTempBuf + *pMsgLen;

					/* No more bytes to get. */
					MsgBytesToGet = 0;     
				}
				else
				{
					/* Reduce the number of bytes to get. */
					MsgBytesToGet -= BytesAfterPrefix;
				}
			}
			else
			{
				/* If the prefix was just found, look for msg end from the prefix to the end of the msg. */
				MsgEnd = FindMsgEnd(PrefixInTempBuf, BytesAfterPrefix, MsgBytesToGet, Suffix, SuffixLen, BytesAfterSuffix);       
			}

			  /* Lines below ONLY for protocol troubleshooting. */
			sprintf(logStr, "MsgEnd in msg with prefix. Length spec'd %d. Bytes to get: %d",
				*pMsgLen, MsgBytesToGet);
			CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);
		}
		else /* Prefix found during a previous read. */
		{
	      /* Msg length within msg. */
			if(MsgLenLSBOffset != 0) 
			{
				/* If MsgBytesToGet is still zero, then we still have not gotten the msg length from the msg. */
				if(MsgBytesToGet == 0)
				{
				  /* Get the payload length. */
					*pMsgLen = GetMsgLen(PrefixInTempBuf, BytesAfterPrefix,
										MsgLenMSBOffset, MsgLenLSBOffset);

					  /* Increase *pMsgLen by header and footer bytes (). */
					 *pMsgLen += WrapBytes;

					  /* Update MsgBytesToGet. If the length could not be obtained
					   * because the BytesAfterPrefix was not large enough (i.e. 
					   * non-zero offset values could not be read because they were
					   * not within TempBuf yet.) 0 will be retured. */
					 MsgBytesToGet = *pMsgLen; 
			         
					  /* Lines below ONLY for protocol troubleshooting. */
					 sprintf(logStr, "Msg length from msg after prefix found %d.", MsgBytesToGet);
					 CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);
				}
			}

			/* If a length was specified, determine if we have all the bytes. */
			if(*pMsgLen != 0)
			{
				/* Set MsgEnd to point just past the end of the msg.
				 * (i.e. point at the next byte). */
				if(BytesRead >= MsgBytesToGet)
				{
					MsgEnd = TempBuf + MsgBytesToGet;  
				}
				else
				{
					/* Reduce the number of bytes to get. */
					MsgBytesToGet -= BytesRead;         
				}
			}
			else
			{
				/* If no length spec'd and the prefix was not just found (i.e. the buf contains data from
				 * a read subsequent to a read where the prefix was found), look for the suffix within the whole msg. */
				MsgEnd = FindMsgEnd(TempBuf, BytesRead, MsgBytesToGet, Suffix, SuffixLen, BytesAfterSuffix);        
			}

			 /* Lines below ONLY for protocol troubleshooting. */
			sprintf(logStr, "MsgEnd in msg after prefix msg. Length spec'd %d. Bytes to get: %d",
				*pMsgLen, MsgBytesToGet);
			CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);
		}

		/*************
		 * Copy data *
		 *************/
			 
		/* If the prefix was just found.. */
		if(PrefixJustFound != 0)
		{
			/* ... and if the msg end has been found.  */
			if(MsgEnd != NULL)
			{
				/* Set the length. */
				MsgBufLen = (U16) (MsgEnd - PrefixInTempBuf);

				/* Copy from the prefix to the msg end. */
				memcpy(Msg, PrefixInTempBuf, (size_t) (MsgBufLen));

				/* Terminate the msg (in case it's ascii). */
				Msg[MsgBufLen] = '\0';

				/* Copy any remaining data to RemainBuf (after determine how many bytes remain). */
				RemainBufLen = (U16) (BytesRead - (MsgEnd - TempBuf));
				memcpy(RemainBuf, MsgEnd, RemainBufLen);

				/* Lines below ONLY for protocol troubleshooting. */
				sprintf(logStr, "Copy the entire msg with buf len: %d from prefix. Copy %d remaining bytes.",
				  MsgBufLen, RemainBufLen);
				CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);

				/* We are done. */
				break;
			}
			else /* Msg end not found. */
			{
				/* Copy all bytes after the prefix. */
				BytesAfterPrefix = BytesRead - (PrefixInTempBuf - TempBuf);			
				memcpy(Msg, PrefixInTempBuf, BytesAfterPrefix);

				/* Set the length. */
				MsgBufLen = BytesAfterPrefix; 

				 /* Lines below ONLY for protocol troubleshooting. */
				sprintf(logStr, "Copy %d msg bytes from prefix. Updated buf len: %d.",
				  BytesAfterPrefix, MsgBufLen);
				CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);
			}
		}
		else /* If the prefix was NOT just found... */
		{
			/* ... and if the msg end has been found. */
			if(MsgEnd != NULL)
			{
				/* Copy from the end of the current buf to the msg end. */
				memcpy(Msg + MsgBufLen, TempBuf, (size_t) (MsgEnd - TempBuf));

				/* Terminate the msg (in case it's ascii). */
				Msg[MsgBufLen + (MsgEnd - TempBuf)] = '\0';

				/* Increase the length accordingly.  */
				MsgBufLen = MsgBufLen + (U16) (MsgEnd - TempBuf);

				/* Copy any remaining data to RemainBuf (after determine how many bytes remain). */
				RemainBufLen = (U16) (BytesRead - (MsgEnd - TempBuf));
				memcpy(RemainBuf, MsgEnd, RemainBufLen);
				
				 /* Lines below ONLY for protocol troubleshooting. */
				sprintf(logStr, "Copy %d bytes as the msg end to offset. Copy %d remaining bytes.",
				  (size_t) (MsgEnd - TempBuf), RemainBufLen);
				CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);

				/* We are done. */
				break;
			}
			else /* Msg end not found. */
			{
				/* Copy all bytes. */
				memcpy(Msg + MsgBufLen, TempBuf, BytesRead);

		        /* Increase the length accordingly. */
				MsgBufLen = MsgBufLen + BytesRead;

				 /* Lines below ONLY for protocol troubleshooting. */
				sprintf(logStr, "Copy %d msg middle bytes. Updated buf len: %d.",
				  BytesRead, MsgBufLen);
				CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04, "CL_ReadMsg: %s", logStr);        
			}
		}

		/* Try another read. */
		continue;
	}

  /* Update *pMsgLen (in case a suffix was used to determine MsgEnd)
   * if it has not been set yet. NOTE THAT IT WILL NOT INCLUDE ANY '\0'
   * FOR RETRIEVED ASCII MSGS.*/
  if(*pMsgLen == 0)
  {
    *pMsgLen = MsgBufLen;
  }

	return RXN_SUCCESS;
}

U08* FindMsgEnd(U08* Buf, U16 BufLen, U16 MsgLen, U08* Suffix, U16 SuffixLen, U16 BytesAfterSuffix)
{
	U08* MsgEnd = NULL;             /* Pointer that will be set to point at the last msg byte (if within pBuf). */
	U08* SuffixInTempBuf = NULL;    /* Temp Pointer. */
	U32 i = 0;                      /* Counter. */
	S32 CmpResult = 1;              /* memcmp result storage. */

	/* If the length is specified, check that the entire msg fits within the buf. */
	if(MsgLen != 0)
	{
		if(BufLen >= MsgLen)
		{
			/* We have the entire msg. */
			MsgEnd = Buf + MsgLen;
			return MsgEnd;
		}
		else
		{
			/* Msg end within a buf that will be acq during a subsequent read. */
			return NULL;
		}
	}

	/* If a suffix has been provided, look for it within the buffer. */
	if(Suffix != NULL)
	{

		/* Look for the prefix. */
		if (SuffixLen == 0)
		{
		  /* Pitfall: Because the following uses strstr() for pattern matching, 
		   * it will terminate as soon as it encounters a string terminator
		   * (0x00 or '\0'), therefore if the either TempBuf or Prefix
		   * contains this byte, the rest of the data ignored. */
			  SuffixInTempBuf = (U08*) strstr((char*) Buf, (char*) Suffix);
		}
		else
		{
		  /* Use alternative search method -- byte comparison. */
		  for (i=0; i<BufLen; i++)
		  {
			CmpResult = memcmp(&Buf[i], Suffix, SuffixLen);

			if (CmpResult == 0)
			{
			  /* match */
			  SuffixInTempBuf = (U08*) &Buf[i];
			  break;
			}
		  }
		}

		// SuffixInTempBuf = strstr((char*) Buf, (char*) Suffix);
		if(SuffixInTempBuf == NULL)
		{
			return NULL;
		}
		else
		{
			MsgEnd = (U08*) (SuffixInTempBuf + BytesAfterSuffix);
			return MsgEnd;
		}
	}
	
	/* Neither a suffix or msg len specified. */
	return NULL;
}

S16 GetMsgLen(U08* PrefixInTempBuf, U16 BytesAfterPrefix, U16 MsgLenMSBOffset, U16 MsgLenLSBOffset)
{
  U16 HigherLenOffset = 0;
  U16 MsgLen = 0;

  /* Get the higher offset. */
  if(MsgLenMSBOffset > MsgLenLSBOffset)
  {
    HigherLenOffset = MsgLenMSBOffset;
  }
  else
  {
    HigherLenOffset = MsgLenLSBOffset;
  }

  /* Verify that both offsets are within bytes already read. */
  if(BytesAfterPrefix > HigherLenOffset)
  {
    if(MsgLenMSBOffset > 0)
    {
      /* Get the total msg length using both MsgLenMSBOffset and MsgLenLSBOffset. */
      MsgLen = PrefixInTempBuf[MsgLenMSBOffset];
      MsgLen = MsgLen << 8;
      MsgLen |= PrefixInTempBuf[MsgLenLSBOffset];
    }
    else
    {
      /* Get the total msg length using only MsgLenLSBOffset. */
      MsgLen |= PrefixInTempBuf[MsgLenLSBOffset];
    }
  }
  else
  {
    return -1;
  }

  return MsgLen;
}
