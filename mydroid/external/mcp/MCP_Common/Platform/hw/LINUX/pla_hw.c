/******************************************************************************\
##                                                                            *
## Unpublished Proprietary and Confidential Information of Texas Instruments  *
## Israel Ltd. Do Not Disclose.                                               *
## Copyright 2008 Texas Instruments Israel Ltd.                               *
## All rights reserved. All unpublished rights reserved.                      *
##                                                                            *
## No part of this work may be used or reproduced in any form or by any       *
## means, or stored in a database or retrieval system, without prior written  *
## permission of Texas Instruments Israel Ltd. or its parent company Texas    *
## Instruments Incorporated.                                                  *
## Use of this work is subject to a license from Texas Instruments Israel     *
## Ltd. or its parent company Texas Instruments Incorporated.                 *
##                                                                            *
## This work contains Texas Instruments Israel Ltd. confidential and          *
## proprietary information which is protected by copyright, trade secret,     *
## trademark and other intellectual property rights.                          *
##                                                                            *
## The United States, Israel  and other countries maintain controls on the    *
## export and/or import of cryptographic items and technology. Unless prior   *
## authorization is obtained from the U.S. Department of Commerce and the     *
## Israeli Government, you shall not export, reexport, or release, directly   *
## or indirectly, any technology, software, or software source code received  *
## from Texas Instruments Incorporated (TI) or Texas Instruments Israel,      *
## or export, directly or indirectly, any direct product of such technology,  *
## software, or software source code to any destination or country to which   *
## the export, reexport or release of the technology, software, software      *
## source code, or direct product is prohibited by the EAR. The subject items *
## are classified as encryption items under Part 740.17 of the Commerce       *
## Control List (“CCL”). The assurances provided for herein are furnished in  *
## compliance with the specific encryption controls set forth in Part 740.17  *
## of the EAR -Encryption Commodities and Software (ENC).                     *
##                                                                            *
## NOTE: THE TRANSFER OF THE TECHNICAL INFORMATION IS BEING MADE UNDER AN     *
## EXPORT LICENSE ISSUED BY THE ISRAELI GOVERNMENT AND THE APPLICABLE EXPORT  *
## LICENSE DOES NOT ALLOW THE TECHNICAL INFORMATION TO BE USED FOR THE        *
## MODIFICATION OF THE BT ENCRYPTION OR THE DEVELOPMENT OF ANY NEW ENCRYPTION.*
## UNDER THE ISRAELI GOVERNMENT'S EXPORT LICENSE, THE INFORMATION CAN BE USED *
## FOR THE INTERNAL DESIGN AND MANUFACTURE OF TI PRODUCTS THAT WILL CONTAIN   *
## THE BT IC.                                                                 *
##                                                                            *
\******************************************************************************/

/** Include Files **/
#include "pla_hw.h"



typedef struct _McpGPIOhandle
{
    McpS32 *nShutdownHnd;
}McpGPIOhandle;


typedef enum _GpsGpioState
{
	GPIO_POWER_OFF,
	GPIO_POWER_ON,
	GPIO_POWER_RESET,
	GPIO_POWER_SLEEP
}GpsGpioState;

/************************************************************************/
/*								APIs                                    */
/************************************************************************/
#ifdef CLIENT_CUSTOM 
#define GPIO_RESET_DEVICE       "/dev/gps_reset"


void set_nShutdown(McpU8 value)
{
    McpBool result;
    static McpS32 gpsreset_fd = -1;
    const char buf[] = { value };   // low level is assert

    if (gpsreset_fd == -1)
    {
      gpsreset_fd = open(GPIO_RESET_DEVICE, O_RDWR);
    }

    if (gpsreset_fd != -1)
    {
        result = (write(gpsreset_fd, buf, 1) ==1);
        if(!result)        
        printf("write to /dev/gps_reset unsuccessful");        
        //close(gpsreset_fd);
    }
    else
    {
        printf("\nunable to open /dev/gps_reset\n");
        perror("error:\n");
    }
}
#endif
/************************************************************************/
/** 
 * \fn     hw_gpio_set
 * \brief  Set the GPIO
 * 
 * This function sets the requested GPIO to the specified value.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	index - GPIO index.
 * \param	value - value to set.
 * \return 	Result of operation: OK or ERROR
 * \sa     	hw_gpio_set
 */
EMcpfRes	hw_gpio_set(handle_t hPla, McpU32 index, McpU8 value)
{
	switch(index)
	{
            case BIT_VDD_CORE:
            break;

#ifdef MCP_STU_ENABLE /* Code is not needed for ST in Kernel mode */
            case BIT_GPS_EN_RESET:

#ifdef CLIENT_CUSTOM
             printf("Entering BIT_GPS_EN_RESET value=%d \n", value);
             if(value == GPIO_POWER_OFF)
                  set_nShutdown(0);
             else if(value == GPIO_POWER_ON)
                  set_nShutdown(1);
             else
		          printf("hw_gpio_set: invalid option\n", value);	  	

#else// for ZOOM2
            printf("Entering BIT_GPS_EN_RESET value=%d \n", value);
            if(value == GPIO_POWER_OFF)
                system("echo 0 > /sys/gpsgpio/nshutdown");
            else if(value == GPIO_POWER_ON)
               system("echo 1 > /sys/gpsgpio/nshutdown");
#endif
            break;
#endif

            case BIT_PA_EN:
            break;

            case BIT_SLEEPX:
            break;

	     case BIT_TIMESTAMP:
            break;

	     case BIT_I2C_UART:
            break;

	     case BIT_RESET_INT:
            break;

            default:
            break;
	}
	MCPF_UNUSED_PARAMETER(hPla);
	return RES_COMPLETE;
}

/** 
 * \fn     hw_refClk_set
 * \brief  Enable/Disable the Reference Clock
 * 
 * This function enables or disables the 
 * reference clock (for clock configuration #4).
 * Note: WinXP on PC application can't control the Ref Clk.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	uIsEnable - Enable/Disable the ref clk.
 * \return 	Result of operation: OK or ERROR
 * \sa     	hw_refClk_set
 */
EMcpfRes	hw_refClk_set(handle_t hPla, McpU8 uIsEnable)
{
	MCPF_UNUSED_PARAMETER(hPla);
	MCPF_UNUSED_PARAMETER(uIsEnable);
	
	return RES_COMPLETE;
}
