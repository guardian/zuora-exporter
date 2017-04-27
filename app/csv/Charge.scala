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
   
   
  val Rate_Plan_Charge__Charged_Through_Date = 0
  val Rate_Plan_Charge__DMRC = 1
  val Rate_Plan_Charge__DTCV = 2
  val Rate_Plan_Charge__Effective_End_Date = 3
  val Rate_Plan_Charge__Effective_Start_Date = 4
  val Rate_Plan_Charge__Holiday_End = 5
  val Rate_Plan_Charge__ID = 6
  val Rate_Plan_Charge__MRR = 7
  val Rate_Plan_Charge__Name = 8
  val Rate_Plan_Charge__TCV = 9
  val Rate_Plan_Charge__Version = 10
  val Account__ID = 11
  val Bill_To__ID = 12
  val Default_Payment_Method__ID = 13
  val Product__Name = 14
  val Rate_Plan__Name = 15
  val Sold_To__ID = 16
  val Subscription__ID = 17

  case class SoldToId(value: String)
  case class ChargeFormat(name: String, productName: String, soldTo: SoldToId)

  implicit val soldToDecoder: CellDecoder[SoldToId] =
    CellDecoder.from(id => Result.success(SoldToId(id)))

  implicit val chargeDecoder: RowDecoder[ChargeFormat] =
    RowDecoder.decoder(Rate_Plan_Charge__Name, Product__Name, Sold_To__ID)(ChargeFormat.apply)

}
