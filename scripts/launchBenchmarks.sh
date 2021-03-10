#!/bin/bash
################################################################################
# File used to launch the benchmarks using the configuration files passed as   #
# parameter.                                                                   #
################################################################################

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
PROGRAM_LAUNCHER=beowulfLauncher.sh
#PROGRAM_LAUNCHER=OFPLauncher.sh



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
        echo "[INFO] Directory $NAME created"
    else
        echo "[INFO] Directory $NAME exists"
    fi

    # Check for the presence of the configuration file, copy it is absent
    if [[ -f ${NAME}/${config_file} ]]
    then
        echo "Configuration file already present"
        diff ${config_file} ${NAME}/${config_file} > /dev/null 2>&1
        if [[ ! $? ]]
        then
            echo "Files are different !!!"
            echo "Skipping"
            continue
        fi
    else
        echo "Copying configuration file ${config_file} in directory ${NAME}"
        cp ${config_file} ${NAME}
    fi

    cd $NAME
    source ../${PROGRAM_LAUNCHER}
    cd ..
    # Check if previous executions of benchmarks were made
    # This check is made based on the STDOUT and STDERR ouput file names.
    # If launching a program benchmark would produce files with the same names as
    # existing ones, the execution is skipped to avoid over-writing existing files
    # To discard previous executions and re-run the benchmark, previous files must
    # be manually removed first.
    # TODO ...
done
