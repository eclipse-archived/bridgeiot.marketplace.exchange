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

import io.circe.{Decoder, Encoder}
import io.funcqrs.AggregateId
import microservice._
import microservice.entity._
import sangria.macros.derive.GraphQLDescription

import exchange.api.consumer.ConsumerToOfferingSubscription
import exchange.api.offeringquery.QueryToOfferingSubscription

object subscription {

  val serviceName = "Subscription"
  val Arrow = "=="

  val defaultTokenExpiryPeriod = 60 * 60 // 1 hour

  case class SubscriptionId(value: String) extends AggregateId

  object SubscriptionId {
    def apply(subscriberId: Id, subscribableId: Id): SubscriptionId = SubscriptionId(subscriberId + Arrow + subscribableId)
    implicit def fromString(aggregateId: String): SubscriptionId = SubscriptionId(aggregateId)
  }

  object SubscriptionStatus extends Enumeration {
    type SubscriptionStatus= Value
    val Active, Inactive, Idle = Value
  }

  implicit val subscriptionStatusDecoder: Decoder[SubscriptionStatus.Value] = Decoder.enumDecoder(SubscriptionStatus)
  implicit val subscriptionStatusEncoder: Encoder[SubscriptionStatus.Value] = Encoder.enumEncoder(SubscriptionStatus)

  import SubscriptionStatus._

  trait Subscription extends Entity {
    def id: SubscriptionId
    def status: SubscriptionStatus
  }

  case class Subscriptions(consumerSubscriptions: List[ConsumerToOfferingSubscription] = Nil,
                           querySubscriptions: List[QueryToOfferingSubscription] = Nil)

  case class AccessReport(accesses: Long, records: Long, inputBytes: Long, outputBytes: Long,
                          totalAccesses: Long, totalRecords: Long, totalInputBytes: Long, totalOutputBytes: Long)

  case class ReportingPeriod(start: Long, end: Long)

  case class TrackProviderAccess(id: SubscriptionId, accessSessionId: Id, report: AccessReport, time: ReportingPeriod,
                                 meta: Meta = Meta())  extends SubscriptionCommand
  case class TrackConsumerAccess(id: SubscriptionId, accessSessionId: Id, report: AccessReport, time: ReportingPeriod,
                                 meta: Meta = Meta())  extends SubscriptionCommand

  sealed trait SubscriptionCommand extends Command
  sealed trait SubscriptionEvent extends Event

  @GraphQLDescription("Create new Subscription")
  case class CreateSubscription(id: SubscriptionId, subscriberId: Id, subscribableId: Id, secret: String, meta: Meta = Meta()) extends SubscriptionCommand
  case class SubscriptionCreated(id: SubscriptionId, subscriberId: Id, subscribableId: Id, accessToken: String, meta: Meta) extends SubscriptionEvent

  @GraphQLDescription("Delete Subscription")
  case class DeleteSubscription(id: SubscriptionId, subscriberId: Id, subscribableId: Id, meta: Meta = Meta()) extends SubscriptionCommand
  case class SubscriptionDeleted(id: SubscriptionId, subscriberId: Id, subscribableId: Id, meta: Meta) extends SubscriptionEvent

  @GraphQLDescription("Track accesses from consumer lib")
  case class TrackConsumerAccesses(accesses: List[TrackConsumerAccess])
  @GraphQLDescription("Track accesses from provider lib")
  case class TrackProviderAccesses(accesses: List[TrackProviderAccess])
  case class SubscriptionAccessed(id: SubscriptionId, accesses: Long, records: Long, inputBytes: Long, outputBytes: Long,
                                  start: Long, end: Long, meta: Meta) extends SubscriptionEvent

  // Errors
  case class InconsistentSubscription(override val id: Id, subscriberId: Id, subscribableId: Id, override val meta: Meta)
    extends Error(id, "InconsistentSubscription", s"$subscriberId and $subscribableId do not match $id", meta)
}
