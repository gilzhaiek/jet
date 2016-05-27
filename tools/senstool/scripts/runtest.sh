#!/bin/sh
sens_poll() {
if [ $1 -eq 1 ]
then
    sens="accel"
fi
if [ $1 -eq 2 ]
then
    sens="mag"
fi
if [ $1 -eq 4 ]
then
    sens="gyro"
fi
echo setting sensor $1 to 100ms delay 
senstest 3 $1 100
echo enabling sensor $1
senstest 1 $1
echo polling
timeout senstest 4 >> $sens 
ext=".csv"
ext2="_stat.txt"
grep X $sens | tr -d '[]XYZ:' | awk '{print $2 "," $3 "," $4}' >> $sens$ext
stats=$sens$ext2
printf "stddev: x=" >> $stats
printf "$(awk -F , '{sum+=$1; sumsq+=$1*$1} END {print sqrt(sumsq/NR - (sum/NR)**2)}' $sens$ext)" >> $stats
printf "\ty=" >> $stats
printf "$(awk -F , '{sum+=$2; sumsq+=$2*$2} END {print sqrt(sumsq/NR - (sum/NR)**2)}' $sens$ext)" >> $stats
printf "\tz=" >> $stats
printf "$(awk -F , '{sum+=$3; sumsq+=$3*$3} END {print sqrt(sumsq/NR - (sum/NR)**2)}' $sens$ext)\n" >> $stats
cat $stats
echo done... disabling sensor
senstest 2 $1
}
echo enter last four digits of serial number
read sn
mkdir results
cd results
mkdir $sn
cd $sn
while :
do
echo "1) Accelerometer"
echo "2) Magnetometer"
echo "3) Gyro"
echo "q) Quit"
read choice
case "$choice" in
    1|acc) sens_poll 1 ;;
    2|mag) sens_poll 2 ;;
    3|gyro) sens_poll 4 ;;
    q|quit) exit ;;
esac
done
