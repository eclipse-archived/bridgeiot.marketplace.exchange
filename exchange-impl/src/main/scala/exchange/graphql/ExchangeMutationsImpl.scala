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
package exchange.graphql

import scala.concurrent.Promise
import akka.stream.scaladsl.SourceQueue

import microservice._
import microservice.entity.{DeletedEntity, Entity, Id}

import exchange.api._
import exchange.api.consumer._
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization._
import exchange.api.provider._
import exchange.api.semantics._
import exchange.api.subscription._

class ExchangeMutationsImpl(requesterId: Option[Id], organizationId: Option[Id],
                            commands: CommandQueues, pending: SourceQueue[PendingRequest]) extends ExchangeMutations {

  val meta = Meta(requesterId, organizationId)

  private def promise[E] = {
    val promise = Promise[E]
    pending.offer(PendingRequest(meta.requestId, promise.asInstanceOf[Promise[Entity]]))
    promise.future
  }

  def createOfferingCategory(cmd: CreateOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def deprecateOfferingCategory(cmd: DeprecateOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def undeprecateOfferingCategory(cmd: UndeprecateOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def changeOfferingCategoryName(cmd: ChangeOfferingCategoryName) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def changeOfferingCategoryParent(cmd: ChangeOfferingCategoryParent) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def addInputTypeToOfferingCategory(cmd: AddInputTypeToOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def deprecateInputTypeForOfferingCategory(cmd: DeprecateInputTypeForOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def undeprecateInputTypeForOfferingCategory(cmd: UndeprecateInputTypeForOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def addOutputTypeToOfferingCategory(cmd: AddOutputTypeToOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def deprecateOutputTypeForOfferingCategory(cmd: DeprecateOutputTypeForOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def undeprecateOutputTypeForOfferingCategory(cmd: UndeprecateOutputTypeForOfferingCategory) = {
    commands.semantics.offer(cmd.copy(meta = meta))
    promise[OfferingCategory]
  }

  def createOrganization(cmd: CreateOrganization) = {
    commands.organization.offer(cmd.copy(meta = meta))
    promise[Organization]
  }

  def changeOrganizationName(cmd: ChangeOrganizationName) = {
    commands.organization.offer(cmd.copy(meta = meta))
    promise[Organization]
  }

  def addProvider(cmd: AddProvider) = {
    commands.organization.offer(cmd.copy(meta = meta))
    promise[Provider]
  }

  def deleteProvider(cmd: DeleteProvider) = {
    commands.provider.offer(cmd.copy(meta = meta))
    promise[DeletedEntity]
  }

  def changeProviderName(cmd: ChangeProviderName) = {
    commands.provider.offer(cmd.copy(meta = meta))
    promise[Provider]
  }

  def addOffering(cmd: AddOffering) = {
    commands.provider.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def deleteOffering(cmd: DeleteOffering) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[DeletedEntity]
  }

  def activateOffering(cmd: ActivateOffering) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def deactivateOffering(cmd: DeactivateOffering) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingName(cmd: ChangeOfferingName) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingSpatialExtent(cmd: ChangeOfferingSpatialExtent) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingTemporalExtent(cmd: ChangeOfferingTemporalExtent) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingCategory(cmd: ChangeOfferingCategory) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingOutputs(cmd: ChangeOfferingOutputs) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingAccessWhiteList(cmd: ChangeOfferingAccessWhiteList) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingInputs(cmd: ChangeOfferingInputs) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingLicense(cmd: ChangeOfferingLicense) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingPrice(cmd: ChangeOfferingPrice) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingExtension1(cmd: ChangeOfferingExtension1) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingExtension2(cmd: ChangeOfferingExtension2) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def changeOfferingExtension3(cmd: ChangeOfferingExtension3) = {
    commands.offering.offer(cmd.copy(meta = meta))
    promise[Offering]
  }

  def addConsumer(cmd: AddConsumer) = {
    commands.organization.offer(cmd.copy(meta = meta))
    promise[Consumer]
  }

  def deleteConsumer(cmd: DeleteConsumer) = {
    commands.consumer.offer(cmd.copy(meta = meta))
    promise[DeletedEntity]
  }

  def changeConsumerName(cmd: ChangeConsumerName) = {
    commands.consumer.offer(cmd.copy(meta = meta))
    promise[Consumer]
  }

  def subscribeConsumerToOffering(cmd: SubscribeConsumerToOffering) = {
    commands.consumer.offer(cmd.copy(meta = meta))
    promise[ConsumerToOfferingSubscription]
  }

  def unsubscribeConsumerFromOffering(cmd: UnsubscribeConsumerFromOffering) = {
    commands.consumer.offer(cmd.copy(meta = meta))
    promise[ConsumerToOfferingSubscription]
  }

  def addOfferingQuery(cmd: AddOfferingQuery) = {
    commands.consumer.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  def deleteOfferingQuery(cmd: DeleteOfferingQuery) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[DeletedEntity]
  }

def subscribeQueryToOffering(cmd: SubscribeQueryToOffering) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[QueryToOfferingSubscription]
  }

  def unsubscribeQueryFromOffering(cmd: UnsubscribeQueryFromOffering) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[QueryToOfferingSubscription]
  }

  def changeOfferingQueryName(cmd: ChangeOfferingQueryName) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  def changeOfferingQuerySpatialExtent(cmd: ChangeOfferingQuerySpatialExtent) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  def changeOfferingQueryTemporalExtent(cmd: ChangeOfferingQueryTemporalExtent) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  def changeOfferingQueryCategory(cmd: ChangeOfferingQueryCategory) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  override def changeOfferingQueryOutputs(cmd: ChangeOfferingQueryOutputs) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  override def changeOfferingQueryInputs(cmd: ChangeOfferingQueryInputs) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  def changeOfferingQueryLicense(cmd: ChangeOfferingQueryLicense) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  def changeOfferingQueryPrice(cmd: ChangeOfferingQueryPrice) = {
    commands.query.offer(cmd.copy(meta = meta))
    promise[OfferingQuery]
  }

  def trackConsumerAccess(cmds:TrackConsumerAccesses) = {
    cmds.accesses foreach  { cmd => commands.subscription.offer(cmd.copy(meta = meta)) }
    promise[BaseOfferingSubscription]
  }

  def trackProviderAccess(cmds: TrackProviderAccesses) = {
    cmds.accesses foreach { cmd => commands.subscription.offer(cmd.copy(meta = meta)) }
    promise[BaseOfferingSubscription]
  }
}

