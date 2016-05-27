chmod 777 /system/bin/input

while true
do
read key
if [[ "$key" == "" ]]
then
input keyevent 66
else
case $key in
h)
echo
echo --- Directions ---
echo w      Up
echo a      Left
echo d      Right
echo x      Down
echo s      Center
echo z      Back
echo --- Context ---
echo m      Menu
echo g      Search
echo r      Home
echo Enter  Enter
echo --- Text ---
echo _      Space
echo b      Backspace
echo c      Clear
echo t      Text Mode - Click Enter to Submit text
echo --- Tap/Swipe ---
echo 1      Tap Mode - Enter X Y
echo 2      Swipe
;;
a) input keyevent 21
;;
w) input keyevent 19
;;
d) input keyevent 22
;;
x) input keyevent 20
;;
z) input keyevent 4
;;
s) input keyevent 23
;;
m) input keyevent 82
;;
g) input keyevent 84
;;
r) input keyevent 3
;;
b) input keyevent 67
;;
_) input keyevent 62
;;
c) input keyevent 28
;;
t)
echo
echo Enter Text Followed by an Enter or Space
echo Note: Use _ for Space
while true
do
read -s -n 1 key
if [[ "$key" == "" ]]
then
echo Back to Normal Mode
break
fi
if [[ "$key" == "_" ]]
then
input keyevent 62
else
input text $key
fi
done
;;
1)
echo
echo Please Enter X Y :
echo
read xy
input tap $xy
;;
2)
echo
echo You are in Swipe Mode - Use W,A,D,X and Enter to Exit
while true
do
read -s -n 1 key
if [[ "$key" == "" ]]
then
echo Back to Normal Mode
break
else
case $key in
w) input swipe 214 200 214 50
;;
x) input swipe 214 50 214 200
;;
a) input swipe 350 120 50 120
;;
d) input swipe 50 200 350 50
;;
esac
fi
done
;;
q) exit
;;
esac
fi

done
