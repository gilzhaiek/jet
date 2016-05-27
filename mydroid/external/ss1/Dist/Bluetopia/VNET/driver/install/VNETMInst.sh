#!/bin/sh
transport="SS1VNET"
module="SS1VNETM"
ld_module=$module.ko
numberofdevices=0
action="start"

# Check that the SS1 Virtual Network Module exists and is installed in
# the right location.
[ -f ./$ld_module ] || exit 0

# Make sure that the source function library has been initialized
# and is included.
#. /etc/init.d/functions

# Root or die
if [ "$(id -u)" != "0" ]
then
  echo "You must be root to load or unload kernel modules"
  exit 1
fi

   # This function is responsible for installing the SS1 Virtual
   # Network Module into the system.
start()
{
   local RETVAL=0
   local MINORCOUNT=0
   action="started"

   echo -n "Installing SS1 Virtual Network Driver: "

   # Check to see if a number of devices was specified.
   if [ $numberofdevices -eq 0 ]; then
      # Invoke insmod with the default for the default number of devices (default = 1).
      numberofdevices=1
   fi

   # Invoke insmod with the number of devices that were specified.
   /sbin/insmod $ld_module NumberOfDevices=$numberofdevices > /dev/null 2>&1
   RETVAL=$?

   if [ $RETVAL -eq 0 ]; then
      major=`cat /proc/devices | awk '$2=='"\"$transport\""' {print $1}'`
      RETVAL=$?

      # Insert new devices.
      while [ $numberofdevices -ne 0 ] && [ $RETVAL -eq 0 ]
      do
         # The 2.6 kernel does not create the device nodes for the transport
         # driver automatically.  Normally, a userspace daemon like udev
         # creates them but create them now if they are not already present.
         if [ ! -e /dev/${transport}${MINORCOUNT} ]
         then
            echo "Manually creating node: /dev/${transport}${MINORCOUNT}"
            mknod /dev/${transport}${MINORCOUNT} c $major ${MINORCOUNT} > /dev/null 2>&1
         fi
         
         # Set the correct permissions for all users.
         chmod ugo+rw /dev/${transport}${MINORCOUNT} > /dev/null 2>&1
         numberofdevices=$(($numberofdevices-1))
         MINORCOUNT=$(($MINORCOUNT+1))
         RETVAL=$?
      done

      if [ $RETVAL -eq 0 ]; then
         echo_success
      else
         echo_failure
      fi
   else
      echo failure
   fi

   echo ""

   return $RETVAL
}

   # This function is responsible for removing the SS1 Virtual Network
   # Module from the system.
stop()
{
   action="stopped"
   local RETVAL=0

   echo -n "Removing SS1 Virtual Network Driver: "

   # invoke rmmod with all arguments we got
   /sbin/rmmod $module > /dev/null 2>&1
   RETVAL=$?

   if [ $RETVAL -eq 0 ]; then
      # Remove all stale nodes
      rm -f /dev/${transport}*
      RETVAL=$?

      if [ $RETVAL -eq 0 ]; then
         echo_success
      else
         echo_failure
     fi
   else
      echo_failure
   fi

   echo ""

   return $RETVAL
}

   # This function is for echoing success message.
echo_success()
{
   printf "Driver %s and devices entries refreshed." ${action}
}

   # This function is for echoing success message.
echo_failure()
{
   echo "Driver action failed."
}

   # This function is for restarting the SS1 Virtual Network Module.
restart()
{
   stop
   start
}

   # Check to see if a number of devices was specified.
if [ $# -gt 1 ]
then
   numberofdevices=$2
fi

case "$1" in
  start)
   start
   ;;
  stop)
   stop
   ;;
  restart)
   restart
   ;;
  *)
   echo $"Usage: $0 {start|stop|restart} [number of devices]"
   exit 1
esac

exit $?
