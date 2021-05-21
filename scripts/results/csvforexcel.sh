#!/bin/bash



HOST="ofp4"
PROGRAMS="hugekmeansflat hugekmeanstriangle"
CONFIGS="NoLifeline-nolongpacking Hypercube-nolongpacking lifelinenoanswerafterintraLB Hypercube-bugfix"

for prog in ${PROGRAMS}
do
	# Produce a dedicated CSV for each program which concatenates the result of
	# every config chosen
	OUT=FOR_EXCEL_${prog}.csv
	
	echo "Config; Grain; Iteration; Elapsed Time(ms);" > ${OUT}

	for cfg in ${CONFIGS}
	do
		FILE=${HOST}_${cfg}_${prog}.csv
		cat ${FILE} | grep ";Iter "| sed "s/^/${cfg};/" >> ${OUT}
	done
done
