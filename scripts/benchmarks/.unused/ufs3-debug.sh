# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="ufs3-debug"
export MAIN="handist.ufs.UFS3"

#Arguments are:
#<forest size>                  the number of trees in this Unbalanced Forest Search
#<local forest size>            the number of trees in the non-movable forest located on host 0
#<rounds>                       the number of rounds of the UFS
#<tree depth>                   maximum depth of each tree in the forest
#<local tree depth>             maximum depth of each tree in the forest local to host 0
#<seed>            (optional)   the random seed used to initialize the workers
#<tree per chunk>  (optional)   number of trees per chunk in the underlying collection used to contain the forest

export ARGS="10000 1000 10 5 5 42 10"
export TIMEOUT=5m
export REPETITIONS=1
export GRAIN=3

# Script used to combine the results of this benchmark
PARSER=ufs-parser.sh

