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
package exchange.persistence

import java.io.NotSerializableException
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import akka.serialization.{Serializer, SerializerWithStringManifest}

import io.circe._
import io.circe.generic.extras.auto._
import io.circe.parser._
import io.circe.syntax._
import microservice._

import exchange.api.consumer.{Consumer, ConsumerEvent, ConsumerToOfferingSubscription}
import exchange.api.offering.{Offering, OfferingEvent}
import exchange.api.offeringquery.{OfferingQuery, OfferingQueryEvent, QueryToOfferingSubscription}
import exchange.api.organization.{Organization, OrganizationEvent}
import exchange.api.provider.{Provider, ProviderEvent}
import exchange.api.semantics.{OfferingCategory, SemanticsEvent}
import exchange.api.subscription.SubscriptionEvent
import microservice.entity.Entity

trait FailableSerializer {
  def error(msg: String) = {
    println(s"ERROR $msg")
    throw new NotSerializableException(msg)
  }
}

abstract class EventSerializer[E <: Event : TypeTag : Encoder : Decoder] extends SerializerWithStringManifest with FailableSerializer {
  val eventManifest: String = typeOf[E].typeSymbol.name.toString

  override def manifest(o: AnyRef) = eventManifest

  def rename(fields: (String, String))(json: Json) = json.mapObject { obj =>
    obj(fields._1).map { value =>
      obj.remove(fields._1).add(fields._2, value)
    }.getOrElse(obj)
  }

  def renameAll(fields: List[(String, String)]) = Function.chain(fields.map(rename))

  val renameDataFields = List("inputData" -> "inputs", "inputDataFields" -> "inputs", "outputData" -> "outputs", "outputDataFields" -> "outputs")
  val renameSpatialExtent = List("extent" -> "spatialExtent")

  def renameEvents: Map[String, String] = Map.empty

  def renameFields: List[(String, String)] = Nil

  val pureDecoder = implicitly[Decoder[E]]

  override def toBinary(o: AnyRef) = o match {
    case ev: E => ev.asJson.noSpaces.getBytes(UTF_8)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String) = if (manifest == eventManifest)
    parse(new String(bytes, UTF_8)) match {
      case Right(json) =>
        val decoder = if (renameFields.isEmpty) pureDecoder else
          pureDecoder.prepare { cursor =>
            val eventName = cursor.keys.get.head
            renameEvents.get(eventName).map(newEventName => cursor.withFocus(rename((eventName, newEventName))).downField(newEventName))
              .getOrElse(cursor.downField(eventName)).withFocus(renameAll(renameFields)).up
          }
        decoder.decodeJson(json) match {
          case Right(event) => event
          case Left(err) => error(s"Couldn't decode $json: $err")
        }
      case Left(err) => error(s"Couldn't parse: $err")
    } else error(s"Manifest $manifest unknown")
}

abstract class EntitySerializer[E <: Entity : Encoder : Decoder] extends Serializer with FailableSerializer {
  override def includeManifest = false

  override def toBinary(o: AnyRef) = o match {
    case entity: E => entity.asJson.noSpaces.getBytes(UTF_8)
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]) =
    parse(new String(bytes, UTF_8)) match {
      case Right(json) => implicitly[Decoder[E]].decodeJson(json) match {
        case Right(entity) => entity
        case Left(err) => error(s"Couldn't decode: $err")
      }
      case Left(err) => error(s"Couldn't parse: $err")
    }
}

class OrganizationEventSerializer extends EventSerializer[OrganizationEvent] {
  override def identifier = 200
}

class OrganizationSerializer extends EntitySerializer[Organization] {
  override def identifier = 300
}

class ProviderEventSerializer extends EventSerializer[ProviderEvent] {
  override def identifier = 201

  override def renameFields = renameDataFields ++ renameSpatialExtent
}

class ProviderSerializer extends EntitySerializer[Provider] {
  override def identifier = 301
}

class ConsumerEventSerializer extends EventSerializer[ConsumerEvent] {
  override def identifier = 202

  override def renameFields = renameDataFields ++ renameSpatialExtent
}

class ConsumerSerializer extends EntitySerializer[Consumer] {
  override def identifier = 302
}

class OfferingEventSerializer extends EventSerializer[OfferingEvent] {
  override def identifier = 203

  override def renameFields = renameDataFields ++ renameSpatialExtent

  override def renameEvents = Map(
    "OfferingTypeChanged" -> "OfferingCategoryChanged", "OfferingTypeUnchanged" -> "OfferingCategoryUnchanged",
    "OfferingInputDataChanged" -> "OfferingInputsChanged", "OfferingInputDataUnchanged" -> "OfferingInputsUnchanged",
    "OfferingOutputDataChanged" -> "OfferingOutputsChanged", "OfferingOutputDataUnchanged" -> "OfferingOutputsUnchanged")
}

class OfferingSerializer extends EntitySerializer[Offering] {
  override def identifier = 303
}

class OfferingQueryEventSerializer extends EventSerializer[OfferingQueryEvent] {
  override def identifier = 204

  override def renameFields = renameDataFields ++ renameSpatialExtent

  override def renameEvents = Map(
    "OfferingQueryTypeChanged" -> "OfferingQueryCategoryChanged", "OfferingQueryTypeUnchanged" -> "OfferingQueryCategoryUnchanged",
    "OfferingQueryInputDataChanged" -> "OfferingQueryInputsChanged", "OfferingQueryInputDataUnchanged" -> "OfferingQueryInputsUnchanged",
    "OfferingQueryOutputDataChanged" -> "OfferingQueryOutputsChanged", "OfferingQueryOutputDataUnchanged" -> "OfferingQueryOutputsUnchanged")
}

class OfferingQuerySerializer extends EntitySerializer[OfferingQuery] {
  override def identifier = 304
}

class SubscriptionEventSerializer extends EventSerializer[SubscriptionEvent] {
  override def identifier = 205
}

class QueryToOfferingSubscriptionSerializer extends EntitySerializer[QueryToOfferingSubscription] {
  override def identifier = 305
}

class ConsumerToOfferingSubscriptionSerializer extends EntitySerializer[ConsumerToOfferingSubscription] {
  override def identifier = 306
}

class SemanticsEventSerializer extends EventSerializer[SemanticsEvent] {
  override def identifier = 207

  val renameExperimental = List("experimental" -> "proposed")
  override def renameFields = renameExperimental
}

class OfferingCategorySerializer extends EntitySerializer[OfferingCategory] {
  override def identifier = 307
}

