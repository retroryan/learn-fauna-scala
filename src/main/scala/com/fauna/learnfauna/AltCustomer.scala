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

import faunadb.FaunaClient
import faunadb.query._
import faunadb.values.{Codec, Value}
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}


case class AltCustomer(id: Int, balance: Double)


object AltCustomer extends Logging {


  val ALT_CUSTOMER_CLASS = "alt_customers"
  val ALT_CUSTOMER_INDEX_BY_ID = "customer_by_id"

  implicit val altUserCodec: Codec[AltCustomer] = Codec.caseClass[AltCustomer]

  def createCustomer(customer: AltCustomer)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {
    val createCustomerExpr = Create(
      Class(ALT_CUSTOMER_CLASS), Obj("data" -> customer)
    )

    genericLoggedQuery("Create Alt Customer", createCustomerExpr)
  }

  def readCustomer(custID: Int)(implicit client: FaunaClient, ec: ExecutionContext) = {

    val futureResult = client.query(
      Select("data", Paginate(Match(Index(ALT_CUSTOMER_INDEX_BY_ID), custID)))
    )

    await(futureResult)
    futureResult.foreach { customer =>
      logger.info(s"read customer query: \n$customer")
    }

    // val eventualCustomer = futureResult.map(value => value.to[AltCustomer].get)
    // eventualCustomer
  }

  def createSchema(client: FaunaClient)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info("starting alt customer create schema")

    /*
     * Create an class to hold customers
     */
    val customerObj = Class(ALT_CUSTOMER_CLASS)
    val createCustomer = CreateClass(Obj("name" -> ALT_CUSTOMER_CLASS))
    val conditionalCreateCustomer = If(
      Exists(customerObj),
      Get(customerObj),
      createCustomer
    )

    val indexObj = Index(ALT_CUSTOMER_INDEX_BY_ID)
    val createCustomerIndex = CreateIndex(
      Obj(
        "name" -> ALT_CUSTOMER_INDEX_BY_ID,
        "source" -> Class(ALT_CUSTOMER_CLASS),
        "unique" -> true,
        "terms" -> Arr(Obj("field" -> Arr("data", "id"))),
        "values" -> Arr(Obj("field" -> Arr("data", "balance")))
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
