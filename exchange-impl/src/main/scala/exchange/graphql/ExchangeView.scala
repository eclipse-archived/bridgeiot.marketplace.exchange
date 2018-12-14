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

import scala.util.Success
import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.persistence.query.EventEnvelope2
import akka.stream._
import akka.stream.stage._

import microservice._
import microservice.entity._

import exchange.api.consumer._
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization._
import exchange.api.provider._
import exchange.api.semantics._
import exchange.api.subscription._
import exchange.model.vocabs.BIGIOT
import exchange.repo.{ExchangeRepoMutations, ExchangeRepoQueries, ExchangeSemanticRepo}

case class ExchangeView(allMutationRepos: Seq[ExchangeRepoMutations], queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo)
                       (implicit system: ActorSystem, mat: ActorMaterializer) extends GraphStage[FlowShape[EventEnvelope2, CompletedRequest]] {
  val in = Inlet[EventEnvelope2]("ExchangeView.in")
  val out = Outlet[CompletedRequest]("ExchangeView.out")
  val shape = FlowShape.of(in, out)
  val startupTime = DateTime.now.clicks
  var recovering = true
  var mutationRepos = allMutationRepos filterNot (_.isPersisting)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
      setHandler(shape.in, new InHandler {
        def updateRepos(event: Event) =
          try {
            event match {
              case ev: OfferingCategoryCreated =>
                semanticRepo.offeringCategoryCreated(ev)
              case ev: OfferingCategoryDeprecated =>
                semanticRepo.offeringCategoryDeprecated(ev)
              case ev: OfferingCategoryUndeprecated =>
                semanticRepo.offeringCategoryUndeprecated(ev)
              case ev: OfferingCategoryNameChanged =>
                semanticRepo.offeringCategoryNameChanged(ev)
              case ev: OfferingCategoryParentChanged =>
                semanticRepo.offeringCategoryParentChanged(ev)

              case ev: InputTypeAddedToOfferingCategory =>
                semanticRepo.inputTypeAddedToOfferingCategory(ev)
              case ev: InputTypeDeprecatedForOfferingCategory =>
                semanticRepo.inputTypeDeprecatedForOfferingCategory(ev)
              case ev: InputTypeUndeprecatedForOfferingCategory =>
                semanticRepo.inputTypeUndeprecatedForOfferingCategory(ev)

              case ev: OutputTypeAddedToOfferingCategory =>
                semanticRepo.outputTypeAddedToOfferingCategory(ev)
              case ev: OutputTypeDeprecatedForOfferingCategory =>
                semanticRepo.outputTypeDeprecatedForOfferingCategory(ev)
              case ev: OutputTypeUndeprecatedForOfferingCategory =>
                semanticRepo.outputTypeUndeprecatedForOfferingCategory(ev)

              case ev: OrganizationCreated =>
                mutationRepos.foreach(_.organizationCreated(ev))
              case ev: OrganizationNameChanged =>
                mutationRepos.foreach(_.organizationNameChanged(ev))

              case ev: ProviderCreated =>
                mutationRepos.foreach(_.providerCreated(ev))
              case ev: ProviderDeleted =>
                mutationRepos.foreach(_.providerDeleted(ev))
              case ev: ProviderNameChanged =>
                mutationRepos.foreach(_.providerNameChanged(ev))

              case ev: ConsumerCreated =>
                mutationRepos.foreach(_.consumerCreated(ev))
              case ev: ConsumerDeleted =>
                mutationRepos.foreach(_.consumerDeleted(ev))
              case ev: ConsumerNameChanged =>
                mutationRepos.foreach(_.consumerNameChanged(ev))

              case ev: OfferingCreated =>
                mutationRepos.foreach(_.offeringCreated(ev))
              case ev: OfferingDeleted =>
                mutationRepos.foreach(_.offeringDeleted(ev))
              case ev: OfferingNameChanged =>
                mutationRepos.foreach(_.offeringNameChanged(ev))
              case ev: OfferingCategoryChanged =>
                mutationRepos.foreach(_.offeringCategoryChanged(ev))
              case ev: OfferingEndpointsChanged =>
                mutationRepos.foreach(_.offeringEndpointsChanged(ev))
              case ev: OfferingAccessWhiteListChanged =>
                mutationRepos.foreach(_.offeringAccessWhiteListChanged(ev))
              case ev: OfferingInputsChanged =>
                mutationRepos.foreach(_.offeringInputDataChanged(ev))
              case ev: OfferingOutputsChanged =>
                mutationRepos.foreach(_.offeringOutputDataChanged(ev))
              case ev: OfferingExtension1Changed =>
                mutationRepos.foreach(_.offeringExtension1Changed(ev))
              case ev: OfferingExtension2Changed =>
                mutationRepos.foreach(_.offeringExtension2Changed(ev))
              case ev: OfferingExtension3Changed =>
                mutationRepos.foreach(_.offeringExtension3Changed(ev))
              case ev: OfferingSpatialExtentChanged =>
                mutationRepos.foreach(_.offeringSpatialExtentChanged(ev))
              case ev: OfferingTemporalExtentChanged =>
                mutationRepos.foreach(_.offeringTemporalExtentChanged(ev))
              case ev: OfferingLicenseChanged =>
                mutationRepos.foreach(_.offeringLicenseChanged(ev))
              case ev: OfferingPriceChanged =>
                mutationRepos.foreach(_.offeringPriceChanged(ev))
              case ev: OfferingActivated =>
                mutationRepos.foreach(_.offeringActivated(ev))
              case ev: OfferingDeactivated =>
                mutationRepos.foreach(_.offeringDeactivated(ev))

              case ev: OfferingQueryCreated =>
                mutationRepos.foreach(_.offeringQueryCreated(ev))
              case ev: OfferingQueryDeleted =>
                mutationRepos.foreach(_.offeringQueryDeleted(ev))
              case ev: OfferingQueryNameChanged =>
                mutationRepos.foreach(_.offeringQueryNameChanged(ev))
              case ev: OfferingQueryCategoryChanged =>
                mutationRepos.foreach(_.offeringQueryCategoryChanged(ev))
              case ev: OfferingQueryInputsChanged =>
                mutationRepos.foreach(_.offeringQueryInputDataChanged(ev))
              case ev: OfferingQueryOutputsChanged =>
                mutationRepos.foreach(_.offeringQueryOutputDataChanged(ev))
              case ev: OfferingQuerySpatialExtentChanged =>
                mutationRepos.foreach(_.offeringQuerySpatialExtentChanged(ev))
              case ev: OfferingQueryTemporalExtentChanged =>
                mutationRepos.foreach(_.offeringQueryTemporalExtentChanged(ev))
              case ev: OfferingQueryLicenseChanged =>
                mutationRepos.foreach(_.offeringQueryLicenseChanged(ev))
              case ev: OfferingQueryPriceChanged =>
                mutationRepos.foreach(_.offeringQueryPriceChanged(ev))

              case ev: SubscriptionCreated =>
                mutationRepos.foreach(_.subscriptionCreated(ev))
              case ev: SubscriptionDeleted =>
                mutationRepos.foreach(_.subscriptionDeleted(ev))

              case ev: Unchanged =>
              // ignore
              case ev if event.meta.delay > 0 =>
              // don't log delayed events

              case ev =>
                log.debug(s"ignoring $ev")
            }
          } catch {
            case e: Throwable =>
              log.error(s"Exception thrown in updateRepos: ${e.getMessage}")
              e.getStackTrace foreach { element => log.error("   " + element.toString)}
          }

        def retrieveEntity(event: Event): Option[Entity] =
          event match {
            case ev: OfferingCategoryEvent =>
              val category = semanticRepo.offeringCategory(ev.uri)
              if (category.isEmpty)
                log.info(s"  couldn't find OfferingCategory(${ev.uri}) in ${semanticRepo.allOfferingCategories.show("")}")
              category
            case ev: OrganizationEvent =>
              queryRepo.organization(ev.id.value)
            case ev: ProviderEvent =>
              queryRepo.provider(ev.id.value)
            case ev: ConsumerEvent =>
              queryRepo.consumer(ev.id.value)
            case ev: OfferingEvent =>
              queryRepo.offering(ev.id.value)
            case ev: OfferingQueryEvent =>
              queryRepo.offeringQuery(ev.id.value)
            case ev: SubscriptionAccessed =>
              queryRepo.subscription(ev.id.value).map { subscription =>
                BaseOfferingSubscription(subscription.id, subscription.offering, subscription.status)
              }
            case ev: SubscriptionEvent =>
              queryRepo.subscription(ev.id.value)
          }

        override def onPush() = {
          val envelope = grab(shape.in)
          envelope.event match {
            case event: Event =>
              if (event.meta.time >= startupTime && recovering) {
                recovering = false
                mutationRepos = allMutationRepos
                log.info("Finished recovery")
              }
              updateRepos(event)
              if (!recovering && event.meta.delay == 0 && event.meta.finished) {
                log.debug(s"updated repos with $event")
                val entity = retrieveEntity(event).getOrElse{
                  DeletedEntity(DeletedId(event.id))
                }
                emit(shape.out, CompletedRequest(event.meta.requestId, Success(entity)))
              } else {
                pull(shape.in)
              }
            case _ =>
              log.error(s"Wrong event in envelope: $envelope")
          }
        }
      })
      setHandler(shape.out, new OutHandler {
        override def onPull() = {
          pull(shape.in)
        }
      })
    }

}
