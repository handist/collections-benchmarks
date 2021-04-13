#!/bin/bash

# For the Kmeans program, we simply remove the initial MPJ messages about host configuration and concatenate all the std output message
echo "${PARAM};Step;ElapsedTime"
for value in ${VALUES}
do
    cat *${PARAM}${value}_*.out.txt | sed /Starting/d | sed /nohup/d | sed "s/^/${value};/"
#    find . -type f -name '*Run*.out.txt' -exec cat {} \; | sed /Starting/d | sed /nohup/d
done
