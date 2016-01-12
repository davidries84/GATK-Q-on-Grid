PATH=/vol/r-2.15/bin:$PATH
export PATH

LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/ java -d64 -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ -Djava.io.tmpdir=/vol/codine-tmp -jar /prj/gf-nugget/bin/Queue-2.8-1-g932cd3a/Queue.jar  -R /prj/gf-nugget/processed-data/Assemblies/beet/RefBeet-1.2/RefBeet-1.2.joined.fa  -S /prj/gf-nugget/scripts/DepthOfCoverage.scala -I Interval_Syn_KWS_Stru_green.bam   --start_from_scratch -jobResReq vf=12g  -jobRunner GridEngine  --job_parallel_env multislot -run  -jobNative " -v PATH=/vol/biotools/bin:/vol/r-2.15/bin: -v  LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/: -l arch=lx24-amd64"
