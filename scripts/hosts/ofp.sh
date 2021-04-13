################################################################################
# Configuration for executions on OFP                                          #
################################################################################
export HOST=ofp
export HOST_TYPE=ofp

# List of benchmarks that we want to run
export BENCHMARKS="kmeans.sh kmeans-grain.sh"
# List of library configruations that we want to test
export CONFIGS="74-explicititerations.sh" 
#export CONFIGS="merge75-config.sh 74-explicititerations.sh 74-arrayiterations.sh 74-arrayiterationsV2.sh"
# Path to the MPI bindings
export JAVALIBRARYPATH=/work/gp43/share/mpj-v0_44/lib
# Number of hosts desired
export NB_HOSTS=1
# Number of concurrent workers *(-Dglb.workers)
export WORKERS=68
# Core restriction
export CORE_RESTRICTION="0-67"

########################## Options specific to OFP #############################
# Resource group
export RSCGRP="regular-flat"
