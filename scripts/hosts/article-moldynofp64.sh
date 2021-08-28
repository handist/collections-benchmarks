################################################################################
# Configuration for executions on OFP                                          #
################################################################################
export HOST=article-moldynofp64
export HOST_TYPE=ofp

# List of benchmarks that we want to run
export BENCHMARKS="moldyn-hybrid-large.sh moldyn-hybrid.sh moldyn-mpi.sh moldyn-dist.sh"
# List of library configruations that we want to test
export CONFIGS="76-2dtrial-outputproduct.sh"
# Path to the MPI bindings
export JAVALIBRARYPATH=/work/gp43/share/mpj-v0_44/lib
# Number of hosts desired
export NB_HOSTS=64
# Number of concurrent workers *(-Dglb.workers)
export WORKERS=1
# Number of divide block
export SPLIT=64
# Core restriction
export CORE_RESTRICTION="0-67"

########################## Options specific to OFP #############################
# Resource group
export RSCGRP="regular-flat"
