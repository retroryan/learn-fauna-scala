/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fauna.learnfauna

/*
 * These imports are for basic functionality around logging and JSON handling and Futures.
 * They should best be thought of as a convenience items for our demo apps.
 */
import grizzled.slf4j.Logging

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

/*
 * These are the required imports for Fauna.
 *
 * For these examples we are using the 2.1.0 version of the JVM driver. Also notice that we aliasing
 * the query and values part of the API to make it more obvious we we are using Fauna functionality.
 *
 */
import faunadb.FaunaClient
import faunadb.{query => q}
import faunadb.{values =>v}


object Lesson1 extends App with Logging {
  import ExecutionContext.Implicits._

  /*
   * Get the configuration values for FaunaDB contained in the application.conf file.
   */
  val config = FaunaDBConfig.getConfig

  logger.info(s"FaunaDB root URL: ${config("root_url")}")
  logger.info(s"FaunaDB security key: ${config("root_token")}")

  /*
   * Create an admin connection to FaunaDB.
   *
   * This is only used if you are using your own Developers or Enterprise edition of FaunaDB
   */
  val adminClient = FaunaClient(endpoint = config("root_url"), secret = config("root_token"))

  logger.info("Succesfully connected to FaunaDB as Admin!")

  /*
   * Create a database
   */
  val dbName = "TestDB"

  var queryResponse = adminClient.query(
    q.CreateDatabase(q.Obj("name" -> dbName))
  )
  Await.result(queryResponse, Duration.Inf)
  logger.info(s"Created database: ${dbName} :: \n${JsonUtil.toJson(queryResponse)}")

  /*
   * Delete the Database that we created
   */
  queryResponse = adminClient.query(
    q.If(
      q.Exists(q.Database(dbName)),
      q.Delete(q.Database(dbName)),
      true
    )
  )
  Await.result(queryResponse, Duration.Inf)
  logger.info(s"Deleted database: ${dbName} :: \n${JsonUtil.toJson(queryResponse)}")

  /*
   * Just to keep things neat and tidy, close the client connection
   */
  adminClient.close()

  logger.info("Disconnected from FaunaDB as Admin!")

  // add this at the end of execution to make things shut down nicely
  System.exit(0)
}

