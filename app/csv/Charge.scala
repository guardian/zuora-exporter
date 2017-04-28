package csv

import kantan.codecs.Result
import kantan.csv.{ CellDecoder, RowDecoder }

object Charge {

   /*
   delivery file format we require:
Customer_Reference //sub name
Contract_ID
Customer_Full_Name //sold to name
Customer_Job_Title
Customer_Company
Customer_Department
Customer_Address_Line_1 //  sold to address without postcode or city
Customer_Address_Line_2
Customer_Address_Line_3
Customer_Town //sold to town
Customer_PostCode // sold to post code
Delivery_Quantity // 1.0
Customer_Telephone
Property_type
Front_Door_Access
Door_Colour
House_Details
Where_to_Leave
Landmarks
Additional_Information // delivery information (from salesforce!!)
Letterbox
Source_campaign
Sent_Date // day of the run (I guess)
Delivery_Date // date we're running for
Returned_Date
Delivery_problem
Delivery_problem_notes
Charge_day // day we're running for
 "A-S00062368","","DNVdgozI30Qh47okU1c DNVdgozI30Qh47okU1c","","","","123 Fake Street, 123 Fake Street","","","Faketown","123 123","1.0",
 "","","","","","","","Papers please","","","27/04/2017","28/04/2017","","","","Friday"
 
    */
   /*
   SELECT
      Subscription.Name
      SoldToContact.Address1
      SoldToContact.Address2
      SoldToContact.City
      SoldToContact.Country
      SoldToContact.FirstName
      SoldToContact.LastName
      SoldToContact.PostalCode
      SoldToContact.State
   FROM rateplancharge
   WHERE
      (((((((((Subscription.Status != 'Draft' and Subscription.Status != 'Pending Activation') and Subscription.Status != 'Expired') and Subscription.Status != 'Suspended') and Subscription.Status != 'Pending Acceptance')
       and Account.Balance >= 0)
        and ProductRatePlanCharge.ProductType__c = 'Print Saturday')
         and Product.Name = 'Newspaper Delivery')
          and RatePlanCharge.EffectiveStartDate <= '27/04/2017')
           and RatePlanCharge.EffectiveEndDate >= '27/04/2017')

  HOLIDAY SUSPENSION
  SELECT Subscription.Name
  FROM rateplancharge
  WHERE (((RatePlanCharge.EffectiveStartDate <= '28/04/2017'
   and RatePlanCharge.HolidayEnd__c >= '28/04/2017')
    and ProductRatePlanCharge.ProductType__c = 'Adjustment')
     and RatePlanCharge.Name = 'Holiday Credit')
    */

  val Subscription_Name = 0
  val SoldToContact_Address1 = 1
  val SoldToContact_Address2 = 2
  val SoldToContact_City = 3
  val SoldToContact_Country = 4
  val SoldToContact_FirstName = 5
  val SoldToContact_LastName = 6
  val SoldToContact_PostalCode = 7
  val SoldToContact_State = 8

  case class SoldToId(value: String)
  case class ChargeFormat(
    subscriptionName: String,
    address1: String,
    address2: String,
    city: String,
    country: String,
    firstName: String,
    lastName: String,
    postCode: String,
    state: String
  )
  case class SuspensionFormat(
    subscriptionName: String
  )

  //  implicit val soldToDecoder: CellDecoder[SoldToId] =
  //    CellDecoder.from(id => Result.success(SoldToId(id)))
  //
  //  implicit val chargeDecoder: RowDecoder[ChargeFormat] =
  //    RowDecoder.decoder(Rate_Plan_Charge__Name, Product__Name, Sold_To__ID)(ChargeFormat.apply)

}
