# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="teamedkmeans"
export MAIN="handist.noglb.kmeans.KMeans"
# There are 5 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <chunk size> <nb of chunks> [seed]
export ARGS="3 50 30 10000000 2000 42"
export TIMEOUT=10:00
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=kmeans-grain-parser.sh
