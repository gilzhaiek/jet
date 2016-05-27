#!/bin/bash
destination=$1;
while read line
do
    wget https://s3.amazonaws.com/geotilebucket/1/base/$line.rgz -O $destination/$line.rgz;
done