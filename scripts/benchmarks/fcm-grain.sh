# Configuration for the K-Means benchmark
export BENCHMARK_NAME="fcm-grain"
export MAIN="handist.noglb.fuzzykmeans.FuzzyKMeans"
# There are 6 compulsory arguments and 1 optional:
# <dimension> <number of clusters k> <fuzziness m> <nb of iterations> <number of chunks> <size of individual chunks> [seed]
export ARGS="3 5 2.0 30 40 1000 42"
export TIMEOUT=5:00
export BEO_TIMEOUT=10m
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-grain-parser.sh

# OVERRIDE OF VARIABLES SET BY OTHER SCRIPTS
# Use the "one parameter" launcher
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-oneparam.sh"
PARAM="GRAIN"
VALUES="4 10 20 40 100 200 1000 10000"
#VALUES="10 1000 10000"
