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

import exchange.api.consumer.{Consumer, ConsumerId}
import exchange.api.extent.{SpatialExtent, TemporalExtent}
import exchange.api.license.License
import exchange.api.offering._
import exchange.api.price.Price
import exchange.api.semantics.{DataField, DataFieldInput, RdfAnnotation, RdfContext}
import exchange.api.subscription.SubscriptionId
import exchange.api.subscription.SubscriptionStatus.{Active, SubscriptionStatus}

object offeringquery {

  val serviceName = "OfferingQuery"

  case class OfferingQueryId(value: String) extends OrganizationChildId

  object OfferingQueryId {
    implicit def fromString(aggregateId: String): OfferingQueryId = OfferingQueryId(aggregateId)
  }

  @GraphQLDescription("Offering Query to find and subscribe matching Offerings")
  @Lenses
  case class OfferingQuery(id: OfferingQueryId, name: String, consumer: Option[Consumer] = None,
                           rdfContext: Option[RdfContext], rdfAnnotation: Option[RdfAnnotation],
                           outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                           spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                           license: Option[License], price: Option[Price],
                           subscriptions: List[QueryToOfferingSubscription] = Nil) extends Entity {
    override def show(indent: String) = s"${indent}OfferingQuery($id, $name, $rdfAnnotation)"
  }

  @GraphQLDescription("Result of subscribing an OfferingQuery to an Offering")
  @Lenses
  case class QueryToOfferingSubscription(id: SubscriptionId, offering: Offering, accessToken: String, query: Option[OfferingQuery] = None,
                                         status: SubscriptionStatus = Active) extends OfferingSubscription

  @GraphQLDescription("List of OfferingQueries")
  case class OfferingQueries(offeringQueries: List[OfferingQuery])

  sealed trait OfferingQueryCommand extends Command
  sealed trait OfferingQueryEvent extends Event

  @GraphQLDescription("New OfferingQuery to be created")
  case class CreateOfferingQuery(id: OfferingQueryId, consumerId: ConsumerId, name: String, rdfUri: Option[String], rdfContext: Option[RdfContext],
                                 outputs: List[DataFieldInput] = Nil, inputs: List[DataFieldInput] = Nil, 
                                 spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                                 license: Option[License], price: Option[Price], meta: Meta = Meta()) extends OfferingQueryCommand
  case class CreateOfferingQueryValidated(id: OfferingQueryId, consumerId: ConsumerId, name: String, rdfUri: Option[String], rdfContext: Option[RdfContext],
                                 outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                                 spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                                 license: Option[License], price: Option[Price], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryCreated(id: OfferingQueryId, consumerId: ConsumerId, name: String, rdfUri: Option[String], rdfContext: Option[RdfContext],
                                  outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                                  spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                                  license: Option[License], price: Option[Price], meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Delete OfferingQuery")
  case class DeleteOfferingQuery(id: OfferingQueryId, meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryDeleted(id: OfferingQueryId, consumerId: ConsumerId, meta: Meta = Meta()) extends OfferingQueryEvent

  @GraphQLDescription("Change OfferingQuery name")
  case class ChangeOfferingQueryName(id: OfferingQueryId, name: String, meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryNameChanged(id: OfferingQueryId, name: String, meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryNameUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Change OfferingQuery category")
  case class ChangeOfferingQueryCategory(id: OfferingQueryId, rdfUri: Option[String], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryCategoryChanged(id: OfferingQueryId, rdfUri: Option[String], meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryCategoryUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Change OfferingQuery input data fields")
  case class ChangeOfferingQueryInputs(id: OfferingQueryId, inputs: List[DataFieldInput], meta: Meta = Meta()) extends OfferingQueryCommand
  case class ChangeOfferingQueryInputsValidated(id: OfferingQueryId, inputs: List[DataField], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryInputsChanged(id: OfferingQueryId, inputs: List[DataField], meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryInputsUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Change OfferingQuery output data fields")
  case class ChangeOfferingQueryOutputs(id: OfferingQueryId, outputs: List[DataFieldInput], meta: Meta = Meta()) extends OfferingQueryCommand
  case class ChangeOfferingQueryOutputsValidated(id: OfferingQueryId, outputs: List[DataField], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryOutputsChanged(id: OfferingQueryId, outputs: List[DataField], meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryOutputsUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Change OfferingQuery spatial extent")
  case class ChangeOfferingQuerySpatialExtent(id: OfferingQueryId, spatialExtent: Option[SpatialExtent], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQuerySpatialExtentChanged(id: OfferingQueryId, spatialExtent: Option[SpatialExtent], meta: Meta) extends OfferingQueryEvent
  case class OfferingQuerySpatialExtentUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Change OfferingQuery temporal extent")
  case class ChangeOfferingQueryTemporalExtent(id: OfferingQueryId, temporalExtent: Option[TemporalExtent], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryTemporalExtentChanged(id: OfferingQueryId, temporalExtent: Option[TemporalExtent], meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryTemporalExtentUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Change OfferingQuery license")
  case class ChangeOfferingQueryLicense(id: OfferingQueryId, license: Option[License], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryLicenseChanged(id: OfferingQueryId, license: Option[License], meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryLicenseUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Change OfferingQuery price")
  case class ChangeOfferingQueryPrice(id: OfferingQueryId, price: Option[Price], meta: Meta = Meta()) extends OfferingQueryCommand
  case class OfferingQueryPriceChanged(id: OfferingQueryId, price: Option[Price], meta: Meta) extends OfferingQueryEvent
  case class OfferingQueryPriceUnchanged(id: OfferingQueryId, meta: Meta) extends OfferingQueryEvent with Unchanged

  @GraphQLDescription("Subscribe OfferingQuery to Offering")
  case class SubscribeQueryToOffering(id: OfferingQueryId, offeringId: Id, meta: Meta = Meta()) extends OfferingQueryCommand
  case class SubscribedQueryToOffering(id: OfferingQueryId, offeringId: Id, meta: Meta) extends OfferingQueryEvent

  @GraphQLDescription("Unsubscribe OfferingQuery from Offering")
  case class UnsubscribeQueryFromOffering(id: OfferingQueryId, offeringId: Id, meta: Meta = Meta()) extends OfferingQueryCommand
  case class UnsubscribedQueryFromOffering(id: OfferingQueryId, offeringId: Id, meta: Meta) extends OfferingQueryEvent

  // Errors
  case class OfferingQueryDoesNotExist(override val id: Id, override val meta: Meta) extends Error(id, "OfferingQueryDoesNotExist", meta = meta)
  case class CannotChangeOfferingQueryDoesNotExist(override val id: Id, override val meta: Meta) extends Error(id, "OfferingQueryDoesNotExist", meta = meta)

  case class QueryAlreadySubscribedToOffering(override val id: Id, offeringId: Id, override val meta: Meta)
    extends Error(id, "QueryAlreadySubscribedToOffering", s"Query $id, Offering $offeringId", meta)
  case class QueryAlreadySubscribedToOfferings(override val id: Id, offeringIds: List[Id], override val meta: Meta)
    extends Error(id, "QueryAlreadySubscribedToOfferings", s"Query $id, Offerings $offeringIds", meta)
  case class QueryNotSubscribedToOffering(override val id: Id, offeringId: Id, override val meta: Meta)
    extends Error(id, "QueryNotSubscribedToOffering", s"Query $id, Offering $offeringId", meta)
  case class QueryNotSubscribedToOfferings(override val id: Id, offeringIds: List[Id], override val meta: Meta)
    extends Error(id, "QueryNotSubscribedToOfferings", s"Query $id, Offerings $offeringIds", meta)

}
