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

import faunadb.values._
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

case class Customer(id: Int, balance: Int)

object Customer extends Logging {


  val CUSTOMER_CLASS = "customers"
  val CUSTOMER_INDEX = s"$CUSTOMER_CLASS-by-name"

  //val CUSTOMER_INDEX_BY_ID = "customer_by_id"

  implicit val userCodec: Codec[Customer] = Codec.caseClass[Customer]

  //Lesson 5 Customer Operations
  def create20Customers()(implicit client: FaunaClient, ec: ExecutionContext) = {

    val futureResult = client.query(
      Map((1 to 20).toList,
        Lambda { id =>
          Create(
            Class(Customer.CUSTOMER_CLASS),
            Obj("data" -> Obj("id" -> id, "balance" -> Multiply(id, 10)))
          )
        }
      )
    )

    //If needed you can get a list of reference id's from the query using:
    val listOfRefIds = futureResult.map { customerList =>
      val refVList = customerList.collect(Field("ref").to[RefV]).get
      val listOfRefIds = refVList.map(ref => ref.id)
      logger.info(s"listOfRefIds: $listOfRefIds")
    }


    //this maps the created customer list to a list of the actual Customer instances created
    futureResult.map { customerList =>
      customerList.collect(Field("data").to[Customer]).get
    }
  }

  def readGroupCustomers()(implicit client: FaunaClient, ec: ExecutionContext): Future[Seq[Customer]] = {

    println(s"readCustomerByIds")

    val futureResult = client.query(
      Map(
        Paginate(
          Union(
            Match(Index(Customer.CUSTOMER_INDEX), 1),
            Match(Index(Customer.CUSTOMER_INDEX), 3),
            Match(Index(Customer.CUSTOMER_INDEX), 8)
          )
        ),
        Lambda { x => Select("data", Get(x)) }
      )
    ).map(value => value("data").to[Seq[Customer]].get)

    futureResult.foreach { customer =>
      logger.info(s"Read Group Customers -> $customer")
    }

    futureResult
  }

  def readCustomerByIds()(implicit client: FaunaClient, ec: ExecutionContext): Future[Seq[Customer]] = {

    println(s"readCustomerByIds")

    val range = List(1, 3, 6, 7)
    val futureResult = client.query(
      Map(
        Paginate(
          Union(
            Map(range,
              Lambda { y => Match(Index(Customer.CUSTOMER_INDEX), y) }
            )
          )
        ),
        Lambda { x => Select("data", Get(x)) }
      )
    ).map(value => value("data").to[Seq[Customer]].get)

    futureResult.foreach { customer =>
      logger.info(s"Read Customers by ID -> $customer")
    }

    futureResult
  }

  //Original Lesson Customer Operations
  def createCustomer(customer: Customer)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {
    val createCustomerExpr = Create(
      Class(CUSTOMER_CLASS), Obj("data" -> customer)
    )

    genericLoggedQuery("Create Customer", createCustomerExpr)
  }

  def createListCustomer(customerList: Seq[Customer])(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {

    /*
     * Create a list of customer (records) using the customer codec
     *
     * What is neat about this example is it passes the Scala list directly to the Fauna query.
     * This is possible because the customer codec converts it into the correct list type
     *
     */
    val futureResult = client.query(
      Foreach(
        customerList,
        Lambda { customer =>
          Create(
            Class(CUSTOMER_CLASS),
            Obj("data" -> customer))
        }))

    futureResult.foreach { result =>
      logger.info(s"Created ${customerList.size} customers: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }

  def readCustomer(custID: Int)(implicit client: FaunaClient, ec: ExecutionContext): Future[Customer] = {

    val futureResult = client.query(
      Select("data", Get(Match(Index(Customer.CUSTOMER_INDEX), custID)))
    ).map(value => value.to[Customer].get)

    futureResult.foreach { customer =>
      logger.info(s"Read \'customer\' $custID: \n$customer")
    }

    futureResult
  }

  def updateCustomer(customer: Customer)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {

    val updateCustomerExp = Update(
      Select("ref", Get(Match(Index(Customer.CUSTOMER_INDEX), customer.id))),
      Obj("data" -> customer)
    )

    genericLoggedQuery("Update the customer", updateCustomerExp)
  }

  def deleteCustomer(custID: Int)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {

    val deleteCustomerExp =
      Delete(
        Select("ref", Get(Match(Index(Customer.CUSTOMER_INDEX), custID)))
      )

    genericLoggedQuery("Delete the customer", deleteCustomerExp)
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
    * Create the Indexes within the database. We will use these to access customer records by ID.
    *
    * IMPORTANT - the field name here must match the field name in the Customer class because it is automatically derived
    * as part of the custom codex
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


