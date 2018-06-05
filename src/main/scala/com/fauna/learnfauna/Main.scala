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
  logger.info("starting customer tests")
  val faunaClient = createFaunaClient

  val customer = Customer(faunaClient)


  /*faunaClient.foreach { faunaClient =>

    val customer = new Customer(faunaClient)
    customer.createSchema()
    customer.createCustomer(0, 100)
    customer.readCustomer(0)
    customer.updateCustomer(0, 200)
    customer.readCustomer(0)
    customer.deleteCustomer(0)
**/

  /*
 * Just to keep things neat and tidy, close the client connection
 */
  //  faunaClient.close()

  //  logger.info("Disconnected from FaunaDB!")

  // add this at the end of execution to make things shut down nicely

  //wait for the fauna client
  await(customer)

  logger.info("finished customer tests")

  System.exit(0)

  def createFaunaClient: FaunaClient = {
    logger.info("starting create fauna client")

    val faunaDBConfig = FaunaDBConfig.getFaunaDBConfig

    //Create an admin client. This is the client we will use to create the database
    val adminClient = FaunaClient(faunaDBConfig.secret, faunaDBConfig.endPoint)
    logger.info("Succesfully connected to FaunaDB as Admin!")

    val databaseRequest = createFaunaDatabase(faunaDBConfig, adminClient)

    //only block on startup when we create the database
    val createDBResponse = await(databaseRequest)

    logger.info(s"Created database: $LEDGER_DB :: \n${JsonUtil.toJson(createDBResponse)}")

    /*
    * Create a key specific to the database we just created. We will use this to
    * create a new client we will use in the remainder of the examples.
    */
    await(databaseRequest)
    val keyReq = adminClient.query(CreateKey(Obj("database" -> Database(LEDGER_DB), "role" -> "server")))
      .flatMap { v =>
        Future.fromTry(v("secret").to[String].toEither.left.map(x => new Exception(s"errs ${x}")).toTry)
      }
    val serverKey = await(keyReq)
    adminClient.close
    FaunaClient(serverKey, faunaDBConfig.endPoint)
  }

  /*
  * The code below creates the Database that will be used for this example. Please note that
  * the existence of the database is  evaluated and one of two options is followed in a single call to the Fauna DB:
  * --- If the DB already exists, just return the existing DB
  * --- Or delete the existing DB and recreate
  */
  private def createFaunaDatabase(faunaDBConfig: FaunaDBConfig, adminClient: FaunaClient) = {
    if (faunaDBConfig.deleteDB) {
      logger.info(s"deleting existing database")
      adminClient.query(
        Arr(
          If(
            Exists(Database(LEDGER_DB)),
            Delete(Database(LEDGER_DB)),
            true
          ),
          CreateDatabase(Obj("name" -> LEDGER_DB)))
      )
    }
    else {
      logger.info(s"creating or getting database")
      adminClient.query(
        If(
          Exists(Database(LEDGER_DB)),
          Get(Database(LEDGER_DB)),
          CreateDatabase(Obj("name" -> LEDGER_DB))
        )
      )
    }
  }

  def await[T](f: Future[T]): T = Await.result(f, 5.second)
}

