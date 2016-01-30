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

import htsjdk.samtools.FixBAMFile

class FixBAM extends QScript {
  // Create an alias 'qscript' to be able to access variables
  // in the  HaplotypeCaller.
  // 'qscript' is now the same as ' HaplotypeCaller_pipeline.this'
  qscript =>



  // Required arguments.  All initialized to empty values.

  /*
  ** Alle dieser Argumente, die einen "shortName" haben, sind Input zum initialen GATK Aufruf
  */

  @Input(doc="One or more bam files.", shortName="I")
  var bamFiles: List[File] = Nil

  // This trait allows us set the variables below in one place,
  // and then reuse this trait on each CommandLineGATK function below.
  trait  FixBAMArguments extends CommandLineGATK {
    // Set the memory limit to 3 gigabytes on each command.
    this.memoryLimit = 3
    
  }

  def script() {
    



    val fb = new FixBAMFile //with FixBAMArguments

//    fb.memoryLimit = 3
    fb.args ++= bamFiles
//    fb.jobResourceRequests = Seq("vf=3g")
//    fb.out = swapExt(fb.input_file, "bam", "fixed.bam")
    add(fb)
 
  }
}
