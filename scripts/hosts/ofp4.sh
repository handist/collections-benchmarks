################################################################################
# Configuration for executions on OFP                                          #
################################################################################
export HOST=ofp4
export HOST_TYPE=ofp

# List of benchmarks that we want to run
#export BENCHMARKS="cardgame2-larger.sh cardgame2-huge.sh cardgame2-largerv2.sh cargame2-hugev2.sh flatcardgame-larger.sh flatcardgame-huge.sh"
#export BENCHMARKS="ufs-small.sh ufs-sfbt8.sh ufs-sfbt9.sh ufs-sfbt10.sh ufs-sfbt11.sh ufs-sfbt11v2.sh ufs2-small.sh ufs2ub-small.sh ufs2-small-grain.sh"
export BENCHMARKS="kmeans-grain.sh trianglekmeans-small.sh flatkmeans-small.sh trianglekmeans-large.sh flatkmeans-large.sh flatkmeans-huge.sh trianglekmeans-huge.sh"
# trianglekmeans-larger.sh flatkmeans-larger.sh flatkmeans-huge.sh trianglekmeans-huge.sh"
#"trianglekmeans2-small.sh"

# List of library configruations that we want to test
export CONFIGS="nolifeline-nolongboxing.sh hypercube-nolongboxing.sh lifeline-noanswerafterintraLB.sh" #" # lifeline-debug.sh 
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
