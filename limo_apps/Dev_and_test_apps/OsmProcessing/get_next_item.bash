# Used by clients Is only here as reference
PWD=`pwd`;
client=`hostname`":$PWD";
result=`ssh tiler@10.10.75.82 'cd tiling; bash next_item.bash to_do.txt "'$client'" is_doing'`
echo $result | grep $client | awk '{$1="";$2="";$3=""; print $0}' > NextItem.txt
echo $result | grep $client | awk '{print $2}' > task_hash.txt
