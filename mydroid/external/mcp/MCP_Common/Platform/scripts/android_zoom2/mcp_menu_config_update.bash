#!/bin/bash

# $1 - the string to be printed
function print_highlight {
echo -e "\033[7m$1\033[0m"
} 


# $1 - string to be replaced
# $1 - new string 
function replace_string { 
#echo "Replacing <$1> to --> <$2>"
sed s/"$1"/"$2"/ .config_after_change > .temp
cp .temp .config_after_change
} 


# $1 - string to search
# $2 - string to insert after the searched one
function insert_string_after_string { 
echo "Insert <$1> after --> <$2>"
sed /"$1"/"a$2" .config_after_change > .temp
cp .temp .config_after_change
} 

# $1 - string to search
# $2 - string to insert after the searched one
function insert_string_before_string { 
echo "Insert $1 before --> $2"
sed /"$1"/"i$2" .config_after_change > .temp
cp .temp .config_after_change
} 


function remove_line { 
echo "Removing line contains <$1>"
sed "/$1/d" .config_after_change > .temp
cp .temp .config_after_change
} 


function set_key { 
echo "Setting key <$1>"
replace_string "# $1 is not set" "$1=y"
} 

function set_key_to_module_type { 
echo "Setting key to module type <$1>"
replace_string "# $1 is not set" "$1=m"
} 

function unset_key { 
echo "Unsetting key <$1>"
replace_string "$1=y" "# $1 is not set"
} 

rm .config_after_change
cp .config .config_after_change


print_highlight "Configuring HID keys in .config file"
unset_key 'CONFIG_BT_L2CAP'
unset_key 'CONFIG_BT_SCO'
remove_line 'CONFIG_BT_RFCOMM=y'
remove_line 'CONFIG_BT_RFCOMM_TTY=y'
remove_line 'CONFIG_BT_BNEP=y'
remove_line '# CONFIG_BT_BNEP_MC_FILTER is not set'
remove_line '# CONFIG_BT_BNEP_PROTO_FILTER is not set'
set_key 'CONFIG_BT_TIHIDP'
remove_line 'CONFIG_BT_HIDP=y'


print_highlight "Configuring PAN keys in .config file"
print_highlight "Configuring TUN/TAP driver keys.."
set_key 'CONFIG_TUN'


print_highlight "Configuring Bridge driver keys.."
insert_string_after_string 'CONFIG_NETFILTER_ADVANCED=y' 'CONFIG_BRIDGE_NETFILTER=y'
insert_string_after_string '# CONFIG_NETFILTER_XT_MATCH_POLICY is not set' '# CONFIG_NETFILTER_XT_MATCH_PHYSDEV is not set'
insert_string_after_string 'CONFIG_IP_NF_ARP_MANGLE=y' '# CONFIG_BRIDGE_NF_EBTABLES is not set'
insert_string_after_string '# CONFIG_ATM is not set' 'CONFIG_STP=y'
set_key 'CONFIG_BRIDGE'
insert_string_after_string '# CONFIG_DECNET is not set' 'CONFIG_LLC=y'

print_highlight "Configuring Netfilter keys.."
insert_string_before_string '# CONFIG_NETFILTER_NETLINK_QUEUE is not set' 'CONFIG_NETFILTER_NETLINK=y'
set_key 'CONFIG_NETFILTER_NETLINK_LOG'
set_key 'CONFIG_NF_CT_ACCT'
set_key 'CONFIG_NF_CONNTRACK_MARK'
set_key 'CONFIG_NF_CONNTRACK_EVENTS'
unset_key 'CONFIG_NF_CONNTRACK_FTP'
unset_key 'CONFIG_NF_CONNTRACK_H323'
unset_key 'CONFIG_NF_CONNTRACK_IRC'
unset_key 'CONFIG_NF_CONNTRACK_SIP'
unset_key 'CONFIG_NF_CONNTRACK_TFTP'
remove_line '# CONFIG_NETFILTER_TPROXY is not set'
set_key 'CONFIG_NETFILTER_XT_TARGET_CONNMARK'
remove_line '# CONFIG_NETFILTER_XT_TARGET_DSCP is not set'
set_key 'CONFIG_NETFILTER_XT_TARGET_MARK'
set_key 'CONFIG_NETFILTER_XT_TARGET_NFLOG'
set_key 'CONFIG_NETFILTER_XT_TARGET_NFQUEUE'
remove_line '# CONFIG_NETFILTER_XT_TARGET_NOTRACK is not set'
set_key 'CONFIG_NETFILTER_XT_TARGET_RATEEST'
remove_line '# CONFIG_NETFILTER_XT_TARGET_TRACE is not set'
set_key 'CONFIG_NETFILTER_XT_TARGET_TCPMSS'
remove_line '# CONFIG_NETFILTER_XT_TARGET_TCPOPTSTRIP is not set'
set_key 'CONFIG_NETFILTER_XT_MATCH_COMMENT'
set_key 'CONFIG_NETFILTER_XT_MATCH_CONNBYTES'
set_key 'CONFIG_NETFILTER_XT_MATCH_CONNLIMIT'
set_key 'CONFIG_NETFILTER_XT_MATCH_CONNMARK'
set_key 'CONFIG_NETFILTER_XT_MATCH_CONNTRACK'
set_key 'CONFIG_NETFILTER_XT_MATCH_DCCP'
set_key 'CONFIG_NETFILTER_XT_MATCH_DSCP'
set_key 'CONFIG_NETFILTER_XT_MATCH_ESP'
set_key 'CONFIG_NETFILTER_XT_MATCH_HASHLIMIT'
set_key 'CONFIG_NETFILTER_XT_MATCH_HELPER'
set_key 'CONFIG_NETFILTER_XT_MATCH_LENGTH'
set_key 'CONFIG_NETFILTER_XT_MATCH_LIMIT'
set_key 'CONFIG_NETFILTER_XT_MATCH_MARK'
set_key 'CONFIG_NETFILTER_XT_MATCH_OWNER'
set_key 'CONFIG_NETFILTER_XT_MATCH_POLICY'
set_key 'CONFIG_NETFILTER_XT_MATCH_PHYSDEV'
set_key 'CONFIG_NETFILTER_XT_MATCH_PKTTYPE'
set_key 'CONFIG_NETFILTER_XT_MATCH_QUOTA'
set_key 'CONFIG_NETFILTER_XT_MATCH_RATEEST'
set_key 'CONFIG_NETFILTER_XT_MATCH_REALM'
set_key 'CONFIG_NETFILTER_XT_MATCH_RECENT'
insert_string_after_string 'CONFIG_NETFILTER_XT_MATCH_RECENT=y' '# CONFIG_NETFILTER_XT_MATCH_RECENT_PROC_COMPAT is not set'
set_key 'CONFIG_NETFILTER_XT_MATCH_SCTP'
set_key 'CONFIG_NETFILTER_XT_MATCH_STATE'
set_key 'CONFIG_NETFILTER_XT_MATCH_STATISTIC'
set_key 'CONFIG_NETFILTER_XT_MATCH_STRING'
set_key 'CONFIG_NETFILTER_XT_MATCH_TCPMSS'
set_key 'CONFIG_NETFILTER_XT_MATCH_TIME'
set_key 'CONFIG_NETFILTER_XT_MATCH_U32'
set_key 'CONFIG_IP_NF_MATCH_ADDRTYPE'
set_key 'CONFIG_IP_NF_MATCH_AH'
set_key 'CONFIG_IP_NF_MATCH_ECN'
set_key 'CONFIG_IP_NF_MATCH_TTL'
set_key 'CONFIG_IP_NF_TARGET_REJECT'
set_key 'CONFIG_IP_NF_TARGET_LOG'
set_key 'CONFIG_IP_NF_TARGET_ULOG'
set_key 'CONFIG_IP_NF_TARGET_NETMAP'

unset_key 'CONFIG_NF_NAT_FTP'
unset_key 'CONFIG_NF_NAT_IRC'
unset_key 'CONFIG_NF_NAT_TFTP'
unset_key 'CONFIG_NF_NAT_H323'

unset_key 'CONFIG_NF_NAT_SIP'
unset_key 'CONFIG_IP_NF_MANGLE'
unset_key 'CONFIG_IP_NF_RAW'

insert_string_before_string "# CONFIG_DCB is not set" "CONFIG_NET_CLS_ROUTE=y"


remove_line '# CONFIG_IP_NF_TARGET_CLUSTERIP is not set'
remove_line '# CONFIG_IP_NF_TARGET_ECN is not set'
remove_line '# CONFIG_IP_NF_TARGET_TTL is not set'
unset_key 'CONFIG_IP_NF_ARPTABLES'
remove_line 'CONFIG_IP_NF_ARPTABLE=y'
remove_line 'CONFIG_IP_NF_ARPFILTER=y'
remove_line 'CONFIG_IP_NF_ARP_MANGLE=y'

insert_string_after_string 'CONFIG_ZLIB_DEFLATE=y' 'CONFIG_TEXTSEARCH=y'
insert_string_after_string 'CONFIG_TEXTSEARCH=y' 'CONFIG_TEXTSEARCH_KMP=y'
insert_string_after_string 'CONFIG_TEXTSEARCH_KMP=y' 'CONFIG_TEXTSEARCH_BM=y'
insert_string_after_string 'CONFIG_TEXTSEARCH_BM=y' 'CONFIG_TEXTSEARCH_FSM=y'


print_highlight "Configuring Shared Transport keys:"
set_key_to_module_type 'CONFIG_TI_ST_GPS'


cp .config_after_change .config

print_highlight "Updating .config file finished."
