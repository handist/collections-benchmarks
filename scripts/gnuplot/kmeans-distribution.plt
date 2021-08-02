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

set xtics rotate by 45 nomirror offset -5,-2.5,0
set yrange [0:20000]
set ytics nomirror
set y2range [0:6000000]

set y2tics 1000000
set xlabel " "
set ylabel "iteration time (ms)"
set y2label "number of points per host"

plot DATA u 2 title "iteration time" with lines, \
 DATA u 6:xtic(1) title "Place 0 load" axes x1y2, \
 DATA u 7:xtic(1) title "Place 1 load" axes x1y2, \
 DATA u 8:xtic(1) title "Place 2 load" axes x1y2, \
 DATA u 9:xtic(1) title "Place 3 load" axes x1y2