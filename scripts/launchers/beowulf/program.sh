#!/bin/bash

timeout -k ${BEO_TIMEOUT} -s 9 ${BEO_TIMEOUT} \
taskset -ca ${CORE_RESTRICTION} \
mpirun -n ${NB_HOSTS} --hostfile ${HOSTFILE} \
java -classpath ${COLLECTIONS_LIBRARY}:${DEPENDENCIES}/* \
-Djava.library.path=${JAVALIBRARYPATH} \
-Dglb.grain=${GRAIN} \
-Dglb.lifeline=${LIFELINE} \
        -Dglb.trace=${TRACE} \
        -Dglb.serialization=${SERIALIZATION} \
handist.collections.launcher.Launcher 0 0 native \
${MAIN} ${ARGS} ${EXTRA_ARGS}
