#!/bin/bash

find . -type f -name '*Run*.out.txt' -exec tail -n 15 {} \; 
