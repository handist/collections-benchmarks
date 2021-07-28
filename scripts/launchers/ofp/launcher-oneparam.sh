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
    export FILE_PREFIX=${CONFIG_NAME}_${BENCHMARK_NAME}_${PARAM}${value}_Run${run}
    if [[ -f ${OUTFILE} || -f ${ERRFILE} ]]
    then
        echo "[$STAMP] Skipping ${PARAM}:${value} Run #${run}, execution output files present"
    else
	# Creating the file will prevent another submission job to launch a 
	# second job from being submitted with the same output file
	touch ${OUTFILE}
	touch ${ERRFILE}
	echo "[${PARAM}:${value} Run ${run}] `pjsub -N ${BENCHMARK_NAME}${run} -X --rsc-list rscgrp=${RSCGRP},node=${NB_HOSTS},elapse=${TIMEOUT} -o ${OUTFILE} -e ${ERRFILE} ${BENCH_HOME}/launchers/${HOST_TYPE}/program.sh 2>&1`"
    fi
done
done
STAMP=`date +"%m-%d-%y %R"`
echo "[${STAMP}] All executions for benchmark ${BENCHMARK_NAME} submitted"
