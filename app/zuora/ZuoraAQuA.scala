package zuora

import play.api.http.Writeable
import play.api.libs.json.{ JsPath, JsValue, Json, Writes }
import play.api.libs.ws.{ WSClient, WSRequest }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object ZuoraAQuA {

  case class QueryObject(query: String)

  implicit def jsWriteable[A](implicit wa: Writes[A], wjs: Writeable[JsValue]): Writeable[A] = wjs.map(a => Json.toJson(a))

  implicit val queryWriter: Writes[QueryObject] = new Writes[QueryObject] {
    override def writes(queryObject: QueryObject): JsValue = {
      Json.obj("asdf" -> queryObject.query)
    }
  }

  def batchQuery(query: String) = { zuoraWs: (String => WSRequest) =>
    val ws = zuoraWs("https://www.zuora.com/apps/api/batch-query/")
    ws.post(QueryObject(query)).map { wsResponse =>
      wsResponse.status + wsResponse.body
    }
  }

}
