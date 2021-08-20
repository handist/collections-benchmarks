# Configuration for the Renaissance K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-nompi.sh"
export BENCHMARK_NAME="nompi-kmeans"
export MAIN="handist.noglb.kmeans.SingleHostKMeans"
# There are 5 compulsory arguments, 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <number of points> <chunk size> [seed]
export ARGS="7 5 30 10000000 5000 42"
export TIMEOUT=10.00
export BEO_TIMEOUT=10m
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-parser.sh

