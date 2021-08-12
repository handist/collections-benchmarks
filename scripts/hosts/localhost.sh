################################################################################
# Name of this configuration and path to library JAR                           #
#------------------------------------------------------------------------------#
# These variables are used to identify the library / name of the version being #
# tested. They will be used to create directories and name files accordingly   #
################################################################################
export HOST=localhost
export HOST_TYPE=beowulf

# All the benchmarks that we want to run with this host
# export BENCHMARKS="kmeans.sh kmeans-grain.sh"
export BENCHMARKS="article-renaissancekmeans.sh article-nompikmeans.sh"
# All the configurations that we want to test with this host
export CONFIGS="logger-nolifeline.sh"
# Path to the MPI-binding libraries for this host
export JAVALIBRARYPATH="${MPJ_HOME}/lib"
# Hostfile to use for this host
export HOSTFILE=${BENCH_HOME}/hostfile/localhost
# Number of hosts in which the benchmarks should run
export NB_HOSTS=1
# Number of concurrent workers (-Dglb.workers)
export WORKERS=3
# Core restriction (option of taskset -ca <core_restriction>)
export CORE_RESTRICTION="0-3"

