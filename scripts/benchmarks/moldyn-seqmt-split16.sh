# Configuration for the Moldyn benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-nompi.sh"
export BENCHMARK_NAME="moldynseqmt"
export MAIN="handist.moldyn.MoldynSeqMT"
# There are 4 compulsory arguments and 1 optional:
# <datasize index(0or1or2)> <number of workers> <number of split>
export ARGS="1 ${WORKERS} 16"
export TIMEOUT=5:00
export BEO_TIMEOUT=5m
export REPETITIONS=2
# Script used to combine the results of this benchmark
#PARSER=kmeans-parser.sh
