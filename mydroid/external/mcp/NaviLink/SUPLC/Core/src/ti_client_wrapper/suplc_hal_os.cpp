/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_hal_os.cpp
 *
 * Description      :   Common interface between SUPL Core and NaviLink.
 *
 * Author           :   Praneet Kumar A
 * Date             :   02nd Feb 2010
 *
 ******************************************************************************
 */


#include "ti_client_wrapper/suplc_hal_os.h"

typedef unsigned char   uInt8;
typedef unsigned short  uInt16;
typedef unsigned long   uInt32;

typedef signed char     sInt8;
typedef signed short    sInt16;
typedef signed long     sInt32;

extern SEM_HANDLE gp_semHandle;

/**
 *  Function:        SUPLC_createSemaphore
 *  Brief:           Creates unnamed semaphore.
 *  Description:
 *  Note:            Internal function.
 *  Params:          p_semHandle - Semaphore Handle.
 *  Return:
 ***/
char SUPLC_createSemaphore(SEM_HANDLE p_semHandle)
{
    sInt8 retVal = 0;

    /* Create an unnamed semaphore */
    retVal = sem_init(p_semHandle,                 /* p_semHandle. */
                      0,                           /* Not shared between processes */
                      0);                          /* Not available */
    if (retVal < 0)
    {
        DEBUG_SUPLC_HAL_OS(" SUPLC_createSemaphore: sem_init FAILED !!! \n");
        return -1;
    }

    DEBUG_SUPLC_HAL_OS(" SUPLC_createSemaphore: Exiting Successfully. \n");
    return 0;
}

/**
 *  Function:        SUPLC_waitForSemaphore
 *  Brief:           Blocks on a semaphore.
 *  Description:
 *  Note:            Internal function.
 *  Params:          p_semaphoreHandle - Semaphore Handle.
 *  Return:
 */
char SUPLC_waitForSemaphore(SEM_HANDLE p_semaphoreHandle)
{
    sInt8 retVal = 0;

    retVal = sem_wait(p_semaphoreHandle);
    if (retVal != 0)
    {
        DEBUG_SUPLC_HAL_OS(" SUPLC_waitForSemaphore: sem_wait FAILED !!! \n");
        return -1;
    }

    DEBUG_SUPLC_HAL_OS(" SUPLC_waitForSemaphore: Exiting Successfully. \n");
    return 0;
}

/**
 * Function:        SUPLC_releaseSemaphore
 * Brief:           Releaase the semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semaphoreHandle - Semaphore Handle.
 * Return:
 */
char SUPLC_releaseSemaphore(SEM_HANDLE p_semaphoreHandle)
{
    sInt8 retVal = 0;
    DEBUG_SUPLC_HAL_OS(" releaseSemaphore: Entering. \n");

    retVal = sem_post(p_semaphoreHandle);
    if (retVal != 0)
    {
        DEBUG_SUPLC_HAL_OS(" SUPLC_releaseSemaphore: sem_post FAILED !!! \n");
        return -1;
    }

    DEBUG_SUPLC_HAL_OS(" SUPLC_releaseSemaphore: Exiting Successfully. \n");
    return 0;
}

/**
 *  Function:        SUPLC_destroySemaphore
 *  Brief:           Destroys unnamed semaphore.
 *  Description:
 *  Note:            Internal function.
 *  Params:          p_semHandle - Semaphore Handle.
 *  Return:
 ***/
char SUPLC_destroySemaphore(SEM_HANDLE p_semHandle)
{
    sInt8 retVal = 0;

    /* Destroy unnamed semaphore */
    retVal = sem_destroy(p_semHandle);

	if (retVal < 0)
    {
        DEBUG_SUPLC_HAL_OS(" SUPLC_destroySemaphore: sem_destroy FAILED !!! \n");
        return -1;
    }

    DEBUG_SUPLC_HAL_OS(" SUPLC_destroySemaphore: Exiting Successfully. \n");
    return 0;
}

