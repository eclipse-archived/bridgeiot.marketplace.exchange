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

import monocle.macros.Lenses
import sangria.macros.derive.GraphQLDescription
import microservice._
import microservice.entity.{Entity, Id}

import exchange.api.extent.{SpatialExtent, TemporalExtent}
import exchange.api.license.License
import exchange.api.offering.{Offering, OfferingSubscription}
import exchange.api.offeringquery.OfferingQuery
import exchange.api.organization.{Organization, OrganizationId}
import exchange.api.price.Price
import exchange.api.semantics.{DataFieldInput, RdfContext}
import exchange.api.subscription.SubscriptionId
import exchange.api.subscription.SubscriptionStatus.{Active, SubscriptionStatus}

object consumer {

  val serviceName = "Consumer"

  case class ConsumerId(value: String) extends OrganizationChildId


  object ConsumerId {
    implicit def fromString(aggregateId: String): ConsumerId = ConsumerId(aggregateId)
  }

  @GraphQLDescription("Offering Consumer (Application or Service)")
  @Lenses
  case class Consumer(id: ConsumerId, name: String, organization: Option[Organization] = None, queries: List[OfferingQuery] = Nil,
                      subscriptions: List[ConsumerToOfferingSubscription] = Nil) extends Entity {
    override def show(indent: String) = (s"${indent}Consumer($id, $name):" +: queries.map(_.show(indent+spaces))).mkString("\n")
  }

  @GraphQLDescription("Result of subscribing a Consumer to an Offering")
  @Lenses
  case class ConsumerToOfferingSubscription(id: SubscriptionId, offering: Offering, accessToken: String, consumer: Option[Consumer] = None,
                                            status: SubscriptionStatus = Active) extends OfferingSubscription

  sealed trait ConsumerCommand extends Command
  sealed trait ConsumerEvent extends Event

  case class CreateConsumer(id: ConsumerId, organizationId: OrganizationId, name: String, meta: Meta = Meta()) extends ConsumerCommand
  case class ConsumerCreated(id: ConsumerId, organizationId: OrganizationId, name: String, secret: String, meta: Meta) extends ConsumerEvent
  case class ConsumerUnchanged(id: ConsumerId, meta: Meta) extends ConsumerEvent with Unchanged

  @GraphQLDescription("Delete Consumer")
  case class DeleteConsumer(id: ConsumerId, meta: Meta = Meta()) extends ConsumerCommand
  case class ConsumerDeleted(id: ConsumerId, organizationId: OrganizationId, meta: Meta = Meta()) extends ConsumerEvent

  @GraphQLDescription("Change Consumer name")
  case class ChangeConsumerName(id: ConsumerId, name: String, meta: Meta = Meta()) extends ConsumerCommand
  case class ConsumerNameChanged(id: ConsumerId, name: String, meta: Meta) extends ConsumerEvent
  case class ConsumerNameUnchanged(id: ConsumerId, meta: Meta) extends ConsumerEvent with Unchanged

  @GraphQLDescription("Add new Offering")
  case class AddOfferingQuery(id: ConsumerId, name: String, localId: Option[Id], rdfContext: Option[RdfContext], rdfUri: Option[String],
                              outputs: List[DataFieldInput] = Nil, inputs: List[DataFieldInput] = Nil,
                              spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                              license: Option[License], price: Option[Price], meta: Meta = Meta()) extends ConsumerCommand
  case class OfferingQueryAdded(id: ConsumerId, name: String, localId: Option[Id], rdfContext: Option[RdfContext], rdfUri: Option[String],
                                outputs: List[DataFieldInput] = Nil, inputs: List[DataFieldInput] = Nil,
                                spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                                license: Option[License], price: Option[Price], meta: Meta) extends ConsumerEvent

  @GraphQLDescription("Subscribe Consumer to Offering")
  case class SubscribeConsumerToOffering(id: ConsumerId, offeringId: Id, meta: Meta = Meta()) extends ConsumerCommand
  case class SubscribedConsumerToOffering(id: ConsumerId, offeringId: Id, meta: Meta) extends ConsumerEvent

  @GraphQLDescription("Unsubscribe Consumer from Offering")
  case class UnsubscribeConsumerFromOffering(id: ConsumerId, offeringId: Id, meta: Meta = Meta()) extends ConsumerCommand
  case class UnsubscribedConsumerFromOffering(id: ConsumerId, offeringId: Id, meta: Meta) extends ConsumerEvent

  // Errors
  case class ConsumerDoesNotExist(override val id: Id, override val meta: Meta) extends Error(id, "ConsumerDoesNotExist", meta = meta)

  case class ConsumerAlreadySubscribedToOffering(override val id: Id, offeringId: Id, override val meta: Meta)
    extends Error(id, "ConsumerAlreadySubscribedToOffering", s"Consumer $id, Offering $offeringId", meta)
  case class ConsumerAlreadySubscribedToOfferings(override val id: Id, offeringIds: List[Id], override val meta: Meta)
    extends Error(id, "ConsumerAlreadySubscribedToOfferings", s"Consumer $id, Offerings $offeringIds", meta)
  case class ConsumerNotSubscribedToOffering(override val id: Id, offeringId: Id, override val meta: Meta)
    extends Error(id, "ConsumerNotSubscribedToOffering", s"Consumer $id, Offering $offeringId", meta)
  case class ConsumerNotSubscribedToOfferings(override val id: Id, offeringIds: List[Id], override val meta: Meta)
    extends Error(id, "ConsumerNotSubscribedToOfferings", s"Consumer $id, Offerings $offeringIds", meta)

}
