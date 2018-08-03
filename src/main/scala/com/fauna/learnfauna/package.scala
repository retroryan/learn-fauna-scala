package com.fauna

import scala.concurrent.duration._
import faunadb.FaunaClient
import faunadb.query.Expr
import faunadb.values.Value

import scala.concurrent.{Await, ExecutionContext, Future}

package object learnfauna {

  def genericLoggedQuery(operation: String, expr: Expr)(implicit client: FaunaClient, ec: ExecutionContext): Future[Value] = {

    val futureResult = client.query(expr)

    futureResult.foreach { result =>
      println(s"$operation: \n${JsonUtil.toJson(result)}")
    }

    futureResult
  }

  def await[T](f: Future[T]): T = Await.result(f, 5.second)

}
