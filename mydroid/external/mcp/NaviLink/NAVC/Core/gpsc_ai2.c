/*
 * $Header: /FoxProjects/FoxSource/win32/LocationManager 1/10/04 7:53p Lleirer $
 ******************************************************************************
 *  Copyright (C) 1999 SnapTrack, Inc.

 *

 *                  SnapTrack, Inc.

 *                  4040 Moorpark Ave, Suite 250

 *                  San Jose, CA  95117

 *

 *     This program is confidential and a trade secret of SnapTrack, Inc. The

 * receipt or possession of this program does not convey any rights to

 * reproduce or disclose its contents or to manufacture, use or sell anything

 * that this program describes in whole or in part, without the express written

 * consent of SnapTrack, Inc.  The recipient and/or possessor of this program

 * shall not reproduce or adapt or disclose or use this program except as

 * expressly allowed by a written authorization from SnapTrack, Inc.

 *

 *

 ******************************************************************************/


 /*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*


   L O C A T I O N   S E R V I C E S   M A N A G E R   M O D U L E


  Copyright (c) 2002 by QUALCOMM INCORPORATED. All Rights Reserved.

 

 Export of this technology or software is regulated by the U.S. Government.

 Diversion contrary to U.S. law prohibited.

 *====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/


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
 * FileName			:	gpsc_ai2.c
 *
 * Description     	:
 * This file contains private definitions and interfaces to the
 * Air Interface Independent (AI2) module
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */


#include "gpsc_types.h"
#include "gpsc_data.h"
#include "gpsc_mgp.h"
#include "gpsc_ai2_api.h"
#include "gpsc_msg.h"


#define GPSC_AI2_SMLC_VIRTUAL_SERIAL_FLAG 255


/*===========================================================================

FUNCTION
  Ai2RxInstall

DESCRIPTION
  This function is used to install the Ai2 receive protocol.  It is passed
  pointers to an application provided receive buffer as well as the receive
  buffer length.

PARAMETERS
  p_Rx     - Pointer to Ai2Rx Structure
  p_RxBuff - Pointer to user defined receive buffer
  q_RxLen  - Length of user defined receive buffer

===========================================================================*/

void Ai2RxInstall
(
  Ai2Rx*  p_Rx,
  U8*   p_RxBuff,
  U32  q_RxLen
)
{
  p_Rx->p_Buff = p_RxBuff;
  p_Rx->q_BuffLength = q_RxLen;
}


/*===========================================================================

FUNCTION
  Ai2RxInit

DESCRIPTION
  Ai2RxInit initializes the referenced Ai2Rx structure. This
  function must be called before any bytes are processed using
  Ai2RxBuild.

PARAMETERS
  p_Rx - Pointer to the Ai2Rx structure to be initialized

===========================================================================*/

void Ai2RxInit
(
  Ai2Rx*  p_Rx
)
{
  p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_SYNC;
}


/*===========================================================================

FUNCTION
  Ai2RxBuild

DESCRIPTION
  Ai2RxBuild is used to construct a Ai2Rx structure from a sequence of
  received serial bytes. Upon successful construction of a Ai2 message
  a TRUE result is returned.

PARAMETERS
  p_Rx   - Pointer to the Ai2Rx structure under construction
  u_Byte - A received serial byte

RETURN VALUE
  TRUE - If a complete message is available in Ai2Rx

===========================================================================*/

U8 Ai2RxBuild
(
  Ai2Rx*  p_Rx,
  U8    u_Byte
)
{
  U8    u_GoodCmd;
  U32   q_CheckSum, q_Count;
  U8*   p_Buff = p_Rx->p_Buff;
  U8*   p_B;
  U8    u_Cs0, u_Cs1;

  /* u_GoodCmd is the return code.
      This will only be set to  C_AI2BUILDER_MSG_BUILT  if there is
     a good command to be processed in the u_Buff */

  u_GoodCmd = C_AI2BUILDER_MSG_INCOMPLETE;

  /* Check to see if amount of stored data exceeds maximum AI2 Rx buffer length,
     if so, return to SYNC state. */

  if (p_Rx->u_Ai2RxMode != C_AI2_RX_MODE_SYNC)
  {
    if (p_Rx->w_BuffIndex >= p_Rx->q_BuffLength)
    {
      /* Buffer overrun ... This should never happen, but just in case
         restart the sync process. This will stop collection until the
         next <DLE> is received */
      p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_SYNC;

      ERRORMSG("AI2 Buffer Overrun from");
    }
  }

  switch( p_Rx->u_Ai2RxMode )
  {
    case C_AI2_RX_MODE_SYNC:
    {
      /* Field Parsing Starts at Byte 2 */
      p_Rx->w_Field = 2;

      /* Byte 0 is the sync character. Synch on receiving a <DLE> byte */
      if (u_Byte == C_DLE)
      {
         p_Rx->p_Buff[ 0 ] = u_Byte;
         p_Rx->w_BuffIndex = 1;
         p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_GET_DATA;

	      if (p_Rx->q_lostSyncBytes)
	      {
	 	      ERRORMSG2("Error: AI2 SyncFail Loss: %d bytes", p_Rx->q_lostSyncBytes);
	 	      p_Rx->q_lostSyncBytes = 0;
	      }
      }
      else
      {
      	   if (p_Rx->q_lostSyncBytes)
      	   {
		p_Rx->q_lostSyncBytes++;
      	   }
          else
          {
        ERRORMSG("Error: AI2 SyncFail : got data without getting sync");
		p_Rx->q_lostSyncBytes++;
          }
      }
      break;
    }

    case C_AI2_RX_MODE_GET_DATA:
    {
      /* If u_Byte is not a <DLE> character, store it, otherwise go to
         GET_DLE state */
      if ( u_Byte != C_DLE )
        p_Rx->p_Buff[ p_Rx->w_BuffIndex++ ] = u_Byte;
      else
        p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_GET_DATA_DLE;

      break;
    }

    case C_AI2_RX_MODE_GET_DATA_DLE:
    {
      switch ( u_Byte )
      {
        case C_DLE:
        {
          /* Store the <DLE> in the Rx Buffer and return to
             GET_DATA state. */
          p_Rx->p_Buff[ p_Rx->w_BuffIndex++ ] = u_Byte;
          p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_GET_DATA;

          break;
        }

        case C_ETX:
        {
          /* Check message length, if less than 4 bytes, this is not a complete
           * packet, go back to Sync State */

          if ( p_Rx->w_BuffIndex < 4 )
          {
            p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_SYNC;
            ERRORMSG("Error: AI2 Short Packet received < 4 bytes");
            break;
          }

          /* Get the ACK request/response byte now */
          p_Rx->u_Ack = p_Rx->p_Buff[ 1 ];

          /* Run a check sum over the bytes in the receive buffer starting
             with the sync byte but not including the checksum itself */

          q_CheckSum = 0;
          p_B = &p_Buff[ 0 ];

          /* -2 to discount checksum0, checksum1 */
          for ( q_Count = p_Rx->w_BuffIndex - 2; q_Count; q_Count-- )
          {
            q_CheckSum += (U32) *p_B++;
          }

          /* Only the 2 ls bytes of the checksum are significant */

          u_Cs0 = ( U8 )   q_CheckSum;
          u_Cs1 = ( U8 ) ( q_CheckSum >> 8 );

          /* The next 2 bytes in the buffer are the 2 ls bytes of the
             checksum. */

          if (*p_B++ == u_Cs0 )
          {
            if (*p_B++ == u_Cs1)
              u_GoodCmd = C_AI2BUILDER_MSG_READY;
	 	      else
      			ERRORMSG("AI2 message received with bad checksum.");
          }


		  if (p_Rx->w_BuffIndex == 4)
		  {
			  if (p_Rx->u_Ack == C_AI2_ACK_RESPOND)
				  u_GoodCmd = C_AI2BUILDER_ACK_DETECTED;

			  else
				ERRORMSG("Error: Zero length packet recieved with invalid Ack bit");
		  }

          /* Regardless of whether this is a good message or not,
             we always return to message sync in preparation for the next byte. */
          p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_SYNC;

          break;
        }

        default:
        {
          /* If the byte following a <DLE> is not another <DLE> or an <ETX>
             we assume the previous byte was a sync byte.
             Re-initialize and process this byte */

          p_Rx->p_Buff[ 0 ] = C_DLE;
          p_Rx->w_BuffIndex = 1;

          /* Field Parsing Starts at Byte 2 */
          p_Rx->w_Field = 2;

          /* Store Byte */
          p_Rx->p_Buff[ p_Rx->w_BuffIndex++ ] = u_Byte;

          /* Go to Get Data State */
          p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_GET_DATA;

          break;
        }
      }

      break;
    }

    default:
    {
      p_Rx->u_Ai2RxMode = C_AI2_RX_MODE_SYNC;
      /* This should never occur */
      ERRORMSG("Error: AI2 Illegal RxParse State");
    }
  }

return( u_GoodCmd );
}



/*===========================================================================

FUNCTION
  Ai2RxFieldGet

DESCRIPTION
  Extracts the next field from the completed message. This
  generalizes the process of teasing apart multiple field
  messages. If there are no more fields to be read, then
  the function returns FALSE.

PARAMETERS
  p_Rx    - Pointer to the Ai2Rx structure to be read
  p_Field - Pointer to the field structure to be filled

RETURN VALUE
  TRUE if there is a field available, FALSE otherwise.

===========================================================================*/

U8 Ai2RxFieldGet
(
  Ai2Rx*     p_Rx,
  Ai2Field*  p_Field
)
{
  S32  l_BytesLeft;
  U8*  p_B = &p_Rx->p_Buff[ p_Rx->w_Field ];

  /* Check that this field is consistent with the
     overall message length */

  l_BytesLeft = p_Rx->w_BuffIndex - p_Rx->w_Field;

  /* Account for the 2 byte checksum */
  l_BytesLeft -= 2;

  /* Minimum number of bytes for a record is 3 */
  if (l_BytesLeft < 3)
  {
#ifdef REPORT_COMM_ERRORS
    /* It seems that this is path of return when a packet
     *  is not completely received. So a return here is not
     *  a failure, but an indicator to "Try Again".
     */
    /* sm_LogMsgf("AI2 Corrupt (short) FieldLength from %s\n",p_Rx->u_LinkType==SENSOR ? "Sensor": "SMLC");*/
#endif

    return FALSE;
  }

  p_Field->u_Id      = *p_B++;
  p_Field->w_Length  = *p_B++;
  p_Field->w_Length |= (U16) ( *p_B++ ) << 8;
  p_Field->p_B       = p_B;

  /* Check to see if the field length (plus the field ID and length bytes)
     exceeds the number of bytes left in the message, if so something is
     wrong. Return a status of False. */
  if( l_BytesLeft < (p_Field->w_Length + 3))
  {
     ERRORMSG("Error: AI2 Corrupt FieldLength, longer than expected");
     return FALSE;
  }
  else
  {
     /* Adjust the Field index to access the next field */
     p_Rx->w_Field = (U16)(p_Rx->w_Field + 3 + p_Field->w_Length);

    return TRUE;
  }
}


/*===========================================================================

FUNCTION
  Ai2AddByte

DESCRIPTION
  This function adds a byte to the message buffer and
  also adds the byte to the running checksum

PARAMETERS
  p_Tx   - Pointer to AI2 Transmit structure
  u_Data - Data to add to message buffer

===========================================================================*/

void Ai2AddByte
(
  Ai2Tx*  p_Tx,
  U8    u_Data
)
{
  U8* p_Buff = &p_Tx->p_Buff[ p_Tx->w_ByteCount ];

  /* Add Byte to Message Buffer */
  *p_Buff++ = u_Data;
  p_Tx->w_ByteCount++;

  if (u_Data == C_DLE)
  {
	  // Add escape character if character was DLE
	  *p_Buff++ = u_Data;
	  p_Tx->w_ByteCount++;
  }

  // Add Byte to Checksum
  p_Tx->w_Checksum = (U16)(p_Tx->w_Checksum + u_Data);
}


/*===========================================================================

FUNCTION
  Ai2TxInstall

DESCRIPTION
  This function is used to install the Ai2 transmit protocol. It is
  passed pointers to an application provided transmit buffer as well
  as the transmit buffer length

PARAMETERS
  p_Tx     - Pointer to Ai2Tx structure
  p_TxBuff - Pointer to user defined transmit buffer
  q_TxLen  - Length of user defined transmit buffer

===========================================================================*/

void Ai2TxInstall
(
  Ai2Tx*  p_Tx,
  U8*     p_TxBuff,
  U32     q_TxLen
)
{
  p_Tx->p_Buff = p_TxBuff;
  p_Tx->q_BuffLength = q_TxLen;
}


/*===========================================================================

FUNCTION
  Ai2TxInit

DESCRIPTION
  This function is used to initialise the Ai2Tx structure prior to building
  the remainder of the message. This function must be called before using
  Ai2TxFieldAdd and Ai2TxEnd.

PARAMETERS
  p_Tx      - Pointer to theAi2Tx structure to be initialized.
  u_AckType - Ack Request/Response value. See ai2_api.h for enumerations

===========================================================================*/

void Ai2TxInit
(
  Ai2Tx*  p_Tx,
  U8      u_AckType
)
{
  /* Clear Checksum */
  p_Tx->w_Checksum = 0;

  p_Tx->p_Buff[0] = C_DLE;
  p_Tx->w_ByteCount = 1;

  /* Add ACK Type */
  Ai2AddByte( p_Tx, u_AckType );
}

void Ai2SetAck
(
  Ai2Tx*  p_Tx,
  U8      u_AckType
)
{
	p_Tx->w_Checksum  = (U16)(p_Tx->w_Checksum - p_Tx->p_Buff[1]);
	p_Tx->p_Buff[1] = u_AckType;
	p_Tx->w_Checksum =(U16)(p_Tx->w_Checksum + u_AckType);
}


/*===========================================================================

FUNCTION
  Ai2TxFieldAdd

DESCRIPTION
  This function is used to append the various fields to the complete message.
  This function updates the total byte count and places the sub field id,
  sub field size and constituent byte into the Tx message. A check is
  made to ensure that inclusion of the field does not cause the maximum
  transmit message size to be violated.

PARAMETERS
  p_Tx    - Pointer to the Ai2Tx structure to be added to.
  p_Field - Pointer to the Ai2Field structure to add

===========================================================================*/

void Ai2TxFieldAdd
(
  Ai2Tx*     p_Tx,
  Ai2Field*  p_Field
)
{
  U8*   p_Data;
  U32  q_BytesToCopy;

  // Each field consists of 1 byte ID, 2 bytes size field plus the data
  if ( (U32) ( p_Tx->w_ByteCount + p_Field->w_Length + (1 + 2) ) >
                 p_Tx->q_BuffLength )
  {
     ERRORMSG("Warning : Ai2 Message Exceeds Storage" );
	  return;
  }

  // Add Id Byte 
  Ai2AddByte( p_Tx, p_Field->u_Id );

  // Add w_Length
  Ai2AddByte( p_Tx, ( U8 ) p_Field->w_Length );
  Ai2AddByte( p_Tx, ( U8 ) ( p_Field->w_Length >> 8 ) );

  // Copy the field data into the AI2 TX Buffer
  p_Data = p_Field->p_B;

  for ( q_BytesToCopy = p_Field->w_Length; q_BytesToCopy; q_BytesToCopy-- )
  {
     Ai2AddByte( p_Tx, *p_Data++ );
  }
}


/*===========================================================================

FUNCTION
  Ai2TxEnd

DESCRIPTION
  This function adds the checksum to the end of the message.

PARAMETERS
  p_Tx - Pointer to the Ai2Tx structure to be added to.

===========================================================================*/

void Ai2TxEnd
(
  Ai2Tx*  p_Tx
)
{
  U8*  p_B = &p_Tx->p_Buff[ p_Tx->w_ByteCount ];

  /* Add Checksum Bytes to Transmit Buffer */
  /* Add a DLE to the checksum to account for the header DLE that will be added
     when the message is sent */

  p_Tx->w_Checksum += C_DLE;

  *p_B++ = ( U8 )   p_Tx->w_Checksum;
  /*If checksum byte has  a DLE in it, escape with another DLE*/
  if ((p_Tx->w_Checksum &0xFF) == C_DLE)
  {
	  *p_B++ = C_DLE;
	  p_Tx->w_ByteCount++;
  }


  /*If checksum byte has  a DLE in it, escape with another DLE*/
  *p_B++ = ( U8 ) ( p_Tx->w_Checksum >> 8 );
  if ((p_Tx->w_Checksum >> 8) == C_DLE)
  {
	  *p_B++ = C_DLE;
	  p_Tx->w_ByteCount++;
  }

  *p_B++ = C_DLE;
  *p_B++ = C_ETX;

  p_Tx->w_ByteCount += 4;
}



