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
import io.funcqrs.behavior.handlers.just.{ManyEvents, OneEvent}
import io.funcqrs.behavior.{Behavior, Types}
import microservice._
import microservice.entity._

import exchange.api.semantics._

object OfferingCategory extends Types[OfferingCategory] {
  type Id = OfferingCategoryId
  type Command = OfferingCategoryCommand
  type Event = OfferingCategoryEvent

  val tag = Tags.aggregateTag("OfferingCategory")

  def create = {
    actions
      .rejectCommand {
        case cmd: OfferingCategoryCommand if cmd.hasNoOrganization =>
          NotAuthorized(cmd)
        case cmd: CreateOfferingCategory if !cmd.proposed =>
          NotAuthorized(cmd, "only proposed categories are allowed")

        case CreateOfferingCategory(name, _, uri, _, meta) if uri.isEmpty && name.isEmpty =>
          EmptyNameOfferingCategory(uri, meta)
        case ChangeOfferingCategoryName(id, name, meta) if name.isEmpty =>
          EmptyNameOfferingCategory(id, meta)

        case AddInputTypeToOfferingCategory(categoryUri, typeUri, typeName, typeProposed, meta) if typeUri.isEmpty && typeName.isEmpty =>
          EmptyNameDataType(categoryUri, typeUri, meta)
        case AddOutputTypeToOfferingCategory(categoryUri, typeUri, typeName, typeProposed, meta) if typeUri.isEmpty && typeName.isEmpty =>
          EmptyNameDataType(categoryUri, typeUri, meta)
      }
      .commandHandler {
        OneEvent {
          case CreateOfferingCategory(name, parent, explicitUri, proposed, meta) =>
            val uri = createUri(explicitUri, name)
            OfferingCategoryCreated(uri, uri, createLabel(name, uri), parent, proposed, meta)
        }
      }
      .commandHandler {
        // Create aggregates for non-proposed categories before applying change events, since they don't exist yet
        ManyEvents {
          case cmd@ChangeOfferingCategoryName(categoryUri, name, meta) =>
            List(OfferingCategoryCreated(cmd.id, categoryUri, "", "", proposed = false, meta.copy(finished = false)),
              OfferingCategoryNameChanged(cmd.id, categoryUri, name, meta))

          case cmd@AddInputTypeToOfferingCategory(categoryUri, typeUri, typeName, typeProposed, meta) =>
            val rdfAnnotation = RdfAnnotation(createUri(typeUri, typeName), typeName, typeProposed)
            List(OfferingCategoryCreated(cmd.id, categoryUri, "", "", proposed = false, meta.copy(finished = false)),
              InputTypeAddedToOfferingCategory(cmd.id, categoryUri, rdfAnnotation, meta))

          case cmd@AddOutputTypeToOfferingCategory(categoryUri, typeUri, typeName, typeProposed, meta) =>
            val rdfAnnotation = RdfAnnotation(createUri(typeUri, typeName), typeName, typeProposed)
            List(OfferingCategoryCreated(cmd.id, categoryUri, "", "", proposed = false, meta.copy(finished = false)),
              OutputTypeAddedToOfferingCategory(cmd.id, categoryUri, rdfAnnotation, meta))
        }
      }
      .eventHandler {
        case OfferingCategoryCreated(id, uri, name, parent, proposed, meta) =>
          ActiveOfferingCategory(id, uri, name, parent, proposed, meta.requesterOrgId.get)
      }
  }

  def behavior(id: OfferingCategoryId) =
    Behavior
      .first {
        create
      }
      .andThen {
        case category: OfferingCategory => category.acceptCommands
      }
}

sealed trait OfferingCategory extends Aggregate[OfferingCategory, OfferingCategory.Command, OfferingCategory.Event]

case class ActiveOfferingCategory(id: OfferingCategoryId, categoryUri: String, name: String, parent: String, proposed: Boolean, creator: Id,
                                  deprecated: Boolean = false, inputs: List[RdfAnnotation] = Nil, outputs: List[RdfAnnotation] = Nil) extends OfferingCategory {
  def acceptCommands =
    OfferingCategory.actions
      .rejectCommand {
        case cmd: OfferingCategoryCommand if cmd.hasNoOrganization =>
          NotAuthorized(cmd)

        case cmd@CreateOfferingCategory(newName, newParent, newCategoryUri, newProposed, meta) if meta.requesterOrgId.get != creator &&
          (newName != name || newParent != parent || !newCategoryUri.isEmpty && newCategoryUri != categoryUri || newProposed != proposed) =>
          NotAuthorized(cmd, s"created by $creator")
        case cmd@CreateOfferingCategory(_, _, newCategoryUri, _, meta) if !newCategoryUri.isEmpty && newCategoryUri != categoryUri =>
          NotAuthorized(cmd, "not allowed to change uri")
        case cmd@CreateOfferingCategory(_, _, _, newProposed, meta) if newProposed != proposed =>
          NotAuthorized(cmd, "not allowed to change proposed flag")

        case ChangeOfferingCategoryName(uri, newName, meta) if newName.isEmpty =>
          EmptyNameOfferingCategory(uri, meta)
        case cmd: ChangeOfferingCategoryName if cmd.meta.requesterOrgId.get != creator =>
          NotAuthorized(cmd, s"this category may only be changed by $creator")
        case cmd: ChangeOfferingCategoryParent if cmd.meta.requesterOrgId.get != creator =>
          NotAuthorized(cmd, s"this category may only be changed by $creator")
        case DeprecateOfferingCategory(_, meta) if deprecated =>
          OfferingCategoryAlreadyDeprecated(id, meta)
        case UndeprecateOfferingCategory(_, meta) if !deprecated =>
          OfferingCategoryNotDeprecated(id, meta)

        case cmd: AddTypeToOfferingCategoryCommand if !cmd.proposed =>
          NotAuthorized(cmd, "only proposed types are allowed")
        case cmd: AddTypeToOfferingCategoryCommand if cmd.typeUri.isEmpty && cmd.typeName.isEmpty =>
          EmptyNameDataType(id, cmd.typeUri, cmd.meta)

        case AddInputTypeToOfferingCategory(_, typeUri, typeName, _, meta)
          if typeUri.isEmpty && inputs.exists { rdfAnnotation => rdfAnnotation.uri == proposedUri(typeName) && !rdfAnnotation.deprecated} =>
          InputTypeExistsAlready(id, categoryUri, proposedUri(typeName), meta)
        case AddInputTypeToOfferingCategory(_, typeUri, typeName, _, meta)
          if inputs.exists { rdfAnnotation => rdfAnnotation.uri == typeUri && !rdfAnnotation.deprecated} =>
          InputTypeExistsAlready(id, categoryUri, typeUri, meta)

        case DeprecateInputTypeForOfferingCategory(_, typeUri, meta) if !inputs.exists(_.uri == typeUri) =>
          InputTypeDoesntExist(id, categoryUri, typeUri, meta)
        case UndeprecateInputTypeForOfferingCategory(_, typeUri, meta) if !inputs.exists(_.uri == typeUri) =>
          InputTypeDoesntExist(id, categoryUri, typeUri, meta)

        case AddOutputTypeToOfferingCategory(_, typeUri, typeName, _, meta)
          if typeUri.isEmpty && outputs.exists { rdfAnnotation => rdfAnnotation.uri == proposedUri(typeName) && !rdfAnnotation.deprecated} =>
          OutputTypeExistsAlready(id, categoryUri, proposedUri(typeName), meta)
        case AddOutputTypeToOfferingCategory(_, typeUri, typeName, _, meta)
          if outputs.exists { rdfAnnotation => rdfAnnotation.uri == typeUri && !rdfAnnotation.deprecated} =>
          OutputTypeExistsAlready(id, categoryUri, typeUri, meta)

        case DeprecateOutputTypeForOfferingCategory(_, typeUri, meta) if !outputs.exists(_.uri == typeUri) =>
          OutputTypeDoesntExist(id, categoryUri, typeUri, meta)
        case UndeprecateOutputTypeForOfferingCategory(_, typeUri, meta) if !outputs.exists(_.uri == typeUri) =>
          OutputTypeDoesntExist(id, categoryUri, typeUri, meta)
      }
      .commandHandler {
        OneEvent {
          case ChangeOfferingCategoryName(_, newName, meta) if newName == name =>
            OfferingCategoryUnchanged(id, categoryUri, meta)
          case ChangeOfferingCategoryName(_, newName, meta) =>
            OfferingCategoryNameChanged(id, categoryUri, newName, meta)

          case ChangeOfferingCategoryParent(_, newParent, meta) if newParent == parent =>
            OfferingCategoryUnchanged(id, categoryUri, meta)
          case ChangeOfferingCategoryParent(_, newParent, meta) =>
            OfferingCategoryParentChanged(id, categoryUri, newParent, parent, meta)

          case DeprecateOfferingCategory(_, meta) =>
            OfferingCategoryDeprecated(id, categoryUri, meta)
          case UndeprecateOfferingCategory(_, meta) =>
            OfferingCategoryUndeprecated(id, categoryUri, meta)

          case AddInputTypeToOfferingCategory(_, typeUri, typeName, typeProposed, meta) =>
            val rdfAnnotation = RdfAnnotation(createUri(typeUri, typeName), typeName, typeProposed)
            if (inputs.exists(_.uri == typeUri))
              InputTypeUndeprecatedForOfferingCategory(id, categoryUri, rdfAnnotation.uri, meta)
            else
              InputTypeAddedToOfferingCategory(id, categoryUri, rdfAnnotation, meta)
          case DeprecateInputTypeForOfferingCategory(_, typeUri, meta) =>
            InputTypeDeprecatedForOfferingCategory(id, categoryUri, typeUri, meta)
          case UndeprecateInputTypeForOfferingCategory(_, typeUri, meta) =>
            InputTypeUndeprecatedForOfferingCategory(id, categoryUri, typeUri, meta)

          case AddOutputTypeToOfferingCategory(_, typeUri, typeName, typeProposed, meta) =>
            val rdfAnnotation = RdfAnnotation(createUri(typeUri, typeName), typeName, typeProposed)
            if (inputs.exists(_.uri == typeUri))
              OutputTypeUndeprecatedForOfferingCategory(id, categoryUri, rdfAnnotation.uri, meta)
            else
              OutputTypeAddedToOfferingCategory(id, categoryUri, rdfAnnotation, meta)
          case DeprecateOutputTypeForOfferingCategory(_, typeUri, meta) =>
            OutputTypeDeprecatedForOfferingCategory(id, categoryUri, typeUri, meta)
          case UndeprecateOutputTypeForOfferingCategory(_, typeUri, meta) =>
            OutputTypeUndeprecatedForOfferingCategory(id, categoryUri, typeUri, meta)
        }
      }
      .commandHandler {
        ManyEvents {
          case CreateOfferingCategory(newName, newParent, _, _  , meta) =>
            var events: List[OfferingCategoryEvent] = Nil
            if (newName != name) events ::= OfferingCategoryNameChanged(id, categoryUri, newName, meta)
            if (newParent != parent) events ::= OfferingCategoryParentChanged(id, categoryUri, newParent, parent, meta)
            if (deprecated) events ::= OfferingCategoryUndeprecated(id, categoryUri, meta)

            if (events.isEmpty)
              List(OfferingCategoryUnchanged(id, categoryUri, meta))
            else
              markLast(events)
        }
    }
      .eventHandler {
        case OfferingCategoryParentChanged(_, _, newParent, _, _) =>
          copy(parent = newParent)
        case OfferingCategoryParentChanged(_, _, newParent, _, _) =>
          copy(parent = newParent)

        case OfferingCategoryDeprecated(_, _, _) =>
          copy(deprecated = true)
        case OfferingCategoryUndeprecated(_, _, _) =>
          copy(deprecated = false)

        case InputTypeAddedToOfferingCategory(_, _, rdfAnnotation, _) =>
          copy(inputs = inputs :+ rdfAnnotation)
        case InputTypeDeprecatedForOfferingCategory(_, _, typeUri, _) =>
          copy(inputs = inputs map { rdfAnnotation =>
            if (rdfAnnotation.uri == typeUri) rdfAnnotation.copy(deprecated = true) else rdfAnnotation})
        case InputTypeUndeprecatedForOfferingCategory(_, _, typeUri, _) =>
          copy(inputs = inputs map { rdfAnnotation =>
            if (rdfAnnotation.uri == typeUri) rdfAnnotation.copy(deprecated = false) else rdfAnnotation})

        case OutputTypeAddedToOfferingCategory(_, _, rdfAnnotation, _) =>
          copy(outputs = outputs :+ rdfAnnotation)
        case OutputTypeDeprecatedForOfferingCategory(_, _, typeUri, _) =>
          copy(outputs = outputs map { rdfAnnotation =>
            if (rdfAnnotation.uri == typeUri) rdfAnnotation.copy(deprecated = true) else rdfAnnotation})
        case OutputTypeUndeprecatedForOfferingCategory(_, _, typeUri, _) =>
          copy(outputs = outputs map { rdfAnnotation =>
            if (rdfAnnotation.uri == typeUri) rdfAnnotation.copy(deprecated = false) else rdfAnnotation})

        case _ =>
          this
      }
}
