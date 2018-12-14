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
package exchange.model

import io.funcqrs.Tags
import io.funcqrs.behavior.handlers.ManyEvents
import io.funcqrs.behavior.handlers.just.OneEvent
import io.funcqrs.behavior.{Behavior, Types}
import microservice._
import microservice.entity.Id

import exchange.api.access.Endpoint
import exchange.api.extent.{SpatialExtent, TemporalExtent}
import exchange.api.license.License
import exchange.api.offering._
import exchange.api.price.Price
import exchange.api.provider.ProviderId
import exchange.api.semantics.DataField

object Offering extends Types[Offering] {
  type Id = OfferingId
  type Command = OfferingCommand
  type Event = OfferingEvent

  val tag = Tags.aggregateTag("Offering")

  def create = {
    actions
      .rejectCommand {
        case cmd: OfferingCommand if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case CreateOfferingValidated(id, providerId, name, categoryUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
            extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, activation, meta) =>
            OfferingCreated(id, providerId, name, categoryUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
              extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, activation, meta)
        }
      }
      .eventHandler {
        case OfferingCreated(id, providerId, name, categoryUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
        extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, activation, meta) =>
          ActiveOffering(providerId, name, categoryUri, accessWhiteList, endpoints, outputs, inputs,
            extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, activation)
      }
  }

  def behavior(id: OfferingId) =
    Behavior
      .first {
        create
      }
      .andThen {
        case offering: Offering => offering.acceptCommands
      }
}

sealed trait Offering extends Aggregate[Offering, Offering.Command, Offering.Event]

case class ActiveOffering(providerId: ProviderId, name: String, categoryUri: String,
                          accessWhiteList: List[Id] = Nil, endpoints: List[Endpoint] = Nil,
                          outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                          extension1: Option[String], extension2: Option[String], extension3: Option[String],
                          spatialExtent: Option[SpatialExtent], temporalExtent: Option[TemporalExtent],
                          license: License, price: Price, activation: Activation) extends Offering {
  def acceptCommands =
    Offering.actions
      .rejectCommand {
        case cmd: OfferingCommand if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case DeleteOffering(id, meta) =>
            OfferingDeleted(id, providerId, meta)

          case ChangeOfferingName(id, newName, meta) if newName == name =>
            OfferingNameUnchanged(id, meta)
          case ChangeOfferingName(id, newName, meta) =>
            OfferingNameChanged(id, newName, meta)

          case ChangeOfferingCategory(id, newCategoryUri, meta) if newCategoryUri == categoryUri =>
            OfferingCategoryUnchanged(id, meta)

          case ChangeOfferingAccessWhiteList(id, newAccessWhiteList, meta) if newAccessWhiteList == accessWhiteList =>
            OfferingAccessWhiteListUnchanged(id, meta)
          case ChangeOfferingAccessWhiteList(id, newAccessWhiteList, meta) =>
            OfferingAccessWhiteListChanged(id, newAccessWhiteList, meta)

          case ChangeOfferingEndpoints(id, newEndpoints, meta) if newEndpoints == endpoints =>
            OfferingEndpointsUnchanged(id, meta)
          case ChangeOfferingEndpoints(id, newEndpoints, meta) =>
            OfferingEndpointsChanged(id, newEndpoints.map(_.toEndpoint), meta)

          case ChangeOfferingInputsValidated(id, newInputs, meta) if newInputs == inputs =>
            OfferingInputsUnchanged(id, meta)
          case ChangeOfferingInputsValidated(id, newInputs, meta) =>
            OfferingInputsChanged(id, categoryUri, newInputs, meta)

          case ChangeOfferingOutputsValidated(id, newOutputs, meta) if newOutputs == outputs =>
            OfferingOutputsUnchanged(id, meta)
          case ChangeOfferingOutputsValidated(id, newOutputData, meta) =>
            OfferingOutputsChanged(id, categoryUri, newOutputData, meta)

          case ChangeOfferingExtension1(id, newExtension1, meta) if newExtension1 == extension1 =>
            OfferingExtension1Unchanged(id, meta)
          case ChangeOfferingExtension1(id, newExtension1, meta) =>
            OfferingExtension1Changed(id, newExtension1, meta)
          case ChangeOfferingExtension2(id, newExtension2, meta) if newExtension2 == extension2 =>
            OfferingExtension2Unchanged(id, meta)
          case ChangeOfferingExtension2(id, newExtension2, meta) =>
            OfferingExtension2Changed(id, newExtension2, meta)
          case ChangeOfferingExtension1(id, newExtension3, meta) if newExtension3 == extension3 =>
            OfferingExtension3Unchanged(id, meta)
          case ChangeOfferingExtension3(id, newExtension3, meta) =>
            OfferingExtension3Changed(id, newExtension3, meta)

          case ChangeOfferingSpatialExtent(id, newSpatialExtent, meta) if newSpatialExtent == spatialExtent =>
            OfferingSpatialExtentUnchanged(id, meta)
          case ChangeOfferingSpatialExtent(id, newSpatialExtent, meta) =>
            OfferingSpatialExtentChanged(id, newSpatialExtent, meta)

          case ChangeOfferingTemporalExtent(id, newTemporalExtent, meta) if newTemporalExtent == temporalExtent =>
            OfferingTemporalExtentUnchanged(id, meta)
          case ChangeOfferingTemporalExtent(id, newTemporalExtent, meta) =>
            OfferingTemporalExtentChanged(id, newTemporalExtent, meta)

          case ChangeOfferingLicense(id, newLicense, meta) if newLicense == license =>
            OfferingLicenseUnchanged(id, meta)
          case ChangeOfferingLicense(id, newLicense, meta) =>
            OfferingLicenseChanged(id, newLicense, meta)

          case ChangeOfferingPrice(id, newPrice, meta) if newPrice == price =>
            OfferingPriceUnchanged(id, meta)
          case ChangeOfferingPrice(id, newPrice, meta) =>
            OfferingPriceChanged(id, newPrice, meta)

          case ActivateOffering(id, expirationTime, meta) if activation.status && activation.expirationTime == expirationTime =>
            OfferingActivationUnchanged(id, meta)
          case ActivateOffering(id, expirationTime, meta) =>
            OfferingActivated(id, if (expirationTime == 0) defaultActivationExpirationTime else expirationTime, meta)

          case DeactivateOffering(id, meta) if !activation.status =>
            OfferingActivationUnchanged(id, meta)
          case DeactivateOffering(id, meta) =>
            OfferingDeactivated(id, meta)
        }
      }
      .commandHandler {
        ManyEvents {
          case CreateOfferingValidated(id, _, newName, newCategoryUri, _, newAccessWhiteList, newEndpoints, newOutputs, newInputs,
          newExtension1, newExtension2, newExtension3, newSpatialExtent, newTemporalExtent, newLicense, newPrice, newActivation, meta) =>
            var events: List[OfferingEvent] = Nil
            if (newName != name) events ::= OfferingNameChanged(id, name, meta.copy())
            if (newCategoryUri != categoryUri) events ::= OfferingCategoryChanged(id, newCategoryUri, meta.copy())
            if (newAccessWhiteList != accessWhiteList) events ::= OfferingAccessWhiteListChanged(id, newAccessWhiteList, meta.copy())
            if (newEndpoints != endpoints) events ::= OfferingEndpointsChanged(id, newEndpoints, meta.copy())
            if (newInputs != inputs) events ::= OfferingInputsChanged(id, newCategoryUri, newInputs, meta.copy())
            if (newOutputs != outputs) events ::= OfferingOutputsChanged(id, newCategoryUri, newOutputs, meta.copy())
            if (newExtension1 != extension1) events ::= OfferingExtension1Changed(id, newExtension1, meta.copy())
            if (newExtension2 != extension2) events ::= OfferingExtension2Changed(id, newExtension2, meta.copy())
            if (newExtension3 != extension3) events ::= OfferingExtension3Changed(id, newExtension3, meta.copy())
            if (newSpatialExtent != spatialExtent) events ::= OfferingSpatialExtentChanged(id, newSpatialExtent, meta.copy())
            if (newTemporalExtent != temporalExtent) events ::= OfferingTemporalExtentChanged(id, newTemporalExtent, meta.copy())
            if (newLicense != license) events ::= OfferingLicenseChanged(id, newLicense, meta.copy())
            if (newPrice != price) events ::= OfferingPriceChanged(id, newPrice, meta.copy())
            if (newActivation != activation)
              if (newActivation.status) events ::= OfferingActivated(id, newActivation.expirationTime, meta.copy())
              else events ::= OfferingDeactivated(id, meta.copy())
            if (events.isEmpty)
              List(OfferingUnchanged(id, meta))
            else
              markLast(events)
          case ChangeOfferingCategory(id, newCategoryUri, meta) =>
            var events: List[OfferingEvent] = List(OfferingCategoryChanged(id, newCategoryUri, meta))
            if (inputs.nonEmpty) events ::= OfferingInputsChanged(id, newCategoryUri, Nil, meta.copy())
            if (outputs.nonEmpty) events ::= OfferingOutputsChanged(id, newCategoryUri, Nil, meta.copy())
            markLast(events)
        }
      }
      .eventHandler {
        case OfferingDeleted(_, _, _) =>
          DeletedOffering()
        case OfferingNameChanged(id, newName, _) =>
          copy(name = newName)
        case OfferingCategoryChanged(id, newCategoryUri, _) =>
          copy(categoryUri = newCategoryUri)
        case OfferingAccessWhiteListChanged(id, newAccessWhiteList, _) =>
          copy(accessWhiteList = newAccessWhiteList)
        case OfferingEndpointsChanged(id, newEndpoints, _) =>
          copy(endpoints = newEndpoints)
        case OfferingInputsChanged(id, _, newInputData, _) =>
          copy(inputs = newInputData)
        case OfferingOutputsChanged(id, _, newOutputData, _) =>
          copy(outputs = newOutputData)
        case OfferingExtension1Changed(id, newExtension1, _) =>
          copy(extension1 = newExtension1)
        case OfferingExtension2Changed(id, newExtension2, _) =>
          copy(extension2 = newExtension2)
        case OfferingExtension3Changed(id, newExtension3, _) =>
          copy(extension3 = newExtension3)
        case OfferingSpatialExtentChanged(id, newSpatialExtent, _) =>
          copy(spatialExtent = newSpatialExtent)
        case OfferingTemporalExtentChanged(id, newTemporalExtent, _) =>
          copy(temporalExtent = newTemporalExtent)
        case OfferingLicenseChanged(id, newLicense, _) =>
          copy(license = newLicense)
        case OfferingPriceChanged(id, newPrice, _) =>
          copy(price = newPrice)
        case OfferingActivated(id, expirationTime, _) =>
          copy(activation = Activation(status = true, expirationTime))
        case OfferingDeactivated(id, _) =>
          copy(activation = Inactive)
        case _ =>
          this
      }
}

case class DeletedOffering() extends Offering {
  def acceptCommands = Offering.create
}
