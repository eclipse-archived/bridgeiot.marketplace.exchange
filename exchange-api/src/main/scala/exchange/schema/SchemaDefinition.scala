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
package exchange.schema

import io.circe.generic.extras.auto._
import sangria.marshalling.circe._
import microservice.{Meta, circeConfiguration, decodeAggregateId, encodeAggregateId}
import sangria.macros.derive._
import sangria.schema
import sangria.schema._
import microservice.entity.{DeletedEntity, DeletedId}

import exchange.api._
import exchange.api.access._
import exchange.api.consumer._
import exchange.api.extent._
import exchange.api.license._
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization._
import exchange.api.price._
import exchange.api.provider._
import exchange.api.semantics
import exchange.api.semantics._
import exchange.api.subscription._

case class ExchangeCtx(queries: ExchangeQueries, mutations: ExchangeMutations)

object SchemaDefinition {
  // Object Types
  implicit lazy val PrefixType = deriveObjectType[Unit, Prefix]()
  implicit lazy val RdfContextType = deriveObjectType[Unit, RdfContext]()
  implicit lazy val RdfAnnotationType = deriveObjectType[Unit, RdfAnnotation]()
  implicit lazy val RdfAnnotationsType = deriveObjectType[Unit, RdfAnnotations]()

  implicit lazy val LocationType = deriveObjectType[Unit, Location]()
  implicit lazy val BoundingBoxType = deriveObjectType[Unit, BoundingBox]()
  implicit lazy val SpatialExtentType = deriveObjectType[Unit, SpatialExtent]()
  implicit lazy val TemporalExtentType = deriveObjectType[Unit, TemporalExtent]()

  implicit lazy val ValueTypeType: UnionType[Unit] = UnionType("ValueType", Some("Value type for DataField"),
    List(NumberTypeType, IntegerTypeType, BooleanTypeType, TextTypeType, DateTimeTypeType, ArrayTypeType, ObjectTypeType, RdfReferenceTypeType, UndefinedTypeType))
  implicit lazy val DataFieldType = deriveObjectType[Unit, DataField](
    ReplaceField("value", Field("value", ValueTypeType, resolve = _.value.value))
  )

  implicit lazy val NumberTypeType = deriveObjectType[Unit, NumberType](AddFields(Field("type", StringType, resolve = _.value.`type`)))
  implicit lazy val IntegerTypeType = deriveObjectType[Unit, IntegerType](AddFields(Field("type", StringType, resolve = _.value.`type`)))
  implicit lazy val BooleanTypeType = deriveObjectType[Unit, BooleanType](AddFields(Field("type", StringType, resolve = _.value.`type`)))
  implicit lazy val TextTypeType = deriveObjectType[Unit, TextType](AddFields(Field("type", StringType, resolve = _.value.`type`)))
  implicit lazy val DateTimeTypeType = deriveObjectType[Unit, DateTimeType](AddFields(Field("type", StringType, resolve = _.value.`type`)))
  implicit lazy val ArrayTypeType = deriveObjectType[Unit, ArrayType](AddFields(Field("type", StringType, resolve = _.value.`type`)),
    ReplaceField("element", Field("element", ValueTypeType, resolve = _.value.element)))
  implicit lazy val ObjectTypeType = deriveObjectType[Unit, semantics.ObjectType](AddFields(Field("type", StringType, resolve = _.value.`type`)))
  implicit lazy val RdfReferenceTypeType = deriveObjectType[Unit, RdfReferenceType](AddFields(Field("type", StringType, resolve = _.value.`type`)))
  implicit lazy val UndefinedTypeType = deriveObjectType[Unit, UndefinedType](AddFields(Field("type", StringType, resolve = _.value.`type`)))

  implicit lazy val EndpointTypeType = EnumType("EndpointType", Some("Type of Endpoint"), endpointTypes.map { value => EnumValue[EndpointType](value, value = value) })
  implicit lazy val AccessInterfaceTypeType = EnumType("AccessInterfaceType", Some("Type of AccessInterface"), accessInterfaceTypes.map { value => EnumValue[AccessInterfaceType](value, value = value) })
  implicit lazy val EndpointType1 = deriveObjectType[Unit, Endpoint]()

  implicit lazy val CurrencyType = EnumType("Currency", Some("Currency"), currencies.map { value => EnumValue[Currency](value, value = value) })
  implicit lazy val MoneyType = deriveObjectType[Unit, Money]()
  implicit lazy val PricingModelType = EnumType("PricingModel", Some("Model for Pricing"), pricingModels.map { value => EnumValue[PricingModel](value, value = value) })
  implicit lazy val PriceType = deriveObjectType[Unit, Price]()

  implicit lazy val LicenseType = EnumType("License", Some("Type of License"), licenses.map { value => EnumValue[License](value, value = value) })

  implicit lazy val CategoryIdType = ScalarAlias[OfferingCategoryId, String](StringType, _.value, s => Right(OfferingCategoryId(s)))
  implicit lazy val OfferingCategoryType: schema.ObjectType[Unit, OfferingCategory] = deriveObjectType(
    ReplaceField("subCategories", Field("subCategories", ListType(OfferingCategoryType), resolve = _.value.subCategories)))

  implicit lazy val DeletedIdType = ScalarAlias[DeletedId, String](StringType, _.value, s => Right(DeletedId(s)))
  implicit lazy val DeletedEntityType = deriveObjectType[Unit, DeletedEntity]()

  implicit lazy val OfferingIdType = ScalarAlias[OfferingId, String](StringType, _.value, s => Right(OfferingId(s)))
  implicit lazy val ActivationType = deriveObjectType[Unit, Activation]()
  implicit lazy val OfferingType: schema.ObjectType[Unit, Offering] = deriveObjectType(
    ReplaceField("provider", Field("provider", OptionType(ProviderType), resolve = _.value.provider)))

  implicit lazy val ProviderIdType = ScalarAlias[ProviderId, String](StringType, _.value, s => Right(ProviderId(s)))
  implicit lazy val ProviderType: schema.ObjectType[Unit, Provider] = deriveObjectType(
    ExcludeFields("secret"),
    ReplaceField("organization", Field("organization", OptionType(OrganizationType), resolve = _.value.organization)))

  implicit lazy val SubscriptionIdType = ScalarAlias[SubscriptionId, String](StringType, _.value, s => Right(SubscriptionId(s)))
  implicit lazy val SubscriptionStatusType = deriveEnumType[SubscriptionStatus.Value]()
  implicit lazy val OfferingSubscriptionType: UnionType[Unit] = UnionType("OfferingSubscription", None,
    List(ConsumerToOfferingSubscriptionType, QueryToOfferingSubscriptionType))
  implicit lazy val BaseOfferingSubscriptionType = deriveObjectType[Unit, BaseOfferingSubscription]()
  implicit lazy val ConsumerToOfferingSubscriptionType = deriveObjectType[Unit, ConsumerToOfferingSubscription](
    ReplaceField("consumer", Field("consumer", OptionType(ConsumerType), resolve = _.value.consumer)))
  implicit lazy val QueryToOfferingSubscriptionType = deriveObjectType[Unit, QueryToOfferingSubscription](
    ReplaceField("query", Field("query", OptionType(OfferingQueryType), resolve = _.value.query)))
  implicit lazy val SubscriptionsType = deriveObjectType[Unit, Subscriptions]()

  implicit lazy val OfferingQueryIdType = ScalarAlias[OfferingQueryId, String](StringType, _.value, s => Right(OfferingQueryId(s)))
  implicit lazy val OfferingQueryType: schema.ObjectType[Unit, OfferingQuery] = deriveObjectType(
    ReplaceField("consumer", Field("consumer", OptionType(ConsumerType), resolve = _.value.consumer)))
  implicit lazy val OfferingQueriesType = deriveObjectType[Unit, OfferingQueries]()

  implicit lazy val ConsumerIdType = ScalarAlias[ConsumerId, String](StringType, _.value, s => Right(ConsumerId(s)))
  implicit lazy val ConsumerType: schema.ObjectType[Unit, Consumer] = deriveObjectType(
    ReplaceField("organization", Field("organization", OptionType(OrganizationType), resolve = _.value.organization)))

  implicit lazy val OrganizationIdType = ScalarAlias[OrganizationId, String](StringType, _.value, s => Right(OrganizationId(s)))
  implicit lazy val OrganizationType: schema.ObjectType[Unit, Organization] = deriveObjectType()
  implicit lazy val OrganizationsType: schema.ObjectType[Unit, Organizations] = deriveObjectType()

  // Input Object Types
  implicit lazy val PrefixInputType = deriveInputObjectType[Prefix](InputObjectTypeName("PrefixInput"))
  implicit lazy val RdfContextInputType = deriveInputObjectType[RdfContext](InputObjectTypeName("RdfContextInput"))

  implicit lazy val LocationInputType = deriveInputObjectType[Location](InputObjectTypeName("LocationInput"))
  implicit lazy val BoundingBoxInputType = deriveInputObjectType[BoundingBox](InputObjectTypeName("BoundingBoxInput"))
  implicit lazy val SpatialExtentInputType = deriveInputObjectType[SpatialExtent](InputObjectTypeName("SpatialExtentInput"))
  implicit lazy val TemporalExtentInputType = deriveInputObjectType[TemporalExtent](InputObjectTypeName("TemporalExtentInput"))

  implicit lazy val ValueTypeInputType: InputObjectType[ValueTypeInput] = deriveInputObjectType[ValueTypeInput](
    ReplaceInputField("members", InputField("members", OptionInputType(ListInputType(DataFieldInputType)))),
    ReplaceInputField("element", InputField("element", OptionInputType(ValueTypeInputType))))
  implicit lazy val DataFieldInputType: InputObjectType[DataFieldInput] = deriveInputObjectType[DataFieldInput]()
  implicit lazy val EndpointInputType = deriveInputObjectType[EndpointInput]()

  implicit lazy val MoneyInputType = deriveInputObjectType[Money](InputObjectTypeName("MoneyInput"))
  implicit lazy val PriceInputType = deriveInputObjectType[Price](InputObjectTypeName("PriceInput"))

  implicit lazy val MetaInputType = deriveInputObjectType[Meta]()
  
  implicit lazy val CreateOrganizationInputType = deriveInputObjectType[CreateOrganization](ExcludeInputFields("meta"))
  implicit lazy val ChangeOrganizationNameInputType = deriveInputObjectType[ChangeOrganizationName](ExcludeInputFields("meta"))

  implicit lazy val AddProviderInputType = deriveInputObjectType[AddProvider](ExcludeInputFields("meta"))
  implicit lazy val DeleteProviderInputType = deriveInputObjectType[DeleteProvider](ExcludeInputFields("meta"))
  implicit lazy val ChangeProviderNameInputType = deriveInputObjectType[ChangeProviderName](ExcludeInputFields("meta"))

  implicit lazy val CreateOfferingCategoryInputType = deriveInputObjectType[CreateOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val DeprecateOfferingCategoryInputType = deriveInputObjectType[DeprecateOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val UndeprecateOfferingCategoryInputType = deriveInputObjectType[UndeprecateOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingCategoryNameInputType = deriveInputObjectType[ChangeOfferingCategoryName](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingCategoryParentInputType = deriveInputObjectType[ChangeOfferingCategoryParent](ExcludeInputFields("meta"))

  implicit lazy val AddInputTypeToOfferingCategoryInputType = deriveInputObjectType[AddInputTypeToOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val DeprecateInputTypeForOfferingCategoryInputType = deriveInputObjectType[DeprecateInputTypeForOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val UndeprecateInputTypeForOfferingCategoryInputType = deriveInputObjectType[UndeprecateInputTypeForOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val AddOutputTypeToOfferingCategoryInputType = deriveInputObjectType[AddOutputTypeToOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val DeprecateOutputTypeForOfferingCategoryInputType = deriveInputObjectType[DeprecateOutputTypeForOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val UndeprecateOutputTypeForOfferingCategoryInputType = deriveInputObjectType[UndeprecateOutputTypeForOfferingCategory](ExcludeInputFields("meta"))

  implicit lazy val ActivationInputType = deriveInputObjectType[Activation](InputObjectTypeName("ActivationInput"))
  implicit lazy val AddOfferingInputType = deriveInputObjectType[AddOffering](ExcludeInputFields("meta"))
  implicit lazy val DeleteOfferingInputType = deriveInputObjectType[DeleteOffering](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingNameInputType = deriveInputObjectType[ChangeOfferingName](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingCategoryInputType = deriveInputObjectType[ChangeOfferingCategory](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingAccessWhiteListInputType = deriveInputObjectType[ChangeOfferingAccessWhiteList](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingEndpointsInputType = deriveInputObjectType[ChangeOfferingEndpoints](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingSpatialExtentInputType = deriveInputObjectType[ChangeOfferingSpatialExtent](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingTemporalExtentInputType = deriveInputObjectType[ChangeOfferingTemporalExtent](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingInputsInputType = deriveInputObjectType[ChangeOfferingInputs](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingOutputsInputType = deriveInputObjectType[ChangeOfferingOutputs](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingLicenseInputType = deriveInputObjectType[ChangeOfferingLicense](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingPriceInputType = deriveInputObjectType[ChangeOfferingPrice](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingExtension1InputType = deriveInputObjectType[ChangeOfferingExtension1](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingExtension2InputType = deriveInputObjectType[ChangeOfferingExtension2](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingExtension3InputType = deriveInputObjectType[ChangeOfferingExtension3](ExcludeInputFields("meta"))
  implicit lazy val ActivateOfferingInputType = deriveInputObjectType[ActivateOffering](ExcludeInputFields("meta"))
  implicit lazy val DeactivateOfferingInputType = deriveInputObjectType[DeactivateOffering](ExcludeInputFields("meta"))

  implicit lazy val AddConsumerInputType = deriveInputObjectType[AddConsumer](ExcludeInputFields("meta"))
  implicit lazy val DeleteConsumerInputType = deriveInputObjectType[DeleteConsumer](ExcludeInputFields("meta"))
  implicit lazy val ChangeConsumerNameInputType = deriveInputObjectType[ChangeConsumerName](ExcludeInputFields("meta"))

  implicit lazy val SubscribeConsumerToOfferingInputType = deriveInputObjectType[SubscribeConsumerToOffering](ExcludeInputFields("meta"))
  implicit lazy val UnsubscribeConsumerFromOfferingInputType = deriveInputObjectType[UnsubscribeConsumerFromOffering](ExcludeInputFields("meta"))

  implicit lazy val AddOfferingQueryInputType = deriveInputObjectType[AddOfferingQuery](ExcludeInputFields("meta"))
  implicit lazy val DeleteOfferingQueryInputType = deriveInputObjectType[DeleteOfferingQuery](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQueryNameInputType = deriveInputObjectType[ChangeOfferingQueryName](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQueryCategoryInputType = deriveInputObjectType[ChangeOfferingQueryCategory](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQuerySpatialExtentInputType = deriveInputObjectType[ChangeOfferingQuerySpatialExtent](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQueryTemporalExtentInputType = deriveInputObjectType[ChangeOfferingQueryTemporalExtent](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQueryInputsInputType = deriveInputObjectType[ChangeOfferingQueryInputs](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQueryOutputsInputType = deriveInputObjectType[ChangeOfferingQueryOutputs](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQueryLicenseInputType = deriveInputObjectType[ChangeOfferingQueryLicense](ExcludeInputFields("meta"))
  implicit lazy val ChangeOfferingQueryPriceInputType = deriveInputObjectType[ChangeOfferingQueryPrice](ExcludeInputFields("meta"))

  implicit lazy val SubscribeQueryToOfferingInputType = deriveInputObjectType[SubscribeQueryToOffering](ExcludeInputFields("meta"))
  implicit lazy val UnsubscribeQueryFromOfferingInputType = deriveInputObjectType[UnsubscribeQueryFromOffering](ExcludeInputFields("meta"))

  implicit lazy val AccessReportInputType = deriveInputObjectType[AccessReport](InputObjectTypeName("AccessReportInput"))
  implicit lazy val ReportingPeriodInputType = deriveInputObjectType[ReportingPeriod](InputObjectTypeName("ReportingPeriodInput"))
  implicit lazy val TrackConsumerAccessInputType = deriveInputObjectType[TrackConsumerAccess](ExcludeInputFields("meta"))
  implicit lazy val TrackProviderAccessInputType = deriveInputObjectType[TrackProviderAccess](ExcludeInputFields("meta"))
  implicit lazy val TrackConsumerAccessesInputType = deriveInputObjectType[TrackConsumerAccesses]()
  implicit lazy val TrackProviderAccessesInputType = deriveInputObjectType[TrackProviderAccesses]()

  val QueryType = deriveContextObjectType[ExchangeCtx, ExchangeQueries, Unit](_.queries,
    ReplaceField("subscription", Field("subscription", OptionType(OfferingSubscriptionType),
      arguments = Argument("id", SubscriptionIdType) :: Nil, resolve = ctx => ctx.ctx.queries.subscription(ctx.arg("id"))))
  )
  val MutationType = deriveContextObjectType[ExchangeCtx, ExchangeMutations, Unit](_.mutations)

  val ExchangeSchema = Schema(QueryType, Some(MutationType), additionalTypes = SpatialExtentType :: Nil)
}
