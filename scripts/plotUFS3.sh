#!/bin/bash

CONFIGS="NoLifeline-priority Hypercube-priority"
HOSTS="4 16"
PROBLEM="ufs4-small ufs4-small2 ufs4-small3 ufs5-small ufs6-small"

echo "In directory `pwd`"

# Make the directory in which all plots will be placed
mkdir -p ufs3plots

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
            OUTFILE="ufs3plots/ofp${hst}_${SUFFIX}"

            if [[ -f ${OUTFILE} ]]
            then
                echo "Overwriting ${OUTFILE}"
            else
                echo "Creating file ${OUTFILE}"
            fi
            cat ${file} | sed '/^Starting\b/d' > data.csv
            gnuplot -c gnuplot/ufs3.plt ${hst} data.csv > $OUTFILE

        done
        done
    done
done

# Remove temporary file
rm data.csv
