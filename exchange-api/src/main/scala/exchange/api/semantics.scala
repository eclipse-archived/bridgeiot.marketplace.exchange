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

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.language.implicitConversions
import scala.util.Try

import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import io.circe.generic.extras.auto._
import io.circe.syntax._
import io.funcqrs.AggregateId
import microservice.entity.{Entity, Id, normalize}
import microservice.{Command, CreationCommand, Error, Event, Meta, Unchanged, circeConfiguration}
import monocle.macros.Lenses
import sangria.macros.derive.GraphQLDescription

object semantics {
  val serviceName = "semantics"
  val isoFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  val OfferingCategoryPrefix = "urn:big-iot:"
  val RootOfferingCategoryUri = OfferingCategoryPrefix + "allOfferingsCategory"

  case class OfferingCategoryId(value: String) extends AggregateId

  def proposedUri(label: String) = "urn:proposed:" + normalize(label)
  def createUri(uri: String, label: String) = if (uri.isEmpty) proposedUri(label) else normalize(uri)
  def createLabel(name: String, uri: String) = if (name.isEmpty) uri else name

  object OfferingCategoryId {
    implicit def fromString(aggregateId: String): OfferingCategoryId = OfferingCategoryId(aggregateId)
    def fromUri(uri: String) = OfferingCategoryId(normalize(uri))
    def fromLabel(label: String) = OfferingCategoryId(proposedUri(label))
    def create(uri: String, label: String) = OfferingCategoryId(createUri(uri, label))
  }

  @GraphQLDescription("RDF Annotation")
  @Lenses
  case class RdfAnnotation(uri: String, label: String, proposed: Boolean = false, deprecated: Boolean = false)
  case class RdfAnnotations(rdfAnnotations: List[RdfAnnotation])

  @GraphQLDescription("Categorization of Offerings")
  @Lenses
  case class OfferingCategory(rdfAnnotation: RdfAnnotation, subCategories: List[OfferingCategory] = Nil,
                              inputs: List[RdfAnnotation] = Nil, outputs: List[RdfAnnotation] = Nil) extends Entity {
    def id = OfferingCategoryId(normalize(rdfAnnotation.uri))
    override def show(indent: String): String =
      (s"${indent}OfferingCategory(${rdfAnnotation.uri}, ${rdfAnnotation.label + (if (rdfAnnotation.proposed) ", proposed" else "")}):" +:
        subCategories.map(_.show(indent+spaces))).mkString("\n")
  }

  abstract class ValueType(val `type`: String)

  val Number = "number"
  val Integer = "integer"
  val Boolean = "boolean"
  val Text = "text"
  val Datetime = "datetime"
  val Obj = "object"
  val Array = "array"
  val Reference = "reference"
  val Undefined = "undefined"

  implicit lazy val encodeValueType: Encoder[ValueType] = new Encoder[ValueType] {
    final def apply(valueType: ValueType): Json = valueType match {
      case vt: NumberType => asJson(vt, Number)
      case vt: IntegerType => asJson(vt, Integer)
      case vt: BooleanType => asJson(vt, Boolean)
      case vt: TextType => asJson(vt, Text)
      case vt: DateTimeType => asJson(vt, Datetime)
      case vt: ObjectType => asJson(vt, Obj)
      case vt: ArrayType => asJson(vt, Array)
      case vt: RdfReferenceType => asJson(vt, Reference)
      case vt: UndefinedType => asJson(vt, Undefined)
      case _ => Json.Null
    }

    private def asJson[T <: ValueType : Encoder](vt: T, typ: String) =
      Json.obj("type" -> Json.fromString(typ)).deepMerge(vt.asJson)
  }

  implicit val decodeValueType: Decoder[ValueType] = new Decoder[ValueType] {
    final def apply(c: HCursor): Decoder.Result[ValueType] =  c.downField("type").as[String] match {
      case Right(Number) => c.as[NumberType]
      case Right(Integer) => c.as[IntegerType]
      case Right(Boolean) => c.as[BooleanType]
      case Right(Text) => c.as[TextType]
      case Right(Datetime) => c.as[DateTimeType]
      case Right(Obj) => c.as[ObjectType]
      case Right(Array) => c.as[ArrayType]
      case Right(Reference) => c.as[RdfReferenceType]
      case Right(Undefined) => c.as[UndefinedType]
      case _ => Left(DecodingFailure("Wrong ValueType", c.history))
    }
  }

  @GraphQLDescription("Number type")
  case class NumberType(minNumber: Option[Double] = None, maxNumber: Option[Double] = None) extends ValueType(Number)

  @GraphQLDescription("Integer type")
  case class IntegerType(minInt: Option[Int] = None, maxInt: Option[Int] = None) extends ValueType(Integer)

  @GraphQLDescription("Boolean type")
  case class BooleanType() extends ValueType(Boolean)

  @GraphQLDescription("Text type")
  case class TextType() extends ValueType(Text)

  @GraphQLDescription("DateTime type")
  case class DateTimeType(minDateTime: Option[Long] = None, maxDateTime: Option[Long] = None) extends ValueType(Datetime)

  @GraphQLDescription("Complex type")
  case class ObjectType(members: List[DataField]) extends ValueType(Obj)

  @GraphQLDescription("Array type")
  case class ArrayType(element: ValueType) extends ValueType(Array)

  @GraphQLDescription("RDF reference type")
  case class RdfReferenceType(rdfReference: String) extends ValueType(Reference)

  @GraphQLDescription("Undefined value type")
  case class UndefinedType() extends ValueType(Undefined)

  private def invalid(path: List[String], msg: String) = Some(s"${path.mkString(" - ")}: $msg")

  @GraphQLDescription("ValueType input")
  case class ValueTypeInput(`type`: String, members: Option[List[DataFieldInput]] = None, element: Option[ValueTypeInput] = None,
                            minNumber: Option[Double] = None, maxNumber: Option[Double] = None, minInt: Option[Int] = None, maxInt: Option[Int] = None,
                            minDateTime: Option[String] = None, maxDateTime: Option[String] = None, rdfReference: Option[String] = None) {
    private def isComplex(valueType: Option[ValueType]) = valueType.isDefined && (valueType.get.`type` == Obj || valueType.get.`type` == Array)
    private def checkSimple(path: List[String], valueType: Option[ValueType]) =
      if (isComplex(valueType)) invalid(path, s"invalid structure: ${`type`} instead of ${valueType.get.`type`}") else None

    def validate(path: List[String], default: Option[ValueType], forOffering: Boolean = true): Option[String] = `type` match {
      case Number => checkSimple(path, default)
      case Integer => checkSimple(path, default)
      case Datetime => checkSimple(path, default)
      case Boolean => checkSimple(path, default)
      case Text => checkSimple(path, default)
      case Obj => default match {
        case Some(ObjectType(defaultMembers)) =>
          val defaultMap = defaultMembers.map(member => member.rdfAnnotation.uri -> member).toMap
          members match {
            case None => None
            case Some(Nil) => invalid(path, "no members provided")
            case Some(dataFieldInputs) =>
              val error = dataFieldInputs.flatMap { dataFieldInput =>
                val default = defaultMap.get(dataFieldInput.rdfUri)
                dataFieldInput.validate(path, default, forOffering)
              }.mkString(", ")
              if (error.isEmpty) None else Some(error)
          }
        case None => None
        case Some(typ) => invalid(path, s"invalid structure: ${typ.`type`} instead of $Obj")
      }
      case Array => default match {
        case Some(ArrayType(defaultElement)) => if (element.isEmpty) None
          else element flatMap(valueType => valueType.validate(path, Some(defaultElement), forOffering))
        case None => None
        case Some(typ) => invalid(path, s"invalid structure: ${typ.`type`} instead of $Array")
      }
      case Reference => if (rdfReference.isEmpty) invalid(path, "'rdfReference' field missing for reference type") else None
      case Undefined => if (forOffering) invalid(path, "Value type must be defined for Offering") else None
      case typ => invalid(path, s"Wrong value type '$typ'")
    }

    def toValueType(default: Option[ValueType]): Option[ValueType] = `type` match {
      case Number => Some(NumberType(minNumber, maxNumber))
      case Integer => Some(IntegerType(minInt, maxInt))
      case Datetime =>
        val min = minDateTime.flatMap(minStr => Try(OffsetDateTime.parse(minStr, isoFormat).toInstant.toEpochMilli).toOption)
        val max = maxDateTime.flatMap(maxStr => Try(OffsetDateTime.parse(maxStr, isoFormat).toInstant.toEpochMilli).toOption)
        Some(DateTimeType(min, max))
      case Boolean => Some(BooleanType())
      case Text => Some(TextType())
      case Obj => default match {
        case Some(ObjectType(defaultMembers)) =>
          val defaultMap = defaultMembers.map(member => member.rdfAnnotation.uri -> member).toMap
          members match {
            case None => Some(ObjectType(defaultMembers))
            case Some(Nil) => Some(ObjectType(defaultMembers))
            case Some(dataFieldInputs) =>
              val dataFields = dataFieldInputs.flatMap { dataFieldInput =>
                val default = defaultMap.get(dataFieldInput.rdfUri)
                dataFieldInput.toDataField(default)
              }
              if (dataFields.isEmpty) None else Some(ObjectType(dataFields))
          }
        case _ => None
      }
      case Array => element flatMap (_.toValueType(default)) map ArrayType
      case Reference => rdfReference map RdfReferenceType
      case Undefined => Some(UndefinedType())
      case _ => None
    }
  }

  @GraphQLDescription("DataField description")
  case class DataField(name: String, rdfAnnotation: RdfAnnotation, value: ValueType, encodingType: String = "query", required: Boolean = false)

  @GraphQLDescription("DataField input description")
  case class DataFieldInput(name: Option[String], rdfUri: String, value: Option[ValueTypeInput], encodingType: String = "query", required: Boolean = false) {
    def validate(path: List[String], default: Option[DataField], forOffering: Boolean = true): Option[String] =
      if (rdfUri.isEmpty) invalid(path, s"rdfUri is empty (name:${name.getOrElse("None")}")
      else value.flatMap(_.validate(path :+ s"${name.getOrElse("")}($rdfUri)", default.map(_.value), forOffering))

    def toDataField(default: Option[DataField]): Option[DataField] = {
      value.flatMap(_.toValueType(default.map(_.value))).orElse(default.map(_.value)) map { valueType =>
        val n = name.orElse(default.map(_.name)).getOrElse("")
        val rdfAnnotation = default.map(_.rdfAnnotation).getOrElse(RdfAnnotation(rdfUri, ""))
        DataField(n, rdfAnnotation, valueType, encodingType, required)
      }
    }
  }

  @GraphQLDescription("RDF context and prefixes")
  case class RdfContext(context: Option[String], prefixes: List[Prefix])

  @GraphQLDescription("Prefix to enable shortcuts for semantic links")
  case class Prefix(prefix: String, uri: String)

  sealed trait SemanticsCommand extends Command
  sealed trait SemanticsEvent extends Event

  sealed trait OfferingCategoryCommand extends SemanticsCommand
  sealed trait OfferingCategoryEvent extends SemanticsEvent { def uri: String }

  @GraphQLDescription("Create new OfferingCategory")
  case class CreateOfferingCategory(name: String, parent: String, uri: String = "", proposed: Boolean = true, meta: Meta = Meta())
    extends OfferingCategoryCommand with CreationCommand {
    def id = OfferingCategoryId.create(uri, name)
  }
  case class OfferingCategoryCreated(id: OfferingCategoryId, uri: String, name: String, parent: String, proposed: Boolean, meta: Meta) extends OfferingCategoryEvent
  case class OfferingCategoryUnchanged(id: OfferingCategoryId, uri: String, meta: Meta) extends OfferingCategoryEvent with Unchanged

  sealed trait ChangeOfferingCategoryCommand extends OfferingCategoryCommand {
    def uri: String
    def id = OfferingCategoryId.fromUri(uri)
  }

  @GraphQLDescription("Change OfferingCategory name")
  case class ChangeOfferingCategoryName(uri: String, name: String, meta: Meta = Meta()) extends ChangeOfferingCategoryCommand
  case class OfferingCategoryNameChanged(id: OfferingCategoryId, uri: String, name: String, meta: Meta) extends OfferingCategoryEvent

  @GraphQLDescription("Change OfferingCategory parent")
  case class ChangeOfferingCategoryParent(uri: String, parent: String, meta: Meta = Meta()) extends ChangeOfferingCategoryCommand
  case class OfferingCategoryParentChanged(id: OfferingCategoryId, uri: String, parent: String, oldParent: String = "", meta: Meta) extends OfferingCategoryEvent

  @GraphQLDescription("Deprecate OfferingCategory")
  case class DeprecateOfferingCategory(uri: String, meta: Meta = Meta()) extends ChangeOfferingCategoryCommand
  case class OfferingCategoryDeprecated(id: OfferingCategoryId, uri: String, meta: Meta) extends OfferingCategoryEvent
  @GraphQLDescription("Undeprecate OfferingCategory")
  case class UndeprecateOfferingCategory(uri: String, meta: Meta = Meta()) extends ChangeOfferingCategoryCommand
  case class OfferingCategoryUndeprecated(id: OfferingCategoryId, uri: String, meta: Meta) extends OfferingCategoryEvent

  sealed trait TypeForOfferingCategoryCommand extends ChangeOfferingCategoryCommand {
    def typeUri: String
  }

  sealed trait AddTypeToOfferingCategoryCommand extends TypeForOfferingCategoryCommand {
    def typeName: String
    def proposed: Boolean
  }

  @GraphQLDescription("Add recommended input type to OfferingCategory")
  case class AddInputTypeToOfferingCategory(uri: String, typeUri: String, typeName: String, proposed: Boolean = true, meta: Meta = Meta()) extends AddTypeToOfferingCategoryCommand
  case class InputTypeAddedToOfferingCategory(id: OfferingCategoryId, uri: String, rdfAnnotation: RdfAnnotation, meta: Meta) extends OfferingCategoryEvent

  @GraphQLDescription("Add recommended output type to OfferingCategory")
  case class AddOutputTypeToOfferingCategory(uri: String, typeUri: String, typeName: String, proposed: Boolean = true, meta: Meta = Meta()) extends AddTypeToOfferingCategoryCommand
  case class OutputTypeAddedToOfferingCategory(id: OfferingCategoryId, uri: String, rdfAnnotation: RdfAnnotation, meta: Meta) extends OfferingCategoryEvent

  sealed trait ChangeTypeForOfferingCategoryCommand extends TypeForOfferingCategoryCommand

  @GraphQLDescription("Deprecate input type for OfferingCategory")
  case class DeprecateInputTypeForOfferingCategory(uri: String, typeUri: String, meta: Meta = Meta()) extends ChangeTypeForOfferingCategoryCommand
  case class InputTypeDeprecatedForOfferingCategory(id: OfferingCategoryId, uri: String, typeUri: String, meta: Meta) extends OfferingCategoryEvent
  @GraphQLDescription("Undeprecate input type for OfferingCategory")
  case class UndeprecateInputTypeForOfferingCategory(uri: String, typeUri: String, meta: Meta = Meta()) extends ChangeTypeForOfferingCategoryCommand
  case class InputTypeUndeprecatedForOfferingCategory(id: OfferingCategoryId, uri: String, typeUri: String, meta: Meta) extends OfferingCategoryEvent

  @GraphQLDescription("Deprecate output type for OfferingCategory")
  case class DeprecateOutputTypeForOfferingCategory(uri: String, typeUri: String, meta: Meta = Meta()) extends ChangeTypeForOfferingCategoryCommand
  case class OutputTypeDeprecatedForOfferingCategory(id: OfferingCategoryId, uri: String, typeUri: String, meta: Meta) extends OfferingCategoryEvent
  @GraphQLDescription("Undeprecate output type for OfferingCategory")
  case class UndeprecateOutputTypeForOfferingCategory(uri: String, typeUri: String, meta: Meta = Meta()) extends ChangeTypeForOfferingCategoryCommand
  case class OutputTypeUndeprecatedForOfferingCategory(id: OfferingCategoryId, uri: String, typeUri: String, meta: Meta) extends OfferingCategoryEvent

  // Errors
  case class EmptyNameOfferingCategory(override val id: Id, override val meta: Meta) extends
    Error(id, "EmptyLabelOfferingCategory", s"uri $id: label must be non empty", meta)
  case class InvalidOfferingCategory(override val id: Id, override val meta: Meta) extends
    Error(id, "InvalidOfferingCategory", s"uri $id is invalid", meta)
  case class NoDataFieldsAllowedWithoutOfferingCategory(override val id: Id, override val meta: Meta) extends
    Error(id, "NoDataFieldsAllowedWithoutOfferingCategory", id, meta)
  case class OfferingCategoryDoesNotExist(override val id: Id, override val meta: Meta) extends
    Error(id, "OfferingCategoryDoesNotExist", id, meta)
  case class OfferingCategoryParentDoesNotExist(override val id: Id, parent: String, override val meta: Meta) extends
    Error(id, "OfferingCategoryParentDoesNotExist", parent, meta)
  case class OfferingCategoryAlreadyDeprecated(override val id: Id, override val meta: Meta) extends
    Error(id, "OfferingCategoryAlreadyDeprecated", id, meta)
  case class OfferingCategoryNotDeprecated(override val id: Id, override val meta: Meta) extends
    Error(id, "OfferingCategoryNotDeprecated", id, meta)

  case class EmptyNameDataType(override val id: Id, uri: String, override val meta: Meta) extends
    Error(id, "EmptyLabelDataType", s"uri $uri: label must be non empty", meta)
  case class InvalidDataTypes(override val id: Id, messages: List[String], override val meta: Meta) extends
    Error(id, "InvalidDataTypes", messages.mkString("; "), meta)
  case class InputTypeExistsAlready(override val id: Id, categoryUri: String, typeyUri: String, override val meta: Meta) extends
    Error(id, "InputTypeExistsAlready", s"category:$categoryUri, type:$typeyUri", meta)
  case class InputTypeDoesntExist(override val id: Id, categoryUri: String, typeyUri: String, override val meta: Meta) extends
    Error(id, "InputTypeDoesntExist", s"category:$categoryUri, type:$typeyUri", meta)
  case class OutputTypeExistsAlready(override val id: Id, categoryUri: String, typeyUri: String, override val meta: Meta) extends
    Error(id, "OutputTypeExistsAlready", s"category:$categoryUri, type:$typeyUri", meta)
  case class OutputTypeDoesntExist(override val id: Id, categoryUri: String, typeyUri: String, override val meta: Meta) extends
    Error(id, "OutputTypeDoesntExist", s"category:$categoryUri, type:$typeyUri", meta)
}
