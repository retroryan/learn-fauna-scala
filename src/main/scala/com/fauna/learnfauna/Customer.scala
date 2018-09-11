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

import com.fauna.learnfauna.FaunaUtils.TermField
import com.fauna.learnfauna.NewAddress.Asset
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
import faunadb.values.{FieldPath, ObjectV, Result, Value}

trait Region

case class State(name: String)

case class MultiStates(name: String)

trait Address extends Region

case class OldWorkAddress(city: String, state: String) extends Address

trait NewAddress extends Region

case class HomeAddress(city: String, state: String, dog: String) extends NewAddress

case class WorkAddress(city: String, state: String) extends NewAddress

case object EmptyAddress extends NewAddress

case object Asset extends NewAddress

object Address {

  implicit val addressTrait = Codec.Union[Address]("address")(
    "oldWorkAddress" -> Codec.Record[OldWorkAddress]
  )
}

object NewAddress {

  implicit val newAddressTrait = Codec.Union[NewAddress]("newAddress")(
    "home" -> Codec.Record[HomeAddress],
    "work" -> Codec.Record[WorkAddress],
    "empty" -> Codec.Record(EmptyAddress),
    "asset" -> Codec.Record(Asset)
  )


  type Asset = Asset.type

}

// deeply nested traits
// codec of type aliases
// type parameters
// codec on a type class


case class Customer(id: Int, balance: Int, newAddress: Asset)

case class ProtoCustomer(id: Int, balance: Int, address: Address, newAddress: NewAddress)

case class NewCustomer[+T <: Region](id: Int, balance: Int, address: Address, newAddress: T)

object Customer extends Logging {


  val CUSTOMER_CLASS = "customers"
  val CUSTOMER_INDEX = s"customer-byid"

  implicit val userCodec: Codec[Customer] = Codec.Record[Customer]

  //Lesson 5 Customer Operations
  def create20Customers()(implicit client: FaunaClient, ec: ExecutionContext) = {

    val futureResult = client.query(
      Map((100 to 120).toList,
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
            Match(Index(Customer.CUSTOMER_INDEX), 2),
            Match(Index(Customer.CUSTOMER_INDEX), 3)
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

    println(s"Reading Customer: $custID")

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


  def createSchema(implicit client: FaunaClient, ec: ExecutionContext): Future[Unit] = {
    logger.info("starting customer create schema")

    val termFields = Seq(TermField("id"))

    for {
      createClassResult <- FaunaUtils.createClass(CUSTOMER_CLASS)
      createIndexResult <- FaunaUtils.createIndex(CUSTOMER_INDEX, CUSTOMER_CLASS, termFields, Seq())
      createIndexResult <- FaunaUtils.createClassIndex(CUSTOMER_CLASS)
    } yield {
      logger.info(s"Created customer class :: \n${JsonUtil.toJson(createClassResult)}")
      logger.info(s"Created customer_by_id index :: \n${JsonUtil.toJson(createIndexResult)}")
    }
  }
}


