import play.api.ApplicationLoader.Context
import play.api.{ BuiltInComponentsFromContext, Configuration }
import play.api.routing.Router
import controllers._
import router.Routes

import scala.concurrent.duration._
import play.api.libs.ws.ahc.AhcWSComponents
import com.gu.googleauth.GoogleAuthConfig
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{ WSAuthScheme, WSRequest }
import zuora.{ ZuoraAQuA, ZuoraCreds }

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with StrictLogging {

  def mandatoryConfig(key: String): String = configuration.getString(key).getOrElse(sys.error(s"Missing config key: $key"))

  val googleAuthConfig = GoogleAuthConfig(
    clientId = mandatoryConfig("google.clientId"),
    clientSecret = mandatoryConfig("google.clientSecret"),
    redirectUrl = mandatoryConfig("google.redirectUrl"),
    domain = "guardian.co.uk"
  )

  val zuoraCreds = {
    val username = mandatoryConfig("secret.zuora.username")
    logger.info(s"using username $username")
    ZuoraCreds(username, mandatoryConfig("secret.zuora.password"))
  }

  val zuoraAQuAApi = mandatoryConfig("zuora.aquaApi.baseUrl")

  val zuoraWs: String => WSRequest = { url =>
    wsClient.url(zuoraAQuAApi + url).withAuth(zuoraCreds.username, zuoraCreds.password, WSAuthScheme.BASIC)
  }

  val callbackUrl = mandatoryConfig("zuora.aquaApi.callbackUrl")
  lazy val zuoraApi = new ZuoraAQuA(zuoraWs, callbackUrl)(actorSystem)

  val applicationController = new Application(zuoraApi)
  val healthcheckController = new Healthcheck
  val loginController = new Login(wsClient, googleAuthConfig)
  val assets = new Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, applicationController, healthcheckController, loginController, assets)
}

