################################################################################
# Configuration for executions on OFP                                          #
################################################################################
export HOST=article-ofp1
export HOST_TYPE=ofp

# List of benchmarks that we want to run
#export BENCHMARKS=""
export BENCHMARKS="article-ofp-renaissancekmeans.sh article-ofp-nompikmeans.sh article-ofp-teamedkmeans.sh article-ofp-teamedkmeans2.sh article-ofp-nompikmeans2.sh article-ofp-renaissancekmeans2.sh article-ofp-renaissancekmeans3.sh article-ofp-nompikmeans3.sh article-ofp-teamedkmeans3.sh"
# List of library configruations that we want to test
export CONFIGS="logger-nolifeline.sh"
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
