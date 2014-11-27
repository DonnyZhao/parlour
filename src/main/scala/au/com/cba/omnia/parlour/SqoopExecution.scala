//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package au.com.cba.omnia.parlour

import java.util.UUID

import org.apache.hadoop.conf.Configuration

import com.twitter.scalding.{Args, Execution, Mode, Read, Source, Write}


/** Factory for Sqoop Executions */
object SqoopExecution {
  /**
    * An Execution that uses sqoop to import data to a tap.
    *
    * `options` does not need any information about the destination.
    * We infer that information from `sink`.
    */
  def sqoopImport(options: ParlourImportOptions[_], sink: Source): Execution[Unit] = {
    //TODO fix when scalding provides access to the mode
    val mode = Mode(Args("--hdfs"), new Configuration)
    val tap  = Some(sink.createTap(Write)(mode))
    val flow = new ImportSqoopFlow(s"SqoopExecutionImport-${UUID.randomUUID}", options, None, tap) 
    Execution.fromFuture(_ => Execution.run(flow)).unit
  }

  /** An Execution that uses sqoop to import data. */
  def sqoopImport(options: ParlourImportOptions[_]): Execution[Unit] = {
    val flow = new ImportSqoopFlow(s"SqoopExecutionImport-${UUID.randomUUID}", options, None, None)
    Execution.fromFuture(_ => Execution.run(flow)).unit
  }

  /**
    * An Execution that uses sqoop to export data from a tap to a database.
    *
    * `options` does not need any information about the source.
    * We infer that information from `source`.
    */
  def sqoopExport(options: ParlourExportOptions[_], source: Source): Execution[Unit] = {
    //TODO fix when scalding provides access to the mode
    val mode = Mode(Args("--hdfs"), new Configuration)
    val tap  = Some(source.createTap(Read)(mode))
    val flow = new ExportSqoopFlow(s"SqoopExecutionExport-${UUID.randomUUID}", options, tap, None)
    Execution.fromFuture(_ => Execution.run(flow)).unit
  }

  /** An Execution that uses sqoop to export data to a database. */
  def sqoopExport(options: ParlourExportOptions[_]): Execution[Unit] = {
    val flow = new ExportSqoopFlow(s"SqoopExecutionExport-${UUID.randomUUID}", options, None, None)
    Execution.fromFuture(_ => Execution.run(flow)).unit
  }
}
