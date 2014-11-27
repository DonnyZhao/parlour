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

import com.twitter.scalding._

import cascading.tap.Tap

import au.com.cba.omnia.parlour.SqoopSyntax.ParlourImportDsl

/**
 * Sqoop import Job that can be embedded within a Cascade
 */
class ImportSqoopJob(
  options: ParlourImportOptions[_],
  source: Tap[_, _, _],
  sink: Tap[_, _, _]
)(args: Args) extends Job(args) with HadoopConfigured {

  def this(options: ParlourImportOptions[_], sink: Tap[_, _, _])(args: Args)(implicit mode: Mode) =
    this(options, TableTap(options.toSqoopOptions), sink)(args)

  /** Helper constructor that allows easy usage from Scalding */
  def this(options: ParlourImportOptions[_], source: Source, sink: Source)(args: Args)(implicit mode: Mode) =
    this(options, source.createTap(Read), sink.createTap(Write))(args)

  /** Helper constructor that allows easy usage from Scalding */
  def this(options: ParlourImportOptions[_], sink: Source)(args: Args)(implicit mode: Mode) =
    this(options, TableTap(options.toSqoopOptions), sink.createTap(Write))(args)

  def this(options: ParlourImportOptions[_])(args: Args)(implicit mode: Mode) =
    this(options, TargetDirTap(options.toSqoopOptions))(args)

  override def buildFlow = {
    val dsl = ParlourImportDsl(options.updates)
    val withConfig = getHadoopConf.fold(dsl)(dsl config _)
    new ImportSqoopFlow(s"$name-${UUID.randomUUID}", withConfig, Some(source), Some(sink))
  }

  /** Can't validate anything because this doesn't use a Hadoop FlowDef. */
  override def validate = ()
}
