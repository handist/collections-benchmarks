# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="ufs-small"
export MAIN="handist.ufs.UFS"

#Arguments are:
#<forest size>                  the number of trees in this Unbalanced Forest Search
#<rounds>                       the number of rounds of the UFS
#<tree depth>                   maximum depth of each tree in the forest
#<seed>            (optional)   the random seed used to initialize the workers
#<tree per chunk>  (optional)   number of trees per chunk in the underlying collection used to contain the forest

export ARGS="100000 15 6 42 1000"
export TIMEOUT=30:00
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=ufs-parser.sh

