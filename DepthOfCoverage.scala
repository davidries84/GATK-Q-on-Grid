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

package org.broadinstitute.sting.queue.qscripts.examples


import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils

import org.broadinstitute.gatk.queue.extensions.picard.MergeSamFiles
import org.broadinstitute.gatk.queue.extensions.gatk.DepthOfCoverage
import org.broadinstitute.gatk.queue.extensions.gatk.IntervalScatterFunction
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction




class DepthOfCoverageCalculation extends QScript {
  qscript =>

  // Required arguments.  All initialized to empty values.
  @Input(doc="An optional file with a list of intervals to proccess.", shortName="L", required=false)
  var intervals: File = _
  @Input(doc="The reference file for the bam files.", shortName="R")
  var referenceFile: File = _ // _ is scala shorthand for null
  @Input(doc="The bam files to Calculate the Coverage for.", shortName="I")
  var bamFiles: List[File] = Nil // _ is scala shorthand for null


  trait DepthOfCoverageArguments extends CommandLineGATK with ScatterGatherableFunction {

    this.intervals = if (qscript.intervals == null) Nil else List(qscript.intervals)
    // Set the memory limit to 2 gigabytes on each command.
    this.memoryLimit = 4
    this.reference_sequence = referenceFile

    }

  def script() {

    val dOC = new DepthOfCoverage with DepthOfCoverageArguments
    dOC.input_file ++= bamFiles
    dOC.scatterCount = 100
    dOC.scatterClass = classOf[IntervalScatterFunction]
    dOC.memoryLimit = 4
    dOC.omitIntervalStatistics = true
    dOC.omitDepthOutputAtEachBase = true
    dOC.omitPerSampleStats = false

    
   // dOC.outputFormat = "csv"
    dOC.out = "DepthOfCoverage"

    add(dOC)    



  
  }
}
