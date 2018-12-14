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

import scala.concurrent.Future

import sangria.macros.derive.{GraphQLDescription, GraphQLField}
import microservice.entity.DeletedEntity

import exchange.api.consumer._
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization._
import exchange.api.provider._
import exchange.api.semantics._
import exchange.api.subscription._

@GraphQLDescription("BIG IoT Exchange mutations")
trait ExchangeMutations {
  @GraphQLField
  @GraphQLDescription("Create new OfferingCategory")
  def createOfferingCategory(input: CreateOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Deprecate OfferingCategory")
  def deprecateOfferingCategory(input: DeprecateOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Undeprecate OfferingCategory")
  def undeprecateOfferingCategory(input: UndeprecateOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Change OfferingCategory name")
  def changeOfferingCategoryName(input: ChangeOfferingCategoryName): Future[OfferingCategory]

//  @GraphQLField
//  @GraphQLDescription("Change OfferingCategory parent")
//  def changeOfferingCategoryParent(input: ChangeOfferingCategoryParent): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Add recommended input type to OfferingCategory")
  def addInputTypeToOfferingCategory(input: AddInputTypeToOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Deprecate input type for OfferingCategory")
  def deprecateInputTypeForOfferingCategory(input: DeprecateInputTypeForOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Undeprecate input type for OfferingCategory")
  def undeprecateInputTypeForOfferingCategory(input: UndeprecateInputTypeForOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Add recommended output type to OfferingCategory")
  def addOutputTypeToOfferingCategory(input: AddOutputTypeToOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Deprecate output type for OfferingCategory")
  def deprecateOutputTypeForOfferingCategory(input: DeprecateOutputTypeForOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Undeprecate output type for OfferingCategory")
  def undeprecateOutputTypeForOfferingCategory(input: UndeprecateOutputTypeForOfferingCategory): Future[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Create new Organization")
  def createOrganization(input: CreateOrganization): Future[Organization]

  @GraphQLField
  @GraphQLDescription("Change Organization name")
  def changeOrganizationName(input: ChangeOrganizationName): Future[Organization]

  @GraphQLField
  @GraphQLDescription("Add Provider")
  def addProvider(input: AddProvider): Future[Provider]

  @GraphQLField
  @GraphQLDescription("Delete Provider")
  def deleteProvider(input: DeleteProvider): Future[DeletedEntity]

  @GraphQLField
  @GraphQLDescription("Change Provider name")
  def changeProviderName(input: ChangeProviderName): Future[Provider]

  @GraphQLField
  @GraphQLDescription("Add Offering")
  def addOffering(input: AddOffering): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Delete Offering")
  def deleteOffering(input: DeleteOffering): Future[DeletedEntity]

  @GraphQLField
  @GraphQLDescription("Activate Offering")
  def activateOffering(input: ActivateOffering): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Deactivate Offering")
  def deactivateOffering(input: DeactivateOffering): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering name")
  def changeOfferingName(input: ChangeOfferingName): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering spatial extent")
  def changeOfferingSpatialExtent(input: ChangeOfferingSpatialExtent): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering temporal extent")
  def changeOfferingTemporalExtent(input: ChangeOfferingTemporalExtent): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering category")
  def changeOfferingCategory(input: ChangeOfferingCategory): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering output data fields")
  def changeOfferingAccessWhiteList(input: ChangeOfferingAccessWhiteList): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering output data fields")
  def changeOfferingOutputs(input: ChangeOfferingOutputs): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering input data fields")
  def changeOfferingInputs(input: ChangeOfferingInputs): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering license")
  def changeOfferingLicense(input: ChangeOfferingLicense): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering price")
  def changeOfferingPrice(input: ChangeOfferingPrice): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering extension 1")
  def changeOfferingExtension1(input: ChangeOfferingExtension1): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering extension 2")
  def changeOfferingExtension2(input: ChangeOfferingExtension2): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Change Offering extension 3")
  def changeOfferingExtension3(input: ChangeOfferingExtension3): Future[Offering]

  @GraphQLField
  @GraphQLDescription("Add Consumer")
  def addConsumer(input: AddConsumer): Future[Consumer]

  @GraphQLField
  @GraphQLDescription("Delete Consumer")
  def deleteConsumer(input: DeleteConsumer): Future[DeletedEntity]

  @GraphQLField
  @GraphQLDescription("Change Consumer name")
  def changeConsumerName(input: ChangeConsumerName): Future[Consumer]

  @GraphQLField
  @GraphQLDescription("Subscribe Consumer to Offering")
  def subscribeConsumerToOffering(input: SubscribeConsumerToOffering): Future[ConsumerToOfferingSubscription]

  @GraphQLField
  @GraphQLDescription("Delete Subscription")
  def unsubscribeConsumerFromOffering(input: UnsubscribeConsumerFromOffering): Future[ConsumerToOfferingSubscription]

  @GraphQLField
  @GraphQLDescription("Add OfferingQuery")
  def addOfferingQuery(input: AddOfferingQuery): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Delete OfferingQuery")
  def deleteOfferingQuery(input: DeleteOfferingQuery): Future[DeletedEntity]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery name")
  def changeOfferingQueryName(input: ChangeOfferingQueryName): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery spatial extent")
  def changeOfferingQuerySpatialExtent(input: ChangeOfferingQuerySpatialExtent): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery temporal extent")
  def changeOfferingQueryTemporalExtent(input: ChangeOfferingQueryTemporalExtent): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery category")
  def changeOfferingQueryCategory(input: ChangeOfferingQueryCategory): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery output data fields")
  def changeOfferingQueryOutputs(input: ChangeOfferingQueryOutputs): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery input data fields")
  def changeOfferingQueryInputs(input: ChangeOfferingQueryInputs): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery license")
  def changeOfferingQueryLicense(input: ChangeOfferingQueryLicense): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Change OfferingQuery price")
  def changeOfferingQueryPrice(input: ChangeOfferingQueryPrice): Future[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("Subscribe OfferingQuery to Offering")
  def subscribeQueryToOffering(input: SubscribeQueryToOffering): Future[QueryToOfferingSubscription]

  @GraphQLField
  @GraphQLDescription("Delete Subscription")
  def unsubscribeQueryFromOffering(input: UnsubscribeQueryFromOffering): Future[QueryToOfferingSubscription]

  @GraphQLField
  @GraphQLDescription("Track access from consumer lib")
  def trackConsumerAccess(input: TrackConsumerAccesses): Future[BaseOfferingSubscription]

  @GraphQLField
  @GraphQLDescription("Track access from provider lib")
  def trackProviderAccess(input: TrackProviderAccesses): Future[BaseOfferingSubscription]

}
