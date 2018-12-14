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
package exchange.repo.inmemory

import scala.collection.TraversableView
import akka.http.scaladsl.model.DateTime

import monocle.Lens
import microservice.entity.normalize

import exchange.api.extent.SpatialExtent
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.price._
import exchange.api.semantics._
import exchange.repo.{ExchangeRepoQueries, ExchangeSemanticRepo}

trait InMemoryExchangeSemanticRepo extends ExchangeSemanticRepo {

  protected def exchange: Exchange
  protected def queryRepo: ExchangeRepoQueries

  val availableParkingSpaces = RdfAnnotation("datex:parkingNumberOfVacantSpaces", "Available Parking Spaces")
  val occupiedParkingSpaces = RdfAnnotation("datex:parkingOccupancy", "Occupied Parking Spaces")
  val parkingStatus = RdfAnnotation("datex:parkingSpaceStatus", "Parking Status")
  val latitude = RdfAnnotation("schema:latitude", "Latitude")
  val longitude = RdfAnnotation("schema:longitude", "Longitude")
  val location = RdfAnnotation("http://schema.org/geoMidpoint", "Location")
  val radius = RdfAnnotation("schema:radius", "Radius")
  val distance = RdfAnnotation("datex:distanceFromParkingSpace", "Distance")
  val airQuality = RdfAnnotation("bigiot:airQuality", "Air Quality")
  val trafficCount = RdfAnnotation("bigiot:TrafficCount", "Traffic Count")
  val trafficControl = RdfAnnotation("bigiot:TrafficControl", "Traffic Control")

  var allDataTypes = List(availableParkingSpaces, occupiedParkingSpaces, parkingStatus,
    latitude, longitude, radius, distance, airQuality, trafficCount, trafficControl)

  case class DataFields(category: OfferingCategory, inputs: List[DataField], outputs: List[DataField])

  protected var rootOfferingCategory = OfferingCategory(RdfAnnotation(RootOfferingCategoryUri, "All Categories"), List(
    OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "MobilityCategory", "Mobility"), List(
      OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "ParkingCategory", "Parking"),
        outputs = List(availableParkingSpaces, occupiedParkingSpaces, parkingStatus, location, distance),
        inputs = List(location, radius)),
      OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "TrafficCategory", "Traffic"),
        outputs = List(trafficCount, latitude, longitude), inputs = List(latitude, longitude, radius))
    )),
    OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "AirQualityCategory", "Air Quality"),
      outputs = List(airQuality, latitude, longitude), inputs = List(latitude, longitude, radius)),
    OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "SmartHomeCategory", "Smart Home"), List(
      OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "HomeAppliances", "Home Appliances"), List(
        OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "CoolingCategory", "Cooling"),
          outputs = List(latitude, longitude, distance),
          inputs = List(latitude, longitude, radius)),
        OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "WashingCategory", "Washing"),
          outputs = List(latitude, longitude), inputs = List(latitude, longitude, radius)),
        OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "CookingCategory", "Cooking"),
          outputs = List(latitude, longitude), inputs = List(latitude, longitude, radius))
      )),
      OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "LightingCategory", "Lighting")))),
    OfferingCategory(RdfAnnotation(OfferingCategoryPrefix + "OthersCategory", "Others"))))

  protected var defaultDataFields = Map(
    availableParkingSpaces.uri -> DataField("available", availableParkingSpaces, IntegerType()),
    occupiedParkingSpaces.uri -> DataField("occupied", occupiedParkingSpaces, IntegerType()),
    parkingStatus.uri -> DataField("status", parkingStatus, TextType()),
    latitude.uri -> DataField("latitude", latitude, NumberType()),
    longitude.uri -> DataField("longitude", longitude, NumberType()),
    location.uri -> DataField("location", location, ObjectType(List(DataField("latitude", latitude, NumberType()), DataField("longitude", longitude, NumberType())))),
    radius.uri -> DataField("radius", radius, NumberType()),
    distance.uri -> DataField("distance", distance, NumberType()),
    airQuality.uri -> DataField("airQuality", airQuality, TextType()),
    trafficCount.uri -> DataField("trafficCount", trafficCount, IntegerType()),
    trafficControl.uri -> DataField("trafficControl", trafficControl, IntegerType())
  )

  private def addDefaultDataField(rdfAnnotation: RdfAnnotation, valueType: ValueType) =
    defaultDataFields = defaultDataFields.updated(rdfAnnotation.uri, DataField(normalize(rdfAnnotation.label), rdfAnnotation, valueType))

  def allOfferingCategories = rootOfferingCategory

  private def offeringCategoryUris(category: OfferingCategory): List[String] =
    category.rdfAnnotation.uri :: (category.subCategories flatMap offeringCategoryUris)

  def allOfferingCategoryUris = offeringCategoryUris(allOfferingCategories)

  private def traverseOfferingCategoriesWithIdxs(r: OfferingCategory, parentIdxs: List[Int]):
  TraversableView[(OfferingCategory, List[Int]), Traversable[(OfferingCategory, List[Int])]] =
    (r.subCategories.zipWithIndex foldLeft Traversable((r, parentIdxs)).view) {
      case (acc, (offeringCategory, idx)) =>
        (acc ++ traverseOfferingCategoriesWithIdxs(offeringCategory, parentIdxs :+ idx)).
          asInstanceOf[TraversableView[(OfferingCategory, List[Int]), Traversable[(OfferingCategory, List[Int])]]]
    }

  private def offeringCategoryWithIdxs(rdfUri: String) =
    traverseOfferingCategoriesWithIdxs(allOfferingCategories, Nil) find (_._1.rdfAnnotation.uri == rdfUri)

  def offeringCategory(rdfUri: String) =
    if (rdfUri.isEmpty)
      None
    else
      offeringCategoryWithIdxs(rdfUri) map (_._1)

  private def updateInCategory(category: OfferingCategory, parent: String, f: OfferingCategory => OfferingCategory): Option[OfferingCategory] = {
    if (category.rdfAnnotation.uri == parent) {
      Some(f(category))
    } else {
      category.subCategories.map(updateInCategory(_, parent, f)).zipWithIndex collectFirst {
        case (Some(updatedSubCategory), idx) =>
          OfferingCategory.subCategories.modify(_.updated(idx, updatedSubCategory))(category)
      }
    }
  }

  private def updateCategory(parent: String,  f: OfferingCategory => OfferingCategory) =
    updateInCategory(rootOfferingCategory, parent, f) foreach { newRoot => rootOfferingCategory = newRoot }

  def offeringCategoryCreated(ev: OfferingCategoryCreated) = {
    val category = OfferingCategory(RdfAnnotation(ev.uri, ev.name, ev.proposed))
    updateCategory(ev.parent, OfferingCategory.subCategories.modify(_ :+ category))
  }

  def offeringCategoryDeprecated(ev: OfferingCategoryDeprecated) = {
    updateCategory(ev.uri, (OfferingCategory.rdfAnnotation composeLens RdfAnnotation.deprecated).modify(_ => true))
  }

  def offeringCategoryUndeprecated(ev: OfferingCategoryUndeprecated) = {
    updateCategory(ev.uri, (OfferingCategory.rdfAnnotation composeLens RdfAnnotation.deprecated).modify(_ => false))
  }

  def offeringCategoryNameChanged(ev: OfferingCategoryNameChanged) = {
    updateCategory(ev.uri, (OfferingCategory.rdfAnnotation composeLens RdfAnnotation.label).modify(_ => ev.name))
  }

  def offeringCategoryParentChanged(ev: OfferingCategoryParentChanged) = {
    // TODO
  }

  def inputTypeAddedToOfferingCategory(ev: InputTypeAddedToOfferingCategory) = {
    updateCategory(ev.uri, OfferingCategory.inputs.modify(_ :+ ev.rdfAnnotation))
  }

  def inputTypeDeprecatedForOfferingCategory(ev: InputTypeDeprecatedForOfferingCategory) =
    updateCategory(ev.uri, OfferingCategory.inputs.modify(_ map { rdfAnnotation =>
      if (rdfAnnotation.uri == ev.typeUri) rdfAnnotation.copy(deprecated = true) else rdfAnnotation}))

  def inputTypeUndeprecatedForOfferingCategory(ev: InputTypeUndeprecatedForOfferingCategory) =
    updateCategory(ev.uri, OfferingCategory.inputs.modify(_ map { rdfAnnotation =>
      if (rdfAnnotation.uri == ev.typeUri) rdfAnnotation.copy(deprecated = false) else rdfAnnotation}))

  def outputTypeAddedToOfferingCategory(ev: OutputTypeAddedToOfferingCategory) =
    updateCategory(ev.uri, OfferingCategory.outputs.modify(_ :+ ev.rdfAnnotation))

  def outputTypeDeprecatedForOfferingCategory(ev: OutputTypeDeprecatedForOfferingCategory) =
    updateCategory(ev.uri, OfferingCategory.outputs.modify(_ map { rdfAnnotation =>
      if (rdfAnnotation.uri == ev.typeUri) rdfAnnotation.copy(deprecated = true) else rdfAnnotation}))

  def outputTypeUndeprecatedForOfferingCategory(ev: OutputTypeUndeprecatedForOfferingCategory) =
    updateCategory(ev.uri, OfferingCategory.outputs.modify(_ map { rdfAnnotation =>
      if (rdfAnnotation.uri == ev.typeUri) rdfAnnotation.copy(deprecated = false) else rdfAnnotation}))

  private def createType(categoryUri: String, lens: Lens[OfferingCategory, List[RdfAnnotation]],
                         typeUri: String, name: String, proposed: Boolean = true) = {
    val inputType = RdfAnnotation(typeUri, name, proposed)
    updateCategory(categoryUri, lens.modify(_ :+ inputType))
    inputType
  }

  def outputDataField(typeUri: String, categoryUri: String) = {
    val rdfAnnotation = offeringCategory(categoryUri).map { category =>
      category.outputs.find(_.uri == typeUri) getOrElse createType(categoryUri, OfferingCategory.outputs, typeUri, "")
    } getOrElse RdfAnnotation(typeUri, "", proposed = true)
    defaultDataFields.get(rdfAnnotation.uri)
  }

  def inputDataField(typeUri: String, categoryUri: String) = {
    val rdfAnnotation = offeringCategory(categoryUri).map { category =>
      category.inputs.find(_.uri == typeUri) getOrElse createType(categoryUri, OfferingCategory.inputs, typeUri, "")
    } getOrElse RdfAnnotation(typeUri, "", proposed = true)
    defaultDataFields.get(rdfAnnotation.uri)
  }

  private def priceMatches(queryPriceOpt: Option[Price], offeringPrice: Price) =
    queryPriceOpt forall { queryPrice =>
      (queryPrice, offeringPrice) match {
        case (_, Price(FREE, _)) => true
        case (Price(queryModel, Some(Money(querygAmount, queryCurrency))),
        Price(offeringModel, Some(Money(offeringAmount, offeringCurrency))))
          if queryModel == offeringModel && queryCurrency == offeringCurrency => offeringAmount <= querygAmount
        case _ => false
      }
    }

  private def offeringMatchesQuery(offering: Offering, query: OfferingQuery) = {
    val annotationMatches = query.rdfAnnotation forall (_.uri.trim == offering.rdfAnnotation.uri.trim)
    val outputsMatch = query.outputs forall (output => offering.outputs exists (_.rdfAnnotation.uri == output.rdfAnnotation.uri))
    val inputsMatch = query.inputs forall (input => offering.inputs exists (_.rdfAnnotation.uri == input.rdfAnnotation.uri))
    val spatialExtentMatches = (query.spatialExtent, offering.spatialExtent) match {
      case (None, _) =>
        true
      case (Some(SpatialExtent(queryCity, _)), Some(SpatialExtent(offeringCity, _))) =>
        queryCity.isEmpty || queryCity.trim == offeringCity.trim
      case _ =>
        false
    }
    val licenseMatches = query.license forall (_ == offering.license)
    val active = offering.activation.status && offering.activation.expirationTime > DateTime.now.clicks
    active && annotationMatches && outputsMatch && inputsMatch && spatialExtentMatches && licenseMatches &&
      priceMatches(query.price, offering.price)
  }

  private def offeringIdsMatchingQuery(query: OfferingQuery) = for {
    organization <- exchange.organizations
    provider <- organization.providers
    offering <- provider.offerings if offeringMatchesQuery(offering, query)
  } yield offering.id.value

  def matchingOfferingIds(queryId: OfferingQueryId) =
    queryRepo.offeringQuery(queryId).toList flatMap offeringIdsMatchingQuery

  def isOfferingConsistent(ev: OfferingCreated) = true
  def isOfferingQueryConsistent(ev: OfferingQueryCreated) = true

}
