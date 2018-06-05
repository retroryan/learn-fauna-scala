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

import faunadb.values.{Codec, Value}
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

case class Customer(custID: Int, balance: Int)

object Customer extends Logging {


  val CUSTOMER_CLASS = "customers"
  val CUSTOMER_INDEX = s"$CUSTOMER_CLASS-by-name"

  implicit val userCodec: Codec[Customer] = Codec.caseClass[Customer]

  def createCustomer(customer:Customer)(implicit client:FaunaClient, ec: ExecutionContext): Future[Value] = {
    /*
     * Create a customer (record) using the customer codec
     */
    val futureResult = client.query(
      Create(
        Class(CUSTOMER_CLASS), Obj("data" -> Obj("data" -> customer))
      )
    )

    futureResult.foreach { result =>
      logger.info(s"Create \'customer\' ${customer.custID}: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }

  def readCustomer(custID: Int)(implicit client:FaunaClient, ec: ExecutionContext): Future[Value] = {

    logger.info(s"starting read customer $custID")

    /*
     * Read and return the customer
     */
    val futureResult = client.query(
      Select("data", Get(Match(Index(Customer.CUSTOMER_INDEX), custID)))
    )

   /* futureResult.foreach { customer =>
      logger.info(s"Read \'customer\' ${custID}: \n$customer")
    }
*/
    futureResult
  }

  def updateCustomer(customer:Customer)(implicit client:FaunaClient, ec: ExecutionContext): Future[Customer] = {

    logger.info(s"starting update customer")

    /*
     * Update the customer
     */
    val futureResult = client.query(
      Update(
        Select("ref", Get(Match(Index(Customer.CUSTOMER_INDEX), customer.custID))),
        Obj("data" -> Obj("data" -> customer)
        )
      )
    ).map(value => value.to[Customer].get)

    futureResult.foreach { result =>
      logger.info(s"Update \'customer\' ${customer.custID}: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }

  def deleteCustomer(custID: Int)(implicit client:FaunaClient, ec: ExecutionContext): Future[Value] = {

    logger.info(s"starting delete customer")

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

  def apply(client: FaunaClient)(implicit ec: ExecutionContext): Future[Unit] = {
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
    }
  }
}


