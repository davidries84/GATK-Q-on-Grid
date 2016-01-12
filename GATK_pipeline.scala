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
//import org.broadinstitute.gatk.tools.walkers.genotyper.GenotypeLikelihoodsCalculationModel
import org.broadinstitute.gatk.queue.extensions.picard.MarkDuplicates
import  org.broadinstitute.gatk.tools.walkers.varianteval.stratifications.VariantType
//import net.sf.samtools.SAMFileReader.ValidationStringency

class ExampleUnifiedGenotyper extends QScript {
  // Create an alias 'qscript' to be able to access variables
  // in the ExampleUnifiedGenotyper.
  // 'qscript' is now the same as 'ExampleUnifiedGenotyper.this'
  qscript =>





  // Required arguments.  All initialized to empty values.

  /*
  ** Alle dieser Argumente, die einen "shortName" haben, sind Input zum initialen GATK Aufruf (Realigner)
  */
  @Input(doc="The reference file for the bam files.", shortName="R")
  var referenceFile: File = _ // _ is scala shorthand for null

  @Input(doc="One or more bam files.", shortName="I")
  var bamFiles: List[File] = Nil

  @Input(doc="An optional file with a list of intervals to proccess.", shortName="L", required=false)
  var intervals: File = _

  // This trait allows us set the variables below in one place,
  // and then reuse this trait on each CommandLineGATK function below.
  trait UnifiedGenotyperArguments extends CommandLineGATK {
    this.reference_sequence = qscript.referenceFile
    this.intervals = if (qscript.intervals == null) Nil else List(qscript.intervals)
    // Set the memory limit to 2 gigabytes on each command.
    this.memoryLimit = 2
    //this.validation_strictness = net.sf.samtools.SAMFileReader.ValidationStringency.LENIENT
    //this.validation_strictness = "LENIENT"
    
  }

  def script() {
    // Create the four functions that we may run depending on options.
    val genotyper = new UnifiedGenotyper with UnifiedGenotyperArguments
    val selectVars = new SelectVariants with UnifiedGenotyperArguments
    val baseRecal = new BaseRecalibrator with UnifiedGenotyperArguments
    val printReads = new PrintReads with UnifiedGenotyperArguments
    


    var realignedFiles: List[File] = Nil

    if (bamFiles.size >= 1) {
    for (bamFile <- bamFiles) {


    val dedup = new MarkDuplicates  
    dedup.input :+= bamFile
    dedup.output =  swapExt(bamFile, "bam", "dedup.bam")
    dedup.metrics = swapExt(bamFile, "bam", "dedup.metrics")
    dedup.REMOVE_DUPLICATES = true
    dedup.isIntermediate = true


    dedup.memoryLimit = 16
//    dedup.analysisName = queueLogDir + outBam + ".dedup"
//    dedup.jobName = queueLogDir + outBam + ".dedup"


    val realigner = new RealignerTargetCreator with UnifiedGenotyperArguments
    realigner.input_file :+= dedup.output
    realigner.scatterCount = 16
    realigner.nt = 4
    realigner.memoryLimit = 12 
    realigner.out = swapExt(bamFile, "bam", "recalibrationTargets.intervals")

    val indelAligner = new IndelRealigner with UnifiedGenotyperArguments
    indelAligner.input_file :+= dedup.output
    indelAligner.targetIntervals = realigner.out
    indelAligner.scatterCount = 4
    indelAligner.memoryLimit = 4
    indelAligner.out = swapExt(bamFile, "bam", "realigned.bam")
    indelAligner.isIntermediate = true
    realignedFiles :+= indelAligner.out




    add(dedup, realigner, indelAligner) }
    }


   


    genotyper.scatterCount = 16
    genotyper.nct = 3
    genotyper.nt = 2
    genotyper.memoryLimit = 16  // 32
    genotyper.input_file ++= realignedFiles
    genotyper.out = "recalibrationSNPs.vcf"
    add(genotyper)

    
    selectVars.variant = genotyper.out
    selectVars.select =  Seq("QD > 2.0 && FS < 60.0 && MQ > 40.0 && ReadPosRankSum > -8.0") 
    selectVars.out = swapExt(genotyper.out, "vcf", "qual.filtered.vcf")
    add(selectVars)

 
    baseRecal.input_file ++= realignedFiles
    baseRecal.knownSites = Seq(selectVars.out)
    baseRecal.scatterCount = 4
    baseRecal.memoryLimit = 4
    baseRecal.nct = 8
    baseRecal.jobResourceRequests = Seq("vf=10g")
    baseRecal.out = "recalibration_report.grp"

    add(baseRecal)


    printReads.input_file ++= realignedFiles
    printReads.BQSR = baseRecal.out
    printReads.nct = 8
    printReads.memoryLimit = 4
    printReads.out = "recalibrated.bam"

    add(printReads)    



    val finalGenotyper = new UnifiedGenotyper with UnifiedGenotyperArguments
    finalGenotyper.scatterCount = 16
    finalGenotyper.nct = 3
    finalGenotyper.nt = 2
    finalGenotyper.memoryLimit = 24 //32
    finalGenotyper.input_file = List[File](printReads.out)
    finalGenotyper.out = "UnifiedGenotyperVariations.vcf"
    finalGenotyper.genotype_likelihoods_model = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypeLikelihoodsCalculationModel.Model.BOTH
    add(finalGenotyper)



    var LTypeSelect: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LTypeSelect :+= htsjdk.variant.variantcontext.VariantContext.Type.SNP

    val selectSNPs = new SelectVariants with UnifiedGenotyperArguments
    selectSNPs.variant =  finalGenotyper.out
    selectSNPs.select =  Seq("QD > 2.0 && FS < 60.0 && MQ > 40.0 && ReadPosRankSum > -8.0" ) // && MappingQualityRankSum > -12.5") // && ReadPosRankSum > -8.0")
    selectSNPs.selectTypeToInclude = LTypeSelect
    selectSNPs.restrictAllelesTo =  org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectSNPs.out = "UnifiedGenotyper_biallelic_true_SNPS.qual.filtered.vcf"
    add(selectSNPs)
 

    var LtypeSelect2: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LtypeSelect2 :+= htsjdk.variant.variantcontext.VariantContext.Type.INDEL

    val selectIndels = new SelectVariants with UnifiedGenotyperArguments
    selectIndels.variant =  finalGenotyper.out
    selectIndels.select = Seq("QD > 2.0 && FS < 200.0 && ReadPosRankSum > -8.0")
    selectIndels.selectTypeToInclude = LtypeSelect2
    selectIndels.restrictAllelesTo = org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectIndels.out = "UnifiedGenotyper_biallelic_true_INDELS.qual.filtered.vcf"
    add(selectIndels)



    val hc = new HaplotypeCaller with UnifiedGenotyperArguments

    hc.scatterCount = 200
    hc.nct = 8
    hc.memoryLimit = 32
    hc.input_file = List[File](printReads.out)
    hc.out = "HaplotypeCallerVariations.vcf"
    add(hc)

    val selectSNPsHC = new SelectVariants with UnifiedGenotyperArguments
    selectSNPsHC.variant =  hc.out
    selectSNPsHC.select =  Seq("QD > 2.0 && FS < 60.0 && MQ > 40.0 && ReadPosRankSum > -8.0" ) 
    selectSNPsHC.selectTypeToInclude = LTypeSelect
    selectSNPsHC.restrictAllelesTo =  org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectSNPsHC.out = "HaplotypeCaller_biallelic_true_SNPS.qual.filtered.vcf"
    add(selectSNPsHC)
 
    val selectIndelsHC = new SelectVariants with UnifiedGenotyperArguments
    selectIndelsHC.variant =  hc.out
    selectIndelsHC.select = Seq("QD > 2.0 && FS < 200.0 && ReadPosRankSum > -8.0")
    selectIndelsHC.selectTypeToInclude = LtypeSelect2
    selectIndelsHC.restrictAllelesTo = org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectIndelsHC.out = "HaplotypeCaller_biallelic_true_INDELS.qual.filtered.vcf"
    add(selectIndelsHC)





  
  }
}
