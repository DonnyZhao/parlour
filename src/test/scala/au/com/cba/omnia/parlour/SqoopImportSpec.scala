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

import scalaz._, Scalaz._

import com.cloudera.sqoop.SqoopOptions

import cascading.tap.Tap

import com.twitter.scalding.Write
import com.twitter.scalding.Csv

import scalikejdbc.{AutoSession, ConnectionPool, SQL}
import scalikejdbc.LoanPattern._

import au.com.cba.omnia.thermometer.context.Context
import au.com.cba.omnia.thermometer.core.ThermometerSpec
import au.com.cba.omnia.thermometer.core.Thermometer._
import au.com.cba.omnia.thermometer.fact.Fact
import au.com.cba.omnia.thermometer.fact.PathFactoids._

import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.Fixture

class SqoopImportSpec extends ThermometerSpec { def is = s2"""
  Sqoop Import Flow/Job Spec
  ==========================

  end to end sqoop flow test      $endToEndFlow
  end to end sqoop job test       $endToEndJob
"""

  def endToEndFlow = withData(List(
    ("001", "Micky Mouse",  10,  1000),
    ("002", "Donald Duck", 100, 10000)
  ))( opts => {
    val source = TableTap(opts)
    val sink   = Csv((dir </> "output").toString).createTap(Write)
    val flow   = new ImportSqoopFlow("endToEndFlow", opts, source, sink)

    println(s"=========== endToEndFlow test running in $dir ===============")

    flow.complete
    flow.getFlowStats.isSuccessful must beTrue
    facts(dir </> "output" </> "part-m-00000" ==> lines(List(
      "001,Micky Mouse,10,1000",
      "002,Donald Duck,100,10000"
    )))
  })

  def endToEndJob = withData(List(
    ("003", "Robin Hood", 0, 0),
    ("004", "Little John", -1, -100)
  ))( opts => {
    val source = TableTap(opts)
    val sink   = Csv((dir </> "output").toString).createTap(Write)
    val job    = new ImportSqoopJob(opts, source, sink)(scaldingArgs)
    job.withFacts(dir </> "output" </> "part-m-00000" ==> lines(List(
      "003,Robin Hood,0,0",
      "004,Little John,-1,-100"
    )))
  })

  def facts(facts: Fact*): Result =
    facts.toList.map(fact => fact.run(Context(jobConf))).suml(Result.ResultMonoid)
}

/** Provides a temporary dummy table for sqoop iport tests */
case class withData(data: List[(String, String, Int, Int)]) extends Fixture[SqoopOptions] {
  Class.forName("org.hsqldb.jdbcDriver")

  def apply[R: AsResult](test: SqoopOptions => R): Result = {
    val table   = s"table_${UUID.randomUUID.toString.replace('-', '_')}"
    val connStr = "jdbc:hsqldb:mem:sqoopdb"
    val user    = "sa"
    val pwd     = ""

    ConnectionPool.singleton(connStr, user, pwd)
    implicit val session = AutoSession

    SQL(s"""
      create table $table (
        id varchar(20),
        customer varchar(20),
        balance integer,
        balance_cents integer
      )
    """).execute.apply()

    val inserted = data.map { case (id, customer, balance, balance_cents) =>
      SQL(s"""
        insert into $table(id, customer, balance, balance_cents)
        values (?, ?, ?, ?)
      """).bind(id, customer, balance, balance_cents).update.apply()
    }

    if (inserted.exists(_ != 1)){
      failure.updateMessage("failed to insert data into the test table")
    }
    else {
      val opts = new SqoopOptions
      opts.setConnectString(connStr)
      opts.setUsername(user)
      opts.setPassword(pwd)
      opts.setTableName(table)
      opts.setNumMappers(1)
      opts.setHadoopMapRedHome(System.getProperty("user.home") + "/.ivy2/cache")

      AsResult(test(opts))
    }
  }
}
