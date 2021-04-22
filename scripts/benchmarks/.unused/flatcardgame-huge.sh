# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="flatcardgame-huge"
export MAIN="handist.cardgame.CardGame2"

#Arguments are:
#<players>                 the number of players participating in the game
#<nb of chunks>            number of chunks in which the players are spread
#<rounds>                  the number of rounds in the game
#<player weight>           base computation weight of the players
#<player increased weight> computation weight of players that have won a round
#<nb of winners>           minimum number of players that win each round and suffer the increased computation weight in the next round
#<seed>                    the random seed used to initialize the workers
export ARGS="1000000 100 20 6 6 3000 42"
export TIMEOUT=29m
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=

