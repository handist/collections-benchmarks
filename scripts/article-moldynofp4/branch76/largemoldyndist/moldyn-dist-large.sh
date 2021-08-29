# Configuration for the Moldyn benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="largemoldyndist"
export MAIN="handist.moldyn.MoldynDist"
# There are 4 compulsory arguments and 1 optional:
# <datasize index(0or1or2)>
export ARGS="2"
export TIMEOUT=50:00
export BEO_TIMEOUT=5m
export REPETITIONS=2

# Script used to combine the results of this benchmark
#PARSER=kmeans-parser.sh
