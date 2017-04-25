package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSRequest
import play.api.mvc.{Action, Controller}
import zuora.ZuoraAQuA

class Application(zuoraWs: String => WSRequest) extends Controller {

  def callZuora = Action.async {
    ZuoraAQuA.batchQuery("Select * from aslkdj")(zuoraWs).map { message =>
      Ok(views.html.zuoraResult(s"http response was: $message"))
    }
  }

  def index = Action {
    Ok(views.html.index())
  }

}
