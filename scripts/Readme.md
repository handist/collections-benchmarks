# handistCollection benchmark execution framework

## 1. Introduction

This part of the repository is prepared to easily allow for new environments, library versions and their settings, and benchmarks to be added.

### 1.1 Benchmark configuration

The execution of the benchmarks is managed by three different scripts:

+ "host script": define general parameters related to the execution environment
+ "library configuration script": define the library version considered as well as some settings
+ "benchmark script": define the main and the parameters of the program to launch as a benchmark

The script `launchBenchamrks.sh` takes a "host script" as parameter and will launch the various benchmarks and library configurations specified inside this script.

#### Host configurations

The configuration files placed in sub-directory `hosts` contain configuration general to a particular execution environment. These files also define which configuration and which benchmark should be run for a particular host.
The program launcher also needs to be specified, that is, should benchmark be launched directly (as is the case for a localhost execution or a beowulf cluster) or does a job submission program have to be used (as is the case for a supercomputer environment).

Example: hosts/localhost.sh

```
################################################################################
# Name of this configuration and path to library JAR                           #
#------------------------------------------------------------------------------#
# These variables are used to identify the library / name of the version being #
# tested. They will be used to create directories and name files accordingly   #
################################################################################
export HOST=`hostname` # This makes a directory with your actual host name
export PROGRAM_LAUNCHER="${BENCH_HOME}/beowulfLauncher.sh" # beowulf launcher: direct execution of benchmarks

# All the benchmarks that we want to run with this host
export BENCHMARKS="kmeans.sh" # these scripts are expected to be placed in directory "benchamrks"
# All the configurations that we want to test with this host
export CONFIGS="merge75-config.sh" # these scripts are expected to be placed in directory "configurations"

# Path to the MPI-binding libraries for this host
export JAVALIBRARYPATH="${MPJ_HOME}/lib"
# Hostfile to use for this host
export HOSTFILE=${BENCH_HOME}/hostfile/localhost
# Number of hosts with which the benchmarks should run
export NB_HOSTS=1
# Number of concurrent workers (-Dglb.workers)
export WORKERS=3
# Core restriction (option of taskset -ca <core_restriction>)
export CORE_RESTRICTION="0-3"
```

When we want to test newer versions of the library or run additional benchmarks, the corresponding name of the scripts can be added to variables `BENCHMARKS` and `CONFIGS`.

If execution files from a previous `./launchBenchamrks` call are detected on the file system, re-launching the benchmarks will not override these files. Instead, the benchmark that have already been executed will be skipped.

#### Configuration scripts

The configuration files placed in directory `configurations` contain configurations that defines the distributed collections library version to use, as well as the configuration of that library

Example: configurations/merge75-config.sh

```
################################################################################
# Configuration for the collections library whose last commit was the merge    #
# from branch 75                                                               #
################################################################################
export CONFIG_NAME="Merge75"
export COLLECTIONS_LIBRARY=${BENCH_HOME}/jar/collections-merge75.jar
export DEPENDENCIES=${BENCH_HOME}/jar/deps
################################################################################
# Options and configurations of the handist collections library                #
#------------------------------------------------------------------------------#
# These variables will be used to set the appropriate -D<property>=<value>     #
# options to the Java process and other options and variables in the script    #
# used to launch the benchamrks.                                               #
################################################################################
# Granularity (-Dglb.grain=<grain>)
export GRAIN=100
# Lifeline strategy (-Dglb.lifeline=<lifeline>)
export LIFELINE="handist.collections.glb.lifeline.NoLifeline"
# Mode used to make lifeline answers (-glb.serialization=<serialization>)
SERIALIZATION="kryo"
```

Notice that the JAR to be used is described by its name in variable `COLLECTIONS_LIBRARY`.

If a single library version (i.e. the same JAR) needs to be tested with different settings, a new configuration script should be created. This new script can then use

#### Benchmark scripts

The configuration files under directory `benchmarks` define the benchmarks (main class and parameters). Of course, a single program (i.e. main class) can yield multiple benchmarks by changing the parameters in use (size of the input data ...)

Example: benchamrks/kmeans.sh

```
# Configuration for the K-Means benchmark
export BENCHMARK_NAME="kmeans"  # name used to create the directory
export MAIN="handist.kmeans.KMeans" # main class
# There are 5 compulsory arguments and 1 optional:
# <dimension> <number of clusters> <nb of iterations> <number of chunks> <size of individual chunks> [seed]
export ARGS="3 5 30 1000 10000"
export TIMEOUT=5m     # maximum time allowed to complete an execution
export REPETITIONS=5  # number of times the benchmark should be repeated
```

#### Other files and directories

The Jar files used by the benchmark library are placed. The various distributed collection library versions under consideration are placed in directory `jar`, while the common dependencies are placed in directory `jar/deps`.

Directory `hostfile` keeps the hostfiles used by the various hosts configurations.

## 2. Pre-prepared configurations

### 2.1 localhost

Run command `./launchBenchamrks hosts/localhost.sh`

This is meant to be used your own developer machine. A single host will be used for the benchmarks with a limit of 4 concurrent workers.

### 2.2 harp

Run command `./launchBenchmark hosts/harp.sh`

This is meant to launch the benchmarks on machine Harp of our HPCGATE cluster.

### 2.3 OakForest-PACS

Run command `./launchBenchmark hosts/ofp.sh`.

This will submit jobs using the `pjsub` command to be run on the OakForest-PACS supercomputer.
