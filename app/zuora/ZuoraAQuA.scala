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

case class QueryObject(
  name: String,
  query: String
)

object ZuoraAQuA {

  def accountQuery = QueryObject(
    "Account",
    "SELECT Account.Balance,Account.AutoPay,Account.MRR,Account.CrmId,Account.CreditBalance,Account.Currency,Account.ID," +
      "Account.IdentityId__c,Account.LastInvoiceDate,Account.Name,Account.sfContactId__c,BillToContact.ID,SoldToContact.ID FROM Account"
  )
  def contactQuery = QueryObject(
    "Contact",
    "SELECT Contact.AccountID,Contact.City,Contact.Country,Contact.ID FROM Contact"
  )
  def paymentMethodQuery = QueryObject(
    "PaymentMethod",
    "SELECT PaymentMethod.AccountID,PaymentMethod.BankTransferType," +
      "PaymentMethod.CreditCardExpirationMonth,PaymentMethod.CreditCardExpirationYear,PaymentMethod.ID," +
      "PaymentMethod.LastFailedSaleTransactionDate,PaymentMethod.LastTransactionDateTime," +
      "PaymentMethod.LastTransactionStatus,PaymentMethod.MandateID,PaymentMethod.Name," +
      "PaymentMethod.NumConsecutiveFailures,PaymentMethod.PaymentMethodStatus,PaymentMethod.PaypalBAID," +
      "PaymentMethod.SecondTokenID,PaymentMethod.TokenID,PaymentMethod.Type FROM PaymentMethod"
  )
  def subscriptionQuery = QueryObject(
    "Subscription",
    "SELECT Subscription.ActivationDate__c,Subscription.AutoRenew,Subscription.CancellationReason__c," +
      "Subscription.CancelledDate,Subscription.ContractAcceptanceDate,Subscription.ContractEffectiveDate," +
      "Subscription.IPCountry__c,Subscription.ID,Subscription.InvoiceOwnerID,Subscription.Name," +
      "Subscription.PromotionCode__c,Subscription.ReaderType__c,Subscription.ServiceActivationDate," +
      "Subscription.Status,Subscription.SubscriptionEndDate,Subscription.SubscriptionStartDate," +
      "Subscription.TermEndDate,Subscription.TermStartDate,Subscription.UpdatedByID,Subscription.Version," +
      "Account.ID,BillToContact.ID,DefaultPaymentMethod.ID,SoldToContact.ID,SubscriptionVersionAmendment.ID FROM Subscription"
  )
  def subscriptionAmendmentQuery = QueryObject(
    "SubscriptionAmendment",
    "SELECT Amendment.ContractEffectiveDate,Amendment.CreatedDate," +
      "Amendment.CustomerAcceptanceDate,Amendment.Description,Amendment.EffectiveDate,Amendment.ID," +
      "Amendment.ServiceActivationDate,Amendment.Status,Amendment.SubscriptionID,Amendment.Type FROM Amendment"
  )
  def ratePlanChargeQuery = QueryObject(
    "RatePlanCharge",
    "SELECT RatePlanCharge.ChargedThroughDate,RatePlanCharge.DMRC,RatePlanCharge.DTCV," +
      "RatePlanCharge.EffectiveEndDate,RatePlanCharge.EffectiveStartDate," +
      "RatePlanCharge.HolidayEnd__c," +
      "RatePlanCharge.ID,RatePlanCharge.MRR,RatePlanCharge.Name,RatePlanCharge.TCV,RatePlanCharge.Version,Account.ID," +
      "BillToContact.ID,DefaultPaymentMethod.ID,Product.Name,RatePlan.Name,SoldToContact.ID,Subscription.ID FROM RatePlanCharge"
  )

  val allQueries = List(
    ZuoraAQuA.accountQuery,
    ZuoraAQuA.contactQuery,
    ZuoraAQuA.paymentMethodQuery,
    ZuoraAQuA.subscriptionQuery,
    ZuoraAQuA.subscriptionAmendmentQuery,
    ZuoraAQuA.ratePlanChargeQuery
  )
}

case class Batch(
  batchId: String,
  name: String,
  status: String,
  fileId: Option[String],
  recordCount: Int,
  query: String,
  message: Option[String]
)

object Batch {
  implicit val batchFmt = Json.format[Batch]
}

class ZuoraAQuA(zuoraWs: (String => WSRequest), callbackUrl: String)(implicit system: ActorSystem) {

  def batchQuery(queries: List[QueryObject]) = {
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
            "name" -> q.name,
            "query" -> q.query,
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
      println(wsResponse.body)
      (wsResponse.json \ "batches").as[List[Batch]]
    }
  }

  def getBatchResult(batch: Batch) = {

    implicit val materializer = ActorMaterializer()

    val ws = zuoraWs(s"/file/${batch.fileId.get}")
    println(ws.uri)
    val file = s"${batch.name}.csv"
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
