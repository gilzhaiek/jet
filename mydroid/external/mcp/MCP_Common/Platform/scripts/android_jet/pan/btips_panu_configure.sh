#!/system/bin/sh

echo "I am being executed: $0 $1 $2"
case "$1" in
	create)
		# nothing to do
		;;

	connect)
		# enable interface
		ifconfig $2 up
		# start DHCP and IPv4LL client
		setprop ctl.start dhcpc_panu
		;;

	disconnect)
		# stop DHCP and IPv4LL client
		setprop ctl.stop dhcpc_panu
		# disable interface
		ifconfig $2 down
		;;

	destroy)
		# nothing to do
		;;
esac

exit 0

