package com.knoldus.neo4jServices.routes

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import com.knoldus.neo4jServices.factories.{DatabaseAccess, User}
import spray.json.DefaultJsonProtocol



object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val PortofolioFormats = jsonFormat4(User)
}

trait Neo4jService extends DatabaseAccess {

  import UserJsonSupport._

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val logger = Logging(system, getClass)

  implicit def myExceptionHandler = {
    ExceptionHandler {
      case e: ArithmeticException =>
        extractUri { uri =>
          complete(HttpResponse(StatusCodes.InternalServerError,
            entity = s"Data is not persisted and something went wrong"))
        }
    }
  }

  val neo4jRoutes: Route = {
    post {
      path("insert") {
        entity(as[User]) { entity =>
          complete {
            try {
              val isPersisted: Int = insertRecord(entity)
              isPersisted match {
                case 1 => HttpResponse(StatusCodes.Created,
                  entity = "Data is successfully persisted")
                case _ => HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data")
            }
          }
        }
      }
    }  ~ path("get" / "email" / Segment) { (email: String) =>
      get {
        complete {
          try {
            val idAsRDD: Option[User] = retrieveRecord(email)
            idAsRDD match {
              case Some(data) =>
                HttpResponse(StatusCodes.OK, entity = data.name)
              case None => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not fetched and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for user : $email")
          }
        }
      }
    } ~ path("update" / "name" / Segment / "email" / Segment) { (name: String, email: String) =>
      get {
        complete {
          try {
            val isPersisted = updateRecord(email, name)
            isPersisted match {
              case true => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case false => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for email : $email")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for email : $email")
          }
        }
      }
    }  ~ path("createrelation" / "name" / Segment / "relation" / Segment / "user_list" / Segment) { (name: String, relation: String, user_list: String) =>
      get {
        complete {
          try {
            val friend_list: List[String] = user_list.split(":").toList
            val isPersisted: Int = createNodesWithRelation(name,friend_list,relation)
            isPersisted match {
              case data if data > 0 => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for user")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for user ")
          }
        }
      }
    } ~ path("delete" / "email" / Segment) { (email: String) =>
      get {
        complete {
          try {
            val idAsRDD = deleteRecord(email)
            idAsRDD match {
              case 1 => HttpResponse(StatusCodes.OK, entity = "Data is successfully deleted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not deleted and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for email : $email")
          }
        }
      }
    }
  }
}
