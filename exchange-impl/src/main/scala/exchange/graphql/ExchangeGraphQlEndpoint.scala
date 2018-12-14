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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, ClosedShape}

import io.circe.Json
import io.circe.generic.auto._
import microservice.persistence.CassandraEventSource
import microservice.{encodeAggregateId, cmdTopic, envSuffix, errorTopic}
import microservice.{MessageQueue, PendingRequest, PendingRequestHandler, SourceQueue, TopicSource}
import org.slf4j.LoggerFactory
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer

import exchange.api
import exchange.api.consumer.ConsumerCommand
import exchange.api.offering.OfferingCommand
import exchange.api.offeringquery.OfferingQueryCommand
import exchange.api.organization.{OrganizationCommand, OrganizationId}
import exchange.api.provider.ProviderCommand
import exchange.api.semantics.SemanticsCommand
import exchange.api.subscription.SubscriptionCommand
import exchange.repo.{ExchangeRepoMutations, ExchangeRepoQueries, ExchangeSemanticRepo}
import exchange.schema.{ExchangeCtx, SchemaDefinition}
import exchange.server.Exchange

case class GraphQLRequest(query: String, operationName: Option[String], variables: Option[Json])
case class GraphQLError(error: String)

case class CommandQueues(implicit system: ActorSystem, mat: ActorMaterializer) {
  val semantics = MessageQueue(cmdTopic[SemanticsCommand](api.semantics.serviceName), "CommandQueues.semantics")
  val organization = MessageQueue(cmdTopic[OrganizationCommand](api.organization.serviceName), "CommandQueues.organization")
  val provider = MessageQueue(cmdTopic[ProviderCommand](api.provider.serviceName), "CommandQueues.provider")
  val consumer = MessageQueue(cmdTopic[ConsumerCommand](api.consumer.serviceName), "CommandQueues.consumer")
  val offering = MessageQueue(cmdTopic[OfferingCommand](api.offering.serviceName), "CommandQueues.offering")
  val query = MessageQueue(cmdTopic[OfferingQueryCommand](api.offeringquery.serviceName), "CommandQueues.query")
  val subscription = MessageQueue(cmdTopic[SubscriptionCommand](api.subscription.serviceName), "CommandQueues.subscription")
}

object ExchangeGraphQlEndpoint {

  val log = LoggerFactory.getLogger(this.getClass)

  val exceptionHandler = ExceptionHandler {
    case (m, e: Throwable) => HandledException(e.getMessage)
  }

  def apply(queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo, mutationRepos: Seq[ExchangeRepoMutations])
           (implicit system: ActorSystem, mat: ActorMaterializer) = {
    val pendingRequestQueue = RunnableGraph.fromGraph(GraphDSL.create(SourceQueue[PendingRequest]) { implicit b =>
      addQueue =>
        val events = b.add(CassandraEventSource(Exchange.tag))
        val errors = b.add(TopicSource(errorTopic, "ExchangeGraphQlEndpoint.PendingRequestHandler" + envSuffix))
        val view = b.add(ExchangeView(mutationRepos, queryRepo, semanticRepo))
        val pending = b.add(PendingRequestHandler("ExchangeGraphQlEndpoint" + envSuffix))

        addQueue ~> pending.add
        events ~> view ~> pending.complete
        errors ~> pending.error
        pending.out ~> Sink.ignore

        ClosedShape
    }).run()

    val commandQueues = CommandQueues()

    val route: Route =
      (post & path("graphql") & optionalHeaderValueByName("requesterId") & optionalHeaderValueByName("organizationId")) {
        (requesterId, requesterOrgId) =>
          import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

          entity(as[GraphQLRequest]) { case GraphQLRequest(query, operation, vars) ⇒
            log.info(s"requesterId: $requesterId, requesterOrgId: $requesterOrgId, operation: $operation, vars: ${vars.toString.replace("\r\n", " ").replace("\n", " ")}")
            QueryParser.parse(query) match {
              // query parsed successfully, time to execute it!
              case Success(queryAst) ⇒
                val ctx = ExchangeCtx(new ExchangeQueriesImpl(requesterId, requesterOrgId.map(OrganizationId(_)), queryRepo, semanticRepo),
                  new ExchangeMutationsImpl(requesterId, requesterOrgId, commandQueues, pendingRequestQueue))
                complete(Executor.execute(SchemaDefinition.ExchangeSchema, queryAst, ctx, variables = vars.getOrElse(Json.obj()),
                  operationName = operation, exceptionHandler = exceptionHandler)
                  .map(OK -> _)
                  .recover {
                    case error: QueryAnalysisError =>
                      log.warn(s"QueryAnalysisError: ${error.resolveError}")
                      BadRequest -> error.resolveError
                    case error: ErrorWithResolver =>
                      log.warn(s"ErrorWithResolver: ${error.resolveError}")
                      InternalServerError -> error.resolveError
                  })

              // can't parse GraphQL query, return error
              case Failure(error) ⇒
                log.error(s"PARSE FAILURE: ${error.getMessage}")
                complete(BadRequest -> GraphQLError(error.getMessage))
            }
          }
      } ~
        (get & path("schema")) {
          complete(OK, SchemaRenderer.renderSchema(SchemaDefinition.ExchangeSchema))
        } ~
        get {
          getFromResource("graphiql.html")
        }

    Route.asyncHandler(route)
  }
}
