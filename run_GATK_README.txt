Anleitung zum starten von GATK-Queue Jobs auf dem BRF-cluster (GridEngine)

Die Queue-scipte sind in scala geschrieben (z. Bsp. "HelloWorld.scala").
Queue wird verwendet um diese scripte zu starten und die Jobs auf den Cluster zu verteilen.
Queue selbst wird wiederum mit shell scripten (.sh) gestarten. Diese müssen korrekt eingerichtet sein, 
um den cluster nicht übermässig zu belasten, z.B. durch unnötig große environments.
Das environment ist alles, was in der PATH Variable steht, und somit auf jedem Rechner durchsucht wird.
Je mehr im PATH, desto mehr Ordner werden in jedem submitierten Job durchsucht.
Der PATH muss also alles enthalten, was die scripte brauchen um zu laufen, aber nicht mehr.
 
# zu beginn speichern wir im PATH genau nur:

PATH=/vol/r-2.15/bin:/vol/java-7/bin:/vol/python-2.7/bin:/vol/staden-2.0/bin:/vol/biotools/bin:/vol/gnu/bin:/vol/local/bin:/vol/X11/bin:/usr/bin:/bin:/usr/local/bin:/vol/codine-6.2/bin/lx24-amd64

# und leeren :
LIBATH=
JAVA_HOME=

export PATH
export LIBATH
export JAVA_HOME


#Eine notwendige library ist "libdrama.so". Diese liegt unter "/vol/codine-6.2/lib/[architektur]/"
# Der noetige Pfad wird unter Linux mit -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ mitgegeben , also folgendes zum einrichten das Java environments unter  lqxterm,
# bzw. wenn man von einem linux rechner aus startet:
LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/ java -d64 -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ -Djava.io.tmpdir=/vol/codine-tmp


# Der naechste Teil bezieht sich auf Queue. Diesem wird ein "script.scala" uebergeben. 
# Da wir uns auf der SGE befinden, wird der GridEngine Jobrunner verwendet
# -jobRunner GridEngine
# mit -jobNative uebergeben wir schlussendlich noch die Umgebungsvariablen, die wir normalerweise bei qsub mitggegeben haetten: 
# -v PATH=/vol/biotools/bin:/vol/r-2.15/bin: 
# -v  LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/: 
# und -l arch=lx24-amd64, da wir GATK wiederum (ausschliesslich) auf Linux Maschienen laufen lassen wollen.
# mit -jar "Pfad zu Queue" geben wir den Ort an, wo der Queue java file liegt
# mit -S "scalaScipt.scala" das zu startende script


# vollständig also:
LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/ java -d64 -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ -Djava.io.tmpdir=/vol/codine-tmp -jar queue/Queue_2.7.2/Queue.jar -S ~/Downloads/HelloWorld.scala -jobRunner GridEngine -run -startFromScratch  -jobNative " -v PATH=/vol/biotools/bin:/vol/r-2.15/bin: -v  LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/: -l arch=lx24-amd64"

# Zur Erläuterung, warum zweimal die PATH Variable modifiziert wird:
# Zuerst muss der PATH korrekt angegeben werden, für den Rechner von dem wir starten. Danach aber auch noch einmal für die Rechner, auf denen dann das script
# tatsächlich verteilt laufen wird. Da Queue zusätzlich zu mit "-v" mitgegebenen Parametern auch alles übergeben bekommt, was beim submittieren im Pfad steht,
# muss der PATH ganz explizit schon vor dem submitieren möglichst leer sein.


# Durch aufrufen des sh files durch "sh run_GATK-script.sh" von einem linux rechner aus startet man dann die Queue.


Folgend ein Beispiel für einen vollständigen file:

##################################
PATH=/vol/java-7/bin:/vol/python-2.7/bin:/vol/staden-2.0/bin:/vol/biotools/bin:/vol/gnu/bin:/vol/local/bin:/vol/X11/bin:/usr/bin:/bin:/usr/local/bin:/vol/codine-6.2/bin/lx24-amd64
LIBATH=
JAVA_HOME=

export PATH
export LIBATH
export JAVA_HOME



LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/ java -d64 -Djava.library.path=/vol/codine-6.2/lib/lx24-amd64/ -Djava.io.tmpdir=/vol/codine-tmp -jar  /prj/gf-nugget/bin/Queue-3.2-2/Queue.jar   -S /prj/gf-nugget/scripts/GATK_pipeline.scala -R /prj/gf-nugget/processed-data/Assemblies/beet/RefBeet-1.2/RefBeet-1.2.joined.fa  -I red_merged.bam -I green_merged.bam  -jobRunner GridEngine   --job_parallel_env multislot  -jobResReq vf=5g -retry 1   -jobNative " -v PATH=/vol/biotools/bin:/vol/r-2.15/bin: -v  LD_LIBRARY_PATH=/vol/codine-6.2/lib/lx24-amd64/: -l arch=lx24-amd64"

#################################


