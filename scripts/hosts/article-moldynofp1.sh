################################################################################
# Configuration for executions on OFP                                          #
################################################################################
export HOST=article-moldynofp1
export HOST_TYPE=ofp

# List of benchmarks that we want to run
BENCHMARKS="moldyn-mpi.sh moldyn-mpi-large.sh"
BENCHMARKS="${BENCHMARKS} moldyn-seq.sh moldyn-seq-large.sh"
BENCHMARKS="${BENCHMARKS} moldyn-simpleseq.sh moldyn-simpleseq-large.sh"
BENCHMARKS="${BENCHMARKS} moldyn-dist.sh moldyn-dist-large.sh"
BENCHMARKS="${BENCHMARKS} moldyn-seqmt.sh moldyn-seqmt-large.sh"
BENCHMARKS="${BENCHMARKS} moldyn-hybrid.sh moldyn-hybrid-large.sh"
BENCHMARKS="${BENCHMARKS} moldyn-hybrid-split128.sh moldyn-hybrid-largesplit128.sh"
export BENCHMARKS
# List of library configruations that we want to test
export CONFIGS="112-accumulatorV1.sh"
# Path to the MPI bindings
export JAVALIBRARYPATH=/work/gp43/share/mpj-v0_44/lib
# Number of hosts desired
export NB_HOSTS=1
# Core restriction
export CORE_RESTRICTION="0-67"

########################## Options specific to OFP #############################
# Resource group
export RSCGRP="regular-flat"
