package com.blogspot.yawar.weatherapp.backend

import io.circe.syntax._
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
        Ok(Forecast getAll stmt map (_.asJson))

      case GET -> Root / "forecast_ids" =>
        Ok(Forecast getIds stmt map (_.asJson))

      case POST -> Root / "add" / LongVar(id) =>
        Created(Forecast.add(stmt)(id) map (_.asJson))

      case POST -> Root / "remove" / IntVar(id) =>
        Forecast.remove(stmt)(id) flatMap (_ => NoContent())

      case POST -> Root / "move" / "up" / IntVar(id) =>
        Forecast moveUp id; NoContent()

      case POST -> Root / "move" / "down" / IntVar(id) =>
        Forecast moveDown id; NoContent()
    }
  }
}
