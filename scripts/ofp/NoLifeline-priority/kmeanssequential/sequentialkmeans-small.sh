# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="kmeanssequential"
export MAIN="handist.kmeans.KMeansSequential"
# There are 5 compulsory arguments and 1 optional:
# <point dimension> <nb of clusters "k"> <repetitions> <chunk size> <number of points> [seed]
export ARGS="5 10 20 10000 10000000 42"
export TIMEOUT=10:00
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=kmeans-parser.sh
