# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="teamedkmeans3"
export MAIN="handist.noglb.kmeans.KMeans"
# There are 5 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <chunk size> <nb of chunks> [seed]
export ARGS="5 2000 30 10000000 2000 42"
export TIMEOUT=20:00
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=kmeans-parser.sh
