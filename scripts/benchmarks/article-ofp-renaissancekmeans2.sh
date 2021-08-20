# Configuration for the Renaissance K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-nompi.sh"
export BENCHMARK_NAME="renaissance-kmeans2"
export MAIN="renaissance.JavaKMeans"
# There are 5 compulsory arguments:
# <point dimension> <nb of clusters "k"> <repetitions> <number of points> <thread count>
export ARGS="5 250 30 10000000 ${WORKERS}"
export TIMEOUT=5:00
export BEO_TIMEOUT=5m
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=kmeans-parser.sh

