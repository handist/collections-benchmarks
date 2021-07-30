#!/bin/bash

COLLECTIONS_DIR=/home/patrick/handistCollections

CURRENT=`pwd`
for directory in $@
do
    cd $directory
    for subdir in ./*/
    do
        cd $subdir
        for log in `ls *.glblog`
        do
            PLOTFILE=${log/.glblog/.png}
            CSVFILE=${log/.glblog/.csv}
            if [[ -f ${CSVFILE} ]]
            then
                echo "Skipping ${CSVFILE}"
            else
                java -cp ${COLLECTIONS_DIR}/target/collections-v1.0.0-jar-with-dependencies.jar handist.collections.glb.util.ProgramStatistics -w ${CSVFILE} $log
            fi
            if [[ -f ${PLOTFILE} ]]
           then
              echo "Skipping ${PLOTFILE}"
         else
                gnuplot -c ${COLLECTIONS_DIR}/src/main/resources/glb/gnuplot/workerOccupation.plt $CSVFILE $PLOTFILE 4 2
        fi
        done
        cd ..
    done
    cd ${CURRENT}
done
