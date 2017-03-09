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

import com.twitter.scalding._

import org.apache.hadoop.conf.Configuration

import au.com.cba.omnia.parlour.SqoopSetup.Delimiters

class SqoopSetupSpec extends OmniaSpec { def is = s2"""
Export Sqoop Setup
===================

Should be able to infer the path for:
  - TypedPsv ${inferPath.typedPsv}
  - Csv      ${inferPath.csv}

Should be able to infer delimiters for:
  - TypedPsv  ${inferDelimiter.typedPsv}
  - Csv       ${inferDelimiter.csv}

"""

  // Warning: these tests use a hacky way to construct Taps, in that they pass a dummy HDFS path and
  // rely on the implementation-specific detail that under strictSources mode i.e. `Hdfs(true, ...)`
  // the existence of the path is not checked by createTap (that check is deferred to validateTaps).

  object inferPath {
    def typedPsv = {
      //given
      val tap = TypedPsv[String]("/test/path").createTap(Read)(Hdfs(true, new Configuration()))
      //when
      val pathOpt = SqoopSetup.inferPathFromTap(tap)
      //then
      pathOpt must beEqualTo(Some("/test/path"))
    }

    def csv = {
      //given
      val tap = Csv("/test/path").createTap(Read)(Hdfs(true, new Configuration()))
      //when
      val pathOpt = SqoopSetup.inferPathFromTap(tap)
      //then
      pathOpt must beEqualTo(Some("/test/path"))
    }
  }

  object inferDelimiter{
    def typedPsv = {
      //given
      val tap = TypedPsv[String]("test").createTap(Read)(Hdfs(true, new Configuration()))
      //when
      val delims = SqoopSetup.inferDelimitersFromTap(tap)
      //then
      delims match {
        case Delimiters(_, Some(delim)) => delim must beEqualTo('|')
      }
    }

    def csv = {
      //given
      val tap = Csv("test").createTap(Read)(Hdfs(true, new Configuration()))
      //when
      val delims = SqoopSetup.inferDelimitersFromTap(tap)
      //then
      delims match {
        case Delimiters(_, Some(delim)) => delim must beEqualTo(',')
      }
    }
  }
}
