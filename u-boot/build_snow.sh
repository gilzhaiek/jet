if [ $# -eq 1 ]
then
    if [[ "$1" == -* ]]
    then
        if [[ "$1" == *c* ]]
        then
            ./core_build.sh -xc
            exit $?
        fi
    fi
fi

./core_build.sh -x
exit $?
