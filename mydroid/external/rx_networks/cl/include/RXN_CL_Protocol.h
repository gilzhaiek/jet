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
 */

#ifndef RXN_CL_PROTOCOL_H
#define RXN_CL_PROTOCOL_H

#ifdef __cplusplus
extern "C" {
#endif

/* Buffer lengths defined below. */
#define CL_MAX_MSG_BUF_LEN			4096 /* Max buffer for Msg to/from CL_WriteMsg/CL_ReadMsg fcns. */

U16 CL_WriteMsg(CL_Chipset_Attrib_t* pAttrib, U08* Msg, U16 MsgLen);

/* Fcn supporting reading msgs from an opened port. Use this function to read msgs in one
 * of the following ways:
 *
 * -  Specify a prefix byte sequence and prefix length, specify a msg length. The fcn will
 *    look for the msg prefix within an incoming byte stream and then will read bytes
 *    corresponding to the msg length following. Set the Suffix ptr to null along with 
 *    SuffixLen, BytesAfterSuffix, MsgLenMSBOffset, MsgLenLSBOffset, WrapBytes.
 * -  Specify a prefix byte sequence, prefix length, suffix byte sequence and suffix length
 *    and the number of bytes after the suffix.  The fcn will look for the msg prefix and
 *    then suffix and will copy msg bytes between these elements (inclusive). *pMsgLen will
 *    be set with the resulting msg length. Set the initial *pMsgLen value to 0 (not the
 *    pointer, the value). Also set MsgLenMSBOffset, MsgLenLSBOffset and WrapBytes to 0.
 * -  Specify a prefix byte sequence, prefix length and specify where (within the msg) to
 *    look for msg length data. Can specify an offset from msg start to the most sig byte
 *    within the msg that specifies length (MsgLenMSBOffset) and can specify an offset from
 *    the msg start to the least sig byte within the msg that specifies length (MsgLenLSBOffset).
 *    Because MsgLenMSBOffset and MsgLenLSBOffset will collectivly specify only a payload
 *    length, also specify the number of bytes within the msg that wrap the payload (WrapBytes).
 *    Typically, these bytes include protocol header data (includes the msg length) and footer
 *    data (includes the CRC). *pMsgLen will  be set with the resulting msg length. Set the
 *    initial *pMsgLen value to 0 (not the pointer, the value). 
 *
 * NOTES
 *
 * -  Inst must include port data to support port read writes.
 * -  CL_ReadMsg holds on to bytes that it read that followed the end of a defined msg
 *    (end defined by msg length or a suffix). This is helpful where multiple msg reads
 *    may be performed on an incoming stream. If held bytes are to be parsed when looking,
 *    for an incoming msg set BufClear to FALSE, otherwise set BufClear to TRUE to clear
 *    out any previously stored incoming bytes when looking for a msg.
 * -  Specify a MSecTimeout value. The fcn will give up on looking for a msg after MSecTimeout.
 * -  The msg length returned witin *pMsgLen does NOT include a null terminator. Such a
 *    terminator is added to ASCII strings (e.g. NMEA) when they are read.
 *
 */
U16 CL_ReadMsg(CL_Chipset_Attrib_t* pAttrib, BOOL BufClear, U16 MSecTimeout, U08* Prefix, U16 PrefixLen, U08* Msg,
    U08* Suffix, U16 SuffixLen, U16 BytesAfterSuffix, U16* pMsgLen, U16 MsgLenMSBOffset, U16 MsgLenLSBOffset,
    U16 WrapBytes);

////////////////////////////////////////////////////////////////////////////
// Utility Functions ///////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

U08* FindMsgEnd(U08* Buf, U16 BufLen, U16 MsgLen, U08* Suffix, U16 SuffixLen, U16 BytesAfterSuffix);

S16 GetMsgLen(U08* PrefixInTempBuf, U16 BytesAfterPrefix, U16 MsgLenMSBOffset, U16 MsgLenLSBOffset);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL_PROTOCOL_H */
