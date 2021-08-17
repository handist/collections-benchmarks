################################################################################
# Configuration for executions on OFP                                          #
################################################################################
export HOST=article-ofp4
export HOST_TYPE=ofp

# List of benchmarks that we want to run
export BENCHMARKS="article-ofp-teamedkmeans.sh"

# List of library configruations that we want to test
export CONFIGS="logger-nolifeline.sh"
# Path to the MPI bindings
export JAVALIBRARYPATH=/work/gp43/share/mpj-v0_44/lib
# Number of hosts desired
export NB_HOSTS=4
# Number of concurrent workers *(-Dglb.workers)
export WORKERS=68
# Core restriction
export CORE_RESTRICTION="0-67"

########################## Options specific to OFP #############################
# Resource group
export RSCGRP="regular-flat"
