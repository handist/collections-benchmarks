# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-oneparam.sh"
export BENCHMARK_NAME="largekmeanstriangle"
export MAIN="handist.kmeans.KMeansTriangleDistribution"
# There are 5 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <chunk size> <number of points> [seed]
export ARGS="5 100 20 10000 10000000 42"
export TIMEOUT=10:00
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=kmeans-grain-parser.sh

PARAM="GRAIN"
VALUES="4 10 20 40 100 200 400 1000 2000 4000 10000"