RECON_WEB_KEY=$1
YOUR_PATH=`pwd`
SDK_REVISION=4
TMP_FOLDER=${YOUR_PATH}/tmp_sdk
SDK_ADDON_FILE_NAME=`ls out/host/linux-x86/sdk_addon/*.zip | sed -e 's|.*/||g'`
SDK_ADDON_FILE=${YOUR_PATH}/out/host/linux-x86/sdk_addon/${SDK_ADDON_FILE_NAME}
SDK_ADDON_NAME=`echo ${SDK_ADDON_FILE_NAME} | sed -e 's|\.zip||'`
SDK_ADDON_WEB_NAME=jet_sdk_addon-eng.zip
SDK_LICENSE=${YOUR_PATH}/sdk_license.txt
REPOSITORY_XML=${YOUR_PATH}/repository.xml
SERVER_SDK_DIR=""

if [ ! -e $SDK_ADDON_FILE ]; then
   echo "Could not locate $SDK_ADDON_FILE"
   exit 1
fi

# Check to see if this is a trial SDK test run or an official release
while true; do
  echo "Are you making a trial SDK test run?"
  read -n1 ans
  echo " ";
  case $ans in
    [Yy]* ) TRIAL_RUN=1; break;;
    [Nn]* ) TRIAL_RUN=0; break;;
    * ) echo "Please answer Y/N";;
  esac
done

# Confirm with user that this is indeed an official release
if [ $TRIAL_RUN -eq 0 ]
  then
  while true; do
    echo -e "Are you sure this is an\033[1;31m official release?\033[m"
    read -n1 ans
    echo " ";
    case $ans in
      [Yy]* ) TRIAL_RUN=0; break;;
      [Nn]* ) echo ; echo "Exiting..."; exit;;
      * ) echo "Please answer Y/N";;
    esac
  done
fi

if [ $TRIAL_RUN -eq 0 ]
  then
  SERVER_SDK_DIR="/mnt/data/wp-content/recon-sdk/"
  echo -e "\033[1;31mPerforming a SDK release. Server directory: $SERVER_SDK_DIR\033[m"
else
  SERVER_SDK_DIR="/mnt/data/wp-content/beta-recon-sdk/"
  echo "Performing a SDK trial run. Server directory: $SERVER_SDK_DIR"
fi

# Check with the user one more time before we continue
while true; do
  echo -n "Is this OK? "
  read -n1 ans
  case $ans in
    [Yy]* ) echo ; echo "Continuing..."; break;;
    [Nn]* ) echo ; echo "Exiting..."; exit;;
    * ) echo "Please answer Y/N";;
  esac
done

rm -rf ${TMP_FOLDER}
mkdir -p ${TMP_FOLDER}
cd ${TMP_FOLDER}
cp ${SDK_ADDON_FILE} .
unzip ${SDK_ADDON_FILE_NAME}
rm ${SDK_ADDON_FILE_NAME}
cd ${SDK_ADDON_NAME}
# Fix the doc folder
cd docs/
mv com.reconinstruments.os_doc/* .
rm -r com.reconinstruments.os_doc
cd ${TMP_FOLDER}
zip -vr ../${SDK_ADDON_WEB_NAME} ${SDK_ADDON_NAME}
cd ${YOUR_PATH}
rm -rf ${TMP_FOLDER}

# Fix repository.xml
SHA_VALUE=`sha1sum ${SDK_ADDON_WEB_NAME} | sed -e 's| .*||'`
SIZE_VALUE=`stat ${SDK_ADDON_WEB_NAME} | grep Size | sed -e 's|  Size: ||;s| .*||'`

# Create the XML
echo "<sdk:sdk-addon" > ${REPOSITORY_XML}
echo "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" >> ${REPOSITORY_XML}
echo "    xmlns:sdk=\"http://schemas.android.com/sdk/android/addon/1\">" >> ${REPOSITORY_XML}
echo "    <sdk:add-on>" >> ${REPOSITORY_XML}
echo "        <sdk:name>Recon Instruments SDK Add-On</sdk:name>" >> ${REPOSITORY_XML}
echo "        <sdk:api-level>16</sdk:api-level>" >> ${REPOSITORY_XML}
echo "        <sdk:vendor>Recon Instruments</sdk:vendor>" >> ${REPOSITORY_XML}
echo "        <sdk:revision>${SDK_REVISION}</sdk:revision>" >> ${REPOSITORY_XML}
echo "        <sdk:description>Android + Recon Instruments SDK Add-on, API 16, revision ${SDK_REVISION} </sdk:description>" >> ${REPOSITORY_XML}
echo "        <sdk:desc-url>http://reconinstruments.com/wp-content/recon-sdk/</sdk:desc-url>" >> ${REPOSITORY_XML}
echo "        <sdk:uses-license ref=\"recon-instruments-android-addon-license\" />" >> ${REPOSITORY_XML}
echo "        <sdk:archives>" >> ${REPOSITORY_XML}
echo "            <sdk:archive os=\"any\">" >> ${REPOSITORY_XML}
echo "                <sdk:size>${SIZE_VALUE}</sdk:size>" >> ${REPOSITORY_XML}
echo "                <sdk:checksum type=\"sha1\">${SHA_VALUE}</sdk:checksum>" >> ${REPOSITORY_XML}
echo "                <sdk:url>${SDK_ADDON_WEB_NAME}</sdk:url>" >> ${REPOSITORY_XML}
echo "            </sdk:archive>" >> ${REPOSITORY_XML}
echo "        </sdk:archives>" >> ${REPOSITORY_XML}
echo "        <sdk:libs>" >> ${REPOSITORY_XML}
echo "        </sdk:libs>" >> ${REPOSITORY_XML}
echo "    </sdk:add-on>" >> ${REPOSITORY_XML}
echo "    <sdk:license type=\"text\" id=\"recon-instruments-android-addon-license\">" >> ${REPOSITORY_XML}
cat ${SDK_LICENSE} >> ${REPOSITORY_XML}
echo "    </sdk:license>" >> ${REPOSITORY_XML}
echo "</sdk:sdk-addon>" >> ${REPOSITORY_XML}

echo "Here is the SDK File: ${YOUR_PATH}/${SDK_ADDON_WEB_NAME}"
ls -l ${SDK_ADDON_WEB_NAME}
echo "Here is the repository file: ${REPOSITORY_XML}"
ls -l ${REPOSITORY_XML}

# Upload
if [ ! -f $RECON_WEB_KEY ] || [ -z "$RECON_WEB_KEY" ]; then
   echo "Can't upload the file, please use $0 <location of reconweb.pem>"
   exit 1
fi

ssh -i ${RECON_WEB_KEY} ubuntu@s1.reconinstruments.com 'sudo chmod 777 '"${SERVER_SDK_DIR}"'*'
scp -i ${RECON_WEB_KEY} ${REPOSITORY_XML} ubuntu@s1.reconinstruments.com:${SERVER_SDK_DIR}
scp -i ${RECON_WEB_KEY} ${SDK_ADDON_WEB_NAME} ubuntu@s1.reconinstruments.com:${SERVER_SDK_DIR}
ssh -i ${RECON_WEB_KEY} ubuntu@s1.reconinstruments.com 'sudo chmod 755 '"${SERVER_SDK_DIR}"'*'

exit 0
