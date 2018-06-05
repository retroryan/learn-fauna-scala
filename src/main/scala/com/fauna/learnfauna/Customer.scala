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

import grizzled.slf4j.Logging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

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

  def createCustomer(custID: Int, balance: Int): Unit = {
    /*
     * Create a customer (record)
     */
    val result = client.query(
      Create(
        Class("customers"), Obj("data" -> Obj("id" -> custID, "balance" -> balance))
      )
    )
    Await.result(result, Duration.Inf)
    logger.info(s"Create \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
  }

  def readCustomer(custID: Int): Unit = {
    /*
     * Read the customer we just created
     */
    val result = client.query(
      Select("data", Get(Match(Index("customer_by_id"), custID)))
    )
    Await.result(result, Duration.Inf)
    logger.info(s"Read \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
  }

  def updateCustomer(custID: Int, newBalance: Int): Unit = {
    /*
     * Update the customer
     */
    val result = client.query(
      Update(
        Select("ref", Get(Match(Index("customer_by_id"), custID))),
        Obj("data" -> Obj("balance" -> newBalance)
        )
      )
    )
    Await.result(result, Duration.Inf)
    logger.info(s"Update \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
  }

  def deleteCustomer(custID: Int): Unit = {
    /*
     * Delete the customer
     */
    val result = client.query(
      Delete(
        Select("ref", Get(Match(Index("customer_by_id"), custID)))
      )
    )
    logger.info(s"Delete \'customer\' ${custID}: \n${JsonUtil.toJson(result)}")
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
      CreateClass(Obj("name" -> CUSTOMER_CLASS))
    )

    val indexObj = Index(CUSTOMER_INDEX)
    val createCustomerIndex = CreateIndex(
      Obj(
        "name" -> CUSTOMER_INDEX,
        "source" -> Class(CUSTOMER_CLASS),
        "unique" -> true,
        "terms" -> Arr(Obj("field" -> Arr("data", "id")))
      )
    )

    /*
    * Create the Indexes within the database. We will use these to access record in later lessons
    */
    val createIndex = If(
      Exists(indexObj),
      Get(indexObj),
      createCustomerIndex
    )

    for {
      createClassResult <- client.query(createCustomer)
      createIndexResult <- client.query(createIndex)
    } yield {
      logger.info(s"Created customer class :: \n${JsonUtil.toJson(createClassResult)}")
      logger.info(s"Created customer_by_id index :: \n${JsonUtil.toJson(createIndexResult)}")
      new Customer(client)
    }
  }
}


