PATH=/vol/java-7/bin:/vol/python-2.7/bin:/vol/staden-2.0/bin:/vol/biotools/bin:/vol/gnu/bin:/vol/local/bin:/vol/X11/bin:/usr/bin:/bin:/usr/local/bin:/vol/codine-6.2/bin/lx24-amd64
LIBATH=
JAVA_HOME=

export PATH
export LIBATH
export JAVA_HOME


LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/ java -d64 -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ -Djava.io.tmpdir=/vol/codine-tmp -jar /prj/gf-nugget/bin/Queue-3.2-2/Queue.jar  -R /prj/gf-nugget/processed-data/Assemblies/ath/TAIR10/TAIR10.fa  -S /prj/gf-nugget/scripts/MergeBAM.scala -I file.bam -run --start_from_scratch -jobResReq vf=12g  -jobRunner GridEngine  --job_parallel_env multislot  -jobNative " -v PATH=/vol/biotools/bin:/vol/r-2.15/bin: -v  LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/: -l arch=lx24-amd64"
