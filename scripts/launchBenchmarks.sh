#!/bin/bash
################################################################################
# File used to launch the benchmarks using the configuration files passed as   #
# parameter.                                                                   #
################################################################################
BENCHMARK_DIR="`pwd`/benchmarks"
BENCHMARKS="DummyBenchmark.sh"
################################################################################
# CONFIGURATION                                                                #
#------------------------------------------------------------------------------#
# Here are the variables that contain the settings / paths to files /          #
# configurations that are common betwen all the configurations                 #
################################################################################
# Nb of hosts on which to run the benchmark
NB_HOSTS=1

# If using a Beowuld cluster, the path to the hostfile to use
HOSTFILE=hosts

# Timeout in minutes in which the program is allowed to run
TIMEOUT=30

# Number of times any benchmark needs to be run
REPETITIONS=5

# Script which is in charge of eiter directly running the benchmark (in the
# case a Beowulf cluster is used) or submitting the programs to a job
# management system
PROGRAM_LAUNCHER="`pwd`/beowulfLauncher.sh"
#PROGRAM_LAUNCHER=OFPLauncher.sh

echo "[INFO] Using launcher ${PROGRAM_LAUNCHER}"

# For each file given as parameter
for config_file in $@
do
    # Check file presence
    if [[ ! -f $config_file ]]
    then
        echo "[ERROR] File $config_file does not exists, skipping"
        continue
    fi

    # Set all the variables that we need
    source $config_file

    # Check and create directory if needed
    if [[ ! -d $NAME ]]
    then
        mkdir $NAME
        echo "[INFO] Directory ${NAME}/ created"
    else
        echo "[INFO] Directory ${NAME}/ exists"
    fi

    # Check for the presence of the configuration file, copy if absent
    if [[ -f ${NAME}/${config_file} ]]
    then
        echo "[INFO] Previous backup of ${config_file} found"
        diff ${config_file} ${NAME}/${config_file} > /dev/null 2>&1
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
        cp ${config_file} ${NAME}
    fi

    cd $NAME

    echo "[INFO] Launching benchmarks for ${NAME}"
    # Check if previous executions of benchmarks were made
    # This check is made based on the STDOUT and STDERR ouput file names.
    # If launching a program benchmark would produce files with the same names as
    # existing ones, the execution is skipped to avoid over-writing existing files
    # To discard previous executions and re-run the benchmark, previous files must
    # be manually removed first.
    for bnchmrk in ${BENCHMARKS}
    do
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

        source ${PROGRAM_LAUNCHER}
        cd ..
    done
    cd ..
done
