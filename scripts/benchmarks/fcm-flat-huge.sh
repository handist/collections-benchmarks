# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-oneparam.sh"
export BENCHMARK_NAME="fcm-huge"
export MAIN="handist.noglb.fuzzykmeans.FuzzyKMeans"
# There are 6 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <fizziness m> <repetitions> <chunk size> <number of points> [seed]
export ARGS="5 10000 2.0 20 10000 10000000 42"
export TIMEOUT=10:00
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-grain-parser.sh

PARAM="GRAIN"
VALUES="4 10 20 40 100 200 400 1000 2000"
