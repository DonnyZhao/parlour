Parlour
=======

> ***Parlour.*** *a place that sells scoops of ice-cream; a cascading-sqoop integration.*

Parlour provides a basic Cascading/Scalding Sqoop integration allowing export from HDFS.

It also provides for support for the Cloudera/Teradata Connector.

Cascade Job
-----------

    import au.com.cba.omnia.parlour.SqoopSyntax._

    new ExportSqoopJob(
      sqoopOptions()
       .teradata(BatchInsert)
       .connectionString("jdbc:teradata://some.server/database=DB1")
       .username("some username")
       .password(System.getenv("DATABASE_PASSWORD"))
       .tableName("some table"),
      TypedPsv[String]("hdfs/path/to/data/to/export")
    )(args)


Console Job
-----------

Parlour includes a sample job that can be invoked from the command-line:

    hadoop jar <parlour-jar> \
        com.twitter.scalding.Tool \
        au.com.cba.omnia.parlour.ExportSqoopConsoleJob \
        --hdfs \
        --input /data/on/hdfs/to/sqoop \
        --teradata \
        --teradata-method internal.fastload \
        --connection-string "jdbc:teradata://database/database=test" \
        --table-name test \
        --username user1 \
        --password $PASSWORD \
        --mappers 1 \
        --input-field-delimiter \| \
        --input-line-delimiter \n

