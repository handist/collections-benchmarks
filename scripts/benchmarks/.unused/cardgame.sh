# Configuration for the K-Means benchmark
PROGRAM_LAUNCHER="${BENCH_HOME}/launchers/${HOST_TYPE}/launcher.sh"
export BENCHMARK_NAME="cardgame-debug"
export MAIN="handist.cardgame.CardGame"

#Arguments are:
#<players>               the number of players participating in the game
#<rounds>                the number of rounds in the game
#<player weight>         base computation weight of the players
#<process wight>         base computation weight of processing the cards played by workers
#<player increased load> computation weight of players that have won a round
#<nb of winners>         minimum number of players that win each round and suffer the increased computation weight in the next round
#<seed>                  the random seed used to initialize the workers
export ARGS="1000 10 6 0 7 300 42"
export TIMEOUT=5m
export REPETITIONS=1

# Script used to combine the results of this benchmark
PARSER=

