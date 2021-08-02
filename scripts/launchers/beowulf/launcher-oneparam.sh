#!/bin/bash

for value in ${VALUES}
do
    # Set the VALUE to the appropriate Variable
    export ${PARAM}=${value}
    for (( run=1; run<=${REPETITIONS}; run++))
    do
    STAMP=`date +"%m-%d-%y %R"`
    OUTFILE=${CONFIG_NAME}_${BENCHMARK_NAME}_${PARAM}${value}_Run${run}.out.txt
    ERRFILE=${CONFIG_NAME}_${BENCHMARK_NAME}_${PARAM}${value}_Run${run}.err.txt
    export EXTRA_ARGS=${CONFIG_NAME}_${BENCHMARK_NAME}_${PARAM}${value}_Run${run} # used by logging glb
    if [[ -f ${OUTFILE} || -f ${ERRFILE} ]]
    then
        echo "[$STAMP] Skipping  ${PARAM}:${value} Run #${run}, execution output files present"
    else
        echo "[$STAMP] Launching ${PARAM}:${value} Run #${run}"
        nohup ${BENCH_HOME}/launchers/${HOST_TYPE}/program.sh > ${OUTFILE} 2> ${ERRFILE}
    fi
    done
done

STAMP=`date +"%m-%d-%y %R"`
echo "[${STAMP}] All executions for benchmark ${BENCHMARK_NAME} completed"
