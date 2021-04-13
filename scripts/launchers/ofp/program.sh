#!/bin/bash
#PJM -g gp43

export MPJ_HOME=/work/gp43/share/mpj-v0_44
export I_MPI_PIN=0

mpiexec.hydra -n ${PJM_MPI_PROC} \
taskset -ca ${CORE_RESTRICTION} \
java -cp ${COLLECTIONS_LIBRARY}:${DEPENDENCIES}/* \
-Djava.library.path=${JAVALIBRARYPATH} \
-Dglb.grain=${GRAIN} \
-Dglb.lifeline=${LIFELINE} \
-Dglb.serialization=${SERIALIZATION} \
handist.collections.launcher.Launcher 0 0 native \
${MAIN} ${ARGS}
