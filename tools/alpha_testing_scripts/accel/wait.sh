while [ $(adb devices | wc -l) -le 2 ]; do printf .; sleep 1; done
