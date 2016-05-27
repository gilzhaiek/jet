/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_core_api.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#ifndef __SUPL_CORE_API_H__
#define __SUPL_CORE_API_H__

#ifdef __cplusplus
extern "C" {
#endif /*__cplusplus */

#include "mcpf_defs.h"

#include "suplc_defs.h"

#include "suplc_core_wrapper.h"

#define RRLP_MASK 0x02
#define RRC_MASK 0x04

/**
 * Function:        SUPLC_initSuplClient
 * Brief:           Entry functions for INIT state.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_initSuplClient(void *p_inData, const void *const p_inSuplCntl);


/**
 * Function:        SUPLC_deInitSuplClient
 * Brief:           Entry functions for DEINIT state.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_deInitSuplClient(void *p_inData);


/**
 * Function:        SUPLC_handleNetworkMessage
 * Brief:           Handles network messages.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_handleNetworkMessage(const void *const p_data, const void *const p_inSuplCntl);


/**
 * Function:        SUPLC_startSuplSession
 * Brief:           Starts SUPL Session with SLP.
 * Description:
 * Note:
 * Params:          p_inData - Data. NULL here.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_startSuplSession(const void *const p_inData,
                                     const void *const p_inSuplCntl);
eSUPLC_Result SUPLC_stopSuplSession(const void *const p_inData,
                                     const void *const p_inSuplCntl);

/**
 * Function:        SUPLC_activateEeClient
 * Brief:           Activates EE Client.
 * Description:
 * Note:
 * Params:          p_inData - Data. NULL here.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_activateEeClient(const void *const p_inData,
                                     const void *const p_inSuplCntl);


/**
 * Function:        SUPLC_posReport
 * Brief:           Processes pos report from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_posReport(const void *const p_data,const void *const p_suplCntl);



/**
 * Function:        SUPLC_posResponse
 * Brief:           Processes RRLP/RRC position responses from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_posResponse(const void *const p_data, const void *const p_suplCntl);


/**
 * Function:        SUPLC_ConRes
 * Brief:           Processes Server Connection responses from Server.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */

eSUPLC_Result SUPLC_ConRes(const void *const p_data, const void *const p_suplCntl);



/**
  * Function:        registerNalCallbacks
  * Brief:           Registers MCPF_NAL callbacks.
  * Description:
  * Note:            Internal Function.
  * Params:          p_inData - Data (NULL)
  * Return:          Success: SUPLC_SUCCESS.
                     Failure: SUPLC_ERROR_XXX.
*/

static eSUPLC_Result registerNalCallbacks(void *p_inData);

/**
 * Function:        initSuplClient
 * Brief:           Initializes SUPL Client.
 * Description:
 * Note:            Internal Function.
 * Params:          p_inData - Data (NULL)
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result initSuplClient(void *p_inData);


/**
 * Function:        deinitSuplClient
 * Brief:           DeInitializes SUPL Client.
 * Description:
 * Note:            Internal Function.
 * Params:          p_inData - Data (NULL).
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result deInitSuplClient(void *p_inData);

/**
 * Function:        handleNetworkMessage
 * Brief:           Handles network message.
 * Description:
 * Note:
 * Params:          p_inData - Data
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handleNetworkMessage(const void *const p_inData);


/**
 * Function:        startSuplSession
 * Brief:           Starts SUPL Session with SLP.
 * Description:
 * Note:
 * Params:          p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result startSuplSession(const void *const p_inData,
                                      const void *const p_inSuplCntl);
static eSUPLC_Result stopSuplSession(const void *const p_inData,
                                      const void *const p_inSuplCntl);


/**
 * Function:        handlePositionReport
 * Brief:           Handles the position report from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data
                    p_suplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handlePositionReport(const void *const p_data,
                                          const void *const p_suplCntl);


/**
 * Function:        handlePositionResponse
 * Brief:           Handles the position response from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data
                    p_suplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handlePositionResponse(const void *const p_data,
                                            const void *const p_suplCntl);




/**
 * Function:        fillStartRequestData
 * Brief:           Fills StartRequestData Structure .
 * Description:
 * Note:
 * Params:          p_startReqData - Pointer to startReqData structure.
                    p_appId - App Id.
                    p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result fillStartRequestData(StartReqData **p_startReqData,
                                          McpU16 *p_appId,
                                          const void *const p_inData,
                                          const void *const p_inSuplCntl);



/**
 * Function:        fillStartRequestData
 * Brief:           Fills StartRequestData Structure .
 * Description:
 * Note:
 * Params:          p_startReqData - Pointer to startReqData structure.
                    p_outAppId - App Id.
                    p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result fillStartRequestData(StartReqData **p_startReqData,
                                          McpU16 *p_outAppId,
                                          const void *const p_inData,
                                          const void *const p_inSuplCntl);



/**
 * Function:        fillDataReqData
 * Brief:           Fills DataReqData Structure .
 * Description:
 * Note:
 * Params:          p_dataReqData - Pointer to DataReqData structure.
                    p_appId - App Id.
                    p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result fillDataReqData(DataReqData **p_dataReqData,
                                     McpU16 *p_appId,
                                     const void *const p_inData,
                                     const void *const p_inSuplCntl);



/* Function:        fillDataReqDataForPosResp
 * Brief:           Fills DataReqData Structure .
 * Description:
 * Note:
 * Params:          p_dataReqData - Pointer to DataReqData structure.
                    p_appId - App Id.
                    p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result fillDataReqDataForPosResp(DataReqData **p_dataReqData,
                                               McpU16 *p_appId,
                                               const void *const p_inData,
                                               const void *const p_inSuplCntl);



/**
 * Function:        sendDataToCore
 * Brief:           Sends UPL commands to SUPLCore.
 * Description:
 * Note:
 * Params:          u16_appId - App Id.
                    p_data - Pointer to data that has to be sent to Core.
                    u32_command - Command to be sent
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result sendDataToCore(McpU16 u16_appId, McpU8* p_data, McpU32 u32_command);


/**
 * Function:        processResponseMsg
 * Brief:           Processes the UPL response messages from SUPL Client.
 * Description:     It's a callback functions which is called by SUPL Client
                    whenever it has some data to be sent to NAVC.
 * Note:
 * Params:          appId: Application ID.
                    command: SUPL Controller Command.
                    inbuf: Pointer to command data.
 * Return:          Success: TRUE.
                    Failure: FALSE.
 */
static boolVal processResponseMsg(uInt16 appId,
                                 uInt32 Command,
                                 void *inbuf);

/**
 * Function:        processResponseFromSuplClient
 * Brief:           Processes the UPL response messages from SUPL Client.
 * Description:     It's a callback functions which is called by SUPL Client
                    whenever it has some data to be sent to NAVC.
 * Note:
 * Params:          command: SUPL Controller Command.
                    inbuf: Pointer to command data.
 * Return:          Success: TRUE.
                    Failure: FALSE.
 */
static boolVal processResponseFromSuplClient(uInt32 Command, void *inbuf);


/**
  * Function:       nalExecuteCommand
  * Brief:
  * Description:
  * Note:
  * Params:
  * Return:          Success: TRUE.
                     Failure: FALSE.
  */

static eNAL_Result nalExecuteCommand(const eNetCommands e_netCommand, void *p_inBuf);



/**
 * Function:        processAssistanceData
 * Brief:           Processes UPL Commands from SUPLCore and send appropriate
                    requests to NAVC.
 * Description:
 * Note:
 * Params:          p_inBufHandle - Contains RRLP/RRC data from SUPLCore.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result processAssistanceData(void *p_inBufHandle);


/**
 * Function:        processAssistanceData
 * Brief:           Processes UPL Commands from SUPLCore and send appropriate
                    requests to NAVC.
 * Description:
 * Note:
 * Params:          p_inBufHandle - Contains StartLocIndData.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result startNetInitiatedFix(void *p_inBufHandle);

/**
 * Function:        stopNetInitiatedFix
 * Brief:           Processes UPL Commands from SUPLCore and send appropriate
                    requests to NAVC.
 * Description:
 * Note:
 * Params:          p_inBufHandle - Contains StartLocIndData.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
 /* Added for NI */
static eSUPLC_Result stopNetInitiatedFix(void *p_inBufHandle);

static eSUPLC_Result handleAssisAck(const void *const p_data,
                                            const void *const p_suplCntl);


#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPL_CORE_API_H__ */
