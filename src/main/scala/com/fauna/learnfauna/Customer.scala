
package com.fauna.learnfauna


import com.fauna.learnfauna.Address.EmptyType
import com.fauna.learnfauna.FaunaUtils.TermField
import faunadb.values._
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}
import faunadb.FaunaClient
import faunadb.query._
import faunadb.values.Value

trait Address

case class HomeAddress(city: String, state: String, dog: String) extends Address

case class WorkAddress(city: String, state: String) extends Address

case object EmptyAddress extends Address

object Address {

  implicit val addressTrait = Codec.Union[Address]("address")(
    "home" -> Codec.Record[HomeAddress],
    "work" -> Codec.Record[WorkAddress],
    "empty" -> Codec.Record(EmptyAddress)
  )

  type EmptyType = EmptyAddress.type
}

// deeply nested traits
// codec of type aliases
// type parameters
// codec on a type class


case class Customer(id: Int, balance: Int, newAddress: Address)

object Customer extends Logging {


  val CUSTOMER_CLASS = "customers"
  val CUSTOMER_INDEX = s"customer-byid"

  implicit val userCodec: Codec[Customer] = Codec.Record[Customer]

  //Original Lesson Customer Operations
  def createCustomer(customer: Customer)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {
    val createCustomerExpr = Create(
      Class(CUSTOMER_CLASS), Obj("data" -> customer)
    )

    genericLoggedQuery("Create Customer", createCustomerExpr)
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


  def createSchema(implicit client: FaunaClient, ec: ExecutionContext): Future[Unit] = {
    logger.info("starting customer create schema")

    val termFields = Seq(TermField("id"))

    for {
      createClassResult <- FaunaUtils.createClass(CUSTOMER_CLASS)
      createIndexResult <- FaunaUtils.createIndex(CUSTOMER_INDEX, CUSTOMER_CLASS, termFields, Seq())
      createIndexResult <- FaunaUtils.createClassIndex(CUSTOMER_CLASS)
    } yield {
      logger.info(s"Created customer class")
      logger.info(s"Created customer_by_id index")
    }
  }
}


