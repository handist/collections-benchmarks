################################################################################
# Configuration for the collections library whose last commit was the merge    #
# from branch 78                                                               #
################################################################################
export CONFIG_NAME="Merge78"
export COLLECTIONS_LIBRARY=${BENCH_HOME}/jar/collections-v1.0.0.jar
export DEPENDENCIES=${BENCH_HOME}/jar/deps
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
export LIFELINE="handist.collections.glb.lifeline.NoLifeline"
# Mode used to make lifeline answers (-glb.serialization=<serialization>)
SERIALIZATION="kryo"
