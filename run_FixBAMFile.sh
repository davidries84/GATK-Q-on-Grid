# shell script for running the whole pipeline from bam files to quality filtered vcf files.
# This is normaly what you want to do, because it is the easiest way. Input files and paths have to be set correctly, see README.

# clear environment
PATH=/vol/java-7/bin:/vol/python-2.7/bin:/vol/staden-2.0/bin:/vol/biotools/bin:/vol/gnu/bin:/vol/local/bin:/vol/X11/bin:/usr/bin:/bin:/usr/local/bin:/vol/codine-6.2/bin/lx24-amd64
LIBATH=
JAVA_HOME=

export PATH
export LIBATH
export JAVA_HOME


# run Queue
# parameters have to be set according to your files and system
LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/ java -d64 -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ -Djava.io.tmpdir=/vol/codine-tmp -jar  /prj/gf-rhizo/bin/Queue-3.2-2/Queue.jar   -S /prj/gf-rhizo/data/scripts/GATK-Q-on-Grid/FixBAMFile.scala  -I   -jobRunner GridEngine   --job_parallel_env multislot  -jobResReq vf=5g -retry 1 -run  -jobNative " -v PATH=/vol/biotools/bin:/vol/r-2.15/bin: -v  LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/: -l arch=lx24-amd64 -l idle=1"
