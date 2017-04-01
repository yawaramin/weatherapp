package com.blogspot.yawar.weatherapp.backend

import io.circe.{ Decoder, Encoder }
import io.circe.generic.extras.semiauto._
import java.sql.{ ResultSet, Statement }
import org.http4s.circe._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.client.Client
import scalaz.concurrent.Task

case class Forecast(
  id: Forecast.Id, rank: Int, cityId: Long, days: Seq[Forecast.Day])

object Forecast {
  type Id = Long

  object Queries {
    final val Drop: String = "drop table if exists forecast_ids"
    final val Create: String = """create table forecast_ids (
  id int identity primary key, city_id int, rank int unique
)"""

    final val GetCities: String =
      "select id, city_id, rank from forecast_ids"

    final val GetForecastIds: String = "select id from forecast_ids"
    final val GetLastRank: String = "select max(rank) from forecast_ids"

    def insertCityId(id: Long, rank: Int): String =
      s"insert into forecast_ids (rank, city_id) values ($rank, $id)"

    def remove(id: Id): String =
      s"delete from forecast_ids where id = $id"
  }

  case class Day(dt: Long, temp: Day.Temp, clouds: Int)
  object Day {
    case class Temp(min: Float, max: Float)
    object Temp {
      implicit val decoder: Decoder[Temp] = deriveDecoder
      implicit val encoder: Encoder[Temp] = deriveEncoder
    }

    implicit val decoder: Decoder[Day] = deriveDecoder
    implicit val encoder: Encoder[Day] = deriveEncoder
  }

  case class Dto(list: Seq[Day]) extends AnyVal
  object Dto { implicit val decoder: Decoder[Dto] = deriveDecoder }

  implicit val encoder: Encoder[Forecast] = deriveEncoder

  private def cityUrl(id: Id): String =
    "http://api.openweathermap.org/data/2.5/forecast/daily?id=%d&cnt=7&APPID=0e48a956800e3f3a3d05996495285f5b"
      .format(id)

  /**
   * Returns a sequence of items of a given type from a JDBC result set.
   */
  private def resultSetToSeq[A](
    rs: ResultSet)(get: ResultSet => A): Seq[A] = {
    var as = Seq.empty[A]

    rs.beforeFirst()
    while (rs.next) as = as :+ get(rs)
    as
  }

  private val httpClient: Client = PooledHttp1Client()

  def getIds(stmt: Statement): Task[Seq[Id]] =
    Task {
      resultSetToSeq(stmt executeQuery Queries.GetForecastIds) { rs =>
        rs getLong 1
      }
    }

  def getAll(stmt: Statement): Task[Seq[Forecast]] =
    Task {
      resultSetToSeq(stmt executeQuery Queries.GetCities) { rs =>
        // Forecast ID, city ID, rank
        (rs getLong 1, rs getLong 2, rs getInt 3)
      }
    } flatMap { citiesByForecast =>
      Task.gatherUnordered(
        citiesByForecast map { case (forecastId, cityId, rank) =>
          httpClient.expect(cityUrl(cityId))(jsonOf[Dto]) map { dto =>
            Forecast(forecastId, rank, cityId, dto.list)
          }
        })
    }

  def add(stmt: Statement)(id: Long): Task[Id] =
    Task {
      val conn = stmt.getConnection
      // Need to get last rank and insert next rank in same transaction
      conn setAutoCommit false
      var rs = stmt executeQuery Queries.GetLastRank
      rs.first
      val lastRank = rs getInt 1

      stmt.execute(
        Queries.insertCityId(id, lastRank + 1),
        Statement.RETURN_GENERATED_KEYS)

      rs = stmt.getGeneratedKeys
      rs.first
      val result = rs getLong 1
      conn setAutoCommit true
      result
    }

  def remove(stmt: Statement)(id: Id): Task[Unit] =
    Task(stmt execute (Queries remove id))

  def moveUp(id: Id): Unit = ???
  def moveDown(id: Id): Unit = ???
}
