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
	# Creating the file will prevent another submission job to launch a 
	# second job from being submitted with the same output file
	touch ${OUTFILE}
	touch ${ERRFILE}
	echo "[Run ${run}] `pjsub -N ${BENCHMARK_NAME}${run} -X --rsc-list rscgrp=${RSCGRP},node=${NB_HOSTS},elapse=${TIMEOUT} -o ${OUTFILE} -e ${ERRFILE} ${BENCH_HOME}/launchers/${HOST_TYPE}/directmpiprogram.sh 2>&1`"
    fi
done

STAMP=`date +"%m-%d-%y %R"`
echo "[${STAMP}] All executions for benchmark ${BENCHMARK_NAME} submitted"
