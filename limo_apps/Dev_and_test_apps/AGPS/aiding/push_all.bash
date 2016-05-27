#!/bin/bash

PATH_TO_INJECT=/data/data/

adb push almanac_YUMA.alm $PATH_TO_INJECT
for i in *.raw; do adb push $i $PATH_TO_INJECT; done
