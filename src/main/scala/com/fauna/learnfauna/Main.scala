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


import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext
import faunadb.FaunaClient

object Main extends Logging {

  import ExecutionContext.Implicits._


  def main(args: Array[String]): Unit = {

    try {
      runDemo()
    } catch {
      case exc: Throwable =>
        println(s"exc: $exc")
        exc.printStackTrace()
    }
  }

  def runDemo() = {

    //This is the main functionality of lesson 2 - create a fauna client and run the customer tests
    logger.info("starting customer tests")
    implicit val faunaClient: FaunaClient = FaunaUtils.createFaunaClient


    //wait for the work to finish client
    val originalCustomerTestsResults = originalCustomerTests
    await(originalCustomerTestsResults)
    logger.info("finished customer tests")

    faunaClient.close()
    logger.info("Disconnected from FaunaDB!")
    System.exit(0)
  }

  private def originalCustomerTests(implicit client: FaunaClient) = {

    val cust1 = Customer(1, 100, EmptyAddress)
//    val cust2 = Customer(2, 100, WorkAddress("ny", "ny"), HomeAddress("sf", "ca", "fido"))
//    val cust3 = Customer(3, 100, HomeAddress("sf", "ca", "fido"), WorkAddress("ny", "ny"))
//    val cust4 = Customer(4, 100, EmptyAddress, WorkAddress("sd", "ca"))
//    val cust5 = Customer(5, 100, OldWorkAddress("pc", "ut"), EmptyAddress)


    val writeWork = for {
      //Initialize the Customer schema and wait for the creation to finish
      _ <- Customer.createSchema

      _ <- Customer.createCustomer(cust1)
//      _ <- Customer.createCustomer(cust2)
//      _ <- Customer.createCustomer(cust3)
//      _ <- Customer.createCustomer(cust4)
//      _ <- Customer.createCustomer(cust5)


      retCust1 <- Customer.readCustomer(1)
//      retCust2 <- Customer.readCustomer(2)
//      retCust3 <- Customer.readCustomer(3)
//      retCust4 <- Customer.readCustomer(4)
//      retCust5 <- Customer.readCustomer(5)


    } yield {

      logger.info(s"retCust1: $retCust1")
//      logger.info(s"retCust2: $retCust2")
//      logger.info(s"retCust3: $retCust3")
//      logger.info(s"retCust4: $retCust4")
//      logger.info(s"retCust5: $retCust5")


    }

    writeWork
  }


}

