package com.fauna.learnfauna

import faunadb.FaunaClient
import faunadb.query._
import faunadb.values.{Encoder, Value}
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

object FaunaUtils extends Logging {

  /**
    * This takes the following steps:
    * - Create an admin client to the fauna db
    * - Conditionally create a new fauna database
    * - Create a key to the newly created database
    * - Close the admin client connection
    * - Create and return a new fauna client
    *
    * @param ec - implicit execution context
    * @return
    */
  def createFaunaClient(implicit ec: ExecutionContext): FaunaClient = {
    logger.info("starting create fauna client")

    val faunaDBConfig = FaunaDBConfig.getFaunaDBConfig

    //Create an admin client. This is the client we will use to create the database
    val adminClient = FaunaClient(faunaDBConfig.secret, faunaDBConfig.url)

    logger.info("Successfully connected to FaunaDB as Admin!")

    val databaseRequest = createFaunaDatabase(faunaDBConfig.dbName, faunaDBConfig.deleteDB, adminClient)

    //only block on startup when we create the database
    val createDBResponse = await(databaseRequest)

    logger.info(s"Created database: ${faunaDBConfig.dbName}")

    /*
    * Create a key specific to the database we just created. We will use this to
    * create a new client we will use in the remainder of the examples.
    */
    await(databaseRequest)
    val eventualValue = adminClient.query(CreateKey(Obj("database" -> Database(faunaDBConfig.dbName), "role" -> "server")))
    val keyReq = getEventualData[String](eventualValue, "secret")
    val serverKey = await(keyReq)
    adminClient.close
    FaunaClient(serverKey, faunaDBConfig.url)
  }

  /*
 * The code below conditionally creates a Fauna Database depending on the application configuration.
 * If the flag for deleting the database is true it will check if the database already exists,
 * delete it and create a new database.
 * If the flag for deleting is false it will check if the DB already exists return it otherwise create a new instance.
 */
  private def createFaunaDatabase(dbName: String, deleteDB: Boolean, adminClient: FaunaClient)(implicit ec: ExecutionContext) = {
    if (deleteDB) {
      logger.info(s"deleting existing database")
      adminClient.query(
        Do(
          If(
            Exists(Database(dbName)),
            Delete(Database(dbName)),
            true
          ),
          CreateDatabase(Obj("name" -> dbName)))
      )
    }
    else {
      logger.info(s"creating or getting database")
      adminClient.query(
        If(
          Exists(Database(dbName)),
          Get(Database(dbName)),
          CreateDatabase(Obj("name" -> dbName))
        )
      )
    }
  }

  def createClass(className: String)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {
    val classObj = Class(className)
    val createClass = CreateClass(Obj("name" -> className))
    val conditionalCreateClass = If(
      Exists(classObj),
      Get(classObj),
      createClass
    )

    client.query(conditionalCreateClass)
  }

  /**
    * Sealed Trait of Possible Index Field Types
    */
  sealed trait Field

  case class TermField(name: String, casefold: Boolean = false) extends Field

  case class ValueField(name: String, reverse: Boolean = false) extends Field

  case class NestedValueField(n1: String, n2: String, reverse: Boolean = false) extends Field

  case class RefField(name: String = "ref") extends Field

  case class Binding(name: String, expr: Expr) extends Field

  /**
    * Conditionally create a standard Fauna Index with terms and values. This gets the index if it already exists
    * otherwise it creates it.
    *
    * @param indexName - Name of the Index
    * @param className - Source Class of the Index
    * @param terms     - Terms to search by in the Index
    * @param values    - Values to return from a search of the index
    * @param client    - implicit Fauna Client
    * @param ec        - implicit execution context
    * @return
    */
  def createIndex(indexName: String, className: String, terms: Seq[Field], values: Seq[Field], optBinding: Option[Binding] = None)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {
    val indexObj = Index(indexName)

    val termsArr = terms.collect {
      case term: TermField =>
        if (term.casefold)
          Obj("field" -> Arr("data", term.name), "transform" -> "casefold")
        else
          Obj("field" -> Arr("data", term.name))
      case refField: RefField => Obj("field" -> Arr("ref"))
    }

    val valuesArr = values.collect {
      case value: ValueField => Obj("field" -> Arr("data", value.name), "reverse" -> value.reverse)
      case nestedValue: NestedValueField => Obj("field" -> Arr("data", nestedValue.n1, nestedValue.n2), "reverse" -> nestedValue.reverse)
      case refField: RefField => Obj("field" -> Arr("ref"))
      case binding: Binding =>  Obj("binding" -> binding.name)
    }

    val source: (String, Expr) = optBinding match {
      case None => "source" -> Class(className)
      case Some(binding) => "source" -> Obj(
        "class" -> Class(className),
        "fields" -> Obj(binding.name -> binding.expr)
      )
    }

    val createIndex =
      CreateIndex(
        Obj(
          "name" -> indexName,
          source,
          "terms" -> Arr(termsArr: _*),
          "values" -> Arr(valuesArr: _*)
        )
      )

    logger.debug(s"created index: $createIndex")

    val conditionalCreateIndex = If(
      Exists(indexObj),
      Get(indexObj),
      createIndex
    )

    client.query(conditionalCreateIndex)
  }

  /**
    * Conditionally create a Fauna Class Index. A class index lists allows indexing all the values in the class
    * by matching against the index with no terms and then paging over the results. This gets the index if it already exists
    * otherwise it creates it.
    *
    * @param className - Source Class of the Index
    * @param client    - implicit Fauna Client
    * @param ec        - implicit execution context
    * @return
    */
  def createClassIndex(className: String)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {
    val indexName = s"all_${className}s"

    val indexObj = Index(indexName)

    val createIndex = CreateIndex(
      Obj(
        "name" -> indexName,
        "source" -> Class(className)
      )
    )
    val conditionalCreateIndex = If(
      Exists(indexObj),
      Get(indexObj),
      createIndex
    )
    client.query(conditionalCreateIndex)
  }

  /**
    * Save an instance of a fauna class
    *
    * @param clazz  - The name of the class
    * @param value  - The value of the class
    * @param client - implicit Fauna Client
    * @param ec     - implicit execution context
    * @tparam A - The type of the class that is of a type Encoder
    * @return - The results of save the type
    */
  def save[A: Encoder](clazz: String, value: A)(implicit client: FaunaClient, ec: ExecutionContext): Future[String] = {
    val eventualValue = client.query(
      Create(
        Class(clazz),
        Obj("data" -> value))
    )

    if (logger.isDebugEnabled) {
      eventualValue.foreach {
        strz => println(s"created: $strz")
      }
    }

    eventualValue.map(v => v.toString)
  }
}
