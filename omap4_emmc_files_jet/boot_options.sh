while true; do
    read -p "Would you like to [R]eboot / [F]astboot Reboot / [C]ontinue / [E]xit?" ans
    case $ans in
        [Rr]* ) sudo ./fastboot reboot; break;;
        [Ff]* ) sudo ./fastboot reboot-bootloader; break;;
        [Cc]* ) sudo ./fastboot continue; break;;
        [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; break;;
        * ) echo "Please answer R/C/F/E";;
    esac
done

