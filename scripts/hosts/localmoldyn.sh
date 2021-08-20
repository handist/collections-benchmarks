################################################################################
# Name of this configuration and path to library JAR                           #
#------------------------------------------------------------------------------#
# These variables are used to identify the library / name of the version being #
# tested. They will be used to create directories and name files accordingly   #
################################################################################
export HOST=localmoldyn
export HOST_TYPE=beowulf

# All the benchmarks that we want to run with this host
export BENCHMARKS="moldyn.sh"
# All the configurations that we want to test with this host
export CONFIGS="76-2dtrial-outputproduct.sh"
# Path to the MPI-binding libraries for this host
export JAVALIBRARYPATH="${MPJ_HOME}/lib"
# Hostfile to use for this host
export HOSTFILE=${BENCH_HOME}/hostfile/localhost
# Number of hosts in which the benchmarks should run
export NB_HOSTS=1
# Number of concurrent workers (-Dglb.workers)
export WORKERS=4
# Core restriction (option of taskset -ca <core_restriction>)
export CORE_RESTRICTION="0-3"

