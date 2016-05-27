export MYDROID=`pwd`/mydroid
export BOARD_TYPE=jet

echo "Removing obj/APP"
rm -rf ${MYDROID}/out/target/product/${BOARD_TYPE}/obj/APP/*
echo "Done!"
echo "Removing obj/APPS"
rm -rf ${MYDROID}/out/target/product/${BOARD_TYPE}/obj/APPS/*
echo "Done!"

