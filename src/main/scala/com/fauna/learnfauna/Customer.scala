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
 * They should best be thought of as convenience items for our demo apps.
 */

import faunadb.values.Value
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

/*
 * These are the required imports for Fauna.
 *
 * For these examples we are using the 2.2.0 version of the JVM driver. Also notice that we are doing a global import on
 * the query and values part of the API to make it more obvious we we are using Fauna functionality.
 *
 */

import faunadb.FaunaClient
import faunadb.query._


class Customer(val client: FaunaClient) extends Logging {

  import ExecutionContext.Implicits._

  def createCustomer(custID: Int, balance: Int): Future[Value] = {
    /*
     * Create a customer (record)
     */
    val futureResult = client.query(
      Create(
        Class("customers"), Obj("data" -> Obj("id" -> custID, "balance" -> balance))
      )
    )

    futureResult.foreach { result =>
      logger.info(s"Create \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }

  def readCustomer(custID: Int): Future[Value] = {
    /*
     * Read the customer we just created
     */
    val futureResult = client.query(
      Select("data", Get(Match(Index(Customer.CUSTOMER_INDEX), custID)))
    )
    futureResult.foreach { result =>
      logger.info(s"Read \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }

  def updateCustomer(custID: Int, newBalance: Int): Future[Value] = {
    /*
     * Update the customer
     */
    val futureResult = client.query(
      Update(
        Select("ref", Get(Match(Index(Customer.CUSTOMER_INDEX), custID))),
        Obj("data" -> Obj("balance" -> newBalance)
        )
      )
    )
    futureResult.foreach { result =>
      logger.info(s"Update \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }

  def deleteCustomer(custID: Int): Future[Value] = {
    /*
     * Delete the customer
     */
    val futureResult = client.query(
      Delete(
        Select("ref", Get(Match(Index(Customer.CUSTOMER_INDEX), custID)))
      )
    )
    futureResult.foreach { result =>
      logger.info(s"Delete \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }
}

object Customer extends Logging {

  val CUSTOMER_CLASS = "customers"
  val CUSTOMER_INDEX = s"$CUSTOMER_CLASS-by-name"

  def apply(client: FaunaClient)(implicit ec: ExecutionContext): Future[Customer] = {
    logger.info("starting customer create schema")

    /*
     * Create an class to hold customers
     */
    val customerObj = Class(CUSTOMER_CLASS)
    val createCustomer = CreateClass(Obj("name" -> CUSTOMER_CLASS))
    val conditionalCreateCustomer = If(
      Exists(customerObj),
      Get(customerObj),
      createCustomer
    )

    /*
    * Create the Indexes within the database. We will use these to access record in later lessons
    */
    val indexObj = Index(CUSTOMER_INDEX)
    val createCustomerIndex = CreateIndex(
      Obj(
        "name" -> CUSTOMER_INDEX,
        "source" -> Class(CUSTOMER_CLASS),
        "unique" -> true,
        "terms" -> Arr(Obj("field" -> Arr("data", "id")))
      )
    )

    val conditionalCreateIndex = If(
      Exists(indexObj),
      Get(indexObj),
      createCustomerIndex
    )

    for {
      createClassResult <- client.query(conditionalCreateCustomer)
      createIndexResult <- client.query(conditionalCreateIndex)
    } yield {
      logger.info(s"Created customer class :: \n${JsonUtil.toJson(createClassResult)}")
      logger.info(s"Created customer_by_id index :: \n${JsonUtil.toJson(createIndexResult)}")
      new Customer(client)
    }
  }
}


