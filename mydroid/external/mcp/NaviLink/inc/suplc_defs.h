/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_defs.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#ifndef __SUPLC_DEFS_H__
#define __SUPLC_DEFS_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "mcpf_defs.h"
#include "pla_os.h"                             /* For os_print. */


/* Defines */

/* #define ENABLE_SUPLC_DEBUG*/
#ifdef ENABLE_SUPLC_DEBUG
    #define DEBUG_SUPLC(x) x
#else
    #define DEBUG_SUPLC(x)
#endif /* ENABLE_SUPLC_DEBUG */

#define PRINT_TAG os_print



#define MAX_SIZE_PER_ELEMENT    4096            /* Can be reduced. */
#define MAX_NUMBER_OF_ELEMENTS  40


/*  SUPLC Queues' Definition */
typedef enum{
    SUPLC_INTERNAL_EVT_QUEUE_ID = 1,
    SUPLC_CMD_QUEUE_ID,
    SUPLC_ACTION_QUEUE_ID,

    SUPLC_MAX_NUM_QUEUES = 4
}eSUPLC_QueId;

/* SUPLC's Error Conditions */
typedef enum{
    SUPLC_SUCCESS = 0x0,            /* Success */
    SUPLC_ERROR_UNKNOWN = -1,     /* Error: Unknown */
    SUPLC_ERROR_INIT_FAILED = -2,
    SUPLC_ERROR_INVALID_EVENT = -3,
    SUPLC_ERROR_INVALID_MSG = -4,
    SUPLC_ERROR_NULL_PTRS = -5,
    SUPLC_ERROR_INVALID_FIXMODE = -6,
    SUPLC_ERROR_INVALID_POS_PROT = -7,
    SUPLC_ERROR_DATA_FILLING = -8,
    SUPLC_ERROR_MSG_SEND_FAILED = -10,
    SUPLC_ERROR_ASSIST_DATA = -11,
    SUPLC_ERROR_NAL_REG_FAILED = -12,

}eSUPLC_Result;


/* Supported Positioning Protocols (RRC/RRLP) */
typedef enum{
    SUPLC_PROT_NONE,
    SUPLC_PROT_PRIVATE = 1,
    SUPLC_PROT_RRLP,
    SUPLC_PROT_RRC,
    SUPLC_TIA_801,

    SUPLC_PROT_MAX
}eSUPLC_Protocol;

typedef enum{
    SUPLC_FLOW_NONE,
    SUPLC_FLOW_NI = 1,
    SUPLC_FLOW_SI,
    SUPLC_FLOW_MAX
}eSUPLC_Flow;



/* SUPLC's Control Structure */
typedef struct suplCntl{
    handle_t hMcpf;                 /* MCPF Handle*/
    handle_t mMemPool;              /* SUPLC's Memory Pool */

    handle_t hSupl;                 /* SUPLC's Handle. */
    handle_t hRrlp;                 /* For RRLP data processing. */
    handle_t hRrc;                  /* For RRC data processing. */

    eSUPLC_Protocol e_protocol;     /* Positioning Protocol Type. */

    McpU8 u8_fixMode;               /* Location Fix Mode. */
    McpU16 u16_fixFrequency;        /* Location Fix Frequency. */

    EmcpTaskId e_srcTaskId;         /* Source Task ID */
    McpU8 u8_srcQId;                /* Source Queue ID */

	eSUPLC_Flow e_sessFlow;

	McpBool bLpPosReq;

}TSUPL_Ctrl;

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPLC_DEFS_H__ */
