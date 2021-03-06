package com.blogspot.yawar.weatherapp.backend

import io.circe.syntax._
import org.h2.jdbc.JdbcSQLException
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.dsl._

object BackendService {
  val service: HttpService = {
    Class forName "org.h2.Driver"

    val conn = java.sql.DriverManager getConnection "jdbc:h2:mem:"
    val stmt = conn.createStatement

    stmt execute Forecast.Queries.Drop
    stmt execute Forecast.Queries.Create

    HttpService {
      case GET -> Root / "forecasts" =>
        Ok(Forecast refresh stmt map (_.asJson))

      case GET -> Root / "forecast_data" =>
        Ok(Forecast getData stmt map (_.asJson))

      case POST -> Root / "add" / LongVar(id) =>
        Forecast
          .add(stmt)(id)
          .flatMap(forecast => Ok(forecast.asJson))
          .handleWith {
            case _: JdbcSQLException => Conflict(id.asJson)
          }

      case POST -> Root / "remove" / IntVar(id) =>
        Forecast.remove(stmt)(id) flatMap (_ => Ok())

      case POST -> Root / "move" / "up" / IntVar(id) =>
        Forecast
          .moveUp(stmt)(id)
          .flatMap(forecast => Ok(forecast.asJson))
          .handleWith { case _: JdbcSQLException => NotFound() }

      case POST -> Root / "move" / "down" / IntVar(id) =>
        Forecast
          .moveDown(stmt)(id)
          .flatMap(forecast => Ok(forecast.asJson))
          .handleWith { case _: JdbcSQLException => NotFound() }
    }
  }
}
