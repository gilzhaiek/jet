a_list=`git branch | grep -v master| awk 'BEGIN{a=1}{a++; print $1" "$1" off"}'`
temp_file=`(tempfile)`
dialog --checklist "Choose branch" 20 55 12 $a_list 2>  $temp_file
if [ $? -ne "0" ] 
    then
    exit
fi
cat $temp_file
for i in `cat $temp_file`; do echo $i; done | sed s/\"//g | bash delete_branches_if_merged.sh | dialog --progressbox "Please wait..." 20 55
