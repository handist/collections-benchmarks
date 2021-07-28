#!/bin/bash

COLLECTIONS_DIR=/home/patrick/handist/collections

for directory in $@
do
    cd $directory
    for log in `ls *.glblog`
    do
        PLOTFILE=${log/.glblog/.png}
        CSVFILE=${log/.glblog/.csv}
        java -cp ${COLLECTIONS_DIR}/target/collections-v1.0.0-jar-with-dependencies.jar handist.collections.glb.util.ProgramStatistics -w ${CSVFILE} $log
        gnuplot -c ${COLLECTIONS_DIR}/src/main/resources/glb/gnuplot/workerOccupation.plt $CSVFILE $PLOTFILE 4 2
    done
    cd ..
done
