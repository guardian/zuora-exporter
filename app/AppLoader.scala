import com.gu.cm.ConfigurationLoader
import play.api.libs.logback.LogbackLoggerConfigurator
import play.api.{ Application, ApplicationLoader }
import play.api.ApplicationLoader.Context

class AppLoader extends ApplicationLoader {
  def appName(context: Context): String = context.initialConfiguration.getString("play.application.name")
    .getOrElse(throw new RuntimeException("Please define a default application name in application.conf with the property play.application.name"))

  override def load(context: Context): Application = {
    val contextWithConfiguration = ConfigurationLoader.playContext(appName(context), context)
    new LogbackLoggerConfigurator().configure(context.environment)
    new AppComponents(contextWithConfiguration).application
  }
}

