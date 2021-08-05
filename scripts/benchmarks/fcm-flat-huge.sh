# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-oneparam.sh"
export MAIN="handist.noglb.fuzzykmeans.FuzzyKMeans"
export BENCHMARK_NAME="fcm-flat-huge"
# There are 6 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <fuzziness m> <nb iterations> <chunk size> <number of points> [seed]
export ARGS="5 100 2.0 20 10000 10000000 42"
export TIMEOUT=15:00
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-grain-parser.sh

PARAM="GRAIN"
VALUES="4 10 20 40 100 200 400 1000 2000"
