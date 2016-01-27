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
import org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils
import org.broadinstitute.gatk.queue.extensions.gatk._

class HaplotypeCaller_pipeline extends QScript {
  // Create an alias 'qscript' to be able to access variables
  // in the  HaplotypeCaller.
  // 'qscript' is now the same as ' HaplotypeCaller_pipeline.this'
  qscript =>



  // Required arguments.  All initialized to empty values.

  /*
  ** Alle dieser Argumente, die einen "shortName" haben, sind Input zum initialen GATK Aufruf
  */
  @Input(doc="The reference file for the bam files.", shortName="R")
  var referenceFile: File = _ // _ is scala shorthand for null

  @Input(doc="One or more bam files.", shortName="I")
  var bamFiles: List[File] = Nil

  @Input(doc="An optional file with a list of intervals to proccess.", shortName="L", required=false)
  var intervals: File = _

  // This trait allows us set the variables below in one place,
  // and then reuse this trait on each CommandLineGATK function below.
  trait  HaplotypeCallerArguments extends CommandLineGATK {
    this.reference_sequence = qscript.referenceFile
    this.intervals = if (qscript.intervals == null) Nil else List(qscript.intervals)
    // Set the memory limit to 3 gigabytes on each command.
    this.memoryLimit = 3
    
  }

  def script() {
    


    // setting SNP and InDel Type vars 
    var LTypeSelect: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LTypeSelect :+= htsjdk.variant.variantcontext.VariantContext.Type.SNP

    var LtypeSelect2: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LtypeSelect2 :+= htsjdk.variant.variantcontext.VariantContext.Type.INDEL


    val hc = new HaplotypeCaller with  HaplotypeCallerArguments

    hc.scatterCount = 500
    hc.nct = 3
    hc.memoryLimit = 3
    hc.input_file ++= bamFiles
    hc.jobResourceRequests = Seq("vf=3g")
    hc.out = "HaplotypeCallerVariations.vcf"
    add(hc)

    val selectSNPsHC = new SelectVariants with  HaplotypeCallerArguments
    selectSNPsHC.variant =  hc.out
    selectSNPsHC.scatterCount = 20
    selectSNPsHC.select =  Seq("QD > 2.0 && FS < 60.0 && MQ > 40.0 && MQRankSum > -12.5 && ReadPosRankSum > -8.0" ) 
    selectSNPsHC.selectTypeToInclude = LTypeSelect
    selectSNPsHC.restrictAllelesTo =  org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectSNPsHC.out = "HaplotypeCaller_biallelic_true_SNPS.qual.filtered.vcf"
    add(selectSNPsHC)
 
    val selectIndelsHC = new SelectVariants with  HaplotypeCallerArguments
    selectIndelsHC.variant =  hc.out
    selectIndelsHC.scatterCount = 20
    selectIndelsHC.select = Seq("QD > 2.0 && FS < 200.0 && ReadPosRankSum > -8.0")
    selectIndelsHC.selectTypeToInclude = LtypeSelect2
    selectIndelsHC.restrictAllelesTo = org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectIndelsHC.out = "HaplotypeCaller_biallelic_true_INDELS.qual.filtered.vcf"
    add(selectIndelsHC)


    var variantFiles: List[File] = Nil
    variantFiles :+= selectSNPsHC.out
    variantFiles :+= selectIndelsHC.out



    val combineVars = new CombineVariants with HaplotypeCallerArguments
    combineVars.variant ++= variantFiles
    combineVars.genotypeMergeOptions = GATKVariantContextUtils.GenotypeMergeType.UNSORTED
    combineVars.scatterCount = 20
    combineVars.out = "CombineVariants_merged.vcf"
    add(combineVars)



  
  }
}
