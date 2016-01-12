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

// OLD: package org.broadinstitute.sting.queue.qscripts.examples
package org.broadinstitute.gatk.queue.qscripts.examples

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk._

// OLD: import org.broadinstitute.variant.variantcontext._
// OLD: import org.broadinstitute.variant.variantcontext.VariantContext.Type





class VariantFiltration extends QScript {
  // Create an alias 'qscript' to be able to access variables
  // in the VariantFiltration.
  // 'qscript' is now the same as 'ExampleUnifiedGenotyper.this'
  qscript =>

  // Required arguments.  All initialized to empty values.

  /*
  ** Alle dieser Argumente, die einen "shortName" haben, sind Input zum initialen GATK Aufruf (Realigner)
  */

  @Input(doc="An optional file with a list of intervals to proccess.", shortName="L", required=false)
  var intervals: File = _

  @Input(doc="The reference file for the bam files.", shortName="R")
  var referenceFile: File = _ // _ is scala shorthand for null

  @Input(doc="The input vcf file.", shortName="V")
  var variantFile: File = _ // _ is scala shorthand for null




  // This trait allows us set the variables below in one place,
  // and then reuse this trait on each CommandLineGATK function below.
  trait VariantFiltrationArguments extends CommandLineGATK {

    this.intervals = if (qscript.intervals == null) Nil else List(qscript.intervals)
    // Set the memory limit to 2 gigabytes on each command.
    this.memoryLimit = 2
    this.reference_sequence = referenceFile

    }

  def script() {

    var LtypeSelect: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LtypeSelect :+= htsjdk.variant.variantcontext.VariantContext.Type.SNP

    val selectVars = new SelectVariants with VariantFiltrationArguments
    selectVars.variant = variantFile
    selectVars.select =  Seq("QD > 2.0 && FS < 60.0 && MQ > 40.0 && ReadPosRankSum > -8.0 " ) // && MappingQualityRankSum > -12.5
    selectVars.selectTypeToInclude = LtypeSelect
    selectVars.out = "true_SNPS.qual.filtered.vcf"
    add(selectVars)
 

    var LtypeSelect2: List[htsjdk.variant.variantcontext.VariantContext.Type] = Nil
    LtypeSelect2 :+= htsjdk.variant.variantcontext.VariantContext.Type.INDEL

    val selectIndels = new SelectVariants with VariantFiltrationArguments
    selectIndels.variant = variantFile
    selectIndels.select = Seq("QD > 2.0 && FS < 200.0 && ReadPosRankSum > -8.0")
    selectIndels.selectTypeToInclude = LtypeSelect2
    selectIndels.out = "true_INDELS.qual.filtered.vcf"
    add(selectIndels)



    val selectVarsBi = new SelectVariants with VariantFiltrationArguments
    selectVarsBi.variant = selectVars.out
    selectVarsBi.restrictAllelesTo = org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectVarsBi.out = "biallelic_true_SNPS.qual.filtered.vcf"
    add(selectVarsBi)
 

    val selectIndelsBi = new SelectVariants with VariantFiltrationArguments
    selectIndelsBi.variant = selectIndels.out
    selectIndelsBi.restrictAllelesTo = org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction.BIALLELIC
    selectIndelsBi.out = "biallelic_true_INDELS.qual.filtered.vcf"
    add(selectIndelsBi)




  
  }
}
