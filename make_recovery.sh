MY_PATH=`pwd`
COMMAND_FILE_NAME=command
LOG_FILE=make_recovery.log

COMMAND_FILE=${MY_PATH}/${COMMAND_FILE_NAME}

automatic_build=0

trap ctrl_c INT
function ctrl_c() {
    echo "Ctrl-C Pressed Exiting!"
    exit 1;
}

if [[ "$1" == -* ]]
then
  if [[ "$1" == *a* ]]
  then
    echo -e " ...\033[1;32mAutomatic build and push\033[m"
    automatic_build=1
  fi
fi


echo -e " "
echo -e " Welcome to \033[1;31mRecon Instruments\033[m Recovery generator"
echo -e " This script will create a simple command file that will initiate"
echo -e " a recovery by wiping data and cache"
echo -e "---------------------------------------------------------------"

echo "--wipe_data" > ${COMMAND_FILE}
echo "--wipe_cache" >> ${COMMAND_FILE}

while true; do
    if [ ${automatic_build} -eq 1 ]; then
        ans='r'
    else
        echo -e "Would you like to [\033[1;32mR\033[m]ecover / [\033[1;31mE\033[m]xit ?"
        read -n1 ans
        echo " ";
    fi
    case $ans in
        [Rr]* )
            c_cmd="adb shell mkdir -p /cache/recovery"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            c_cmd="adb push ${COMMAND_FILE} /cache/recovery/${COMMAND_FILE_NAME}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            break;;
        [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
        * ) echo "Please answer U/E";;
    esac
done

while true; do
    if [ ${automatic_build} -eq 1 ]; then
        ans='y'
    else
        echo " "
        echo -e "Would you like to reboot to initiate Recovery [\033[1;32mY\033[m]es / [\033[1;31mN\033[m]o ?"
        read -n1 ans
        echo " "
    fi
    case $ans in
        [Yy]* )
            c_cmd="adb reboot recovery"; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            break;;
        [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
        * ) echo "Please answer Y/N";;
    esac
done


