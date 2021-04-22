# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="ufs-debug"
export MAIN="handist.ufs.UFS"

#Arguments are:
#<forest size>                  the number of trees in this Unbalanced Forest Search
#<rounds>                       the number of rounds of the UFS
#<tree depth>                   maximum depth of each tree in the forest
#<seed>            (optional)   the random seed used to initialize the workers
#<tree per chunk>  (optional)   number of trees per chunk in the underlying collection used to contain the forest

export ARGS="10000 10 5 42 100"
export TIMEOUT=5m
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=ufs-parser.sh

