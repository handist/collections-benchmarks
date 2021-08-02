for grain in 4 10 40 100 20 200 400 2000 1000
do
    mkdir grain${grain}
    mv logger-nolifeline_logged-small-flat_GRAIN${GRAIN}_Run1_*.glblog grain${grain}
done
