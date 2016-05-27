#!/system/bin/sh

echo "I am being executed: $0 $1 $2"

case "$1" in
	create)
		# add default gateway - on a real device,
		# this should be handled according to the product,
		# and not by BTIPS PAN
		#route add default gw 192.168.1.1 dev eth0
		# add bridge
		brctl addbr pan0
		# no forwarding delay please
		brctl setfd pan0 0
		# setup bridge and enable
		ifconfig pan0 10.0.0.1 netmask 255.0.0.0 up
		# enable IPv4 forwarding
		echo 1 > /proc/sys/net/ipv4/ip_forward
		# enable NAT
		iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
		# start DHCP server
		setprop ctl.start dhcpd_nap
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
		setprop ctl.stop dhcpd_nap
		# disable NAT
		iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE
		# disable IPv4 forwarding
		echo 0 > /proc/sys/net/ipv4/ip_forward
		# disable interfaces and bridge
		ifconfig pan0 down
		# delete bridge
		brctl delbr pan0
		;;
esac

exit 0

