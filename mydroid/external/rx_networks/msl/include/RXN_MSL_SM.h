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
 * $LastChangedDate: 2009-04-17 13:53:14 -0700 (Fri, 17 Apr 2009) $
 * $Revision: 10192 $
 *************************************************************************
 *
 * This file contains implementation of PGPS functionality.
 *
 */

#ifndef _RXN_MSL_SM_H
#define	_RXN_MSL_SM_H

#ifdef	__cplusplus
extern "C" {
#endif

enum MSL_system_states
{
    MSL_STATE_INITIALIZE,
    MSL_STATE_IDLE,
    MSL_STATE_TIME_CHECK,
    MSL_STATE_SNTP_REQUEST_PENDING,
    MSL_STATE_SEED_CHECK,
    MSL_STATE_SEED_DOWNLOAD_PENDING,
    MSL_STATE_CLOCK_UPDATE_DOWNLOAD_PENDING,
    MSL_STATE_SEED_PROPAGATING,
    MSL_STATE_MAX
};

enum MSL_system_events
{
    MSL_EVENT_IDLE,
    MSL_EVENT_TIME_UPDATED,
    MSL_EVENT_SEED_DOWNLOADED,
    MSL_EVENT_EXIT_TIMEOUT,
    MSL_EVENT_SYSTEM_CHECK,
    MSL_EVENT_FACTORY_RESET,
    MSL_EVENT_WRITE_EPHEMERIS,
    MSL_EVENT_NETWORK_AVAILABLE,
    MSL_EVENT_MAX
};

enum MSL_seed_states
{
    MSL_SEED_OKAY,
    MSL_SEED_DOWNLOAD,
    MSL_SEED_CLOCK_UPDATE,
    MSL_SEED_PROPAGATE,
    MSL_SEED_DOWNLOAD_AND_PROPAGATE_REMAINING,
    MSL_SEED_MSL_GPS_TIME_SOURCE_INVALID
};


void MSL_SM_Initialize();
void MSL_SM_Uninitialize();
void MSL_SM_ResetCounters();
void MSL_SM_FactoryReset();
void MSL_RunStateMachine();
void MSL_SetSystemState(enum MSL_system_states state);
void MSL_GetSystemState(enum MSL_system_states * pState);
void MSL_SetNextSystemEvent(enum MSL_system_events event);
void MSL_GetNextSystemEvent(enum MSL_system_events * pEvent);
void MSL_SetNextSystemState(enum MSL_system_states state);
void MSL_ClearSystemEvent();
BOOL MSL_ProcessingAllowed();

U16 MSL_DoNothing(void);
U16 MSL_SystemInitializeAction(void);
U16 MSL_SystemIdleAction(void);
U16 MSL_SNTPRequestAction(void);
U16 MSL_SeedDownloadAction(void);
U16 MSL_ClockUpdateDownloadAction(void);
U16 MSL_CheckSeedStatus(enum MSL_seed_states * pSeedState, RXN_constel_t constel);
U16 MSL_SeedPropagationAction(void);
U16 MSL_TimeoutAction();
U16 MSL_Timeout(U32 timeoutSeconds);

void MSL_ClockUpdateCountReset();

#ifdef	__cplusplus
}
#endif

#endif	/* _RXN_MSL_SM_H */

