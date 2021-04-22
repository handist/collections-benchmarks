# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="kmeanstriangle"
export MAIN="handist.kmeans.KMeansTriangleDistribution"
# There are 5 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <chunk size> <number of points> [seed]
export ARGS="3 10 20 100 100000 42"
export TIMEOUT=5m
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-parser.sh

