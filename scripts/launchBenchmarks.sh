#!/bin/bash
################################################################################
# File used to launch the benchmarks using the configuration files passed as   #
# parameter.                                                                   #
################################################################################
BENCH_HOME=`pwd`
CONFIG_DIR=${BENCH_HOME}/configurations
BENCHMARK_DIR="${BENCH_HOME}/benchmarks"
BENCHMARKS=""
################################################################################
# CONFIGURATION                                                                #
#------------------------------------------------------------------------------#
# Here are the variables that contain the settings / paths to files /          #
# configurations that are common betwen all the configurations                 #
################################################################################
# Script which is in charge of eiter directly running the benchmark (in the    #
# case a Beowulf cluster is used) or submitting the programs to a job          #
# management system                                                            #

################################################################################
# Override the default settings just above within a  .localconfig.sh file on   #
# your system. This is easier than having to track multiple versions of this   #
# master script file for various systems                                       #
################################################################################
# Set variables "BENCHMARKS", "PROGRAM_LAUNCHER", and "JAVA_LIBRARY_PATH"      #
# approprietely for your system                                                #
source $1


echo "[INFO] Using ${HOST_TYPE} launcher"
echo "[INFO] Placing results in directory $HOST"

mkdir -p $HOST
cd $HOST

# For each configuration file specified
for config_file in ${CONFIGS}
do
    # Check file presence
    if [[ ! -f ${CONFIG_DIR}/${config_file} ]]
    then
        echo "[ERROR] File $config_file does not exists, skipping"
        continue
    fi

    # Set all the variables that we need
    source ../$1 # Re-sets variables that may have been over-written in previous iterations
    source ${CONFIG_DIR}/$config_file

    # Check and create directory if needed
    if [[ ! -d $CONFIG_NAME ]]
    then
        mkdir $CONFIG_NAME
        echo "[INFO] Directory ${CONFIG_NAME}/ created"
    else
        echo "[INFO] Directory ${CONFIG_NAME}/ exists"
    fi

    # Check for the presence of the configuration file, copy if absent
    if [[ -f ${CONFIG_NAME}/${config_file} ]]
    then
        echo "[INFO] Previous backup of ${config_file} found"
        diff ${CONFIG_DIR}/${config_file} ${CONFIG_NAME}/${config_file} > /dev/null 2>&1
        if [[ ! $? ]]
        then
            echo "[ERROR] Current configuration and backup differ"
            echo "[INFO] Skipping ${config_file}, manual intervention required"
            continue
        else
            echo "[INFO] Backup and current ${config_file} match"
        fi
    else
        echo "[INFO] Creating a backup of configuration file ${config_file} in directory ${NAME}"
        cp ${CONFIG_DIR}/${config_file} ${config_file}
    fi

    cd $CONFIG_NAME

    echo "[INFO] Launching benchmarks for ${CONFIG_NAME}"
    # Check if previous executions of benchmarks were made
    # This check is made based on the STDOUT and STDERR ouput file names.
    # If launching a program benchmark would produce files with the same names as
    # existing ones, the execution is skipped to avoid over-writing existing files
    # To discard previous executions and re-run the benchmark, previous files must
    # be manually removed first.
    for bnchmrk in ${BENCHMARKS}
    do
        # Load the desired benchmark configuration
        BENCHMARKTORUN="${BENCHMARK_DIR}/${bnchmrk}"
        if [[ ! -f ${BENCHMARKTORUN} ]]
        then
            echo "Could not find benchmark file ${BENCHMARKTORUN}"
            continue
        else
            echo "[INFO] Loading benchmark configuration ${BENCHMARKTORUN}"
            source ${BENCHMARKTORUN}
        fi

        mkdir -p ${BENCHMARK_NAME}

        # Launch all the runs for the benchmark that were not launched yet
        cd ${BENCHMARK_NAME}
        if [[ -f ${bnchmrk} ]]
        then
            # Check that the configuration of previously launched executions are identical
            echo "Benchmark backup file already present"
            diff ${bnchmrk} ${BENCHMARKTORUN} > /dev/null 2>&1
            if [[ ! $? ]]
            then
                echo "The benchmark configuration has changed. Manual intervention necessary"
                cd ..
                continue
            fi
        else
            # Create a backup
            cp ${BENCHMARKTORUN} ${bnchmrk}
        fi

        # Launch the program executions
        source ${PROGRAM_LAUNCHER}

        # Re-set variables that may have been over-written in previous iterations
        source ../../../$1
        source ${CONFIG_DIR}/$config_file

        cd ..
    done
    cd ..

done
