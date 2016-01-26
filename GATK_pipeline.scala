/*
* Copyright (c) 2012 The Broad Institute
* 
* Permission is hereby granted, free of charge, to any person
* obtaining a copy of this software and associated documentation
* files (the "Software"), to deal in the Software without
* restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following
* conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
* OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.broadinstitute.gatk.queue.qscripts.examples
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils._
import org.broadinstitute.gatk.queue.extensions.picard.MarkDuplicates
import org.broadinstitute.gatk.tools.walkers.varianteval.stratifications.VariantType
import org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils


class pipeline extends QScript {
  // Create an alias 'qscript' to be able to access variables
  // in the pipeline.
  // 'qscript' is now the same as 'pipeline.this'
  qscript =>





  // Required arguments.  All initialized to empty values.

  /*
  ** Alle dieser Argumente, die einen "shortName" haben, sind Input zum initialen GATK Aufruf.
  * All these arguments have to be defined with the initial call of the script:
  */
  @Input(doc="The reference file for the bam files.", shortName="R")
  var referenceFile: File = _ // _ is scala shorthand for null

  @Input(doc="One or more bam files.", shortName="I")
  var bamFiles: List[File] = Nil

  @Input(doc="An optional file with a list of intervals to proccess.", shortName="L", required=false)
  var intervals: File = _

  // This trait allows us set the variables below in one place,
  // and then reuse this trait on each CommandLineGATK function below.
  trait GATK_pipeline extends CommandLineGATK {
    this.reference_sequence = qscript.referenceFile
    this.intervals = if (qscript.intervals == null) Nil else List(qscript.intervals)
    // Set the memory limit to 4 gigabytes on each command.
    this.memoryLimit = 4

  }

  def script() {
    // Create the four functions that we may run depending on options.
  

// define, what a SNP and an InDel is. Needed for SelectVariants
    var LTypeSelect: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LTypeSelect :+= htsjdk.variant.variantcontext.VariantContext.Type.SNP

    var LtypeSelect2: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LtypeSelect2 :+= htsjdk.variant.variantcontext.VariantContext.Type.INDEL

    


    var realignedFiles: List[File] = Nil



// deduplication and indel realignement for each file separately
    if (bamFiles.size >= 1) {
    for (bamFile <- bamFiles) {

// deduplication and calculating metrics
    val dedup = new MarkDuplicates  
    dedup.input :+= bamFile
    dedup.output =  swapExt(bamFile, "bam", "dedup.bam")
    dedup.metrics = swapExt(bamFile, "bam", "dedup.metrics")
    dedup.REMOVE_DUPLICATES = true
    dedup.isIntermediate = true
    dedup.memoryLimit = 4 // seems to be most stable with 4G of mem, not more
    dedup.jobResourceRequests = Seq("vf=4g")

//    dedup.analysisName = queueLogDir + outBam + ".dedup"
//    dedup.jobName = queueLogDir + outBam + ".dedup"


// InDel realignement
    val realigner = new RealignerTargetCreator with GATK_pipeline
    realigner.input_file :+= dedup.output
    realigner.scatterCount = 100
    realigner.nt = 4
    realigner.memoryLimit = 12 
    realigner.jobResourceRequests = Seq("vf=48g")

    realigner.out = swapExt(bamFile, "bam", "recalibrationTargets.intervals")

    val indelAligner = new IndelRealigner with GATK_pipeline
    indelAligner.input_file :+= dedup.output
    indelAligner.targetIntervals = realigner.out
    indelAligner.scatterCount = 20
    indelAligner.memoryLimit = 4
    indelAligner.jobResourceRequests = Seq("vf=4g")

    indelAligner.out = swapExt(bamFile, "bam", "realigned.bam")
    indelAligner.isIntermediate = true



    realignedFiles :+= indelAligner.out

    add(dedup, realigner, indelAligner) }
    }


// from here on the files are processed together
   
// calling and hardfiltering SNPs for BQRS

    val brhc = new HaplotypeCaller with GATK_pipeline

    brhc.scatterCount = 250
    brhc.nct = 4
    brhc.memoryLimit = 32
    brhc.input_file ++= realignedFiles
    brhc.jobResourceRequests = Seq("vf=32g")
    brhc.out = "recalibration_HaplotypeCallerVariations.vcf"

    add(brhc)

    val selectSNPsHC = new SelectVariants with GATK_pipeline
    selectSNPsHC.variant =  brhc.out
    selectSNPsHC.nt = 4
    selectSNPsHC.scatterCount = 20
    selectSNPsHC.select =  Seq("QD > 2.0 && FS < 60.0 && MQ > 40.0 && MQRankSum > -12.5 && ReadPosRankSum > -8.0" ) 
    selectSNPsHC.selectTypeToInclude = LTypeSelect
    selectSNPsHC.restrictAllelesTo =  org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectSNPsHC.out = "recalibrationSNPS.vcf"
    add(selectSNPsHC)
 
// first round BQRS
    val baseRecal = new BaseRecalibrator with GATK_pipeline

    baseRecal.input_file ++= realignedFiles
    baseRecal.knownSites = Seq(selectSNPsHC.out)
    baseRecal.scatterCount = 200
    baseRecal.memoryLimit = 4
    baseRecal.nct = 4
    baseRecal.jobResourceRequests = Seq("vf=4g")
    baseRecal.out = "recalibration_report.grp"

    add(baseRecal)

//post base recalibration

    val post_baseRecal = new BaseRecalibrator with GATK_pipeline

    post_baseRecal.input_file ++= realignedFiles
    post_baseRecal.knownSites = Seq(selectSNPsHC.out)
    post_baseRecal.scatterCount = 200
    post_baseRecal.memoryLimit = 4
    post_baseRecal.nct = 4
    post_baseRecal.BQSR = baseRecal.out
    post_baseRecal.jobResourceRequests = Seq("vf=4g")
    post_baseRecal.out = "post_recalibration_report.grp"

    add(post_baseRecal)

    //plotting effect of baserecalibration
    val AnaCovar = new  AnalyzeCovariates with GATK_pipeline
    AnaCovar.before = baseRecal.out
    AnaCovar.after = post_baseRecal.out
    AnaCovar.plots = "recalibration_plots.pdf"

    add(AnaCovar)
    
    
    // on-the-fly baserecalibration
  
    val printReads = new PrintReads with GATK_pipeline

    printReads.input_file ++= realignedFiles
    printReads.BQSR = baseRecal.out
    printReads.scatterCount = 20
    printReads.nct = 8
    printReads.memoryLimit = 4
    printReads.jobResourceRequests = Seq("vf=4g")

    printReads.out = "recalibrated.bam"

    var recalibratedFile: List[File] = Nil
    recalibratedFile :+= printReads.out

    add(printReads)    




// calling and filtering of variants

    val hc = new HaplotypeCaller with GATK_pipeline

    hc.scatterCount = 500
    hc.nct = 4
    hc.memoryLimit = 32
    hc.jobResourceRequests = Seq("vf=32g")

    hc.input_file = List[File](printReads.out)
    hc.out = "HaplotypeCallerVariations.vcf"
    add(hc)

    val selectSNPsHC2 = new SelectVariants with GATK_pipeline
    selectSNPsHC2.variant =  hc.out
    selectSNPsHC2.nt = 4
    selectSNPsHC2.scatterCount = 20
    selectSNPsHC2.select =  Seq("QD > 2.0 && FS < 60.0 && MQ > 40.0 && MQRankSum > -12.5 && ReadPosRankSum > -8.0" ) 
    selectSNPsHC2.selectTypeToInclude = LTypeSelect
    selectSNPsHC2.restrictAllelesTo =  org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectSNPsHC2.out = "HaplotypeCaller_biallelic_true_SNPS.qual.filtered.vcf"
    add(selectSNPsHC2)
 
    val selectIndelsHC = new SelectVariants with GATK_pipeline
    selectIndelsHC.variant =  hc.out
    selectIndelsHC.scatterCount = 20
    selectIndelsHC.select = Seq("QD > 2.0 && FS < 200.0 && ReadPosRankSum > -8.0")
    selectIndelsHC.selectTypeToInclude = LtypeSelect2
    selectIndelsHC.restrictAllelesTo = org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectIndelsHC.out = "HaplotypeCaller_biallelic_true_INDELS.qual.filtered.vcf"
    add(selectIndelsHC)


    var variantFiles: List[File] = Nil
    variantFiles :+= selectSNPsHC2.out
    variantFiles :+= selectIndelsHC.out



    val combineVars = new CombineVariants with GATK_pipeline
    combineVars.variant ++= variantFiles
    combineVars.genotypeMergeOptions = GATKVariantContextUtils.GenotypeMergeType.UNSORTED
    combineVars.scatterCount = 20
    combineVars.out = "CombineVariants_merged.vcf"
    add(combineVars)
 



  
  }
}
