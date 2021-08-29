# Configuration for the Renaissance K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-nompi.sh"
export BENCHMARK_NAME="receiver-4tbag"
export MAIN="handist.bag.BagEval"
# There are 5 compulsory arguments:
# <actor count> <chunk size> <iteration count> <parallelism> <computation weight>
export ARGS="500000 1000 15 4 5" 
export TIMEOUT=5:00
export BEO_TIMEOUT=5m
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=kmeans-parser.sh

