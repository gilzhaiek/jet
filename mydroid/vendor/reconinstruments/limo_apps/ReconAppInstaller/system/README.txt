This folder contains dead code intended for updating system apks
it uses the same process as the other RIF installer code except it looks for an update_package.xml file
The update_package.xml file is used as a command to install the rif files and also includes md5s for validating
the final apk

If there is a need for validating package md5s in future we can re-adapt this code, otherwise the existing process
for installing rif files can be used for both third party and system apps