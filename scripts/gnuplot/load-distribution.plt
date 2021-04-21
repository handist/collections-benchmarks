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
set yrange [0:*]
set xlabel " "
set ylabel "uts nodes traversed"

# plot DATA u 2 title "elapsed time" with lines, 
plot for [i=1:HOSTS] DATA u 2+HOSTS+i:xtic(1) title sprintf("%s%i", "h", i-1) axes x1y1
replot