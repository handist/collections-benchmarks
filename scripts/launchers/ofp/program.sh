#!/bin/bash
#PJM -g gp43

export MPJ_HOME=/work/gp43/share/mpj-v0_44
export I_MPI_PIN=0

PROC=${PJM_MPI_PROC}
if [ -n "$NB_PROCS" ]; then
 PROC=${NB_PROCS}
fi

mpiexec.hydra -n ${PROC} \
taskset -ca ${CORE_RESTRICTION} \
java -cp ${COLLECTIONS_LIBRARY}:${DEPENDENCIES}/* \
-Xms40g -Xmx80g \
-Djava.library.path=${JAVALIBRARYPATH} \
-Dglb.grain=${GRAIN} \
-Dglb.lifeline=${LIFELINE} \
-Dglb.serialization=${SERIALIZATION} \
-Dglb.trace=${TRACE} \
handist.collections.launcher.Launcher 0 0 native \
${MAIN} ${ARGS} ${EXTRA_ARGS}
