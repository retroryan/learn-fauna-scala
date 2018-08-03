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

object Main extends Logging {

  import ExecutionContext.Implicits._

  /*
   * Name of the test database
   */
  val LEDGER_DB = "LedgerExample"

  def main(args: Array[String]): Unit = {
    //This is the main functionality of lesson 2 - create a fauna client and run the customer tests
    logger.info("starting customer tests")
    implicit val faunaClient: FaunaClient = createFaunaClient

    //val work: Future[Unit] = originalCustomerTests(faunaClient)

    //orgCustomerWork(faunaClient)

    val work = for {
      _ <- AltCustomer.createSchema(faunaClient)
      _ <- AltCustomer.createCustomer(AltCustomer(20,24.32))
    } yield {
      logger.info(s"created alt customer schema")
    }

    //wait for the work to finish client
    await(work)

    try {
      AltCustomer.readCustomer(20)
    }
    catch {
      case exc => println(s"ERROR $exc")
    }

    logger.info("finished alt customer tests")

    /*
  * Just to keep things neat and tidy, close the client connection
  */
    faunaClient.close()
    logger.info("Disconnected from FaunaDB!")
    System.exit(0)
  }

  private def orgCustomerWork(faunaClient: FaunaClient)(implicit client:FaunaClient) = {
    val work = for {
      //Initalize the Customer schema and wait for the creation to finish
      _ <- Customer(faunaClient)
      custList <- Customer.create20Customers()
      _ <- Customer.readGroupCustomers()
      _ <- Customer.readCustomerByIds()
    } yield {
      logger.info(s"Create 20 customer list: $custList")
    }
  }

  private def originalCustomerTests(faunaClient: FaunaClient)(implicit client: FaunaClient) = {
    val cust1 = Customer(1, 100)
    val cust2 = Customer(2, 100)
    val cust3 = Customer(3, 100)
    val cust4 = Customer(4, 100)
    val cust5 = Customer(5, 100)

    val work = for {
      //Initalize the Customer schema and wait for the creation to finish
      _ <- Customer(faunaClient)
      _ <- Customer.createCustomer(cust1)
      retCust1 <- Customer.readCustomer(cust1.id)
      _ <- Customer.updateCustomer(cust1.copy(balance = 200))
      retCust2 <- Customer.readCustomer(cust1.id)
      _ <- Customer.deleteCustomer(cust1.id)
      _ <- Customer.createListCustomer(Seq(cust2, cust3, cust4, cust5))
    } yield {
      logger.info(s"retCust1: $retCust1")
      logger.info(s"retCust2: $retCust2")
    }
    work
  }

  def createFaunaClient: FaunaClient = {
    logger.info("starting create fauna client")

    val faunaDBConfig = FaunaDBConfig.getFaunaDBConfig

    //Create an admin client. This is the client we will use to create the database
    //val adminClient = FaunaClient(faunaDBConfig.secret, faunaDBConfig.endPoint)
    //default to the cloud db
    val adminClient = FaunaClient(faunaDBConfig.secret)

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
    FaunaClient(serverKey)
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
        Do(
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

}

