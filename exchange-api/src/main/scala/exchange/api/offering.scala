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

import akka.http.scaladsl.model.DateTime

import monocle.macros.Lenses
import sangria.macros.derive.GraphQLDescription
import microservice._
import microservice.entity.{Entity, Id}

import exchange.api.access.{Endpoint, EndpointInput}
import exchange.api.license.License
import exchange.api.price.Price
import exchange.api.provider.{Provider, ProviderId}
import exchange.api.semantics.{DataField, DataFieldInput, RdfAnnotation, RdfContext}
import exchange.api.extent.{SpatialExtent, TemporalExtent}
import exchange.api.subscription.{Subscription, SubscriptionId}
import exchange.api.subscription.SubscriptionStatus._

object offering {
  val serviceName = "Offering"

  val defaultActivationExpirationPeriod = 1000 * 60 * 60 // 1 hour
  def defaultActivationExpirationTime = DateTime.now.clicks + defaultActivationExpirationPeriod

  case class OfferingId(value: String) extends OrganizationChildId

  object OfferingId {
    implicit def fromString(aggregateId: String): OfferingId = OfferingId(aggregateId)
  }

  @GraphQLDescription("An Offering is registered on the Exchange by an Offering Provider to be consumed by an Offering Consumer")
  @Lenses
  case class Offering(id: OfferingId, name: String, provider: Option[Provider], activation: Activation,
                      rdfAnnotation: RdfAnnotation, rdfContext: Option[RdfContext], accessWhiteList: List[Id] = Nil,
                      endpoints: List[Endpoint] = Nil, outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                      extension1: Option[String] = None, extension2: Option[String] = None, extension3: Option[String] = None,
                      spatialExtent: Option[SpatialExtent],  temporalExtent: Option[TemporalExtent], license: License, price: Price) extends Entity {
    override def show(indent: String) = s"${indent}Offering($id, $name, $rdfAnnotation)"
  }

  trait OfferingSubscription extends Subscription {
    def offering: Offering
  }

  case class BaseOfferingSubscription(id: SubscriptionId, offering: Offering, status: SubscriptionStatus = Active) extends OfferingSubscription

  @GraphQLDescription("Activation status and expiration time")
  case class Activation(status: Boolean, expirationTime: Long = 0) {
    def isActive = status && expirationTime >= DateTime.now.clicks
  }
  val Inactive = Activation(status = false)

  sealed trait OfferingCommand extends Command
  sealed trait OfferingEvent extends Event

  @GraphQLDescription("Create new Offering")
  case class CreateOffering(id: OfferingId, providerId: ProviderId, name: String, rdfUri: String,
                            rdfContext: Option[RdfContext], accessWhiteList: List[Id] = Nil, endpoints: List[EndpointInput] = Nil,
                            outputs: List[DataFieldInput] = Nil, inputs: List[DataFieldInput] = Nil,
                            extension1: Option[String] = None, extension2: Option[String] = None, extension3: Option[String] = None,
                            spatialExtent: Option[SpatialExtent] = None,  temporalExtent: Option[TemporalExtent] = None,
                            license: License, price: Price, activation: Activation = Inactive, meta: Meta = Meta()) extends OfferingCommand
  case class CreateOfferingValidated(id: OfferingId, providerId: ProviderId, name: String, rdfUri: String,
                            rdfContext: Option[RdfContext], accessWhiteList: List[Id] = Nil, endpoints: List[Endpoint] = Nil,
                            outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                            extension1: Option[String] = None, extension2: Option[String] = None, extension3: Option[String] = None,
                            spatialExtent: Option[SpatialExtent] = None,  temporalExtent: Option[TemporalExtent] = None,
                            license: License, price: Price, activation: Activation = Inactive, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingCreated(id: OfferingId, providerId: ProviderId, name: String, rdfUri: String,
                             rdfContext: Option[RdfContext], accessWhiteList: List[Id] = Nil, endpoints: List[Endpoint] = Nil,
                             outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                             extension1: Option[String] = None, extension2: Option[String] = None, extension3: Option[String] = None,
                             spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                             license: License, price: Price, activation: Activation, meta: Meta) extends OfferingEvent
  case class OfferingUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Delete Offering")
  case class DeleteOffering(id: OfferingId, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingDeleted(id: OfferingId, providerId: ProviderId, meta: Meta = Meta()) extends OfferingEvent

  @GraphQLDescription("Change Offering name")
  case class ChangeOfferingName(id: OfferingId, name: String, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingNameChanged(id: OfferingId, name: String, meta: Meta) extends OfferingEvent
  case class OfferingNameUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering category")
  case class ChangeOfferingCategory(id: OfferingId, rdfUri: String, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingCategoryChanged(id: OfferingId, rdfUri: String, meta: Meta) extends OfferingEvent
  case class OfferingCategoryUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering endpoints")
  case class ChangeOfferingEndpoints(id: OfferingId, endpoints: List[EndpointInput], meta: Meta = Meta()) extends OfferingCommand
  case class OfferingEndpointsChanged(id: OfferingId, endpoints: List[Endpoint], meta: Meta) extends OfferingEvent
  case class OfferingEndpointsUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering access white list (list of Organizations that are allowed to see this Offering)")
  case class ChangeOfferingAccessWhiteList(id: OfferingId, accessWhiteList: List[Id] = Nil, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingAccessWhiteListChanged(id: OfferingId, accessWhiteList: List[Id] = Nil, meta: Meta) extends OfferingEvent
  case class OfferingAccessWhiteListUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering input data fields")
  case class ChangeOfferingInputs(id: OfferingId, inputs: List[DataFieldInput], meta: Meta = Meta()) extends OfferingCommand
  case class ChangeOfferingInputsValidated(id: OfferingId, inputs: List[DataField], meta: Meta = Meta()) extends OfferingCommand
  case class OfferingInputsChanged(id: OfferingId, categoryUri: String = "", inputs: List[DataField], meta: Meta) extends OfferingEvent
  case class OfferingInputsUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering output data fields")
  case class ChangeOfferingOutputs(id: OfferingId, outputs: List[DataFieldInput], meta: Meta = Meta()) extends OfferingCommand
  case class ChangeOfferingOutputsValidated(id: OfferingId, outputs: List[DataField], meta: Meta = Meta()) extends OfferingCommand
  case class OfferingOutputsChanged(id: OfferingId, categoryUri: String = "", outputs: List[DataField], meta: Meta) extends OfferingEvent
  case class OfferingOutputsUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering spatial extent")
  case class ChangeOfferingSpatialExtent(id: OfferingId, spatialExtent: Option[SpatialExtent] = None, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingSpatialExtentChanged(id: OfferingId, spatialExtent: Option[SpatialExtent], meta: Meta) extends OfferingEvent
  case class OfferingSpatialExtentUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering temporal extent")
  case class ChangeOfferingTemporalExtent(id: OfferingId, temporalExtent: Option[TemporalExtent] = None, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingTemporalExtentChanged(id: OfferingId, temporalExtent: Option[TemporalExtent], meta: Meta) extends OfferingEvent
  case class OfferingTemporalExtentUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering license")
  case class ChangeOfferingLicense(id: OfferingId, license: License, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingLicenseChanged(id: OfferingId, license: License, meta: Meta) extends OfferingEvent
  case class OfferingLicenseUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering price")
  case class ChangeOfferingPrice(id: OfferingId, price: Price, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingPriceChanged(id: OfferingId, price: Price, meta: Meta) extends OfferingEvent
  case class OfferingPriceUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering extension 1")
  case class ChangeOfferingExtension1(id: OfferingId, value: Option[String] = None, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingExtension1Changed(id: OfferingId, value: Option[String], meta: Meta) extends OfferingEvent
  case class OfferingExtension1Unchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering extension 2")
  case class ChangeOfferingExtension2(id: OfferingId, value: Option[String] = None, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingExtension2Changed(id: OfferingId, value: Option[String], meta: Meta) extends OfferingEvent
  case class OfferingExtension2Unchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Change Offering extension 3")
  case class ChangeOfferingExtension3(id: OfferingId, value: Option[String] = None, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingExtension3Changed(id: OfferingId, value: Option[String], meta: Meta) extends OfferingEvent
  case class OfferingExtension3Unchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Activate Offering")
  case class ActivateOffering(id: OfferingId, expirationTime: Long = 0, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingActivated(id: OfferingId, expirationTime: Long, meta: Meta) extends OfferingEvent
  case class OfferingActivationUnchanged(id: OfferingId, meta: Meta) extends OfferingEvent with Unchanged

  @GraphQLDescription("Deactivate Offering")
  case class DeactivateOffering(id: OfferingId, meta: Meta = Meta()) extends OfferingCommand
  case class OfferingDeactivated(id: OfferingId, meta: Meta) extends OfferingEvent

  // Errors
  case class OfferingDoesNotExist(override val id: Id, override val meta: Meta) extends Error(id, "OfferingDoesNotExist", meta = meta)
}
