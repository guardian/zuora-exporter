package filter

import csv.Charge.ChargeFormat

object Filter {

  sealed abstract class Run(val chargeName: String)
  object Run {

    case object Monday extends Run("Monday")

  }

  case class FulfilRecord(nameAddressTemp: String)

  def getToFulfil(charges: TraversableOnce[ChargeFormat], run: Run): TraversableOnce[FulfilRecord] = {

    List(1).map { charge =>
      FulfilRecord(charge.toString) // lol
    }
  }

}
