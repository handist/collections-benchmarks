#!/bin/bash

#PJM group
#TODO add fixed settings for OFP

taskset -ca ${CORE_RESTRICTION} \
        mpiexex.hydra -n ${NB_HOSTS} \
        java --classpath ${COLLECTIONS_LIBRARY}:${BENCHMARK_LIBRARY} \
        -Dglb.grain=${GRAIN} \
        -Dglb.lifeline=${LIFELINE} \
        -Dglb.serialization=${SERIALIZATION} \
        ${MAIN} ${ARGS}
