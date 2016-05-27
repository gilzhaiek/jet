/*****< ss1vnetm.c >***********************************************************/
/*      Copyright 2009 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1VNETM - Stonestreet One Virtual Network Module for Linux.              */
/*                                                                            */
/*  Author:  Tim Thomas                                                       */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   08/18/02  T. Thomas      Initial creation.                               */
/******************************************************************************/
#include <linux/module.h>       /* Included for module MACRO's.               */
#include <linux/init.h>         /* Included for MACRO's used in marking module*/
                                /* initialization and cleanup functions.      */
#include <linux/kernel.h>       /* Included for printk().                     */
#include <linux/slab.h>         /* Included for kmalloc() and kfree().        */
#include <linux/cdev.h>         /* Included for cdev structure                */
#include <linux/netdevice.h>    /* Included for Network Device definitions    */
#include <linux/etherdevice.h>  /* Included for Network Device definitions    */
#include <linux/version.h>
#include <asm/uaccess.h>        /* Included for copy_to/from_user().          */
#include <asm/string.h>         /* Included for memcpy()/memset().            */
#include <linux/types.h>        /* Included for Linux Type definitions.       */
#include <linux/errno.h>        /* Included for Error Code definitions.       */
#include <linux/sched.h>        /* Included for Wait Queue Functions.         */
#include <linux/fs.h>           /* Included for xxx_chrdev_xxx() definitions. */

#include "SS1VNETM.h"           /* Main Module Prototypes/Constants.          */

   /* The following constant defines the data packet threshold that is  */
   /* supported by this driver.  This threshold specifies the maximum   */
   /* number of packets that will be buffered by an individual driver   */
   /* before the network stack is told to told to stop sending packets  */
   /* (flow off).  Once the network stack has been flowed off, it will  */
   /* not be restarted again until the number of queued packets falls   */
   /* below the VNET_NUM_PACKET_FLOW_ON_THRESHOLD constant.             */
#define VNET_MAXIMUM_NUMBER_QUEUED_PACKETS       (256)

   /* The following constant defines the data packet threshold (in      */
   /* number of queued packets) that the queue must fall below          */
   /* (threshold) before the network stack is told to resume sending    */
   /* packets (flow on).  This value is used when the network stack has */
   /* been told to stop sending packets (flow off).                     */
#define VNET_NUM_PACKET_FLOW_ON_THRESHOLD        (VNET_MAXIMUM_NUMBER_QUEUED_PACKETS - 32)

   /* Define a Boolean Value that is contained in a Byte.               */
typedef unsigned char Bool_t;

   /* The Linux Kernel makes no definition of TRUE and FALSE, so that   */
   /* they may be used within this module they are being defined here.  */
#ifndef TRUE
   #define TRUE (1 == 1)
#endif

#ifndef FALSE
   #define FALSE (0 == 1)
#endif

/* Uncomment following line to enable debug printks, comment out for no msgs. */
//xxx#define DEBUG_ENABLE

#ifdef DEBUG_ENABLE

   #define DEBUGPRINT(format, args...)  printk(format, ##args)
                                                /* MACRO definition used for  */
                                                /* debug printing.            */
#else

   #define DEBUGPRINT(format, args...)
                                                /* MACRO definition used for  */
                                                /* debug printing.            */
#endif

/* Uncomment following line to enable dumping data received to write to the   */
/* device driver data buffer.  Probably shouldn't be used when moving lots of */
/* data.                                                                      */
//xxx#define TRACE_DATA_ENABLE

#ifdef TRACE_DATA_ENABLE

   /* The following defines the number of bytes that are placed in a    */
   /* single row of a data dump.                                        */
   #define MAXIMUM_BYTES_PER_ROW                     (16)

   /* Allocate a buffer to hold formatted debug messages.  The size of  */
   /* the buffer may be adjusted to accomodate the length of the debug  */
   /* messages used.                                                    */
   #define MAX_DBG_DUMP_BYTES                       (256)

   static void DumpData(unsigned int DataLength, unsigned char *DataPtr);

   #define DUMPDATA(a, b)  DumpData((a), (b))

#else

   #define DUMPDATA(a, b)

#endif

MODULE_LICENSE("GPL");

typedef enum
{
   qsPaused, qsActive
} Queue_State_t;

   /* VNET Driver Information Block.  This structure contains ALL       */
   /* information associated with a specific Minor Device Number (member*/
   /* is present in this structure).                                    */
typedef struct _tagDriverInfo_t
{
   struct semaphore               DriverMutex;
   Bool_t                         DeviceOpen;
   Bool_t                         TransportOpen;
   Bool_t                         TransportWaiting;
   Bool_t                         BluetoothConnected;
   Bool_t                         NetQueueActive;
   Bool_t                         ShutDown;

   int                            CdevAdded;
   struct cdev                    DriverCdev;    /* Holds cdev info     */

   struct net_device             *NetDevice;
   struct net_device_stats        NetDeviceStats;
   unsigned char                  MACAddress[VNET_MAC_ADDRESS_SIZE];

   unsigned int                   DeviceNumber;

   spinlock_t                     PacketCountSpinLock;
   unsigned int                   NumberQueuedPackets;
   unsigned int                   PacketQueueWriteIndex;
   unsigned int                   PacketQueueReadIndex;
   struct sk_buff                *PacketQueueArray[VNET_MAXIMUM_NUMBER_QUEUED_PACKETS];

   unsigned int                   ReadLength;
   wait_queue_head_t              WaitEventWaitQueue;

} DriverInfo_t;

   /* Internal Variables to this Module (Remember that all variables    */
   /* declared static are initialized to 0 automatically by the         */
   /* compiler as part of standard C/C++).                              */
static int NumberOfDevices;                     /* Variable which holds the   */
                                                /* number of devices which    */
                                                /* will be available for use  */
                                                /* within the system. This is */
                                                /* a module parameter that    */
                                                /* maybe set at load time with*/
                                                /* the insmod system call.    */

static DriverInfo_t *DriverInfoList;            /* Variable which holds the   */
                                                /* First Entry (Head of List) */
                                                /* of All currently opened    */
                                                /* VNET Drivers.              */

static struct file_operations VNETFileOperations;  /* Variable which holds the*/
                                                /* File Operation structure   */
                                                /* for the character device   */
                                                /* associated with this       */
                                                /* module.                    */

static int VNETCharacterDeviceMajorNumber;      /* Variable which holds the   */
                                                /* Major Number of the        */
                                                /* character device used as   */
                                                /* the Transport side to the  */
                                                /* Virtual Serial Device.     */

static struct net_device_stats dummyStats;      /* Variable to reference      */
                                                /* status info for devices    */
                                                /* were not located.          */

static struct class *DriverClass;               /* Variable which holds the   */
                                                /* device class under which   */
                                                /* VNET devices are created.  */

   /* The module_param MACRO is used to declare the Number Of Devices   */
   /* Variable as a Module Parameter to be used at load time to set the */
   /* number of devices to insert into the system.                      */
module_param(NumberOfDevices, int, S_IRUGO);

static DriverInfo_t *SearchDeviceEntry(struct net_device *Device);
static void CleanupVNETModule(void);

static void VNET_Device_Init(struct net_device *NetDevice);
static void VNET_Device_Cleanup(DriverInfo_t *DriverInfoPtr);
static int VNET_Device_ReceivePacket(DriverInfo_t *DriverInfoPtr, unsigned char __user *Packet, int PacketLength);
static void VNET_Device_ConfirmPacket(DriverInfo_t *DriverInfoPtr);
static int VNET_Device_Open(struct net_device *NetDevice);
static int VNET_Device_Stop(struct net_device *NetDevice);
static int VNET_Device_Transmit(struct sk_buff *SocketBuffer, struct net_device *NetDevice);
static void VNET_Device_Timeout(struct net_device *NetDevice);
static int VNET_Device_Set_Config(struct net_device *NetDevice, struct ifmap *map);
static struct net_device_stats *VNET_Device_Stats(struct net_device *NetDevice);
static int VNET_Device_Ioctl(struct net_device *NetDevice, struct ifreq *ifreq, int Command);

static int  VNET_Transport_Open(struct inode *INode, struct file *FilePointer);
static int  VNET_Transport_Close(struct inode *INode, struct file *FilePointer);
static ssize_t VNET_Transport_Read(struct file *FilePointer, char *Buffer, size_t Length, loff_t *LongOffset);
static ssize_t VNET_Transport_Write(struct file *FilePointer, const char *Buffer, size_t Length, loff_t *LongOffset);
#if HAVE_UNLOCKED_IOCTL
static long VNET_Transport_Ioctl(struct file *FilePointer, unsigned int Command, unsigned long Parameter);
#else
static int  VNET_Transport_Ioctl(struct inode *INode, struct file *FilePointer, unsigned int Command, unsigned long Parameter);
#endif

#if (LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,28))

static const struct net_device_ops net_device_ops =
{
   .ndo_open        = VNET_Device_Open,
   .ndo_stop        = VNET_Device_Stop,
   .ndo_start_xmit  = VNET_Device_Transmit,
   .ndo_get_stats   = VNET_Device_Stats,
   .ndo_tx_timeout  = VNET_Device_Timeout,
   .ndo_set_config  = VNET_Device_Set_Config,
   .ndo_do_ioctl    = VNET_Device_Ioctl
} ;

#endif

#ifdef TRACE_DATA_ENABLE

   /* The following function is responsible for writing binary debug    */
   /* data to the specified debug file handle.  The first parameter to  */
   /* this function is the handle of the open debug file to write the   */
   /* debug data to.  The second parameter to this function is the      */
   /* length of binary data pointed to by the next parameter.  The final*/
   /* parameter to this function is a pointer to the binary data to be  */
   /* written to the debug file.                                        */
static void DumpData(unsigned int DataLength, unsigned char *DataPtr)
{
   char           Buffer[80];
   char          *BufPtr;
   char          *HexBufPtr;
   unsigned char  DataByte;
   unsigned int   Index;
   static char    HexTable[] = "0123456789ABCDEF\n";
   static char    Header1[]  = "       00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F  ";
   static char    Header2[]  = " -----------------------------------------------------------------------\n";

   /* Before proceeding any further, lets make sure that the parameters */
   /* passed to us appear semi-valid.                                   */
   if((DataLength > 0) && (DataPtr != NULL))
   {
      DataLength = (DataLength > MAX_DBG_DUMP_BYTES)?MAX_DBG_DUMP_BYTES:DataLength;

      /* The parameters which were passed in appear to be at least      */
      /* semi-valid, next write the header out to the file.             */
      printk("%s", Header1);
      printk("%s", HexTable);
      printk("%s", Header2);

      /* Now that the Header is written out, let's output the Debug     */
      /* Data.                                                          */
      memset(Buffer, ' ', sizeof(Buffer));
      BufPtr    = Buffer + sprintf(Buffer," %05X ", 0);
      HexBufPtr = Buffer + sizeof(Header1)-1;
      for(Index=0; Index<DataLength;)
      {
         Index++;
         DataByte     = *DataPtr++;
         *BufPtr++    = HexTable[(unsigned char)(DataByte >> 4)];
         *BufPtr      = HexTable[(unsigned char)(DataByte & 0x0F)];
         BufPtr      += 2;
         *HexBufPtr++ = (char)(((DataByte >= ' ') && (DataByte <= '~') && (DataByte != '\\') && (DataByte != '%'))?DataByte:'.');
         if(((Index % MAXIMUM_BYTES_PER_ROW) == 0) || (Index == DataLength))
         {
            *HexBufPtr++ = '\n';
            *HexBufPtr   = 0;
            DEBUGPRINT(KERN_ERR"%s", Buffer);
            if(Index != DataLength)
            {
               memset(Buffer, ' ', sizeof(Buffer));
               BufPtr    = Buffer + sprintf(Buffer," %05X ", Index);
               HexBufPtr = Buffer + sizeof(Header1)-1;
            }
            else
            {
               HexBufPtr = NULL;
            }
         }
      }

      if(HexBufPtr)
      {
         *HexBufPtr++ = '\n';
         *HexBufPtr   = 0;
         DEBUGPRINT(KERN_ERR "%s", Buffer);
      }
      DEBUGPRINT(KERN_ERR "%s", "\n");
   }
}

#endif

   /* The following function is used to search the Net Device List to   */
   /* locate an entry that is associated with the net device specified. */
   /* If a match is located, the function returns a pointer to the      */
   /* Device Info structure, else it returns NULL.                      */
static DriverInfo_t *SearchDeviceEntry(struct net_device *Device)
{
   int          i;
   DriverInfo_t *ret_val = NULL;

   /* Loop through each device entry until we reach the end or find a   */
   /* match.                                                            */
   i = 0;
   while((!ret_val) && (i < NumberOfDevices))
   {
      if(DriverInfoList[i].NetDevice == Device)
      {
         ret_val = &DriverInfoList[i];
      }
      else
      {
         i++;
      }
   }
   if(!ret_val)
   {
      DEBUGPRINT(KERN_ERR "Device %p Not Found!\n", Device);
   }
   return(ret_val);
}

   /* The following function is responsible for cleaning up the VNET    */
   /* module and all data structures declared within.  This function is */
   /* called upon removal of the module from the kernel via the system  */
   /* call rmmod.  Note that the kernel will not call this function if  */
   /* the Module Use Count is greater then zero.                        */
static void CleanupVNETModule(void)
{
   int i;
   dev_t devno;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   /* remove the character and tty devices from the system.             */
   for(i = 0; i < NumberOfDevices; i++)
   {
      if(DriverInfoList[i].NetDevice)
      {
         /* Unregister the device and free the memory that was          */
         /* allocated.                                                  */
         VNET_Device_Cleanup(&DriverInfoList[i]);
         unregister_netdev(DriverInfoList[i].NetDevice);
         free_netdev(DriverInfoList[i].NetDevice);
      }

      if(DriverClass)
      {
         /* Unregister the device from sysfs.                           */
         devno = MKDEV(VNETCharacterDeviceMajorNumber, i);
         device_destroy(DriverClass, devno); 
      }
   }

   /* Destroy the device class created for this driver.                 */
   if(DriverClass)
      class_destroy(DriverClass);

   /* All through, first clean up any allocated resources.              */
   if(VNETCharacterDeviceMajorNumber)
   {
      devno = MKDEV(VNETCharacterDeviceMajorNumber, 0);
      unregister_chrdev_region(devno, NumberOfDevices);
   }

   /* Now free all of the memory that may have been allocated when the  */
   /* module was initialized.                                           */
   if(DriverInfoList)
      kfree(DriverInfoList);

   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__ );
}

   /* The following function is responsible for initializing the VNET   */
   /* module and ALL data structures required by the module.  This      */
   /* function is called upon insertion of the module into the kernel   */
   /* via the system call insmod.  This function return zero upon       */
   /* successful execution and a negative value on all errors.          */
   /* Newer Linux kernels have a different mechanism for registering    */
   /* char devices, meaning this module is structurally different than  */
   /* the versions written to a 2.4 system, even though the code is     */
   /* quite similar in places.                                          */
int __init InitializeVNETModule(void)
{
   int                    ret_val;
   dev_t                  dev = 0;
   unsigned int           i;
   DriverInfo_t          *DeviceInfo;
   struct net_device     *NetDevice;
   unsigned char          MACAddress[VNET_MAC_ADDRESS_SIZE];

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);
   printk("printk NumberOfDevices %d\n", NumberOfDevices);

   /* First check to see if the Number of Devices Parameter was set to a*/
   /* value other then the default.                                     */
   if(!NumberOfDevices)
   {
      /* The Number of Devices Parameter was not set, set it to the     */
      /* default value.                                                 */
      NumberOfDevices = VNET_DEFAULT_NUMBER_DEVICES;
   }

   /* Now check to make sure that the value that was specified for the  */
   /* Number of Devices appears to be valid.                            */
   if((NumberOfDevices >= VNET_MINIMUM_NUMBER_DEVICES) && (NumberOfDevices <= VNET_MAXIMUM_NUMBER_DEVICES))
   {
      /* Initialize a default MAC Address.                              */
      memcpy(MACAddress, "\x0VNET0", VNET_MAC_ADDRESS_SIZE);

      /* The Number of Devices appears to be at least semi-valid, now   */
      /* attempt to allocate the memory that is required for this module*/
      /* to work with the specified Number of Devices.                  */
      printk("Allocate Memory for %d\n", NumberOfDevices);
      DriverInfoList = (DriverInfo_t *)kmalloc(sizeof(DriverInfo_t)*NumberOfDevices, GFP_KERNEL);
      printk("Return Allocate Memory for %d  Ptr %p\n", NumberOfDevices, DriverInfoList);

      /* Use alloc_chrdev_region to dynamically allocate major device   */
      /* number of physical character device (transport side).  Note    */
      /* minor numbers start at 0.                                      */
      VNETCharacterDeviceMajorNumber = 0;
      ret_val = alloc_chrdev_region(&dev, 0, NumberOfDevices, SS1VNET_DEVICE_NAME);
      printk("alloc_chrdev_region returned %d\n", ret_val);
      if(ret_val >= 0)
      {
         /* Note the major device number that was allocated. Non-zero   */
         /* will be used in cleanup routine to trigger unregistering the*/
         /* character device.                                           */
         VNETCharacterDeviceMajorNumber = MAJOR(dev);
      }

      printk("VNETCharacterDeviceMajorNumber returned %d %d\n", VNETCharacterDeviceMajorNumber, ret_val);
      DEBUGPRINT(KERN_ERR "VNETCharacterDeviceMajorNumber %d, ret_val %d\n", VNETCharacterDeviceMajorNumber, ret_val);

      /* Now make sure that all the memory that was requested was       */
      /* successfully allocated and a device region was setup.          */
      if((DriverInfoList) && (VNETCharacterDeviceMajorNumber))
      {
         /* Populate file ops structure (VNETFileOperations) used for   */
         /* cdev (kernel internal character device representation).     */
         VNETFileOperations.owner          = THIS_MODULE;
         VNETFileOperations.open           = VNET_Transport_Open;
         VNETFileOperations.release        = VNET_Transport_Close;
         VNETFileOperations.read           = VNET_Transport_Read;
         VNETFileOperations.write          = VNET_Transport_Write;
#if HAVE_UNLOCKED_IOCTL
         VNETFileOperations.unlocked_ioctl = VNET_Transport_Ioctl;
#else
         VNETFileOperations.ioctl          = VNET_Transport_Ioctl;
#endif

         /* Create a class under which the VNET devices can be created. */
         if((DriverClass = class_create(THIS_MODULE, SS1VNET_INTERFACE_NAME)) != NULL)
         {
            DEBUGPRINT("Created class \"%s\"\n", SS1VNET_INTERFACE_NAME);

            /* Initialize the Driver Information List.                  */
            ret_val = FALSE;
            for(i = 0; (i < NumberOfDevices) && (!ret_val); i++)
            {
               memset(&DriverInfoList[i], 0, sizeof(DriverInfo_t));
               DriverInfoList[i].DriverCdev.owner = THIS_MODULE;

               sema_init(&DriverInfoList[i].DriverMutex, 1);
               init_waitqueue_head(&DriverInfoList[i].WaitEventWaitQueue);

               /* Register the device with sysfs.                       */
               if(device_create(DriverClass, NULL, MKDEV(VNETCharacterDeviceMajorNumber, i), NULL, "%s%d", SS1VNET_DEVICE_NAME, i) != NULL)
               {
                  DEBUGPRINT("Created device \"%s%d\"\n", SS1VNET_DEVICE_NAME, i);

                  /* Initialize the cdev structure and add to kernel.   */
                  cdev_init(&(DriverInfoList[i].DriverCdev), &VNETFileOperations);

                  ret_val = cdev_add(&(DriverInfoList[i].DriverCdev), MKDEV(VNETCharacterDeviceMajorNumber, i), 1);
                  DEBUGPRINT(KERN_ERR "cdev_add %d returns %d\n", i, ret_val);
                  if(!ret_val)
                  {
                     /* Note that the character device was successfully */
                     /* added.                                          */
                     DriverInfoList[i].CdevAdded = TRUE;

                     /* Create the Network Device.                      */
                     DEBUGPRINT(KERN_ERR "Install Net Device %d\n", i);

                     /* Allocate the memory for the Device and our      */
                     /* private data.                                   */
                     NetDevice = alloc_netdev(0, (SS1VNET_INTERFACE_NAME "%d"), VNET_Device_Init);
                     if(NetDevice)
                     {
                        DEBUGPRINT(KERN_ERR "alloc_netdev\n");
                        /* Save the pointer to the device information in*/
                        /* the Context Structure.                       */
                        DeviceInfo                = &DriverInfoList[i];
                        DeviceInfo->NetDevice     = NetDevice;
                        DeviceInfo->DeviceNumber  = i;
                        DeviceInfo->DeviceOpen    = FALSE;
                        DeviceInfo->TransportOpen = FALSE;

                        memset(&DeviceInfo->NetDeviceStats, 0, sizeof(struct net_device_stats));

                        DeviceInfo->NetQueueActive        = FALSE;
                        DeviceInfo->BluetoothConnected    = FALSE;
                        DeviceInfo->NumberQueuedPackets   = 0;
                        DeviceInfo->PacketQueueReadIndex  = 0;
                        DeviceInfo->PacketQueueWriteIndex = 0;

                        /* Initialize the Spin Lock that is used to     */
                        /* protect the Packet count value.              */
                        spin_lock_init(&DeviceInfo->PacketCountSpinLock);

                        /* Set the MAC Address to a default value.      */
                        memcpy(DeviceInfo->MACAddress, MACAddress, VNET_MAC_ADDRESS_SIZE);

                        DEBUGPRINT(KERN_ERR "register_netdev\n");
                        /* Register the device with the system.         */
                        ret_val = register_netdev(NetDevice);
                        if(ret_val)
                        {
                           DEBUGPRINT(KERN_ERR "Net Device Failed\n");
                           /* We will free the memory for this device.  */
                           /* The remaining device will get free in the */
                           /* cleanup.                                  */
                           free_netdev(NetDevice);
                           DeviceInfo->NetDevice = 0;

                           /* Flag that a device failed to be           */
                           /* registerred.                              */
                           ret_val = -ENODEV;
                        }
                        else
                        {
                           DEBUGPRINT(KERN_ERR "Net Device %s registerred\n", NetDevice->name);
                           /* Increment the Device Number and MAC       */
                           /* Address to the next value.                */
                           MACAddress[VNET_MAC_ADDRESS_SIZE-1]++;
                        }
                     }
                     else
                     {
                        /* We failed to allocate the device so abort the*/
                        /* process.                                     */
                        ret_val = -ENOMEM;
                     }
                  }
               }
               else
               {
                  DEBUGPRINT("Failed to create device \"%s%d\"\n", SS1VNET_DEVICE_NAME, i);
                  /* Flag that a device failed to be registered.        */
                  ret_val = -ENODEV;
               }
            }
         }
         else
         {
            DEBUGPRINT("Failed to create class \"%s\"\n", SS1VNET_INTERFACE_NAME);
            /* Flag that the devices could not be registered because the*/
            /* device class could not be created.                       */
            ret_val = -ENODEV;
         }
      }
      else
      {
         DEBUGPRINT(KERN_ERR "memory allocation failure\n");
         /* An error occurred while attempting to allocate memory for   */
         /* the module to use.  Set the return value to indicate an     */
         /* error.                                                      */
         ret_val = -ENOMEM;
      }

      /* Check to see if and error occurred in the initialization of the*/
      /* module.                                                        */
      if(ret_val)
      {
         /* An error occurred in the initialization of the module,      */
         /* let cleanup function clean up everything.                   */
         CleanupVNETModule();
      }
   }
   else
   {
      DEBUGPRINT(KERN_ERR "Number Of Devices is invalid\n");
      /* The Number Of Devices specified is invalid, set the return     */
      /* value to indicate and error.                                   */
      ret_val = -EINVAL;
   }

   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__ );

   return(ret_val);
}

void __exit ExitVNETModule(void)
{
   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__ );
   CleanupVNETModule();
   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__ );
}

   /* The following function is used to cleanup a NetDevice_t structure.*/
   /* * It will scan the list of packets that are witing to be sent and */
   /* free the memory.                                                  */
static void VNET_Device_Init(struct net_device *NetDevice)
{
   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__ );
   /* Verify that the device info is valid before we dereference it.    */
   if(NetDevice)
   {
      /* Use the Linux Helper to initialize some of the fields.         */
      ether_setup(NetDevice);

#if (LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,28))

      NetDevice->netdev_ops = &net_device_ops;

#else

      NetDevice->open            = VNET_Device_Open;
      NetDevice->stop            = VNET_Device_Stop;
      NetDevice->hard_start_xmit = VNET_Device_Transmit;
      NetDevice->get_stats       = VNET_Device_Stats;
      NetDevice->tx_timeout      = VNET_Device_Timeout;
      NetDevice->set_config      = VNET_Device_Set_Config;
      NetDevice->do_ioctl        = VNET_Device_Ioctl;

#endif

      NetDevice->watchdog_timeo  = HZ*10000;
      NetDevice->hard_header_len = ETH_HLEN;
      NetDevice->tx_queue_len    = VNET_MAXIMUM_NUMBER_QUEUED_PACKETS;
      memcpy(NetDevice->dev_addr, "\x00\x02\x03\x04\x05\x06", 6);
   }
   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
}

   /* The following function is used to cleanup a DriverInfo_t          */
   /* structure. * It will scan the list of packets that are witing to  */
   /* be sent and free the memory.                                      */
static void VNET_Device_Cleanup(DriverInfo_t *DriverInfoPtr)
{
   unsigned long   flags;
   struct sk_buff *SocketBuffer;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__ );

   /* Verify that the parameter passed in appears valid.                */
   if(DriverInfoPtr)
   {
      /* Obtain the Lock that protects the Packet Count.                */
      spin_lock_irqsave(&DriverInfoPtr->PacketCountSpinLock, flags);

      /* If there are any packets that are in queue, release the memory.*/
      while(DriverInfoPtr->PacketQueueReadIndex != DriverInfoPtr->PacketQueueWriteIndex)
      {
         /* Get a socket Buffer from the list.                          */
         SocketBuffer = DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueReadIndex];
         if(SocketBuffer)
         {
            DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueReadIndex] = NULL;;
            dev_kfree_skb_any(SocketBuffer);
         }

         /* Check to see if the read index needs to be wrapped.         */
         DriverInfoPtr->PacketQueueReadIndex++;
         if(DriverInfoPtr->PacketQueueReadIndex == VNET_MAXIMUM_NUMBER_QUEUED_PACKETS)
         {
            DriverInfoPtr->PacketQueueReadIndex = 0;
         }
      }
      /* Reset the Count and Index values to their default values.      */
      DriverInfoPtr->NumberQueuedPackets   = 0;
      DriverInfoPtr->PacketQueueReadIndex  = 0;
      DriverInfoPtr->PacketQueueWriteIndex = 0;

      /* Release the Spinlock that protects the Packet Count.           */
      spin_unlock_irqrestore(&DriverInfoPtr->PacketCountSpinLock, flags);
   }
   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
}

   /* The following function is used to Receive a packet from the       */
   /* virtual layers of this network driver.  This function will receive*/
   /* an IP packet and create a sk_buff to transmit the packet back up  */
   /* the the Linux IP stack.  This function will create memory for a   */
   /* sk_buff that the IP stack will delete when complete.  The first   */
   /* paramerter is a pointer to the device information for the device  */
   /* that will receive the packet.The second parameter is the pointer  */
   /* to the IP Packet (which resides in user space).  The second       */
   /* pointer is the Length of the packet.  The funcion return a        */
   /* negative value on error.                                          */
static int VNET_Device_ReceivePacket(DriverInfo_t *DriverInfoPtr, unsigned char __user *Packet, int PacketLength)
{
   int             ret_val;
   char           *PutDataPtr;
   struct sk_buff *SocketBuffer;

   DEBUGPRINT(KERN_ERR "%s : enter Length %d\n", __FUNCTION__, PacketLength);

   /* Check to make sure we have a semi-valid DriverInfoPtr and that the*/
   /* parameters passed in look semi-valid.                             */
   if((DriverInfoPtr) && (Packet) && (PacketLength))
   {
      /* Create a Socket Buffer which is used to communicate data with  */
      /* the Linux IP stack.                                            */
      SocketBuffer = dev_alloc_skb(PacketLength);
      if(SocketBuffer)
      {
         /* Copy the packet to the socket buffer that will be passed up */
         /* the stack.  The skb_put will move the tail to the end of the*/
         /* memory allocated.  Plus return a pointer to the memory were */
         /* the packet data should be copied to.                        */
         PutDataPtr = skb_put(SocketBuffer, PacketLength);
         if(PutDataPtr)
         {
            DEBUGPRINT(KERN_ERR "Put %p:%d : Data %p:%d\n", SocketBuffer->data, SocketBuffer->len, PutDataPtr, PacketLength);

            /* Copy the data from user space to the Buffer that was     */
            /* allocated.                                               */
            if(!copy_from_user(PutDataPtr, Packet, PacketLength))
            {
               DUMPDATA(SocketBuffer->len, SocketBuffer->data);

               /* Set the additional information for the buffer.        */
               SocketBuffer->ip_summed                 = CHECKSUM_UNNECESSARY;
               SocketBuffer->dev                       = DriverInfoPtr->NetDevice;
               SocketBuffer->protocol                  = eth_type_trans(SocketBuffer, DriverInfoPtr->NetDevice);
               DriverInfoPtr->NetDevice->last_rx       = jiffies;
               DriverInfoPtr->NetDeviceStats.rx_bytes += PacketLength;
               DriverInfoPtr->NetDeviceStats.rx_packets++;

               /* Transmit the new Socket Buffer up to the Linux IP     */
               /* Stack.  The will take care of deleting the memory     */
               /* allocated by this driver.                             */
               /* * NOTE * Here we will use netif_rx_ni() instead of    */
               /*          netif_rx() since we are not in interrupt     */
               /*          context.  The netif_rx() function would cause*/
               /*          a 10ms polling routine to process the data   */
               /*          and thus produce some delays in the          */
               /*          processing of the data.                      */
               netif_rx_ni(SocketBuffer);

               /* Set the return value for success.                     */
               ret_val = 0;
            }
            else
            {
               ret_val = -EFAULT;
            }
         }
         else
         {
            /* We failed to allocate a buffer to hold the data, so we   */
            /* will have to discard this data.                          */
            DriverInfoPtr->NetDeviceStats.rx_dropped++;
            ret_val = -ENOMEM;
         }
      }
      else
         ret_val = -ENOMEM;
   }
   else
      ret_val = -EINVAL;

  DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);

   return(ret_val);
}

   /* The following function is used to handle a confirm for the tranmit*/
   /* of a packet to the virtual network driver.  The first parameter is*/
   /* a pointer to the Network Device that contains the packet that was */
   /* just sent to the virtual device..                                 */
static void VNET_Device_ConfirmPacket(DriverInfo_t *DriverInfoPtr)
{
   unsigned long   flags;
   struct sk_buff *SocketBuffer;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__ );

   /* Check to make sure we have a semi-valid DriverInfoPtr and that the*/
   /* parameters passed in look semi-valid.                             */
   if(DriverInfoPtr)
   {
      /* Get a pointer to the Socket Buffer that was just sent.         */
      SocketBuffer = DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueReadIndex];

      /* Verify that the pointer to the Socket Buffer appears valid.    */
      if(SocketBuffer)
      {
         /* Obtain the Lock that protects the Packet Count.             */
         spin_lock_irqsave(&DriverInfoPtr->PacketCountSpinLock, flags);

         /* Indicate that we have de-queued the packet.                 */
         DriverInfoPtr->NumberQueuedPackets--;

         /* Update the stats with the data that was sent.               */
         DriverInfoPtr->NetDeviceStats.tx_bytes += SocketBuffer->len;
         DriverInfoPtr->NetDeviceStats.tx_packets++;

         /* Remove the packet from the list.                            */
         DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueReadIndex] = NULL;

         /* Update the Read Index.                                      */
         DriverInfoPtr->PacketQueueReadIndex++;
         if(DriverInfoPtr->PacketQueueReadIndex == VNET_MAXIMUM_NUMBER_QUEUED_PACKETS)
         {
            DriverInfoPtr->PacketQueueReadIndex = 0;
         }

         /* Release the Spinlock that protects the Packet Count.        */
         spin_unlock_irqrestore(&DriverInfoPtr->PacketCountSpinLock, flags);

         /* If the network is currently off, we need to see if it is    */
         /* time to turn in back on.                                    */
         if(!DriverInfoPtr->NetQueueActive)
         {
            /* Check to see if we are less than the threshold.          */
            if(DriverInfoPtr->NumberQueuedPackets < VNET_NUM_PACKET_FLOW_ON_THRESHOLD)
            {
               DriverInfoPtr->NetQueueActive = TRUE;

               /* Wake the network stack back up.                       */
               netif_wake_queue(DriverInfoPtr->NetDevice);
            }
         }

         /* Free the Buffer.                                            */
         dev_kfree_skb_any(SocketBuffer);
      }
   }
   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
}

   /* The following function is used to supply the upper layer IP stack */
   /* a way to open the driver.  This function will double check that   */
   /* the driver was initialized correctly.  This function will receive */
   /* a the net_device that is called the driver.  This function's will */
   /* return a 0 if the driver opened properly.                         */
static int VNET_Device_Open(struct net_device *NetDevice)
{
   int           ret_val;
   DriverInfo_t *DriverInfoPtr;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__ );

   /* Check to make sure the values passed in look somewhat valid.      */
   if(NetDevice)
   {
      /* Search for the Device info that is associated with the         */
      /* specified device.                                              */
      DriverInfoPtr = SearchDeviceEntry(NetDevice);
      if(DriverInfoPtr)
      {
         /* Grab the Mutex that protects the Device List.               */
         if(!down_interruptible(&DriverInfoPtr->DriverMutex))
         {
            /* Initialize the index and buffers that hold references to */
            /* packets that are being sent and received.                */
            memset(&DriverInfoPtr->NetDeviceStats, 0, sizeof(struct net_device_stats));
            DriverInfoPtr->NumberQueuedPackets   = 0;
            DriverInfoPtr->PacketQueueReadIndex  = 0;
            DriverInfoPtr->PacketQueueWriteIndex = 0;
            DriverInfoPtr->DeviceOpen            = TRUE;

            /* Check to see of the Bluetooth Device is connected        */
            if(DriverInfoPtr->BluetoothConnected)
            {
               DriverInfoPtr->NetQueueActive = TRUE;
               netif_carrier_on(NetDevice);
            }
            else
            {
               DriverInfoPtr->NetQueueActive = FALSE;
               netif_carrier_off(NetDevice);
            }

            /* Copy the currnet Address into the device.                */
            memcpy(NetDevice->dev_addr, DriverInfoPtr->MACAddress, VNET_MAC_ADDRESS_SIZE);

            /* Start the Network Queue.                                 */
            netif_start_queue(NetDevice);

            /* Release Lock protecting the device info.                 */
            up(&DriverInfoPtr->DriverMutex);

            /* Set the return value to success.                         */
            ret_val = 0;
         }
         else
         {
            ret_val = -EINTR;
         }
      }
      else
      {
         /* The device information was not located, so release the mutex*/
         /* and return an error to the caller.                          */
         ret_val = -ENODEV;
      }
   }
   else
   {
      /* Flag that there is an invlaid argument.                        */
      ret_val = -EINVAL;
   }
   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
   return(ret_val);
}

   /* The following function is used to supply the upper layer IP stack */
   /* a way to stop the driver.  This function will double check that   */
   /* the driver was initialized correctly.  This function will receive */
   /* a the net_device that is called the driver.  This function's will */
   /* return a 0 if the driver opened properly.                         */
static int VNET_Device_Stop(struct net_device *NetDevice)
{
   int          ret_val;
   DriverInfo_t *DriverInfoPtr;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__ );

   /* Check to make sure the values passed in look somewhat valid.      */
   if(NetDevice)
   {
      /* Search for the Device info that is associated with the         */
      /* specified device.                                              */
      DriverInfoPtr = SearchDeviceEntry(NetDevice);
      if(DriverInfoPtr)
      {
         /* Grab the Mutex that protects the Device List.               */
         if(!down_interruptible(&DriverInfoPtr->DriverMutex))
         {
            /* Flag that this device has been stopped.                  */
            DriverInfoPtr->DeviceOpen = FALSE;

            /* Free any packets that are in queue and have not been     */
            /* released yet.                                            */
            VNET_Device_Cleanup(DriverInfoPtr);

            /* Release Lock protecting the device info.                 */
            up(&DriverInfoPtr->DriverMutex);

            /* Set the return value to success.                         */
            ret_val = 0;
         }
         else
         {
            ret_val = -EINTR;
         }
      }
      else
      {
         /* The device information was not located, so release the mutex*/
         /* and return an error to the caller.                          */
         ret_val = -ENODEV;
      }
   }
   else
   {
      /* Flag that there is an invlaid argument.                        */
      ret_val = EINVAL;
   }
   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
   return(ret_val);
}

   /* The following function is used to supply the upper layer IP stack */
   /* a way to transmit packets to the driver.  This function will start*/
   /* a transit by queuing the IP packet with the driver.  The packet   */
   /* will not be officially sent until the NetDeviceConfirmPacket      */
   /* callback function is called.  This function stores the data       */
   /* packets passed in into a Queue in order to delete the memory in   */
   /* the Confirm callback function.  This function's first parameter is*/
   /* the sk_buff pointer that the IP stack uses to communicate data    */
   /* packets.  The second parameter is the network device that called  */
   /* this function.  This function's will return a 0 if the            */
   /* successfully queued the data packet.                              */
   /* * NOTE * This is an atomic operation so the Mutex will not be     */
   /*          grabbed.  We must take care when modifying and testing   */
   /*          values that may be changed outside of this function.     */
static int VNET_Device_Transmit(struct sk_buff *SocketBuffer, struct net_device *NetDevice)
{
   int            ret_val;
   unsigned long  flags;
   DriverInfo_t  *DriverInfoPtr;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__ );

   /* Check to make sure the parameters passed in appear valid.         */
   if((SocketBuffer) && (NetDevice))
   {
      /* Search for the Device info that is associated with the         */
      /* specified device.                                              */
      DriverInfoPtr = SearchDeviceEntry(NetDevice);
      if(DriverInfoPtr)
      {
         /* Check to see if we have room to save this packet.           */
         if(DriverInfoPtr->NumberQueuedPackets < VNET_MAXIMUM_NUMBER_QUEUED_PACKETS)
         {
            /* Obtain the Lock that protects the Packet Count.          */
            spin_lock_irqsave(&DriverInfoPtr->PacketCountSpinLock, flags);

            /* Save the Packet in the Transmit Queue.                   */
            DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueWriteIndex++] = SocketBuffer;

            /* Indicate that we have queued a packet.                   */
            DriverInfoPtr->NumberQueuedPackets++;

            /* Check to see if we need to look that Index.              */
            if(DriverInfoPtr->PacketQueueWriteIndex == VNET_MAXIMUM_NUMBER_QUEUED_PACKETS)
            {
               DriverInfoPtr->PacketQueueWriteIndex = 0;
            }

            /* Release the Spinlock that protects the Packet Count.     */
            spin_unlock_irqrestore(&DriverInfoPtr->PacketCountSpinLock, flags);

            /* Check to see if we need to stop the transmit queue.      */
            if(DriverInfoPtr->NumberQueuedPackets == VNET_MAXIMUM_NUMBER_QUEUED_PACKETS)
            {
               netif_stop_queue(DriverInfoPtr->NetDevice);
               DriverInfoPtr->NetQueueActive = FALSE;
            }

            DUMPDATA(SocketBuffer->len, SocketBuffer->data);

            /* Check to see if the Transport is waiting for a packet.   */
            if(DriverInfoPtr->TransportWaiting)
            {
               /* Wake up anyone who may have been waiting now that we  */
               /* don't own any semaphores.                             */
               wake_up_interruptible(&DriverInfoPtr->WaitEventWaitQueue);
            }
            /* Flag that we were successful.                            */
            ret_val = NETDEV_TX_OK;
         }
         else
         {
            ret_val = NETDEV_TX_BUSY;
         }
      }
      else
      {
         /* The device information was not located, so release the mutex*/
         /* and return an error to the caller.                          */
         ret_val = ENODEV;
      }
   }
   else
   {
      /* Flag that there is an invlaid argument.                        */
      ret_val = EINVAL;
   }
   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
   return(ret_val);
}

   /* This function is provided to allow a mechanism to try to fix this */
   /* driver when a timeout occurs or the driver is not responding      */
   /* properly.  This function will try to fix any internal problems    */
   /* with the driver when called.  This functions first parameter is   */
   /* the net_device that called this function and should be the        */
   /* net_device this driver registered.                                */
static void VNET_Device_Timeout(struct net_device *NetDevice)
{
   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
}

static int VNET_Device_Set_Config(struct net_device *NetDevice, struct ifmap *map)
{
   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   DEBUGPRINT(KERN_ERR "%s : exit\n", __FUNCTION__);
   return(0);
}

   /* This function is provided to allow a mechanism to retrieve the    */
   /* net_device_stats this driver is recording.  This function is      */
   /* defined by the net_device structure and is part of the linux      */
   /* wireless extensions.  This functions first parameter is the       */
   /* net_device that called this function.  This function returns a    */
   /* pointer to the net_device_stats that will contain information on  */
   /* this device and driver.                                           */
static struct net_device_stats *VNET_Device_Stats(struct net_device *NetDevice)
{
   struct net_device_stats *ret_val = NULL;
   DriverInfo_t            *DriverInfoPtr;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   /* Check to make sure the values passed in look somewhat valid.      */
   if(NetDevice)
   {
      DEBUGPRINT(KERN_ERR "VNET_Device_Stats Search\n");

      /* Search for the Device info that is associated with the         */
      /* specified device.                                              */
      DriverInfoPtr = SearchDeviceEntry(NetDevice);
      if(DriverInfoPtr)
      {
         /* Grab the Mutex that protects the Device List.               */
         if(!down_interruptible(&DriverInfoPtr->DriverMutex))
         {
            /* Set the return value to the Net Stats Structure.         */
            ret_val = &DriverInfoPtr->NetDeviceStats;

            /* Release Lock protecting the device info.                 */
            up(&DriverInfoPtr->DriverMutex);
         }
         else
         {
            ret_val = &dummyStats;
         }
      }
      else
      {
         DEBUGPRINT(KERN_ERR "Device not Found\n");
         /* The device information was not located, so release the mutex*/
         /* and return an error to the caller.                          */
         ret_val = &dummyStats;
      }
   }
   DEBUGPRINT(KERN_ERR "%s : exit : ret_val %p\n", __FUNCTION__, ret_val);
   return(ret_val);
}

   /* The following function is used to supply the upper layer IP stack */
   /* a way communicate management commands with the driver.  This      */
   /* function will perform wireless commands based off the wireless    */
   /* extensions version 16.  This functions first parameter This       */
   /* functions first parameter is the net_device that called this      */
   /* function.  The net_device should be the net_device that this      */
   /* driver registered.  The second parameter is the ifreq structure.  */
   /* This structure will container all the values needed to communicate*/
   /* with the wireless extensions 16.  The third parameter passed in   */
   /* will be the command that the user wishes to perform.  This        */
   /* function will return a 0 if successful or a negative value upon   */
   /* failure.                                                          */
static int VNET_Device_Ioctl(struct net_device *NetDevice, struct ifreq *ifreq, int Command)
{
   int              ret_val = -EIO;
   DriverInfo_t    *DriverInfoPtr;

   DEBUGPRINT(KERN_ERR "%s : enter %08X\n", __FUNCTION__, Command);

   /* Check to make sure the parameters passed in look valid and match  */
   /* this NetDevice.                                                   */
   if((NetDevice) && (ifreq))
   {
      /* Search for the Device info that is associated with the         */
      /* specified device.                                              */
      DriverInfoPtr = SearchDeviceEntry(NetDevice);
      if(DriverInfoPtr)
      {
         DEBUGPRINT(KERN_ERR "Command  %08X\n", Command);

         /* Grab the Mutex that protects the Device List.               */
         if(!down_interruptible(&DriverInfoPtr->DriverMutex))
         {
            /* Process the specified command.                           */
            switch(Command)
            {
               default:
                  ret_val = -EOPNOTSUPP;
                  break;
            }
            /* Release Lock protecting the device info.                 */
            up(&DriverInfoPtr->DriverMutex);
         }
         else
         {
            ret_val = -EINTR;
         }
      }
      else
      {
         /* The device information was not located, so release the mutex*/
         /* and return an error to the caller.                          */
         ret_val = -ENODEV;
      }
   }
   else
   {
      /* Flag that there is an invlaid argument.                        */
      ret_val = -EINVAL;
   }
   DEBUGPRINT(KERN_ERR "%s : exit %d\n", __FUNCTION__, ret_val);
   return(ret_val);
}

   /* The following function is responsible for opening the transport   */
   /* side of the Virtual Serial Port Driver.  The first parameter to   */
   /* this function is a pointer to the inode structure that is         */
   /* associated with this character device file. The second parameter  */
   /* to this function is a pointer to the file structure associated    */
   /* with this driver.  This function returns zero upon successful     */
   /* execution or a negative error code as defined in errno.h upon all */
   /* errors.                                                           */
static int VNET_Transport_Open(struct inode *INode, struct file *FilePointer)
{
   int           ret_val;
   DriverInfo_t *DriverInfoPtr;

   DEBUGPRINT(KERN_ERR "%s : enter, Minor = %u\n", __FUNCTION__, MINOR(INode->i_rdev));

   /* Bump the reference count                                          */
   try_module_get(THIS_MODULE);

   /* Before proceeding check the parameters that were passed in to make*/
   /* sure that they appear to be at least semi-valid.                  */
   if((INode) && (FilePointer))
   {
      /* The parameters passed in appear to be at least semi-valid.  Now*/
      /* note the driver information structure associated with this     */
      /* Minor Device Number.                                           */
      DriverInfoPtr = &DriverInfoList[MINOR(INode->i_rdev)];

      /* Next attempt to acquire the semaphore use to protect this      */
      /* Driver Information Entry.                                      */
      if((DriverInfoPtr) && (!down_interruptible(&DriverInfoPtr->DriverMutex)))
      {
         /* Free any packets that are in queue and have not beed        */
         /* released yet.                                               */
         VNET_Device_Cleanup(DriverInfoPtr);

         /* Everything is ok so far, now initialize the remaining items */
         /* pertaining to the Transport Side of the Driver in the Driver*/
         /* Information Entry.                                          */
         DriverInfoPtr->TransportOpen      = TRUE;
         DriverInfoPtr->TransportWaiting   = FALSE;
         DriverInfoPtr->ShutDown           = FALSE;
         DriverInfoPtr->BluetoothConnected = FALSE;

         /* Finally set the private data in the File Information        */
         /* Structure to point to this entry for use with the rest of   */
         /* the functions associated with this driver.                  */
         FilePointer->private_data = (void *)DriverInfoPtr;

         /* Initialize the return value to indicate success.            */
         ret_val = 0;

         /* Release the semaphore used to protect the Driver Information*/
         /* Entry.                                                      */
         up(&DriverInfoPtr->DriverMutex);
      }
      else
         ret_val = -ENODEV;
   }
   else
      ret_val = -EINVAL;

   /* Check to see if an error occurred, if so decrement the Module Use */
   /* Count.                                                            */
   if(ret_val)
   {
      /* Decrement the use count of the module                          */
      module_put(THIS_MODULE);
   }

   DEBUGPRINT(KERN_ERR "%s : exit, ret_val = %d\n", __FUNCTION__, ret_val);

   return(ret_val);
}

   /* The following function is responsible for closing the transport   */
   /* side of the Virtual Serial Port Driver.  Note that the memory     */
   /* associated with this device is only freed if both sides of the    */
   /* driver are closed. The first parameter to this function is a      */
   /* pointer to the inode structure that is associated with this       */
   /* character device.  The second parameter to this function is a     */
   /* pointer to the file structure associated with this this device.   */
   /* This function returns zero upon successful execution or a negative*/
   /* error code as defined in errno.h upon all errors.                 */
static int VNET_Transport_Close(struct inode *INode, struct file *FilePointer)
{
   int                ret_val;
   DriverInfo_t      *DriverInfoPtr;
   wait_queue_head_t *WaitQueuePtr;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   /* Before proceeding check the parameters that were passed in to make*/
   /* sure that they appear to be at least semi-valid.                  */
   if((INode) && (FilePointer))
   {
      /* The parameters passed in appear to be at least semi-valid.  Now*/
      /* note the driver information structure associated with this     */
      /* Minor Device Number.                                           */
      DriverInfoPtr = &DriverInfoList[MINOR(INode->i_rdev)];

      /* Next attempt to acquire the semaphore use to protect this      */
      /* Driver Information Entry.                                      */
      if((DriverInfoPtr) && (!down_interruptible(&DriverInfoPtr->DriverMutex)))
      {
         /* The semaphore protecting this entry was acquired            */
         /* successfully, change the state of the Transport side of the */
         /* Driver.                                                     */
         DriverInfoPtr->TransportOpen = FALSE;
         DriverInfoPtr->ShutDown      = TRUE;

         /* Signal that the Cable has been unplugged.                   */
         DriverInfoPtr->BluetoothConnected = FALSE;
         netif_carrier_off(DriverInfoPtr->NetDevice);

         /* Finally clear the private data in the File Information      */
         /* Structure.                                                  */
         FilePointer->private_data = NULL;

         /* Get a pointer to the WaitQueue before we release the        */
         /* protection.                                                 */
         WaitQueuePtr = &(DriverInfoPtr->WaitEventWaitQueue);

         /* Release the semaphore used to protect the Driver Information*/
         /* Entry.                                                      */
         up(&DriverInfoPtr->DriverMutex);

         /* Wake up anyone who may have been waiting now that we don't  */
         /* own any semaphores.                                         */
         wake_up_interruptible(WaitQueuePtr);

         /* Set the return value to indicate success.                   */
         ret_val = 0;
      }
      else
         ret_val = -EINTR;
   }
   else
      ret_val = -EINVAL;

   /* Decrement the Module Use Count.                                   */
   if(!ret_val)
   {
      /* Decrement the use count of the module                          */
      module_put(THIS_MODULE);
   }

   DEBUGPRINT(KERN_ERR "%s : exit, ret_val = %d\n", __FUNCTION__, ret_val);

   return(ret_val);
}

   /* The following function is responsible for reading data from the   */
   /* input buffer associated with this Virtual Network Driver.  Packets*/
   /* are removed from the Packet queue and delivered to the caller.    */
   /* The first parameter to this function is a pointer to the file     */
   /* structure associated with this device.  The second parameter to   */
   /* this function is a pointer to the buffer in which to read the data*/
   /* into.  The third parameter is the length of the buffer which the  */
   /* previous parameter points to.  The final parameter is the offset  */
   /* within the file.  This function returns the number of bytes which */
   /* were successfully read or a negative error code as defined in     */
   /* errno.h upon all errors.                                          */
static ssize_t VNET_Transport_Read(struct file *FilePointer, char *Buffer, size_t Length, loff_t *LongOffset)
{
   int             ret_val;
   DriverInfo_t   *DriverInfoPtr;
   struct sk_buff *SocketBuffer;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   /* Before proceeding check the parameters that were passed in to make*/
   /* sure that they appear to be at least semi-valid.                  */
   if((FilePointer) && (Buffer))
   {
      /* The parameters passed in appear to be at least semi-valid.  Now*/
      /* make sure that this is a request to read at least some data.   */
      if(Length)
      {
         /* This is a request to read at least some data, retrieve the  */
         /* Driver Information for the FilePointer data member.         */
         DriverInfoPtr = (DriverInfo_t *)FilePointer->private_data;
         if(DriverInfoPtr)
         {
            /* Next attempt to acquire the semaphore use to protect this*/
            /* Driver Information Entry.                                */
            if(!down_interruptible(&DriverInfoPtr->DriverMutex))
            {
               /* Check to see if there are any buffer of data that can */
               /* be transferred at this time.                          */
               if(DriverInfoPtr->NumberQueuedPackets)
               {
                  /* Get the next Socket Buffer from the list and update*/
                  /* the index and count.                               */
                  SocketBuffer = DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueReadIndex];
                  if((SocketBuffer) && (SocketBuffer->len <= Length))
                  {
                     /* Copy the data up to the end of the Buffer.      */
                     if(!copy_to_user(Buffer, SocketBuffer->data, SocketBuffer->len))
                     {
                        DEBUGPRINT(KERN_ERR "&(Buffer of %d bytes read\n", SocketBuffer->len);
                        ret_val = SocketBuffer->len;
                        VNET_Device_ConfirmPacket(DriverInfoPtr);
                     }
                     else
                        ret_val = -EFAULT;
                  }
                  else
                     ret_val = -ENOMEM;
               }
               else
                  ret_val = 0;

               /* Release the semaphore used to protect the Driver      */
               /* Information Entry.                                    */
               up(&DriverInfoPtr->DriverMutex);
            }
            else
               ret_val = -EINTR;
         }
         else
            ret_val = -EINTR;
      }
      else
         ret_val = 0;
   }
   else
      ret_val = -EINVAL;

   DEBUGPRINT(KERN_ERR "%s : exit, ret_val = %d\n", __FUNCTION__, ret_val);

   return(ret_val);
}

   /* The following function is responsible for writing data to the IP  */
   /* Stack which exists within the Transport device associated with    */
   /* this Virtual Network Driver.  The first parameter to this function*/
   /* is a pointer to the file structure associated with this device.   */
   /* The second parameter is a pointer to a buffer of characters which */
   /* is to be written.  The third parameter is the number of characters*/
   /* within the buffer pointed to by the previous parameter.  The final*/
   /* parameter is the offset within the file.  This function returns   */
   /* the number of bytes which were successfully written or a negative */
   /* error code as defined in errno.h upon all errors.                 */
   /* * NOTE * This driver expects to receieve full IP Packets.  The    */
   /*          LongOffset parameter is ignored.                         */
static ssize_t VNET_Transport_Write(struct file *FilePointer, const char *Buffer, size_t Length, loff_t *LongOffset)
{
   int             ret_val;
   DriverInfo_t   *DriverInfoPtr;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   /* Before proceeding check the parameters that were passed in to make*/
   /* sure that they appear to be at least semi-valid.                  */
   if((FilePointer) && (Buffer))
   {
      DEBUGPRINT(KERN_ERR "Buffer %p Length %d\n", Buffer, Length);
      /* The parameters passed in appear to be at least semi-valid.  Now*/
      /* make sure that there is at least some data to be written.      */
      if(Length)
      {
         /* There is a packet of data to be writted, retrieve the Driver*/
         /* Information for the FilePointer data member.                */
         DriverInfoPtr = (DriverInfo_t *)FilePointer->private_data;
         if(DriverInfoPtr)
         {
            DEBUGPRINT(KERN_ERR "Got Driver Pointer Get Mutex\n");
            /* Next attempt to acquire the semaphore use to protect this*/
            /* Driver Information Entry.                                */
            if(!down_interruptible(&DriverInfoPtr->DriverMutex))
            {
               DEBUGPRINT(KERN_ERR "Got Mutex  Open %d\n", DriverInfoPtr->DeviceOpen);
               /* Check to see if the Device side of the driver is open.*/
               if(DriverInfoPtr->DeviceOpen)
               {
                  /* Process the recieved data.                         */
                  ret_val = VNET_Device_ReceivePacket(DriverInfoPtr, (char __user *)Buffer, Length);

                  /* If the packet was received successfully, report    */
                  /* that all the data was written.                     */
                  if(ret_val == 0)
                     ret_val = Length;
               }
               else
               {
                  /* The Device side of the driver is not open so return*/
                  /* that all of the data was written but do not write  */
                  /* any data.                                          */
                  ret_val = Length;
               }

               /* Release the semaphore used to protect the Driver      */
               /* Information Entry.                                    */
               up(&DriverInfoPtr->DriverMutex);
            }
            else
               ret_val = -EINTR;
         }
         else
            ret_val = -EINVAL;
      }
      else
         ret_val = 0;
   }
   else
      ret_val = -EINVAL;

   DEBUGPRINT(KERN_ERR "%s : exit, ret_val = %d\n", __FUNCTION__, ret_val);

   return(ret_val);
}


   /* The following function is responsible for processing the IOCTL    */
   /* requests for the Transport side of the Driver.  The first         */
   /* parameter to this function is a pointer to the inode structure    */
   /* that is associated with this character device.  The second        */
   /* parameter to this function is a pointer to the file structure     */
   /* associated with this device.  The third parameter to this function*/
   /* is the Command to be processed and the final parameter is the     */
   /* Parameter that maybe be associated with this command.  Note that  */
   /* the final parameter need not be valid because not all commands    */
   /* specified or require the parameter to be used.  This function     */
   /* returns zero or a positive value upon success and a negative error*/
   /* code as defined in errno.h upon all errors.                       */
#if HAVE_UNLOCKED_IOCTL
static long VNET_Transport_Ioctl(struct file *FilePointer, unsigned int Command, unsigned long Parameter)
#else
static int VNET_Transport_Ioctl(struct inode *INode, struct file *FilePointer, unsigned int Command, unsigned long Parameter)
#endif
{
   int             ret_val = -EINVAL;
   int             ConnectState;
   DriverInfo_t   *DriverInfoPtr;
   struct sk_buff *SocketBuffer;

   DEBUGPRINT(KERN_ERR "%s : enter\n", __FUNCTION__);

   /* Before proceeding check the parameters that were passed in to make*/
   /* sure that they appear to be at least semi-valid.                  */
   if((FilePointer) && (Parameter))
   {
      DEBUGPRINT(KERN_ERR "Command: %08X %08X\n", Command, (unsigned int)Parameter);
      /* Now determine if the IOCTL command that was specified is a     */
      /* command that is supported by this character device.            */
      switch(Command)
      {
         case VNET_IOCTL_QUERY_ETHERNET_ADDRESS:
         case VNET_IOCTL_SET_ETHERNET_ADDRESS:
         case VNET_IOCTL_SET_ETHERNET_CONNECTED_STATUS:
         case VNET_IOCTL_WAIT_RECEIVED_DATA:
            /* The IOCTL specified is supported by this device now      */
            /* retrieve the Driver Information for the driver via the   */
            /* FilePointer data member.                                 */
            DriverInfoPtr = (DriverInfo_t *)FilePointer->private_data;
            DEBUGPRINT(KERN_ERR "DriverInfoPtr: %p\n", DriverInfoPtr);

            /* Next attempt to acquire the semaphore use to protect this*/
            /* Driver Information Entry.                                */
            if((DriverInfoPtr) && (!down_interruptible(&DriverInfoPtr->DriverMutex)))
            {
               /* Now determine what command this is so the appropriate */
               /* operation can be done.                                */
               switch(Command)
               {
                  case VNET_IOCTL_QUERY_ETHERNET_ADDRESS:
                     DEBUGPRINT(KERN_ERR "Command =  VNET_IOCTL_QUERY_ETHERNET_ADDRESS.\n");
                     if(!copy_to_user((void *)Parameter, DriverInfoPtr->MACAddress, VNET_MAC_ADDRESS_SIZE))
                     {
                        ret_val = 0;
                     }
                     else
                     {
                        ret_val = -EFAULT;
                     }
                     /* Release the semaphore used to protect the Driver*/
                     /* Information Entry.                              */
                     up(&DriverInfoPtr->DriverMutex);
                     break;
                  case VNET_IOCTL_SET_ETHERNET_ADDRESS:
                     DEBUGPRINT(KERN_ERR "Command =  VNET_IOCTL_SET_ETHERNET_ADDRESS.\n");
                     if(!copy_from_user(DriverInfoPtr->MACAddress, (void *)Parameter, VNET_MAC_ADDRESS_SIZE))
                     {
                        ret_val = 0;
                     }
                     else
                     {
                        ret_val = -EFAULT;
                     }

                     /* Release the semaphore used to protect the Driver*/
                     /* Information Entry.                              */
                     up(&DriverInfoPtr->DriverMutex);
                     break;
                  case VNET_IOCTL_SET_ETHERNET_CONNECTED_STATUS:
                     DEBUGPRINT(KERN_ERR "Command =  VNET_IOCTL_SET_ETHERNET_CONNECTED_STATUS.\n");

                     if(!copy_from_user(&ConnectState, (void *)Parameter, sizeof(int)))
                     {
                        DEBUGPRINT(KERN_ERR "Connect State = %d.\n", ConnectState);

                        /* Check to see if we need to signal that cable */
                        /* is plugged in.                               */
                        if((ConnectState) && (!DriverInfoPtr->BluetoothConnected))
                        {
                           DEBUGPRINT(KERN_ERR "Signal Connected\n");
                           netif_carrier_on(DriverInfoPtr->NetDevice);
                           DriverInfoPtr->BluetoothConnected = TRUE;
                        }
                        else
                        {
                           /* Check to see if we need to signal that the*/
                           /* cable is unplugged.                       */
                           if((!ConnectState) && (DriverInfoPtr->BluetoothConnected))
                           {
                              DEBUGPRINT(KERN_ERR "Signal Unplugged\n");
                              netif_carrier_off(DriverInfoPtr->NetDevice);
                              DriverInfoPtr->BluetoothConnected = FALSE;
                           }
                        }
                        ret_val = 0;
                     }
                     else
                     {
                        ret_val = -EFAULT;
                     }
                     /* Release the semaphore used to protect the Driver*/
                     /* Information Entry.                              */
                     up(&DriverInfoPtr->DriverMutex);
                     break;
                  case VNET_IOCTL_WAIT_RECEIVED_DATA:
                     DEBUGPRINT(KERN_ERR "Command =  VSER_IOCTL_WAIT_RECEIVED_DATA. %d\n", DriverInfoPtr->NumberQueuedPackets);
                     /* Check to see if there are any packets in the    */
                     /* queue to be read.                               */
                     if(DriverInfoPtr->NumberQueuedPackets)
                     {
                        /* There is a packet in the queue so retreive   */
                        /* the length of the packet and return this     */
                        /* information.                                 */
                        SocketBuffer = DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueReadIndex];
                        if(SocketBuffer)
                        {
                           DEBUGPRINT(KERN_ERR "Copy to User\n");
                           /* Set the return value to the size of the   */
                           /* packet.                                   */
                           if(!copy_to_user((void *)Parameter, &(SocketBuffer->len), sizeof(int)))
                           {
                              ret_val = 0;
                           }
                           else
                           {
                              ret_val = -EFAULT;
                           }
                        }
                        else
                        {
                           DEBUGPRINT(KERN_ERR "No Packets!\n");
                           ret_val = -EIO;
                        }

                        /* Release the semaphore used to protect the    */
                        /* Driver Information Entry.                    */
                        up(&DriverInfoPtr->DriverMutex);
                     }
                     else
                     {
                        /* Flag thaty we are waiting on an Event.       */
                        DriverInfoPtr->TransportWaiting = TRUE;

                        /* Release the semaphore used to protect the    */
                        /* Driver Information Entry.                    */
                        up(&DriverInfoPtr->DriverMutex);

                        DEBUGPRINT(KERN_ERR "Prepair to Wait %d %d\n", DriverInfoPtr->NumberQueuedPackets, DriverInfoPtr->ShutDown);
                        /* Check to see if one of the specified events  */
                        /* has occurred.                                */
                        if(!(wait_event_interruptible(DriverInfoPtr->WaitEventWaitQueue, ((DriverInfoPtr->NumberQueuedPackets) || (DriverInfoPtr->ShutDown)))))
                        {
                           DEBUGPRINT(KERN_ERR "Wait Complete\n");
                           /* The event has occurred return the event   */
                           /* that has occurred to the user in the      */
                           /* argument parameter.                       */
                           SocketBuffer = DriverInfoPtr->PacketQueueArray[DriverInfoPtr->PacketQueueReadIndex];
                           if(SocketBuffer)
                           {
                              DEBUGPRINT(KERN_ERR "Copy to User\n");
                              /* Set the return value to the size of the*/
                              /* packet.                                */
                              if(!copy_to_user((void *)Parameter, &(SocketBuffer->len), sizeof(int)))
                              {
                                 ret_val = 0;
                              }
                              else
                              {
                                 ret_val = -EFAULT;
                              }
                           }
                           else
                           {
                              DEBUGPRINT(KERN_ERR "No Packets!\n");
                              ret_val = -EIO;
                           }
                        }
                        else
                        {
                           DEBUGPRINT(KERN_ERR "Wait Interruption!\n");
                           ret_val = -EINTR;
                        }

                        /* Denote that we are no longer waiting.        */
                        DriverInfoPtr->TransportWaiting = FALSE;
                     }
                     break;

                     break;
                  default:
                     /* Release the semaphore used to protect the Driver*/
                     /* Information Entry.                              */
                     up(&DriverInfoPtr->DriverMutex);
                     ret_val = -ENOTTY;

                     break;
               }
            }
            else
               ret_val = -EINTR;
            break;
         default:
            ret_val = -ENODEV;
            break;
      }
   }
   else
      ret_val = -EINVAL;

   DEBUGPRINT(KERN_ERR "%s : exit, ret_val = %d\n", __FUNCTION__, ret_val);

   return(ret_val);
}

   /* These MACRO's are used to change the name of the init_module and  */
   /* cleanup_module function.                                          */
module_init(InitializeVNETModule);
module_exit(ExitVNETModule);

