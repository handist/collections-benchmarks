#!/bin/bash

timeout -k ${BEO_TIMEOUT} -s 9 ${BEO_TIMEOUT} \
taskset -ca ${CORE_RESTRICTION} \
java -classpath ${COLLECTIONS_LIBRARY}:${DEPENDENCIES}/* \
-Djava.library.path=${JAVALIBRARYPATH} \
-Dglb.grain=${GRAIN} \
-Dglb.lifeline=${LIFELINE} \
        -Dglb.trace=${TRACE} \
        -Dglb.serialization=${SERIALIZATION} \
${MAIN} ${ARGS} ${EXTRA_ARGS}
