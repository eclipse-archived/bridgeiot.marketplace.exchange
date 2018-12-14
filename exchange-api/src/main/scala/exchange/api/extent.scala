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
import sangria.macros.derive.GraphQLDescription

object extent {
  case class SpatialExtent(city: String, boundary: Option[BoundingBox] = None)

  @GraphQLDescription("Single Point")
  case class Location(lat: Double, lng: Double)

  @GraphQLDescription("Circle with center and radius in meters")
  case class Circle(center: Location, radius: Double)

  @GraphQLDescription("Bounding Box consisting of 2 opposite corner locations")
  case class BoundingBox(l1: Location, l2: Location)

  @GraphQLDescription("Time range")
  case class TemporalExtent(from: Option[Long] = None, to: Option[Long] = None)
}
