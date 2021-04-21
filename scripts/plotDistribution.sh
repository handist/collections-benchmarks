#!/bin/bash

CONFIGS="NoLifeline Hypercube"
HOSTS="4 16"
PROBLEM="ufs-small ufs-large ufs-sfbt8 ufs-sfbt9 ufs-sfbt10 ufs-sfbt11 ufs-sfbt11v2 ufs2-small ufs2-large ufs2ub-small ufs2ub-large"

echo "In directory `pwd`"

# Make the directory in which all plots will be placed
mkdir -p plots

for cfg in $CONFIGS
do
    for hst in $HOSTS
    do
        for pb in $PROBLEM
        do

        FILES_TO_PARSE=`ls ofp${hst}/${cfg}/${pb}/*.out.txt`
        for file in ${FILES_TO_PARSE}
        do
            PREFIX=${file/out.txt/png}
            SUFFIX=${PREFIX#ofp${hst}/${cfg}/${pb}/}
            OUTFILE="plots/ofp${hst}_${SUFFIX}"

            if [[ -f ${OUTFILE} ]]
            then
                echo "Overwriting ${OUTFILE}"
            else
                echo "Creating file ${OUTFILE}"
            fi
            cat ${file} | sed '/^Starting\b/d' > data.csv
            gnuplot -c gnuplot/load-distribution.plt ${hst} data.csv > $OUTFILE

        done
        done
    done
done

# Remove temporary file
rm -f data.csv
