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

object license {

  case class License(value: String)

  val CREATIVE_COMMONS = License("CREATIVE_COMMONS")
  val OPEN_DATA_LICENSE = License("OPEN_DATA_LICENSE")
  val NON_COMMERCIAL_DATA_LICENSE = License("NON_COMMERCIAL_DATA_LICENSE")
  val PROJECT_INTERNAL_USE_ONLY = License("PROJECT_INTERNAL_USE_ONLY")

  lazy val licenses = List(CREATIVE_COMMONS, OPEN_DATA_LICENSE, NON_COMMERCIAL_DATA_LICENSE, PROJECT_INTERNAL_USE_ONLY)

  //converter
  implicit def stringToLicense(str: String): License = License(str)
  implicit def licenseToString(license: License): String = license.value

  implicit def encodeLicense = new Encoder[License] {
    final def apply(license: License): Json = Json.fromString(license.value)
  }

  implicit val decodeLicense = new Decoder[License] {
    final def apply(c: HCursor): Decoder.Result[License] = c.as[String] map stringToLicense
  }

}
