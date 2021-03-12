#!/bin/bash

timeout -k ${TIMEOUT} -s 9 ${TIMEOUT} \
taskset -ca ${CORE_RESTRICTION} \
mpirun -n ${NB_HOSTS} --hostfile ${HOSTFILE} \
java -classpath ${COLLECTIONS_LIBRARY}:${APGAS_LIBRARY_AND_DEPENDANCIES}:${BENCHMARK_LIBRARY} \
-Djava.library.path=${JAVALIBRARYPATH} \
-Dglb.grain=${GRAIN} \
-Dglb.lifeline=${LIFELINE} \
-Dglb.serialization=${SERIALIZATION} \
handist.collections.launcher.Launcher \
${MAIN} ${ARGS}
