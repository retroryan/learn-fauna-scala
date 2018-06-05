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

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._


/*
 * These are the required imports for Fauna.
 *
 * For these examples we are using the 2.2.0 version of the JVM driver. Also notice that we are doing a global import on
 * the query and values part of the API to make it more obvious we we are using Fauna functionality.
 *
 */
import faunadb.query._
import faunadb.FaunaClient

object Main extends App with Logging {

  import ExecutionContext.Implicits._

  /*
   * Name of the test database
   */
  val LEDGER_DB = "LedgerExample"

  //This is the main functionality of lessong 2 - create a fauna client and run the customer tests
  val faunaClient = createFaunaClient

  val customer = new Customer(faunaClient)
  customer.createSchema()
  customer.createCustomer(0, 100)
  customer.readCustomer(0)
  customer.updateCustomer(0, 200)
  customer.readCustomer(0)
  customer.deleteCustomer(0)

  /*
   * Just to keep things neat and tidy, close the client connection
   */
  faunaClient.close()

  logger.info("Disconnected from FaunaDB as Admin!")

  // add this at the end of execution to make things shut down nicely
  System.exit(0)


  def createFaunaClient: FaunaClient = {
    val faunaDBConfig = FaunaDBConfig.getFaunaDBConfig

    //Create an admin client. This is the client we will use to create the database
    val adminClient = FaunaClient(faunaDBConfig.secret, faunaDBConfig.endPoint)
    logger.info("Succesfully connected to FaunaDB as Admin!")

    /*
    * The code below creates the Database that will be used for this example. Please note that
    * the existence of the database is evaluated and only created if it does not exist in a single
    * call to the Fauna DB.
    */
    val databaseRequest = adminClient.query(
      If(
        Exists(Database(LEDGER_DB)),
        Get(Database(LEDGER_DB)),
        CreateDatabase(Obj("name" -> LEDGER_DB))
      )
    )
    val createDBResponse = await(databaseRequest)
    logger.info(s"Created database: $LEDGER_DB :: \n${JsonUtil.toJson(createDBResponse)}")

    val futureKeyValue = adminClient.query(CreateKey(Obj("database" -> Database(LEDGER_DB), "role" -> "server")))

    /*
    * Create a key specific to the database we just created. We will use this to
    * create a new client we will use in the remainder of the examples.
    */
    val keyReq = futureKeyValue
      .flatMap { value =>
        val secretEither = value("secret").to[String].toEither
        val secretTry = secretEither.left.map(x => new Exception(s"errs ${x}")).toTry
        Future.fromTry(secretTry)
      }

    val serverKey = await(keyReq)
    adminClient.close
    //Create a fauna client specific to the DB using the server key just created.
    FaunaClient(serverKey, faunaDBConfig.endPoint)
  }



  def await[T](f: Future[T]): T = Await.result(f, 5.second)
}

