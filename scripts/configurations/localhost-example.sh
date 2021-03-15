################################################################################
# Name of this configuration and path to library JAR                           #
#------------------------------------------------------------------------------#
# These variables are used to identify the library / name of the version being #
# tested. They will be used to create directories and name files accordingly   #
################################################################################
export NAME=ExampleConfiguration
export COLLECTIONS_LIBRARY=/home/patrick/handistCollections/target/collections--SNAPSHOT.jar
export BENCHMARK_LIBRARY=/home/patrick/benchmarks/target/benchmarks-0.0.1-SNAPSHOT.jar
export APGAS_LIBRARY_AND_DEPENDANCIES=/home/apgaslibs/*
export JAVA_LIBRARY_PATH=/home/patrick/mpi-for-java

################################################################################
# Options and configurations of the handist collections library                #
#------------------------------------------------------------------------------#
# These variables will be used to set the appropriate -D<property>=<value>     #
# options to the Java process and other options and variables in the script    #
# used to launch the benchamrks.                                               #
################################################################################
# Granularity (-Dglb.grain=<grain>)
export GRAIN=100
# Lifeline strategy (-Dglb.lifeline=<lifeline>)
export LIFELINE="handist.collections.glb.lifeline.NoFLifeline"
# Mode used to make lifeline answers (-glb.serialization=<serialization>)
SERIALIZATION="kryo"
# Number of concurrent workers (-Dglb.workers)
export WORKERS=3
# Core restriction (option of taskset -ca <core_restriction>)
export CORE_RESTRICTION="0-3"
# If using a Beowuld cluster, the path to the hostfile to use
export HOSTFILE=/home/patrick/harp

