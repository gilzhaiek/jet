/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			    :	Supl_data.h
 *
 * Description     	: this file defines the data structure for hash
 * 
 *
 * Author         	: 	Makbul Siram - makbul@ti.com
 *
 *
 ******************************************************************************
 */
#include<stdio.h>
#include<string.h>
#include<stdlib.h>
//#include "network/supl_types.h"

/*
 * Constant definitions
*/
typedef unsigned char   U8;
typedef unsigned short  U16;
typedef unsigned long   U32;
typedef signed	char     S8;
typedef signed short    S16;
typedef signed long     S32;
typedef double          DBL;
typedef float           FLT;


#define MAX_U8          255
#define MIN_U8          0
#define MAX_U16         65535
#define MIN_U16         0
#define MAX_U32         4294967295
#define MIN_U32         0
#define MAX_S16         32767
#define MIN_S16         -32767
#define MAX_S32         2147483647L
#define MIN_S32         -2147483647L


/* 
 *  This structure will hold context information for the hashing
 *  operation
 */

typedef struct SHA1Context
{
    U32 Message_Digest[5]; /* Message Digest (output)          */

    U32 Length_Low;        /* Message length in bits           */
    U32 Length_High;       /* Message length in bits           */

    U8 Message_Block[64]; /* 512-bit message blocks      */
    U8 Message_Block_Index;    /* Index into message block array   */

    U8 Computed;               /* Is the digest computed?          */
    U8 Corrupted;              /* Is the message digest corruped?  */
} SHA1Context;



