#!/bin/bash
destination=$1;
while read line
do
    wget --spider -q https://s3.amazonaws.com/geotilebucket/1/base/$line.rgz && echo $line " exists" || echo $line " does not exist"
done
