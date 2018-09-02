package com.fauna

import scala.concurrent.duration._
import faunadb.FaunaClient
import faunadb.query.Expr
import faunadb.values.{Decoder, Field, Result, Value}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext, Future}

package object learnfauna {

  def genericLoggedQuery(operation: String, expr: Expr)(implicit client: FaunaClient): Future[Value] = {
    val futureResult = client.query(expr)
    futureResult.foreach { result =>
      println(s"$operation: \n${JsonUtil.toJson(result)}")
    }
    futureResult
  }

  def getEventualData[A : Decoder](eventualValue: Future[Value], key: String = "data"): Future[A] = {
    eventualValue
      .flatMap { value =>
        Future.fromTry(value(key).to[A].toEither.left.map(x => new Exception(s"errs ${x}")).toTry)
      }
  }

  //  [A : Decoder] is the same as adding the following on the end
  // (implicit ev:Decoder[A])
  def getEventualCollection[A : Decoder](eventualValue: Future[Value]): Future[Seq[A]] = {
    eventualValue
      .flatMap { value =>
        Future.fromTry(value.collect(Field.to[A]).toEither.left.map(x => new Exception(s"errs ${x}")).toTry)
      }
  }

  case class DeserializationException(message: String) extends Exception


  implicit class ResultToTry[A](val x: Result[A]) {
    def asTry: scala.util.Try[A] = {
      x.toEither
        .left
        .map(errs => DeserializationException(s"$errs"))
        .toTry
    }
  }

  def await[T](f: Future[T]): T = Await.result(f, 5.second)

}
