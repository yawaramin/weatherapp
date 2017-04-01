package com.blogspot.yawar.weatherapp.backend

import io.circe.{ Decoder, Encoder }
import io.circe.generic.extras.semiauto._
import java.sql.{ ResultSet, Statement }
import org.http4s.circe._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.client.Client
import scalaz.concurrent.Task

case class Forecast(
  id: Forecast.Id, rank: Forecast.Rank, days: Seq[Forecast.Day])

object Forecast {
  type Id = Long

  object Queries {
    final val Drop: String = "drop table if exists cities"
    final val Create: String = """create table cities (
  id int identity primary key, rank int
)"""

    final val Get: String = "select id, rank from cities"
    final val GetData: String = "select id, rank from cities"
    final val GetLastRank: String = "select max(rank) from cities"

    def insert(id: Id, rank: Int): String =
      s"insert into cities (id, rank) values ($id, $rank)"

    def remove(id: Id): String = s"delete from cities where id = $id"
    def getIdFor(rank: Int): String =
      s"select id from cities where rank = $rank"

    def getHigherRankThan(rank: Int): String =
      s"select max(rank) from cities where rank < $rank"

    def getLowerRankThan(rank: Int): String =
      s"select min(rank) from cities where rank > $rank"
  }

  case class Rank(rank: Int) extends AnyVal
  object Rank { implicit val encoder: Encoder[Rank] = deriveEncoder }

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

  private case class Dto(list: Seq[Day]) extends AnyVal
  private object Dto {
    implicit val decoder: Decoder[Dto] = deriveDecoder
  }

  case class Data(id: Id, rank: Int)
  object Data {
    implicit val encoder: Encoder[Data] = deriveEncoder
    val fromResultSet: ResultSet => Data = rs =>
      Data(rs getLong 1, rs getInt 2)
  }

  implicit val encoder: Encoder[Forecast] = deriveEncoder

  private def urlFor(id: Id): String =
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

  private def resultSetToOne[A](
    rs: ResultSet)(get: ResultSet => A): A = {
    rs.first; get(rs)
  }

  // Frequently-used getters.

  private val getIntCol1: ResultSet => Int = _ getInt 1
  private val getLongCol1: ResultSet => Long = _ getLong 1

  private val httpClient: Client = PooledHttp1Client()

  def getData(stmt: Statement): Task[Seq[Data]] =
    Task(
      resultSetToSeq(
        stmt executeQuery Queries.GetData)(Data.fromResultSet))

  def getAll(stmt: Statement): Task[Seq[Forecast]] =
    Task {
      resultSetToSeq(stmt executeQuery Queries.Get) { rs =>
        (rs getLong 1, rs getInt 2) // ID, rank
      }
    } flatMap { cities =>
      Task.gatherUnordered(
        cities map { case (id, rank) =>
          httpClient.expect(urlFor(id))(jsonOf[Dto]) map { dto =>
            Forecast(id, Rank(rank), dto.list)
          }
        })
    }

  def add(stmt: Statement)(id: Id): Task[Rank] =
    Task {
      val conn = stmt.getConnection
      // Need to get last rank and insert next rank in same transaction
      conn setAutoCommit false

      val nextRank =
        resultSetToOne(
          stmt executeQuery Queries.GetLastRank)(getIntCol1) + 1

      stmt execute Queries.insert(id, nextRank)
      conn setAutoCommit true

      Rank(nextRank)
    }

  def remove(stmt: Statement)(id: Id): Task[Unit] =
    Task(stmt execute (Queries remove id))

  def moveUp(stmt: Statement)(rank: Int): Task[Unit] =
    Task {
      val conn = stmt.getConnection
      conn setAutoCommit false

      val id =
        resultSetToOne(
          stmt executeQuery (Queries getIdFor rank))(getLongCol1)

      val higherRank =
        resultSetToOne(
          stmt executeQuery (Queries getHigherRankThan rank))(
          getIntCol1)

      stmt execute s"""update cities
set rank = $rank
where rank = $higherRank"""

      stmt execute s"""update cities
set rank = $higherRank
where id = $id"""

      conn setAutoCommit true
    }

  def moveDown(stmt: Statement)(rank: Int): Task[Unit] =
    Task {
      val conn = stmt.getConnection
      conn setAutoCommit false

      val id =
        resultSetToOne(
          stmt executeQuery (Queries getIdFor rank))(getLongCol1)

      val lowerRank =
        resultSetToOne(
          stmt executeQuery (Queries getLowerRankThan rank))(getIntCol1)

      stmt execute s"""update cities
set rank = $rank
where rank = $lowerRank"""

      stmt execute s"""update cities
set rank = $lowerRank
where id = $id"""

      conn setAutoCommit true
    }
}
