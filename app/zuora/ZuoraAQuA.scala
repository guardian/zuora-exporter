package zuora

import java.io.FileOutputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.http.Writeable
import play.api.libs.json.{ JsPath, JsValue, Json, Writes }
import play.api.libs.ws.{ WSClient, WSRequest }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class Batch(
  batchId: String,
  name: String,
  status: String,
  fileId: Option[String],
  recordCount: Int,
  query: String,
  message: String
)

object Batch {
  implicit val batchFmt = Json.format[Batch]
}

class ZuoraAQuA(zuoraWs: (String => WSRequest), callbackUrl: String)(implicit system: ActorSystem) {

  def batchQuery(queries: List[String]) = {
    val ws = zuoraWs("/batch-query/")
    ws.post(
      Json.obj(
        "format" -> "csv",
        "version" -> "1.0",
        "name" -> "Batch_Query_for_DataLake",
        "encrypted" -> "none",
        "useQueryLabels" -> "true",
        "notifyUrl" -> s"$callbackUrl?jobId=$${JOBID}&status=$${STATUS}",
        "dateTimeUtc" -> "true",
        "queries" -> queries.map { q =>
          Json.obj(
            "name" -> "Query1",
            "query" -> q,
            "type" -> "zoqlexport"
          )
        }
      )
    ).map { wsResponse =>
        wsResponse.status + wsResponse.body
      }
  }

  def getJobResults(jobId: String) = {
    val ws = zuoraWs(s"/batch-query/jobs/$jobId")
    ws.get().map { wsResponse =>
      wsResponse.status + wsResponse.body
      (wsResponse.json \ "batches").as[List[Batch]]
    }
  }

  def getResultsFile(fileId: String) = {

    implicit val materializer = ActorMaterializer()

    val ws = zuoraWs(s"/file/$fileId")
    val file = s"$fileId.csv"
    ws.withMethod("GET").stream().flatMap {
      res =>
        val outputStream = new FileOutputStream(file)

        // The sink that writes to the output stream
        val sink = Sink.foreach[ByteString] { bytes =>
          outputStream.write(bytes.toArray)
        }

        // materialize and run the stream
        res.body.runWith(sink).andThen {
          case result =>
            // Close the output stream whether there was an error or not
            outputStream.close()
            // Get the result or rethrow the error
            result.get
        }.map(_ => file)
    }

  }

}
