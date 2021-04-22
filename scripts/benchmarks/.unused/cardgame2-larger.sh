# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="cardgame2-larger"
export MAIN="handist.cardgame.CardGame2"

#Arguments are:
#<players>                 the number of players participating in the game
#<nb of chunks>            number of chunks in which the players are spread
#<rounds>                  the number of rounds in the game
#<player weight>           base computation weight of the players
#<player increased weight> computation weight of players that have won a round
#<nb of winners>           minimum number of players that win each round and suffer the increased computation weight in the next round
#<seed>                    the random seed used to initialize the workers
export ARGS="100000 10 20 6 8 3000 42"
export TIMEOUT=5m
export REPETITIONS=5

# Script used to combine the results of this benchmark
PARSER=

