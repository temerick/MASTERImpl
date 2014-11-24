package org.oseraf.bullseye.webapp.api

import org.oseraf.bullseye.service.DataService.{BullsEyeEntity, DataService}
import org.json4s.JsonAST.{JArray, JObject, JString}


trait DataAPI extends API with DataService {

  get("/resolve") {
    resolve(params("eId"), params.getAs[Int]("limit"))
  }

  get("/deduplicate") {
    deduplicate()
  }

  get("/search") {
    search(params("query"), params("searchTypeId"), params.getAs[Int]("limit"))
  }

  post("/merge") {
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

    merge(
      body("eIds").asInstanceOf[List[String]].toSeq,
      body("entity").asInstanceOf[BullsEyeEntity]
    )
  }

  post("/split") {
    val body = parsedBody match {
      case JObject(o) => o.toMap.map {
        case (key, JString(id)) => (key, id)
        case (key, JArray(a)) => (key, a.map {
          case o: JObject => o.extract[BullsEyeEntity]
        })
      }
    }

    split(body("eId").asInstanceOf[String], body("entities").asInstanceOf[List[BullsEyeEntity]])
  }

  get("/entity/:eId") {
    getBullsEyeEntityDetails(params("eId"))
  }

  get("/entity/:eId/neighborhood") {
    getNeighborhood(params("eId"))
  }

  get("/searchtypes") {
    bullseyeSearchTypes
  }
}
