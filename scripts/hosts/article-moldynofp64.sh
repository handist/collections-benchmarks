################################################################################
# Configuration for executions on OFP                                          #
################################################################################
export HOST=article-moldynofp1
export HOST_TYPE=ofp

# List of benchmarks that we want to run
export BENCHMARKS=("moldyn-hybrid.sh")
# List of library configruations that we want to test
export CONFIGS="76-2dtrial-outputproduct.sh"
# Path to the MPI bindings
export JAVALIBRARYPATH=/work/gp43/share/mpj-v0_44/lib
# Number of hosts desired
export NB_HOSTS=64
# Number of concurrent workers *(-Dglb.workers)
export WORKERS=68
# Core restriction
export CORE_RESTRICTION="0-67"

########################## Options specific to OFP #############################
# Resource group
export RSCGRP="regular-flat"
