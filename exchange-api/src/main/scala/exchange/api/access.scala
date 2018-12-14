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

object access {

  case class EndpointType(value: String)

  val HTTP_GET = EndpointType("HTTP_GET")
  val HTTP_POST = EndpointType("HTTP_POST")
  val WEBSOCKET = EndpointType("WEBSOCKET")

  lazy val endpointTypes = List(HTTP_GET, HTTP_POST, WEBSOCKET)

  case class AccessInterfaceType(value: String)

  val BIGIOT_LIB = AccessInterfaceType("BIGIOT_LIB")
  val BIGIOT_PROXY = AccessInterfaceType("BIGIOT_PROXY")
  val EXTERNAL = AccessInterfaceType("EXTERNAL")

  lazy val accessInterfaceTypes = List(BIGIOT_LIB, BIGIOT_PROXY, EXTERNAL)

  @GraphQLDescription("Endpoint to access Offering")
  case class EndpointInput(endpointType: EndpointType, uri: String, accessInterfaceType: Option[AccessInterfaceType]) {
    def toEndpoint = Endpoint(endpointType, uri, accessInterfaceType.getOrElse(BIGIOT_LIB))
  }

  @GraphQLDescription("Endpoint to access Offering")
  case class Endpoint(endpointType: EndpointType, uri: String, accessInterfaceType: AccessInterfaceType)

  //converter
  implicit def stringToEndpointType(str: String): EndpointType = EndpointType(str)
  implicit def endpointTypeToString(endpointType: EndpointType): String = endpointType.value

  implicit def encodeEndpointType = new Encoder[EndpointType] {
    final def apply(endpointType: EndpointType): Json = Json.fromString(endpointType.value)
  }

  implicit val decodeEndpointType = new Decoder[EndpointType] {
    final def apply(c: HCursor): Decoder.Result[EndpointType] = c.as[String] map stringToEndpointType
  }

  implicit def stringToAccessInterfaceType(str: String): AccessInterfaceType = AccessInterfaceType(str)
  implicit def accessInterfaceTypeToString(accessInterfaceType: AccessInterfaceType): String = accessInterfaceType.value

  implicit def encodeAccessInterfaceType = new Encoder[AccessInterfaceType] {
    final def apply(accessInterfaceType: AccessInterfaceType): Json = Json.fromString(accessInterfaceType.value)
  }

  implicit val decodeAccessInterfaceType = new Decoder[AccessInterfaceType] {
    final def apply(c: HCursor): Decoder.Result[AccessInterfaceType] = c.as[String] map stringToAccessInterfaceType
  }

}
