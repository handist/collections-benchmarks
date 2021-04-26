# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-oneparam.sh"
export BENCHMARK_NAME="kmeanstriangle"
export MAIN="handist.kmeans.KMeansTriangleDistribution"
# There are 5 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <chunk size> <number of points> [seed]
export ARGS="5 10 20 10000 10000000 42"
export TIMEOUT=10:00
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-parser.sh

PARAM="GRAIN"
VALUES="4 10 20 40 100 200 1000 10000"
