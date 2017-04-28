package controllers

import java.io.FileInputStream

import csv.Charge.ChargeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSRequest
import play.api.mvc.{ Action, Controller }
import zuora.ZuoraAQuA
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import csv.Charge._
import filter.Filter
import play.api.libs.json.Json

import scala.concurrent.Future

class Application(zuoraClient: ZuoraAQuA) extends Controller {

  def zuoraQuery = Action.async {

    zuoraClient.batchQuery(ZuoraAQuA.allQueries).map { message =>
      Ok(views.html.zuoraResult(s"http response was: $message"))
    }
  }

  import zuora.Batch

  def zuoraCallback = Action.async { request =>
    val jobId = request.getQueryString("jobId").get
    println(s"jobId=$jobId")
    val fBatches = request.getQueryString("status") match {
      case Some("completed") => zuoraClient.getJobResults(jobId)
      case _ => Future.successful(List())
    }
    fBatches.map { batches =>
      batches.filter(_.status == "completed").foreach(b => zuoraClient.getBatchResult(b))
      Ok(Json.obj("batches" -> batches))
    }
  }

  def index = Action {
    Ok(views.html.index())
  }

  def csvCharges = Action {
    val inputFile = scala.io.Source.fromFile("/Users/jduffell/Downloads/FulfilmentChargeExport.csv", "UTF-8").reader()
    val csvReader = inputFile.asCsvReader[ChargeFormat](rfc.withHeader)
    val interesting = csvReader.take(20).collect {
      case Success(format) => format.toString
    }
    Ok(views.html.csv(interesting))
  }

  def csvSuspensions = Action {
    val inputFile = scala.io.Source.fromFile("/Users/jduffell/Downloads/FulfilmentChargeSuspensions.csv", "UTF-8").reader()
    val csvReader = inputFile.asCsvReader[SuspensionFormat](rfc.withHeader)
    val interesting = csvReader.take(20).collect {
      case Success(format) => format.toString
    }
    Ok(views.html.csv(interesting))
  }

  def filter = Action {
    val inputFile = scala.io.Source.fromFile("/Users/jduffell/Downloads/RatePlanCharge.csv", "UTF-8").reader()
    val csvReader = inputFile.asCsvReader[ChargeFormat](rfc.withHeader)
    val records = csvReader.collect {
      case Success(format) => format
    }
    val fulfil = Filter.getToFulfil(records, Filter.Run.Monday)
    Ok(views.html.csv(fulfil.map(_.toString)))
  }

}
