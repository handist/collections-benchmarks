# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="ufs4-small3"
export MAIN="handist.ufs.UFS4"

#Arguments are:
#<forest size>                  the number of trees in this Unbalanced Forest Search
#<local forest size>            the number of trees in the non-movable forest located on host 0
#<rounds>                       the number of rounds of the UFS
#<tree depth>                   maximum depth of each tree in the forest
#<local tree depth>             maximum depth of each tree in the forest local to host 0
#<seed>            (optional)   the random seed used to initialize the workers
#<tree per chunk>  (optional)   number of trees per chunk in the underlying collection used to contain the forest

export ARGS="100000 20000 15 8 7 42 10"
export TIMEOUT=15m
export REPETITIONS=5
export GRAIN=4

# Script used to combine the results of this benchmark
PARSER=ufs-parser.sh

