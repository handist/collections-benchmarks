# Configuration for the Moldyn benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="moldynhybrid"
export MAIN="handist.moldyn.MoldynHybrid"
# There are 4 compulsory arguments and 1 optional:
# <datasize index(0or1)> <number of workers> <number of split>
export ARGS="1 ${WORKERS} ${SPLIT} ${HOST}_hybrid_${WORKERS}.csv"
export TIMEOUT=5:00
export BEO_TIMEOUT=5m
export REPETITIONS=5

# Script used to combine the results of this benchmark
#PARSER=kmeans-parser.sh
