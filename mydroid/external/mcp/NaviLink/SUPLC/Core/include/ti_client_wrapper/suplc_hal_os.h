/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_hal_os.h
 *
 * Description      :   Common interface between SUPL Core and NaviLink.
 *
 * Author           :   Praneet Kumar A
 * Date             :   02nd Feb 2010
 *
 ******************************************************************************
 */

#ifndef __SUPLC_HAL_OS_H__
#define __SUPLC_HAL_OS_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include <semaphore.h>

typedef sem_t* SEM_HANDLE;

//#define ENABLE_HAL_OS_DEBUG

#ifdef ENABLE_HAL_OS_DEBUG
#define LOG_TAG ""
#include <utils/Log.h>
    #define  DEBUG_SUPLC_HAL_OS(...)   LOGD(__VA_ARGS__)
#else
    #define  DEBUG_SUPLC_HAL_OS(...)   ((void)0)
#endif /* ENABLE_HAL_OS_DEBUG */


/**
 *  Function:        SUPLC_createSemaphore
 *  Brief:           Creates unnamed semaphore.
 *  Description:
 *  Note:            Internal function.
 *  Params:          p_semHandle - Semaphore Handle.
 *  Return:
 ***/
char  SUPLC_createSemaphore(SEM_HANDLE p_semHandle);

/**
 *  Function:        SUPLC_waitForSemaphore
 *  Brief:           Blocks on a semaphore.
 *  Description:
 *  Note:            Internal function.
 *  Params:          p_semaphoreHandle - Semaphore Handle.
 *  Return:
 */
char SUPLC_waitForSemaphore(SEM_HANDLE p_semaphoreHandle);

/**
 * Function:        SUPLC_releaseSemaphore
 * Brief:           Releaase the semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semaphoreHandle - Semaphore Handle.
 * Return:
 */
char SUPLC_releaseSemaphore(SEM_HANDLE p_semaphoreHandle);

/**
 * Function:        SUPLC_destroySemaphore
 * Brief:           Destroy the semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semaphoreHandle - Semaphore Handle.
 * Return:
 */
char SUPLC_destroySemaphore(SEM_HANDLE p_semaphoreHandle);

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPLC_HAL_OS_H__ */
