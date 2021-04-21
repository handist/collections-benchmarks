# This gnuplot scripty takes as input the table given by a UFS run
###################################################################
# ARG1: number of hosts
# ARG2: input file

HOSTS = ARG1
DATA = ARG2

reset
set terminal pngcairo size 1000,700 enh font ",18"

set datafile separator ";"
set style data histogram
set style histogram cluster gap 3

set key autotitle columnhead

set xtics rotate by 45 nomirror offset -5,-2.5,0
set yrange [0:4500]
set y2range [0:1e9]
set xlabel " "
set ylabel "elapsed time (ms)"
set ytics nomirror
set y2tics nomirror

plot DATA u 2:xticlabels(1) title columnheader axes x1y1 with lines, \
     '' u (column(HOSTS+4)):xticlabels(1) title "unmovable" axes x1y2 fillstyle solid, \
     '' u (column(HOSTS+4)+column(HOSTS+5)):xticlabels(1) title "h0 nodes" axes x1y2, \
     for [i=2:HOSTS] '' u 4+HOSTS+i:xticlabels(1) title columnheader axes x1y2

