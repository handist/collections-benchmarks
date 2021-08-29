# Configuration for the Moldyn benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher-nompi-iter.sh"
export BENCHMARK_NAME="moldynseqmt-split16"
export MAIN="handist.moldyn.MoldynSeqMT"
# There are 4 compulsory arguments and 1 optional:
# <datasize index(0or1or2)> <number of split> <number of workers>
export ARGS="1 16"
PARAM=WORKER
VALUES=(1 4 16 64)

export TIMEOUT=50:00
export BEO_TIMEOUT=5m
export REPETITIONS=2
# Script used to combine the results of this benchmark
#PARSER=kmeans-parser.sh
