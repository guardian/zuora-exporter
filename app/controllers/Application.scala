package controllers

import java.io.FileInputStream

import csv.Charge.ChargeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSRequest
import play.api.mvc.{ Action, Controller }
import zuora.ZuoraAQuA
import kantan.csv._
import kantan.csv.ops._
import csv.Charge._
import filter.Filter

class Application(zuoraWs: String => WSRequest) extends Controller {

  def callZuora = Action.async {
    ZuoraAQuA.batchQuery("Select * from aslkdj")(zuoraWs).map { message =>
      Ok(views.html.zuoraResult(s"http response was: $message"))
    }
  }

  def index = Action {
    Ok(views.html.index())
  }

  def csv = Action {
    val inputFile = scala.io.Source.fromFile("/Users/jduffell/Downloads/RatePlanCharge.csv", "UTF-8").reader()
    val csvReader = inputFile.asCsvReader[ChargeFormat](rfc.withHeader)
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
