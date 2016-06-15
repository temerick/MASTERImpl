package org.oseraf.bullseye.webapp.api

import com.typesafe.scalalogging.slf4j.Logging
import org.oseraf.bullseye.service.DataService.{BullsEyeEntity, DataService, PrincipalAwareDataService}
import org.json4s.JsonAST.{JArray, JObject, JString}
import java.security.Principal

trait DataAPI extends API with DataService with PrincipalAwareDataService with Logging {

  get("/resolve") {
    val principal = request.getUserPrincipal
    logUser(principal, "resolve")
    resolve(principal, params("eId"), params.getAs[Int]("limit"))
  }

  get("/deduplicate") {
    val principal = request.getUserPrincipal
    logUser(principal, "deduplicate")
    deduplicate(principal)
  }

  get("/search") {
    val principal = request.getUserPrincipal
    logUser(principal, "search")
    search(principal, params("query"), params("searchTypeId"), params.getAs[Int]("limit"))
  }

  post("/merge") {
    val principal = request.getUserPrincipal
    logUser(principal, "merge")
    val body = parsedBody match {
      case JObject(o) => o.toMap.map {
        case (key, entity: JObject) => (key, entity.extract[BullsEyeEntity])
        case (key, JArray(r)) => (
          key,
          r.map {
            case JString(v) => v
          }
        )
      }
    }

    merge(principal,
      body("eIds").asInstanceOf[List[String]].toSeq,
      body("entity").asInstanceOf[BullsEyeEntity]
    )
  }

  post("/split") {
    val principal = request.getUserPrincipal
    logUser(principal, "split")
    val body = parsedBody match {
      case JObject(o) => o.toMap.map {
        case (key, JString(id)) => (key, id)
        case (key, JArray(a)) => (key, a.map {
          case o: JObject => o.extract[BullsEyeEntity]
        })
      }
    }

    split(principal, body("eId").asInstanceOf[String], body("entities").asInstanceOf[List[BullsEyeEntity]])
  }

  get("/entity/:eId") {
    val principal = request.getUserPrincipal
    logUser(principal, "entityDetails")
    getBullsEyeEntityDetails(principal, params("eId"))
  }

  get("/entity/:eId/neighborhood") {
    val principal = request.getUserPrincipal
    logUser(principal, "getNeighborhood")
    getNeighborhood(principal, params("eId"))
  }

  get("/nNeighborhoodForSearch") {
    val principal = request.getUserPrincipal
    val key: String = params("key")
    val value: String = params("value")
    val n: Int = params.getAs[Int]("n").get
    getNNeighborhoodForSearch(principal, key, value, n)
  }

  get("/nNeighborhood") {
    val principal = request.getUserPrincipal
    val entityId = params("eId")
    val n = params.getAs[Int]("n").get
    getNNeighborhood(principal, entityId, n)
  }

  get("/searchtypes") {
    bullseyeSearchTypes
  }
  private def logUser(p:Principal, methodName:String) = {
    if (p == null) logger.info(methodName + " issued by un-authenticated user")
    else                   logger.info(methodName + " issued by " + p.getName)
  }
}
