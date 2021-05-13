#!/bin/bash

for (( run=1; run<=${REPETITIONS}; run++))
do
    STAMP=`date +"%m-%d-%y %R"`
    OUTFILE=${CONFIG_NAME}_${BENCHMARK_NAME}_Run${run}.out.txt
    ERRFILE=${CONFIG_NAME}_${BENCHMARK_NAME}_Run${run}.err.txt
    export JFR_FILE=${CONFIG_NAME}_${BENCHMARK_NAME}_Run${run}.jfr
    if [[ -f ${OUTFILE} || -f ${ERRFILE} ]]
    then
        echo "[$STAMP] Skipping Run #${run}, execution output files present"
    else
	touch ${OUTFILE}
	touch ${ERRFILE}
	echo "[Run ${run}] `pjsub -N ${BENCHMARK_NAME}${run} -X --rsc-list rscgrp=${RSCGRP},node=${NB_HOSTS},elapse=${TIMEOUT} -o ${OUTFILE} -e ${ERRFILE} ${BENCH_HOME}/launchers/${HOST_TYPE}/program-JFR.sh 2>&1`"
    fi
done

STAMP=`date +"%m-%d-%y %R"`
echo "[${STAMP}] All executions for benchmark ${BENCHMARK_NAME} completed"
