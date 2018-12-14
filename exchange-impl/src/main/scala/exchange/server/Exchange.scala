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
package exchange.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}

import io.circe.generic.auto._
import io.funcqrs.Tags
import microservice._
import org.slf4j.LoggerFactory

import exchange.graphql.ExchangeGraphQlEndpoint
import exchange.repo.inmemory.{InMemoryExchangeRepoWithSemantics, InMemoryExchangeRepoWithoutSemantics}
import exchange.repo.rdfstore.RDFExchangeRepo
import exchange.repo.{ExchangeRepoMutations, ExchangeRepoQueries, ExchangeSemanticRepo}
import exchange.service._

object Exchange extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  val decider: Supervision.Decider = { e =>
    log.error(s"Unhandled exception in stream: $e")
    log.error("!!! Restarting stream now !!!")
    Supervision.Restart
  }

  implicit val system: ActorSystem = ActorSystem("Exchange")
  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit val materializer: ActorMaterializer = ActorMaterializer(materializerSettings)

  val tag = Tags.aggregateTag("Exchange")

  val (queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo, mutationRepos: Seq[ExchangeRepoMutations]) =
    if (sys.env.getOrElse("REPO", "").toUpperCase == "RDFSTORE") {
      log.info("Using RDFExchangeRepo as semantic Repo")
      val rdfRepo = new RDFExchangeRepo(sys.env.getOrElse("MARKETENV", "local"))
      val inMemoryRepo = new InMemoryExchangeRepoWithoutSemantics(rdfRepo)
      (inMemoryRepo, rdfRepo, List(inMemoryRepo, rdfRepo))
    }
    else {
      log.info("Using InMemoryExchangeRepo as semantic Repo")
      val inMemoryRepo = InMemoryExchangeRepoWithSemantics()
      (inMemoryRepo, inMemoryRepo, List(inMemoryRepo))
    }

  log.info("starting Exchange on port 8080")
  Http().bindAndHandleAsync(ExchangeGraphQlEndpoint(queryRepo, semanticRepo, mutationRepos), "0.0.0.0", sys.props.get("http.port").fold(8080)(_.toInt))

  CommandProcessorStage(OfferingCategoryService(semanticRepo))
  CommandProcessorStage(OrganizationService)
  CommandProcessorStage(ProviderService(semanticRepo))
  CommandProcessorStage(ConsumerService(queryRepo, semanticRepo))
  CommandProcessorStage(OfferingService(queryRepo, semanticRepo))
  CommandProcessorStage(OfferingQueryService(queryRepo, semanticRepo))
  CommandProcessorStage(SubscriptionService(queryRepo))

}
