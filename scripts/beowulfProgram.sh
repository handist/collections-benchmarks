#!/bin/bash

taskset -ca ${CORE_RESTRICTION} \
        mpirun -n ${NB_HOSTS} --hostfile {HOSTFILE} \ 
        java --classpath ${COLLECTIONS_LIBRARY}:${BENCHMARK_LIBRARY} \
        -Dglb.grain=${GRAIN} \
        -Dglb.lifeline=${LIFELINE} \
        -Dglb.serialization=${SERIALIZATION} \
        ${MAIN} ${ARGS}
