#!/bin/bash

./make_app.bash
adb shell am start -n com.reconinstruments.agps/.GpsAssist
