/*****< ss1vnetm.h >***********************************************************/
/*      Copyright 2009 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1VNETM - Stonestreet One Virtual Network Module Header File for Linux.  */
/*                                                                            */
/*  Author:  Damon Lange                                                      */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   08/15/09  D. Lange       Initial creation.                               */
/******************************************************************************/
#ifndef __SS1VNETMH__
#define __SS1VNETMH__

#include <linux/ioctl.h>                 /* Included for IOCTL MACRO's.       */

#define SS1VNET_DEVICE_NAME                  "SS1VNET"  /* Defines the Device */
                                                        /* Name of the SS1    */
                                                        /* Virtual Network    */
                                                        /* Driver (Transport  */
                                                        /* Device Side).      */

#define SS1VNET_INTERFACE_NAME               "ss1vnet"  /* Defines the Network*/
                                                        /* Interface Name of  */
                                                        /* the SS1 Virtual    */
                                                        /* Network Driver.    */

#define VNET_DEFAULT_NUMBER_DEVICES                (1)  /* Defines the default*/
                                                        /* number of devices  */
                                                        /* that will be       */
                                                        /* inserted into the  */
                                                        /* system if a number */
                                                        /* is not specified   */
                                                        /* at load time.      */

#define VNET_MAXIMUM_NUMBER_DEVICES               (32)  /* Defines the Maximum*/
                                                        /* number of devices  */
                                                        /* that may be        */
                                                        /* inserted into the  */
                                                        /* system.            */

#define VNET_MINIMUM_NUMBER_DEVICES                (1)  /* Defines the Minimum*/
                                                        /* number of devices  */
                                                        /* that maybe inserted*/
                                                        /* into the system.   */

#define VNET_IOCTL_MAGIC                         ('v')  /* Denotes the magic  */
                                                        /* number associated  */
                                                        /* with this device.  */

#define VNET_MAC_ADDRESS_SIZE                      (6)  /* Defines the size of*/
                                                        /* the MAC Address in */
                                                        /* bytes.             */

#define VNET_IOCTL_QUERY_ETHERNET_ADDRESS            _IOR(VNET_IOCTL_MAGIC, 0, unsigned char *)
                                                        /* Denotes the IOCTL  */
                                                        /* used to query the  */
                                                        /* Ethernet (NIC)     */
                                                        /* Address that the   */
                                                        /* driver is using.   */

#define VNET_IOCTL_SET_ETHERNET_ADDRESS              _IOW(VNET_IOCTL_MAGIC, 1, unsigned char *)
                                                        /* Denotes the IOCTL  */
                                                        /* used to set the    */
                                                        /* Ethernet (NIC)     */
                                                        /* Address that the   */
                                                        /* driver is to use.  */

#define VNET_IOCTL_SET_ETHERNET_CONNECTED_STATUS     _IOW(VNET_IOCTL_MAGIC, 2, int)
                                                        /* Denotes the IOCTL  */
                                                        /* used to set the    */
                                                        /* current state of   */
                                                        /* the ethernet       */
                                                        /* connection (FALSE  */
                                                        /* is not             */
                                                        /* connected/plugged  */
                                                        /* in, TRUE is        */
                                                        /* connected/plugged  */
                                                        /* in).               */

#define VNET_IOCTL_WAIT_RECEIVED_DATA                _IOR(VNET_IOCTL_MAGIC, 3, int)
                                                        /* Denotes the IOCTL  */
                                                        /* used to get the    */
                                                        /* length on the next */
                                                        /* packet in the      */
                                                        /* queue.  This IOCTL */
                                                        /* may block until a  */
                                                        /* packet is          */
                                                        /* available.         */

#endif

