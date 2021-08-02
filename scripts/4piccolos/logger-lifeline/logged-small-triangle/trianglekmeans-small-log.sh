# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-oneparam.sh"
export BENCHMARK_NAME="logged-small-triangle"
export MAIN="handist.kmeans.KMeansTriangleDistribution"
# There are 5 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <chunk size> <number of points> [seed] [prefix for logged files]
#export ARGS="5 10 20 10000 10000000 42 ${FILE_PREFIX}"
export ARGS="5 10 20 10000 10000 42 ${FILE_PREFIX}" # the file prefix is inserted as an 'extra'
export TIMEOUT=10:00
export BEO_TIMEOUT=20m
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-grain-parser.sh

PARAM="GRAIN"
VALUES="4 10 20 40 100 200 400 1000 2000"
