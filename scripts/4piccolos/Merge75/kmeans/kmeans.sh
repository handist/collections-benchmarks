# Configuration for the K-Means benchmark
export BENCHMARK_NAME="kmeans"
export MAIN="handist.kmeans.KMeans"
# There are 5 compulsory arguments and 1 optional:
# <dimension> <number of clusters> <nb of iterations> <number of chunks> <size of individual chunks> [seed]
export ARGS="3 5 30 1000 10000"
export TIMEOUT=5m
export REPETITIONS=5
