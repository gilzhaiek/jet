#!/system/bin/sh

echo "I am being executed: $0 $1 $2"
case "$1" in
	create)
		# add bridge
		brctl addbr pan0
		# no forwarding delay please
		brctl setfd pan0 0
		# setup bridge and enable
		ifconfig pan0 10.0.0.1 netmask 255.0.0.0 up
		# start DHCP server
		setprop ctl.start dhcpd_gn
		;;

	connect)
		echo "connecting interface $2"
		# enable btips interface
		ifconfig $2 0.0.0.0 up
		# add btips interface to the bridge
		brctl addif pan0 $2
		;;

	disconnect)
		echo "disconnecting interface $2"
		# del btips interface from the bridge
		brctl delif pan0 $2
		# disable btips interface
		ifconfig $2 down
		;;

	destroy)
		# stop DHCP server
		setprop ctl.stop dhcpd_gn
		# disable interfaces and bridge
		ifconfig pan0 down
		# delete bridge
		brctl delbr pan0
		;;
esac

exit 0

