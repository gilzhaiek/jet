#!/bin/bash

#AUTHORED BY: Kevin Chow - 04-2015
#GOOD LUCK TO FUTURE EMPLOYEES

RED='\033[0;31m'
GREEN='\033[1;32m'
BLUE='\033[1;34m'
NOCOLOUR='\033[0m'

HOME_DIR="/home/$(whoami)"

ANDROID_DIR="/home/$(whoami)/Documents"
ANDROID_TAR="android-sdk_r24.1.2-linux.tgz"
ANDROID_APIS="tools,platform-tools,build-tools-19.1.0,android-10,android-16,android-17,android-19"

JET_DIR="/home/$(whoami)/Development/jet"

function ask {
# https://gist.github.com/davejamesmiller/1965569
  while true; do

    if [ "${2:-}" = "Y" ]; then
      prompt="Y/n"
      default=Y
    elif [ "${2:-}" = "N" ]; then
      prompt="y/N"
      default=N
    else
      prompt="y/n"
      default=
    fi

    # Ask the question
    read -p "$1 [$prompt] " REPLY

    # Default?
    if [ -z "$REPLY" ]; then
      REPLY=$default
    fi

    # Check if the reply is valid
    case "$REPLY" in
      Y*|y*) return 0 ;;
      N*|n*) return 1 ;;
    esac

  done
}

#The user's SSH keys must be used; therefore this script cannot be run as root
if [ "$(whoami)" == "root" ]; then
  echo -e "\n\n${RED}This script must be run as yourself!${NOCOLOUR}\n\n"
  exit 1
fi

if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
  echo -e "\n\n${RED}This script must be sourced in order to set environment variables!"
  echo -e "\nPlease run as '. ./new_user_setup.sh' or 'source ./new_user_setup.sh'${NOCOLOUR}\n\n"
  exit 1
fi

#This line's exit code will determine whether or not the user can SSH to GitHub's servers
ssh -q -o StrictHostKeyChecking=no git@github.com
SSH_EXIT=$?

while [ "$SSH_EXIT" != "1" ]; do
  echo -e "\n\n${RED}You have not set up your GitHub account!  This is required for setup."
  echo -e "\nPlease paste the following into your GitHub account SSH keys at:\nhttp://www.github.com/settings/ssh${NOCOLOUR}\n\n"
  ssh-keygen -q -t rsa -f ~/.ssh/id_rsa -N ""
  #Mute these functions -> There is no -q option
  eval "$(ssh-agent -s)" &>/dev/null
  ssh-add ~/.ssh/id_rsa &>/dev/null
  echo -e "\n"
  #Display the user's public SSH key for usage on GitHub
  cat ~/.ssh/id_rsa.pub
  echo -e "\n\n"
  sleep 1
  #Open up SSH keys on github
  if [ hash google-chrome 2>/dev/null ]; then
    google-chrome 'http://www.github.com/settings/ssh'
  elif [ hash firefox 2>/dev/null ]; then
    firefox 'http://www.github.com/settings/ssh'
  fi
  read -p "Press return once you have done this!"
  ssh -q -o StrictHostKeyChecking=no git@github.com
  SSH_EXIT=$?
done

clear
echo -e "\n\n================================================================================="
echo -e "\n> This script sets up the Recon Instruments build environment for new employees."
echo -e "\n\n> Missing packages will be installed and the jet GitHub repository will be cloned"
echo -e "\n> into the Development directory of your home directory."
echo -e "\n=================================================================================\n\n\n"
if ! ask "> Would you like to continue?" "N"; then
  exit 0
fi

#If this is ever needed, it's here

#read -p "GitHub Username: " USERNAME

#PASSWORD=''
#echo "GitHub Password: "
#while IFS= read -rsn1 CHAR; do
#  [[ -z $CHAR ]] && { echo -e "\n"; break; }
#  if [[ $CHAR == $'\x7f' ]]; then
#    [[ -n $PASSWORD ]] && PASSWORD=${PASSWORD%?}
#    printf '\b \b'
#  else
#    PASSWORD+=$CHAR
#    printf '*'
#  fi
#done

#These packages are apparently necessary to guarantee add-apt-repository will work
echo -e "\n${BLUE}> Updating repositories.${NOCOLOUR}\n\n"
sudo apt-get update -y -q
sudo apt-get install -y -q python-software-properties software-properties-common

#Update repositories
#webupd8team/java is required for oracle-java6-installer
#git-core/ppa is required for updated git -> allows cloning to work
sudo add-apt-repository -y ppa:webupd8team/java
sudo add-apt-repository -y ppa:git-core/ppa

#The second line here is designed to automatically accept all prompts in the oracle-java6-installer
echo -e "\n\n${BLUE}> Installing Java6.${NOCOLOUR}\n\n"
sudo echo oracle-java6-installer shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
sudo apt-get update -y -q
sudo apt-get install -y -q oracle-java6-installer

echo -e "\n\n${BLUE}> Installing pre-requisite packages for Android Filesystem 1.${NOCOLOUR}\n\n"
sudo apt-get install -y -q git-core flex bison gperf libesd0-dev zip gawk ant libwxgtk2.6-dev zlib1g-dev build-essential tofrodos

echo -e "\n\n${BLUE}> Installing pre-requisite packages for Android Filesystem 2.${NOCOLOUR}\n\n"
sudo apt-get install -y -q libstdc++6 lib32z1 lib32z1-dev ia32-libs g++-multilib libx11-dev libncurses5-dev uboot-mkimage
sudo apt-get install -y -q libncurses-dev:i386 libxml2-utils
sudo apt-get install -y -q lib32readline-gplv2-dev
#For some reason libncurses-dev:i386 removes g++
sudo apt-get install -y -q g++

#Append any extra required android API's to the end of the third line here
#Should an API newer than those provided by this file is needed, update these filepaths
echo -e "\n\n${BLUE}> Installing Android build environment.${NOCOLOUR}\n\n"
wget -P ${ANDROID_DIR}/ http://dl.google.com/android/${ANDROID_TAR}
tar xf ${ANDROID_DIR}/${ANDROID_TAR} -C ${ANDROID_DIR}
echo y | ${ANDROID_DIR}/android-sdk-linux/tools/android update sdk --no-ui -a --filter ${ANDROID_APIS}
rm ${ANDROID_DIR}/${ANDROID_TAR}*
#Update environment variables for later use
echo "#AndroidDev PATH" | cat - ~/.bashrc > temp && mv temp ~/.bashrc
echo "export PATH=\${PATH}:${ANDROID_DIR}/android-sdk-linux/tools" | cat - ~/.bashrc > temp && mv temp ~/.bashrc
echo "export PATH=\${PATH}:${ANDROID_DIR}/android-sdk-linux/platform-tools" | cat - ~/.bashrc > temp && mv temp ~/.bashrc
echo "export PATH=\${PATH}:${ANDROID_DIR}/android-sdk-linux/platforms" | cat - ~/.bashrc > temp && mv temp ~/.bashrc
echo "export JAVA_HOME=/usr/lib/jvm/java-6-oracle" | cat - ~/.bashrc > temp && mv temp ~/.bashrc
#Update environment variables for current use
export PATH=$PATH:${ANDROID_DIR}/android-sdk-linux/tools
export PATH=$PATH:${ANDROID_DIR}/android-sdk-linux/platform-tools
export PATH=$PATH:${ANDROID_DIR}/android-sdk-linux/platforms
export JAVA_HOME=/usr/lib/jvm/java-6-oracle
. ~/.bashrc

#Clone jet repository into directory
echo -e "\n\n${BLUE}> Cloning jet repository into ${JET_DIR}${NOCOLOUR}\n\n"
git clone git@github.com:ReconInstruments/jet.git $JET_DIR
cd $JET_DIR
git submodule update --init
cd limo_apps
git submodule update --init

cd ${JET_DIR}/mydroid
. build/envsetup.sh
lunch 15

echo -e "\n\n${BLUE}> Installing ADB + MTP.${NOCOLOUR}\n\n"
cd $JET_DIR/omap4_emmc_files_jet
sudo adb kill-server
sudo cp adb $(which adb)

sudo sh -c "echo 'SUBSYSTEM==\"usb\",SYSFS{idVendor}==\"2523\",ATTR{idProduct==\"d109\",SYMLINK+=\"libmtp-%k\",MODE=\"0666\"' >> /etc/udev/rules.d/51-android.rules"
sudo sh -c "echo 'SUBSYSTEM==\"usb_device\".SYSFS{idVendor}==\"2523\",MODE=\"0666\"' >> /etc/udev/rules.d/51-android.rules"

sudo service udev restart
sudo apt-get install -y -q mtpfs
sudo mkdir /media/MTPdevice
sudo chmod 777 /media/MTPdevice
sudo mtpfs -o allow_other /media/MTPdevice

cd $JET_DIR
clear
echo -e "\n\n\n\n\n${GREEN}> Installation finished <${NOCOLOUR}\n\n\n\n\n\n"
