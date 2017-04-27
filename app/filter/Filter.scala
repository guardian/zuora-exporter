package filter

import csv.Charge.ChargeFormat

object Filter {

  sealed abstract class Run(val chargeName: String)
  object Run {

    case object Monday extends Run("Monday")

  }

  case class FulfilRecord(nameAddressTemp: String)

  def getToFulfil(charges: TraversableOnce[ChargeFormat], run: Run): TraversableOnce[FulfilRecord] = {
    val chargesToFulfil = charges.filter { charge =>
      val correctDay = charge.name == run.chargeName
      val correctProduct = charge.productName == "Newspaper Delivery"
      correctDay && correctProduct
    }
    chargesToFulfil.map { charge =>
      FulfilRecord(charge.toString) // lol
    }
  }

}
