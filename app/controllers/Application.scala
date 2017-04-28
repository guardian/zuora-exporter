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

import scala.concurrent.Future

class Application(zuoraClient: ZuoraAQuA) extends Controller {

  def zuoraQuery = Action.async {

    val accountQuery = "SELECT " +
      //                  "Account.AccountBalance," +
      "Account.AutoPay," +
      //      "Account.CMRR," +
      //                  "Account.CRMAccountID," +
      "Account.CreditBalance,Account.Currency,Account.ID," +
      //      "Account.IdentityId," +
      "Account.LastInvoiceDate,Account.Name," +
      //      "Account.sfContactId," +
      "BillToContact.ID," +
      "SoldToContact.ID FROM Account"
    val contactQuery = "SELECT Contact.AccountID,Contact.City,Contact.Country,Contact.ID FROM Contact"
    val paymentMethodQuery = "SELECT PaymentMethod.AccountID,PaymentMethod.BankTransferType," +
      "PaymentMethod.CreditCardExpirationMonth,PaymentMethod.CreditCardExpirationYear,PaymentMethod.ID," +
      "PaymentMethod.LastFailedSaleTransactionDate,PaymentMethod.LastTransactionDateTime," +
      "PaymentMethod.LastTransactionStatus,PaymentMethod.MandateID,PaymentMethod.Name," +
      "PaymentMethod.NumConsecutiveFailures,PaymentMethod.PaymentMethodStatus,PaymentMethod.PaypalBAID," +
      "PaymentMethod.SecondTokenID,PaymentMethod.TokenID,PaymentMethod.Type FROM PaymentMethod"
    val subscriptionQuery = "SELECT Subscription.ActivationDate__c,Subscription.AutoRenew," +
      "Subscription.CancellationReason__c,Subscript ion.CancelledDate,Subscription.ContractAcceptanceDate," +
      "Subscription.ContractEffectiveDate,Subscription.IPCountry__c,Subscription.ID,Subscription.InvoiceOwnerID," +
      "Subscription.Name,Subscription.PromotionCode__c,Subscription.ReaderType__c,Subscription.ServiceActivationDate," +
      "Subscription.Status,Subscription.SubscriptionEndDate,Subscription.SubscriptionStartDate," +
      "Subscription.TermEndDate,Subscription.TermStartDate,Subscription.UpdatedByID,Subscription.Version," +
      "Account.ID,BillToContact.ID,DefaultPaymentMethod.ID,SoldToContact.ID,SubscriptionVersionAmendment.ID FROM Subscription"
    val subscriptionAmendmentQuery = "SELECT Amendment.ContractEffectiveDate,Amendment.CreatedDate," +
      "Amendment.CustomerAcceptanceDate,Amendment.Description,Amendment.EffectiveDate,Amendment.ID," +
      "Amendment.ServiceActivationDate,Amendment.Status,Amendment.SubscriptionID,Amendment.Type FROM Amendment"
    val ratePlanChargeQuery = "SELECT RatePlanCharge.ChargedThroughDate,RatePlanCharge.DMRC,RatePlanCharge.DTCV," +
      "RatePlanCharge.EffectiveEndDate,RatePlanCharge.EffectiveStartDate,RatePlanCharge.HolidayEnd__c," +
      "RatePlanCharge.ID,RatePlanCharge.MRR,RatePlanCharge.Name,RatePlanCharge.TCV,RatePlanCharge.Version,Account.ID," +
      "BillToContact.ID,DefaultPaymentMethod.ID,Product.Name,RatePlan.Name,SoldToContact.ID,Subscription.ID FROM RatePlanCharge"

    val queries = List(
      accountQuery
    //      contactQuery,
    //      paymentMethodQuery,
    //      subscriptionQuery,
    //      subscriptionAmendmentQuery,
    //      ratePlanChargeQuery
    )

    zuoraClient.batchQuery(queries).map { message =>
      Ok(views.html.zuoraResult(s"http response was: $message"))
    }
  }

  import zuora.Batch

  def zuoraCallback = Action { request =>
    val jobId = request.getQueryString("jobId").get
    val fBatches = request.getQueryString("status") match {
      case Some("completed") => zuoraClient.getJobResults(jobId)
      case _ => Future.successful(List())
    }
    fBatches.map { batch =>
      batch.foreach(b => zuoraClient.getResultsFile(b.fileId.get))
    }

    Ok("ok")
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
