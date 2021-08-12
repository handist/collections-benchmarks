#!/bin/bash

for (( run=1; run<=${REPETITIONS}; run++))
do
    STAMP=`date +"%m-%d-%y %R"`
    OUTFILE=${CONFIG_NAME}_${BENCHMARK_NAME}_Run${run}.out.txt
    ERRFILE=${CONFIG_NAME}_${BENCHMARK_NAME}_Run${run}.err.txt
    export FILE_PREFIX=${CONFIG_NAME}_${BENCHMARK_NAME}_${PARAM}${value}_Run${run}
    if [[ -f ${OUTFILE} || -f ${ERRFILE} ]]
    then
        echo "[$STAMP] Skipping Run #${run}, execution output files present"
    else
        echo "[$STAMP] Launching Run #${run}"
        nohup ${BENCH_HOME}/launchers/${HOST_TYPE}/program-nompi.sh > ${OUTFILE} 2> ${ERRFILE}
    fi
done

STAMP=`date +"%m-%d-%y %R"`
echo "[${STAMP}] All executions for benchmark ${BENCHMARK_NAME} completed"
