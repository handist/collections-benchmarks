# Configuration for the Moldyn benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-nompi.sh"
export BENCHMARK_NAME="moldynseq"
export MAIN="handist.moldyn.MoldynSeq"
# There are 4 compulsory arguments and 1 optional:
# <datasize index(0or1or2)>
export ARGS="1"
export TIMEOUT=50:00
export BEO_TIMEOUT=5m
export REPETITIONS=2
# Script used to combine the results of this benchmark
#PARSER=kmeans-parser.sh
