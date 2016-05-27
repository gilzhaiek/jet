chmod 777 /system/bin/input

while true
do
read key 
case $key in
0) input keyevent 7
;;
1) input keyevent 8
;;
2) input keyevent 9
;;
3) input keyevent 10
;;
4) input keyevent 11
;;
5) input keyevent 12
;;
6) input keyevent 13
;;
7) input keyevent 14
;;
8) input keyevent 15
;;
9) input keyevent 16
;;
quit) exit
;;
esac

done
