#!/bin/bash

for (( run=1; run<=${REPETITIONS}; run++))
do
    STAMP=`date +"%m-%d-%y %R"`
    OUTFILE=${NAME}_${BENCHMARK_NAME}_Run${run}.txt
    ERRFILE=${NAME}_${BENCHMARK_NAME}_Run${run}.err.txt
    if [[ -f ${OUTFILE} || -f ${ERRFILE} ]]
    then
        echo "[$STAMP] Skipping Run #${run}, execution output files present"
    else
        echo "[$STAMP] Launching Run #${run}"
        nohup bash beowulfProgram.sh > ${OUTFILE} 2> ${ERRFILE}
    fi
done

STAMP=`date +"%m-%d-%y %R"`
echo "[${STAMP}] All executions for benchmark ${BENCHMARK_NAME} completed"
