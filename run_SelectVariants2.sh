PATH=/vol/r-2.15/bin:$PATH
export PATH

LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/ java -d64 -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ -Djava.io.tmpdir=/vol/codine-tmp -jar /prj/gf-nugget/bin/Queue-3.2-2/Queue.jar  -R /prj/gf-nugget/processed-data/Assemblies/beet/RefBeet-1.2/RefBeet-1.2.joined.fa  -S /prj/gf-nugget/scripts/SelectVariants2.scala -V Variations.vcf -run --start_from_scratch -jobResReq vf=12g  -jobRunner GridEngine  --job_parallel_env multislot  -jobNative " -v PATH=/vol/biotools/bin:/vol/r-2.15/bin: -v  LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/: -l arch=lx24-amd64"
