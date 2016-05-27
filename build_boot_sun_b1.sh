OP=-y1

if [ $# -eq 1 ]
then
    if [[ "$1" == -* ]]
    then
        if [[ "$1" == *c* ]]
        then
            OP=${OP}c
        fi
    fi
fi

./core_build_boot.sh ${OP}
exit $?
