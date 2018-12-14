/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Atos IT Solutions and Services GmbH, National University of Ireland Galway, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package exchange.api

import scala.language._

import io.circe.{Decoder, Encoder, HCursor, Json}
import sangria.macros.derive.GraphQLDescription

object price {

  case class Currency(value: String)

  val EUR = Currency("EUR")
  /* BGN, CZK, DKK, GBP, HRK, HUF, RON, SEK */

  lazy val currencies = List(EUR)

  @GraphQLDescription("Monetary amount with Currency")
  case class Money(amount: BigDecimal, currency: Currency)

  case class PricingModel(value: String)

  val FREE = PricingModel("FREE")
  val PER_MONTH = PricingModel("PER_MONTH")
  val PER_ACCESS = PricingModel("PER_ACCESS")
  val PER_BYTE = PricingModel("PER_BYTE")

  lazy val pricingModels = List(FREE, PER_MONTH, PER_ACCESS, PER_BYTE)

  @GraphQLDescription("Price for accessing an Offering")
  case class Price(pricingModel: PricingModel, money: Option[Money])

  // converter
  implicit def stringToCurrency(str: String): Currency = Currency(str)
  implicit def currencyToString(currency: Currency): String = currency.value

  implicit def encodeCurrency = new Encoder[Currency] {
    final def apply(currency: Currency): Json = Json.fromString(currency.value)
  }

  implicit val decodeCurrency = new Decoder[Currency] {
    final def apply(c: HCursor): Decoder.Result[Currency] = c.as[String] map stringToCurrency
  }

  implicit def stringToPricingModel(str: String): PricingModel = PricingModel(str)
  implicit def pricingModelToString(pricingModel: PricingModel): String = pricingModel.value

  implicit def encodePricingModel = new Encoder[PricingModel] {
    final def apply(pricingModel: PricingModel): Json = Json.fromString(pricingModel.value)
  }

  implicit val decodePricingModel = new Decoder[PricingModel] {
    final def apply(c: HCursor): Decoder.Result[PricingModel] = c.as[String] map stringToPricingModel
  }

}
