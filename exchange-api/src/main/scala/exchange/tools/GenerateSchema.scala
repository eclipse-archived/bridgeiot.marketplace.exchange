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
package tools

import java.io.{File, PrintWriter}

import exchange.schema.{ExchangeCtx, SchemaDefinition}
import sangria.execution.Executor
import sangria.introspection._
import sangria.marshalling.circe._
import sangria.renderer.SchemaRenderer

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object GenerateSchema {
  def main(args: Array[String]) {
    val ctx = ExchangeCtx(null, null)
    val futureOfSchemaJson = Executor.execute(SchemaDefinition.ExchangeSchema, introspectionQuery, ctx)

    val schemaJson = Await.ready(futureOfSchemaJson, 5 second).value.get

    schemaJson match {
      case Success(t) =>
        val jsonFile = new File("schema.json")
        println(s"writing schema to file ${jsonFile.getAbsolutePath}")
        new PrintWriter(jsonFile) {
          write(t.toString)
          close()
        }
      case Failure(t) => println("Could not generate schema.json : " + t.getMessage)
    }

    val schema = SchemaRenderer.renderSchema(SchemaDefinition.ExchangeSchema)
    val schemaFile = new File("schema.graphqls")
    println(s"writing schema to file ${schemaFile.getAbsolutePath}")
    new PrintWriter(schemaFile) {
      write(schema)
      close()
    }
  }
}
