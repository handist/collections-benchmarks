#!/bin/bash

# For the Kmeans program, we simply remove the initial MPJ messages about host configuration and concatenate all the std output message
find . -type f -name '*Run*.out.txt' -exec cat {} \; | sed /Starting/d | sed /nohup/d
