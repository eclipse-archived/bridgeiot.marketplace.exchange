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
import io.funcqrs.behavior.handlers.just.OneEvent
import io.funcqrs.behavior.{Behavior, Types}
import microservice._
import microservice.entity._
import microservice.security._

import exchange.api.subscription._
import exchange.model.Subscription.createToken
import pdi.jwt.{JwtCirce, JwtClaim}

object Subscription extends Types[Subscription] {
  type Id = SubscriptionId
  type Command = SubscriptionCommand
  type Event = SubscriptionEvent

  val tag = Tags.aggregateTag("Subscription")

  def create = {
    actions
      .rejectCommand {
        case cmd: CreateSubscription if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case CreateSubscription(id, subscriberId, subscribableId, secret, meta) =>
            SubscriptionCreated(id, subscriberId, subscribableId, createToken(id, subscriberId, subscribableId, secret), meta)
        }
      }
      .eventHandler {
        case SubscriptionCreated(id, subscriberId, subscribableId, _, _) =>
          ActiveSubscription(id, subscriberId, subscribableId)
      }
  }

  def createToken(id: SubscriptionId, subscriberId: entity.Id, subscribableId: entity.Id, secret: String) = {
    val claim = (JwtClaim() ++ ("subscribableId" -> subscribableId, "subscriberId" -> subscriberId))
      .about(id)
      .issuedNow
      .expiresIn(defaultTokenExpiryPeriod)
    JwtCirce.encode(claim, decodeBase64(secret), algorithm)
  }

  def behavior(id: SubscriptionId): Behavior[Subscription, Command, Event] =
    Behavior
      .first {create}
      .andThen {
        case subscription: ActiveSubscription => subscription.acceptCommands
        case subscription: DeletedSubscription => subscription.acceptCommands
      }
}

sealed trait Subscription {
  def id: SubscriptionId
}

case class ActiveSubscription(id: SubscriptionId, subscriberId: Id, subscribableId: Id) extends Subscription {
  def acceptCommands =
    Subscription.actions
      .rejectCommand {
        case cmd: DeleteSubscription if cmd.meta.hasWrongOrganization(subscriberId) && cmd.meta.hasWrongOrganization(subscribableId) =>
          NotAuthorized(cmd)
        case cmd: SubscriptionCommand if !cmd.isInstanceOf[DeleteSubscription] && cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
        case cmd: TrackConsumerAccess =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case CreateSubscription(_, newSubscriberId, newSubscribableId, secret, meta) =>
            SubscriptionCreated(id, subscriberId, subscribableId, createToken(id, subscriberId, subscribableId, secret), meta)
          case DeleteSubscription(_, _, _, meta) =>
            SubscriptionDeleted(id, subscriberId, subscribableId, meta)
          case TrackProviderAccess(_, _, report, time, meta) =>
            SubscriptionAccessed(id, report.accesses, report.records, report.inputBytes, report.outputBytes, time.start, time.end, meta)
        }
      }
      .eventHandler {
        case ev: SubscriptionCreated =>
          this
        case ev: SubscriptionDeleted =>
          DeletedSubscription(id, subscriberId, subscribableId)
        case ev: SubscriptionAccessed =>
          this
      }
}

case class DeletedSubscription(id: SubscriptionId, subscriberId: Id, subscribableId: Id) extends Subscription {
  def acceptCommands =
    Subscription.actions
      .rejectCommand {
        case CreateSubscription(_, newSubscriberId, newSubscribableId, _, meta) if newSubscriberId != subscriberId || newSubscribableId != subscribableId =>
          InconsistentSubscription(id, subscriberId, subscribableId, meta)
      }
      .commandHandler {
        OneEvent {
          case CreateSubscription(_, newSubscriberId, newSubscribableId, secret, meta) =>
            SubscriptionCreated(id, subscriberId, subscribableId, createToken(id, subscriberId, subscribableId, secret), meta)
        }
      }
      .eventHandler {
        case SubscriptionCreated(_, _, _, _, _) =>
          ActiveSubscription(id, subscriberId, subscribableId)
      }
}

