#!/bin/bash
################################################################################
# Script used to parse the results of the various benchamrk executions and     #
# generate CSV files which can then be used easily as inputs for an Excel file #
#------------------------------------------------------------------------------#
# 1 parameter: the "hosts" script used as configuration when launching the     #
#              programs.                                                       #
################################################################################
BENCH_HOME=`pwd`
CONFIG_DIR=${BENCH_HOME}/configurations
BENCHMARK_DIR=${BENCH_HOME}/benchmarks
PARSERS_DIR=${BENCH_HOME}/parsers

source $1

# Prepare direcotry for the parsed results of this host
mkdir -p results/${HOST}

for config_file in ${CONFIGS}
do
    for bench in ${BENCHMARKS}
    do
        source $1
        source ${CONFIG_DIR}/${config_file}
        source ${BENCHMARK_DIR}/${bench}

        if [[ -f ${PARSERS_DIR}/${PARSER} ]]
        then
            OUTPUT_FILE=${BENCH_HOME}/results/${HOST}_${CONFIG_NAME}_${BENCHMARK_NAME}.csv

            # Apply the parsing script to the execution results and redirect the output
            cd ${HOST}/${CONFIG_NAME}/${BENCHMARK_NAME}

            source ${PARSERS_DIR}/${PARSER} > ${OUTPUT_FILE}

            cd ../../..
        else
            echo "[ERROR] Unable to locate script ${PARSERS_DIR}/${parser.sh}"
        fi

    done

done
