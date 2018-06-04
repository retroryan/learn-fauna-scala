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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import faunadb.query._

/*
 * These are the required imports for Fauna.
 *
 * For these examples we are using the 2.2.0 version of the JVM driver. Also notice that we aliasing
 * the query and values part of the API to make it more obvious we we are using Fauna functionality.
 *
 */

import faunadb.FaunaClient

object Main extends App with Logging {

  import ExecutionContext.Implicits._

  /*
   * Create an admin connection to FaunaDB.
   *
   * If you are using the FaunaDB-Cloud version:
   *  - remove the 'endpoint = endPoint' line below
   *  - substitute your secret for "secret" below
   */
  val faunaDBConfig = FaunaDBConfig.getFaunaDBConfig
  val adminClient = FaunaClient(faunaDBConfig.secret, faunaDBConfig.endPoint)

  logger.info("Succesfully connected to FaunaDB as Admin!")

  /*
   * Create a database
   */
  val TEST_DB = "TestDB"

  val testClient = {
    val databaseRequest = adminClient.query(
      If(
        Exists(Database(TEST_DB)),
        Get(Database(TEST_DB)),
        CreateDatabase(Obj("name" -> TEST_DB))
      )
    )
    val createDBResponse = await(databaseRequest)
    logger.info(s"Created database: $TEST_DB :: \n${JsonUtil.toJson(createDBResponse)}")

    val keyReq = adminClient.query(CreateKey(Obj("database" -> Database(TEST_DB), "role" -> "server")))
      .flatMap { value =>
        val secretEither = value("secret").to[String].toEither
        val secretTry = secretEither.left.map(x => new Exception(s"errs ${x}")).toTry
        Future.fromTry(secretTry)
      }
    val serverKey = await(keyReq)
    adminClient.close
    FaunaClient(serverKey, faunaDBConfig.endPoint)
  }

  /*
   * Delete the Database that we created
   */
  val deleteResponse = testClient.query(
    If(
      Exists(Database(TEST_DB)),
      Delete(Database(TEST_DB)),
      true
    )
  )
  Await.result(deleteResponse, Duration.Inf)
  logger.info(s"Deleted database: ${TEST_DB} :: \n${JsonUtil.toJson(deleteResponse)}")

  /*
   * Just to keep things neat and tidy, close the client connection
   */
  testClient.close()

  logger.info("Disconnected from FaunaDB as Admin!")

  // add this at the end of execution to make things shut down nicely
  System.exit(0)

  def await[T](f: Future[T]): T = Await.result(f, 5.second)
}

