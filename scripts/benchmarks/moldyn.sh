# Configuration for the Moldyn benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="moldyn"
export MAIN="handist.moldyn.Moldyn"
# There are 4 compulsory arguments and 1 optional:
# <datasize> <number of workers> <number of split x> <number of split y>
export ARGS="8 4 3 3"
export TIMEOUT=5m
export REPETITIONS=1

# Script used to combine the results of this benchmark
#PARSER=kmeans-parser.sh
