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
package exchange.repo.rdfstore;

import exchange.api.*;
import exchange.api.consumer.ConsumerDeleted;
import exchange.api.extent.BoundingBox;
import exchange.api.offering.*;
import exchange.api.offeringquery.OfferingQueryLicenseChanged;
import exchange.api.offeringquery.OfferingQueryNameChanged;
import exchange.api.offeringquery.OfferingQueryPriceChanged;
import exchange.api.organization.OrganizationNameChanged;
import exchange.api.provider.ProviderDeleted;
import exchange.api.provider.ProviderNameChanged;
import exchange.api.semantics.DataField;
import exchange.api.semantics.OfferingCategoryNameChanged;
import exchange.api.subscription.SubscriptionDeleted;
import exchange.api.price.PricingModel;
import exchange.model.vocabs.BIGIOT;
import exchange.model.vocabs.PREFIXES;
import exchange.model.vocabs.SCHEMA;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import static exchange.api.offeringquery.*;
import static exchange.api.semantics.*;
import static exchange.model.vocabs.PREFIXES.prefixesToString;
import static scala.collection.JavaConverters.*;


public class QueryFactory {

    public static String OFFERING_GRAPH;
    public static String ONTOLOGY_GRAPH;

    private static final String PREFIXES_str = prefixesToString();
    private static final PricingModel FREE = exchange.api.price$.MODULE$.FREE();
    final static Logger logger = LoggerFactory.getLogger(QueryFactory.class);


    private static class RDFNodeFormatter implements RDFVisitor {

        public Object visitURI(Resource r, String uri) {
            return "<" + uri + ">";
        }

        public Object visitLiteral(Literal l) {
            return "\"" + l + "\"^^<" + l.getDatatypeURI() + ">";
        }

        public Object visitBlank(Resource r, AnonId id) {
            return "_:" + id;
        }

    }

    public static void setOfferingGraph(String graph){
        OFFERING_GRAPH = graph;
    }

    public static String getOfferingGraph() {
        return OFFERING_GRAPH;
    }

    public static String getOntologyGraph() {
        return ONTOLOGY_GRAPH;
    }

    public static void setOntologyGraph(String graph){
        ONTOLOGY_GRAPH = graph;
    }

    public static String getPREFIXES() {
        return PREFIXES_str;
    }

    public static String create(Resource rdfClass) {
        return create(rdfClass, null);
    }

    // TODO move to resources (use Jena's QueryFactory.read())
    public static String create(Resource rdfClass, Map<String, RDFNode> bindings) {
        String str = createString(rdfClass);

        if (bindings != null) {
            for (String v : bindings.keySet()) {
                RDFNodeFormatter formatter = new RDFNodeFormatter();
                String b = (String) bindings.get(v).visitWith(formatter);
                str = str.replace("?" + v, b);
            }
        }

        return str;
    }

    private static String createString(Resource rdfClass) {
        if (rdfClass.equals(BIGIOT.OfferingQuery)) {
            return PREFIXES_str +
                    "CONSTRUCT {\n" +
                    "      ?offeringQuery a bigiot-core:OfferingQuery;\n" +
                    "           bigiot-core:queryId ?id;\n" +
                    "           schema:priceSpecification ?price;\n" +
                    "           schema:name ?name;\n" +
                    "           schema:license ?licenseIndiv;\n" +
                    "           bigiot-core:isRegisteredBy ?consumer;\n" +
                    "           bigiot-core:hasOutput ?outputData;\n" +
                    "           bigiot-core:hasInput ?inputData;\n" +
                    "           schema:category ?category;\n" +
                    "           schema:spatialCoverage ?area;\n" +
                    "           schema:validFrom ?validFrom;\n" +
                    "           schema:validThrough ?validThrough.\n" +
                    "      ?outputData schema:name ?outputName;\n" +
                    "                  bigiot-core:rdfAnnotation ?outputType;\n" +
                    "                  bigiot-core:value ?outputValue.\n" +
                    "      ?outputValue bigiot-core:valueType ?outputValueType.\n" +
                    "      ?inputData schema:name ?inputName;\n" +
                    "                  bigiot-core:rdfAnnotation ?inputType;\n" +
                    "                  bigiot-core:value ?inputValue.\n" +
                    "      ?inputValue bigiot-core:valueType ?inputValueType.\n" +
                    "      ?area rdfs:label ?extent;\n" +
                    "            bigiot-core:lowerBoundLatitude ?lowerLat;\n" +
                    "            bigiot-core:lowerBoundLongitude ?lowerLng;\n" +
                    "            bigiot-core:upperBoundLatitude ?upperLat;\n" +
                    "            bigiot-core:upperBoundLongitude ?upperLng.\n" +
                    "      ?price bigiot-core:pricingModel ?pricingModelIndiv;\n" +
                    "             schema:priceCurrency ?currency;\n" +
                    "             schema:price ?amount.\n" +
                    "      ?pricingModelIndiv rdfs:label ?pricingModel.\n" +
                    "      ?licenseIndiv bigiot-core:licenseType ?licenseType.\n" +
                    "      ?licenseType rdfs:label ?license.\n" +
                    "      ?subscription bigiot-core:subscribedQuery ?offeringQuery.\n" +
                       "}\n" +
                    "FROM <" + OFFERING_GRAPH + "> \n" +
                    "FROM <" + ONTOLOGY_GRAPH + "> \n" +
                    "WHERE{\n" +
                    "      ?offeringQuery a bigiot-core:OfferingQuery;\n" +
                    "           bigiot-core:queryId ?id;\n" +
                    "           schema:name ?name;\n" +
                    "           bigiot-core:isRegisteredBy ?consumer.\n" +
                    "      OPTIONAL{\n" +
                    "           ?subscription bigiot-core:subscribedQuery ?offeringQuery.\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery schema:category ?category.\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery schema:validFrom ?validFrom.\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery schema:validThrough ?validThrough.\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery bigiot-core:hasOutput  ?outputData.\n"+
                    "           ?outputData  bigiot-core:rdfAnnotation ?outputType;\n" +
                    "                        schema:name ?outputName;\n" +
                    "                        bigiot-core:value ?outputValue.\n" +
                    "           OPTIONAL{\n" +
                    "                ?outputValue bigiot-core:valueType ?outputValueType.\n" +
                    "           }\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery bigiot-core:hasInput  ?inputData.\n"+
                    "           ?inputData  bigiot-core:rdfAnnotation ?inputType;\n" +
                    "                       schema:name ?inputName;\n" +
                    "                       bigiot-core:value ?inputValue.\n" +
                    "           OPTIONAL{\n" +
                    "                ?inputValue bigiot-core:valueType ?inputValueType.\n" +
                    "           }\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery schema:spatialCoverage ?area.\n" +
                    "           ?area rdfs:label ?extent.\n" +
                    "           OPTIONAL{\n" +
                    "               ?area bigiot-core:lowerBoundLatitude ?lowerLat;\n" +
                    "                     bigiot-core:lowerBoundLongitude ?lowerLng;\n" +
                    "                     bigiot-core:upperBoundLatitude ?upperLat;\n" +
                    "                     bigiot-core:upperBoundLongitude ?upperLng.\n" +
                    "           }\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery schema:priceSpecification ?price.\n" +
                    "           ?price bigiot-core:pricingModel ?pricingModelIndiv.\n" +
                    "           ?pricingModelIndiv rdfs:label ?pricingModel.\n" +
                    "           OPTIONAL{\n" +
                    "               ?price schema:priceCurrency ?currency;\n" +
                    "                          schema:price ?amount.\n" +
                    "           }\n" +
                    "      }\n" +
                    "      OPTIONAL{\n" +
                    "           ?offeringQuery schema:license ?licenseIndiv.\n" +
                    "           ?licenseIndiv bigiot-core:licenseType ?licenseType.\n" +
                    "           ?licenseType rdfs:label ?license.\n" +
                    "      }\n" +
                    "}\n";
        } else if (rdfClass.equals(BIGIOT.OfferingCategory)) {
            return PREFIXES_str +
                    "CONSTRUCT{\n" +
                    "   ?category a bigiot-core:OfferingCategory;\n" +
                    "      rdfs:label ?label;\n" +
                    "      skos:narrower ?sub;\n" +
                    "      bigiot-core:expectedAnnotation ?prop.\n" +
                    "   ?category a ?proposedType.\n" +
                    "   ?prop a bigiot-core:DatatypeAnnotation;\n" +
                    "         rdfs:label ?propLabel.\n" +
                    "   ?prop a ?proposedDataType.\n" +
                    "}\n" +
                    "FROM <" + ONTOLOGY_GRAPH + "> \n" +
                    " WHERE {" +
                    "   ?category a bigiot-core:OfferingCategory.\n" +
                    "   OPTIONAL {   \n" +
                    "         ?category bigiot-core:expectedAnnotation ?prop.\n" +
                    "            ?prop a bigiot-core:DatatypeAnnotation;\n" +
                    "            rdfs:label ?propLabel.\n" +
                    "         OPTIONAL {\n" +
                    "           ?prop a ?proposedDataType.filter(?proposedDataType=<"+BIGIOT.ProposedDatatypeAnnotation.getURI()+">)\n" +
                    "         }\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category a ?proposedType.filter(?proposedType=<"+BIGIOT.ProposedOfferingCategory.getURI()+">)\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category rdfs:label ?label.\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category skos:narrower ?sub.\n" +
                    "      OPTIONAL {\n" +
                    "          ?sub rdfs:label ?sublabel.\n" +
                    "       }\n" +
                    "   }\n" +
                    "}";
        } else if (rdfClass.equals(BIGIOT.DatatypeAnnotation)) {
            return PREFIXES_str +
                   "CONSTRUCT {\n" +
                   "     ?annotation a bigiot-core:DatatypeAnnotation .\n" +
                     "   ?annotation rdfs:label ?label.\n" +
                    "    ?annotation a ?proposedDataType.\n" +
                    "    ?annotation schema:rangeIncludes ?type.\n" +
                    "    ?annotation bigiot-core:hasMember ?member.\n" +
                   "}\n" +
                    "FROM <" + ONTOLOGY_GRAPH + "> \n" +
                    "WHERE{\n" +
                    "   ?annotation a bigiot-core:DatatypeAnnotation.\n" +
                    "   OPTIONAL {\n" +
                    "       ?annotation a ?proposedDataType.filter(?proposedDataType=<"+BIGIOT.ProposedDatatypeAnnotation.getURI()+">)\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "       ?annotation rdfs:label ?label.\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "       ?annotation schema:rangeIncludes ?type.\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "       ?annotation bigiot-core:hasMember ?member.\n" +
                    "   }\n" +
                    "}\n";
        }
        return null;
    }

    public static String allCategoryUris() {
        return PREFIXES_str +
                "SELECT distinct ?category \n" +
                "FROM <" + ONTOLOGY_GRAPH + "> \n" +
                "WHERE {" +
                "   ?category a bigiot-core:OfferingCategory.\n" +
                "}";
    }

    public static String getDataTypeAnnotationTree() {
        return PREFIXES_str +
                "CONSTRUCT {\n" +
                "     ?annotation a bigiot-core:DatatypeAnnotation .\n" +
                "   ?annotation rdfs:label ?label.\n" +
                "    ?annotation a ?proposedDataType.\n" +
                "    ?annotation schema:rangeIncludes ?type.\n" +
                "    ?annotation bigiot-core:hasMember ?member.\n" +
                "}\n" +
                "FROM <" + ONTOLOGY_GRAPH + "> \n" +
                "WHERE{\n" +
                "   ?annotation a bigiot-core:DatatypeAnnotation.\n" +
                "   OPTIONAL {\n" +
                "       ?annotation a ?proposedDataType.filter(?proposedDataType=<"+BIGIOT.ProposedDatatypeAnnotation.getURI()+">)\n" +
                "   }\n" +
                "   OPTIONAL {\n" +
                "       ?annotation rdfs:label ?label.\n" +
                "   }\n" +
                "   OPTIONAL {\n" +
                "       ?annotation schema:rangeIncludes ?type.\n" +
                "   }\n" +
                "   OPTIONAL {\n" +
                "       ?annotation bigiot-core:hasMember ?member.\n" +
                "   }\n" +
                "}\n";
    }

    public static String getCategoriesTree(boolean isProposedIncluded) {
        String graph = ONTOLOGY_GRAPH;
        if(isProposedIncluded) {
//            graph = QueryFactory.getOfferingGraph();
            return PREFIXES_str +
                    "CONSTRUCT{\n" +
                    "   ?category a bigiot-core:OfferingCategory;\n" +
                    "      rdfs:label ?label;\n" +
                    "      skos:narrower ?sub.\n" +
                    "   ?category a ?proposedType.\n" +
                    "}\n" +
                    "FROM <" + graph + "> \n" +
                    " WHERE {" +
                    "   ?category a bigiot-core:OfferingCategory.\n" +
                    "   OPTIONAL {\n" +
                    "      ?category a ?proposedType.filter(?proposedType=<" + BIGIOT.ProposedOfferingCategory.getURI() + ">)\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category rdfs:label ?label.\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category skos:narrower ?sub.\n" +
                    "   }\n" +
                    "}";
        }else
            return PREFIXES_str +
                    "CONSTRUCT{\n" +
                    "   ?category a bigiot-core:OfferingCategory;\n" +
                    "      rdfs:label ?label;\n" +
                    "      skos:narrower ?sub.\n" +
                    "}\n" +
                    "FROM <"+ graph + "> \n" +
                    "WHERE {\n" +
                    "   ?category a bigiot-core:OfferingCategory.\n" +
                    "   OPTIONAL {\n" +
                    "      ?category rdfs:label ?label.\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category skos:narrower ?sub.\n" +
                    "      OPTIONAL {\n" +
                    "          ?sub rdfs:label ?sublabel.\n" +
                    "       }\n" +
                    "   }\n" +
                    "}\n";
        }

    public static String allOfferingCategories(boolean isProposedIncluded) {
        String graph = ONTOLOGY_GRAPH;
        if(isProposedIncluded) {
//            graph = QueryFactory.getOfferingGraph();
            return PREFIXES_str +
                    "CONSTRUCT{\n" +
                    "   ?category a bigiot-core:OfferingCategory;\n" +
                    "      rdfs:label ?label;\n" +
                    "      skos:narrower ?sub;\n" +
                    "      bigiot-core:expectedAnnotation ?prop.\n" +
                    "   ?category a ?proposedType.\n" +
                    "   ?prop a bigiot-core:DatatypeAnnotation;\n" +
                    "         rdfs:label ?propLabel.\n" +
                    "   ?prop a ?proposedDataType.\n" +
                    "}\n" +
                    "FROM <" + graph + "> \n" +
                    " WHERE {" +
                    "   ?category a bigiot-core:OfferingCategory.\n" +
                    "   OPTIONAL {   \n" +
                    "         ?category bigiot-core:expectedAnnotation ?prop.\n" +
                    "            ?prop a bigiot-core:DatatypeAnnotation;\n" +
                    "            rdfs:label ?propLabel.\n" +
                    "         OPTIONAL {\n" +
                    "           ?prop a ?proposedDataType.filter(?proposedDataType=<"+BIGIOT.ProposedDatatypeAnnotation.getURI()+">)\n" +
                    "         }\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category a ?proposedType.filter(?proposedType=<"+BIGIOT.ProposedOfferingCategory.getURI()+">)\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category rdfs:label ?label.\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category skos:narrower ?sub.\n" +
                    "   }\n" +
                    "}";
        }else
            return PREFIXES_str +
                    "CONSTRUCT{\n" +
                    "   ?category a bigiot-core:OfferingCategory;\n" +
                    "      rdfs:label ?label;\n" +
                    "      skos:narrower ?sub;\n" +
                    "      bigiot-core:expectedAnnotation ?prop.\n" +
                    "   ?prop a bigiot-core:DatatypeAnnotation;\n" +
                    "         rdfs:label ?propLabel.\n" +
                    "}\n" +
                    "FROM <"+ graph + "> \n" +
                    "WHERE {\n" +
                    "   ?category a bigiot-core:OfferingCategory;\n" +
                    "                    bigiot-core:expectedAnnotation ?prop.\n" +
                    "   OPTIONAL {\n" +
                    "      ?category rdfs:label ?label.\n" +
                    "   }\n" +
                    "   OPTIONAL {\n" +
                    "      ?category skos:narrower ?sub.\n" +
                    "      OPTIONAL {\n" +
                    "          ?sub rdfs:label ?sublabel.\n" +
                    "       }\n" +
                    "   }\n" +
                    "   ?prop a bigiot-core:DatatypeAnnotation;\n" +
                    "         rdfs:label ?propLabel.\n" +
                    "}\n";
    }

    public static String providerDeleted(ProviderDeleted ev) {
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE {\n" +
                "      ?provider ?p ?o.\n" +
                "      ?offering ?offp ?offo.\n" +
                "}\n" +
                "WHERE{\n" +
                "      ?provider ?p ?o;\n" +
                "           bigiot-core:providerId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL {\n" +
                "           ?offering bigiot-core:isProvidedBy ?provider; ?offp ?offo.\n" +
                "      }\n" +
                "}\n";
    }

    public static String organizationNameChanged(OrganizationNameChanged ev) {
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?org schema:name ?name.} \n" +
                "INSERT{?org schema:name \"" + ev.name() + "\".} \n" +
                "WHERE{\n" +
                "      ?org schema:name ?name;\n" +
                "           bigiot-core:organizationId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
    }

    public static String providerNameChanged(ProviderNameChanged ev) {
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?pro schema:name ?name.} \n" +
                "INSERT{?pro schema:name \"" + ev.name() + "\".} \n" +
                "WHERE{\n" +
                "      ?pro schema:name ?name;\n" +
                "           bigiot-core:providerId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
    }

    public static String offeringNameChanged(OfferingNameChanged ev) {
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?off schema:name ?name.} \n" +
                "INSERT{?off schema:name \"" + ev.name() + "\".} \n" +
                "WHERE{\n" +
                "      ?off schema:name ?name;\n" +
                "           bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
     }

    public static String offeringLicenseChanged(OfferingLicenseChanged ev) {
        logger.info("update offering {} with new license {}", ev.id(), ev.license().toString());
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?license bigiot-core:licenseType ?licenseType. } \n" +
                "INSERT{?license bigiot-core:licenseType <" + BIGIOT.getLicenseType(ev.license().toString()) + ">.} \n" +
                "WHERE{\n" +
                "      ?off bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                "           schema:license ?license.\n" +
                "      ?license bigiot-core:licenseType ?licenseType. " +
                "}\n";
    }

    public static String offeringPriceChanged(offering.OfferingPriceChanged ev) {
        logger.info("Update offering price {}",ev.price());
        if(ev.price().pricingModel().equals(QueryFactory.FREE)) {
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{" +
                    "    ?priceSpec bigiot-core:pricingModel ?priceModel; " +
                    "                 schema:priceCurrency ?currency;\n" +
                    "                 schema:price ?priceAmount.\n" +
                    "} \n" +
                    "INSERT{" +
                    "    ?priceSpec bigiot-core:pricingModel <" + BIGIOT.getPriceModel(ev.price().pricingModel().toString()) + ">;" +
                    "                  a bigiot-core:Price;\n" +
                    "                  schema:priceCurrency 'EUR';\n" +
                    "                  schema:price '0.0'^^http://www.w3.org/2001/XMLSchema#double.\n" +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?off a bigiot-core:Offering;\n" +
                    "           bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                    "           schema:priceSpecification ?priceSpec.\n" +
                    "           ?priceSpec bigiot-core:pricingModel ?priceModel;\n" +
                    "                      schema:priceCurrency ?currency;\n" +
                    "                      schema:price ?priceAmount.\n" +
                    "}\n";
        }
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?priceSpec bigiot-core:pricingModel ?priceModel;\n " +
                "                 schema:priceCurrency ?currency;\n" +
                "                 schema:price ?priceAmount.\n" +
                "} \n" +
                "INSERT{?priceSpec bigiot-core:pricingModel <" + BIGIOT.getPriceModel(ev.price().pricingModel().toString()) + ">;\n" +
                "                  schema:priceCurrency \"" + ev.price().money().get().currency().value() + "\"^^xsd:string;\n" +
                "                  schema:price " + ev.price().money().get().amount() + ".\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?off a bigiot-core:Offering;\n" +
                "           bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "           ?off schema:priceSpecification ?priceSpec.\n" +
                "           ?priceSpec bigiot-core:pricingModel ?priceModel;\n" +
                "                      schema:priceCurrency ?currency;\n" +
                "                      schema:price ?priceAmount.\n" +
                "}\n";
    }

    public static String offeringTemporalExtentChanged(OfferingTemporalExtentChanged ev) {
        String deletePhrase =  "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                                "DELETE{"+
                                "      ?off schema:validFrom ?fromTime.\n" +
                                "      ?off schema:validThrough ?toTime.\n" +
                                "} \n" ;

        String insertPhrase = " INSERT{";
        String wherePhrase = " WHERE{\n" +
                "      ?off bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL{\n" +
                "           ?off schema:validFrom ?fromTime.\n" +
                "      } \n" +
                "      OPTIONAL{\n" +
                "           ?off schema:validThrough ?toTime.\n" +
                "      }\n" +
                "} \n";

        if(ev.temporalExtent().isDefined()) {
            if(ev.temporalExtent().get().from().isDefined()){
                insertPhrase+= " ?off schema:validFrom \"" + ev.temporalExtent().get().from().get() + "\"^^<" + XSD.xlong.getURI() + ">.\n";
            }else
                insertPhrase+= " ?off schema:validFrom \"" + 0 + "\"^^<" + XSD.xlong.getURI() + ">.\n";
            if(ev.temporalExtent().get().to().isDefined()){
                insertPhrase+= " ?off schema:validThrough \"" + ev.temporalExtent().get().to().get() + "\"^^<" + XSD.xlong.getURI() + ">.\n";
            }else
                insertPhrase+= " ?off schema:validThrough \"" + 0 + "\"^^<" + XSD.xlong.getURI() + ">.\n";

            return PREFIXES_str +
                    deletePhrase +
                    insertPhrase +
                    "} \n" +
                    wherePhrase;
        }else
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{" +
                    "      ?off schema:validFrom ?fromTime.\n" +
                    "      ?off schema:validThrough ?toTime.\n" +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?off bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                    "      OPTIONAL{\n" +
                    "           ?off schema:validFrom ?fromTime.\n" +
                    "      } \n" +
                    "      OPTIONAL{\n" +
                    "           ?off schema:validThrough ?toTime.\n" +
                    "      } \n" +
                    "}\n";
    }

    public static String offeringInputDataFieldsDeleted(OfferingInputsChanged ev) {
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "     ?offering bigiot-core:hasInput  ?inputData.\n"+
                "     ?inputData  bigiot-core:rdfAnnotation ?inputType;\n" +
                "                 schema:name ?inputName.\n" +
                "     ?inputData  bigiot-core:value ?value.\n" +
                "     ?value ?p ?o.\n" +
                "}\n" +
                "WHERE{\n" +
                "      ?offering a bigiot-core:Offering;\n" +
                "           bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL{\n" +
                "           ?offering bigiot-core:hasInput  ?inputData.\n"+
                "           ?inputData  bigiot-core:rdfAnnotation ?inputType;\n" +
                "                        schema:name ?inputName.\n" +
                "           OPTIONAL{\n" +
                "               ?inputData  bigiot-core:value ?value.\n" +
                "               ?value ?p ?o.\n" +
                "           }\n" +
                "      }\n" +
                "}\n";
    }

    public static String offeringDeleted(OfferingDeleted event) {
        logger.info("Deleting offering");
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE {\n" +
                "      ?offering bigiot-core:offeringId \"" + event.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                "           schema:priceSpecification ?priceSpec;\n" +
                "           schema:license ?licenseIndiv;\n" +
                "           ?p ?o.\n" +
                "      ?offering schema:spatialCoverage ?area.\n" +
                "      ?area ?areaP ?areaO.\n" +
                "      ?priceSpec ?priceP ?priceO.\n" +
                "      ?licenseIndiv ?licenseIndivP ?licenseIndivO.\n" +
                "      ?offering bigiot-core:endpoint ?endpoint.\n" +
                "      ?endpoint ?endpointP ?endpointO.\n" +
                "}\n" +
                "WHERE{\n" +
                "      ?offering ?p ?o;\n" +
                "           bigiot-core:offeringId \"" + event.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                "           schema:priceSpecification ?priceSpec;\n" +
                "           schema:license ?licenseIndiv.\n" +
                "      OPTIONAL{\n" +
                "           ?offering schema:spatialCoverage ?area.\n" +
                "           ?area ?areaP ?areaO.\n" +
                "      }\n" +
                "      ?priceSpec ?priceP ?priceO.\n" +
                "      ?licenseIndiv ?licenseIndivP ?licenseIndivO.\n" +
                "      OPTIONAL {\n" +
                "           ?offering bigiot-core:endpoint ?endpoint.\n" +
                "           ?endpoint ?endpointP ?endpointO.\n" +
                "      }\n" +
                "}\n";
    }


    public static String offeringOutputDataFieldsDeleted(OfferingOutputsChanged ev) {
        logger.info("Deleting offering output data");
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "    ?offering bigiot-core:hasOutput  ?outputData.\n"+
                "    ?outputData  ?p ?o.\n" +
                "    ?value ?p1 ?o1.\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?offering a bigiot-core:Offering;\n" +
                "           bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL{\n" +
                "           ?offering bigiot-core:hasOutput  ?outputData.\n"+
                "           ?outputData  ?p ?o.\n" +
                "           OPTIONAL{\n" +
                "               ?outputData  bigiot-core:value ?value.\n" +
                "               ?value ?p1 ?o1.\n" +
                "           }\n" +
                "      }\n" +
                "}\n";
    }

    public static String consumerNameChanged(consumer.ConsumerNameChanged ev) {
        logger.info("Change consumer name");
        String queryStr = QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?off schema:name ?name.} \n" +
                "INSERT{?off schema:name \"" + ev.name() + "\".} \n" +
                "WHERE{\n" +
                "      ?off schema:name ?name;\n" +
                "           bigiot-core:consumerId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
        return queryStr;
    }

    public static String consumerDeleted(ConsumerDeleted event) {
        logger.info("Deleting consumer");
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE {\n" +
                "      ?consumer ?p ?o.\n" +
                "      ?offeringQuery ?offp ?offo.\n" +
                "}\n" +
                "WHERE{\n" +
                "      ?consumer ?p ?o;\n" +
                "           bigiot-core:consumerId \"" + event.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL {\n" +
                "           ?offeringQuery bigiot-core:isRegisteredBy ?consumer; ?offp ?offo.\n" +
                "      }\n" +
                "}\n";
    }

    public static String spatialFilterOfferings(OfferingQuery query){
        logger.info("Finding matching offering with spatial criteria {}",query);
        if(!query.spatialExtent().isDefined())
            return null;

        String queryStr = "SELECT distinct ?id \n" +
                "FROM <" + OFFERING_GRAPH + "> \n" +
                "WHERE{\n" +
                "      ?offering a bigiot-core:Offering;\n" +
                "           bigiot-core:offeringId ?id;\n" +
                "           schema:spatialCoverage ?area.\n"+
                "?area <http://www.w3.org/2003/01/geo/wgs84_pos#geometry> ?geo.\n";
        String spatialFilter = "";
        if(query.spatialExtent().get().boundary().isDefined()){
            BoundingBox boundingBox  = query.spatialExtent().get().boundary().get();
            spatialFilter = "filter (<bif:st_intersects>(?geo," +
                    "bif:st_geomfromtext(\"BOX(" +
                    boundingBox.l1().lng() + " " + boundingBox.l1().lat() +"," +
                    boundingBox.l2().lng() + " "+ boundingBox.l2().lat()
                    + ")\" ))).\n";
        }
        return new StringBuilder().append(PREFIXES_str + "\n")
                                  .append(queryStr)
                                  .append(spatialFilter)
                                  .append("}").toString();
    }

    public static String temporalFilterOfferings(OfferingQuery query){
        logger.info("Finding matching offering with temporal criteria {}",query);
        String queryStr = "SELECT distinct ?id \n" +
                "FROM <" + OFFERING_GRAPH + "> \n" +
                "WHERE{\n" +
                "      ?offering a bigiot-core:Offering;\n" +
                "           bigiot-core:offeringId ?id;\n"+
                "           schema:validFrom ?validFrom;\n"+
                "           schema:validThrough ?validThrough.\n";

        String temporalFilter = "";

        Long from = (Long)query.temporalExtent().get().from().get();
        Long to = (Long) query.temporalExtent().get().to().get();
        if(from>0) {
            if(to>0)
                temporalFilter += "FILTER((?validFrom <= \"" + to + "\"^^xsd:long && ?validThrough >= \"" + from + "\"^^xsd:long)" +
                                         " || (?validThrough=0 && ?validFrom!=0))\n";
            else
                temporalFilter += "FILTER((?validThrough >= \"" + from + "\"^^xsd:long)" +
                                        "|| (?validThrough=0))\n";
        }else{
            if(to>0)
                temporalFilter += "FILTER((?validThrough <= \"" + to + "\"^^xsd:long && ?validThrough!=0) " +
                                            "|| (?validFrom <= \"" + to + "\"^^xsd:long && ?validFrom!=0))\n";
            else {
                long current = System.currentTimeMillis();
                temporalFilter += "FILTER((?validThrough = 0) || (?validThrough >= \"" + current + "\"^^xsd:long))\n";
            }
        }

        logger.debug("temporal filter {}",temporalFilter);
        return new StringBuilder().append(PREFIXES_str + "\n ")
                .append(queryStr)
                .append(temporalFilter)
                .append("}").toString();
    }

    public static String dataTypeFilterOffering(OfferingQuery query) {
        logger.info("Finding matching offering {}",query);
        String queryStr = "SELECT distinct ?id \n" +
                "FROM <" + OFFERING_GRAPH + "> \n" +
                "WHERE{\n" +
                "      ?offering a bigiot-core:Offering;\n" +
                "           bigiot-core:offeringId ?id.\n";

        if (!query.outputs().isEmpty()) {
            logger.info("build output data query {}",query.outputs());
            Collection<DataField> col = asJavaCollection(query.outputs());
            Iterator<DataField> iter = col.iterator();
            int i = 0;
            while(iter.hasNext()){
                DataField dataField = iter.next();
                String dataType = PREFIXES.getPrefixModel().expandPrefix(dataField.rdfAnnotation().uri());
                i++;
                queryStr += " ?offering bigiot-core:hasOutput  ?outputData" + i +".\n"+
                        " ?outputData" + i + "  bigiot-core:rdfAnnotation <" + dataType + ">.\n";
            }
        }
        if (!query.inputs().isEmpty()) {
            logger.info("build input data query {}",query.inputs());
            Collection<DataField> col = asJavaCollection(query.inputs());
            Iterator<DataField> iter = col.iterator();
            int i = 0;
            while(iter.hasNext()){
                DataField dataField = iter.next();
                String dataType = PREFIXES.getPrefixModel().expandPrefix(dataField.rdfAnnotation().uri());
                i++;
                queryStr += " ?offering bigiot-core:hasInput  ?inputData" + i +".\n"+
                        " ?inputData" + i + "  bigiot-core:rdfAnnotation <" + dataType + ">.\n";
            }
        }
        queryStr = PREFIXES_str +
                "\n" + queryStr + "}";
        return queryStr;
    }


    public static String findMatchingOfferings(OfferingQuery query) {
        logger.info("Finding matching offering {}",query);
        String queryStr = "SELECT distinct ?id \n" +
                "FROM <" + OFFERING_GRAPH + "> \n" +
                "FROM <" + ONTOLOGY_GRAPH + "> \n" +
                "WHERE{\n" +
                "      ?offering a bigiot-core:Offering;\n" +
                "           bigiot-core:offeringId ?id;\n" +
                "           bigiot-core:offeringExpirationTime ?expirationTime;\n" +
                "           bigiot-core:isActivated \"true\"^^<" + XSD.xboolean.getURI() + ">.\n";

        if (!query.outputs().isEmpty()) {
            logger.info("build output data query {}",query.outputs());
            Collection<DataField> col = asJavaCollection(query.outputs());
            Iterator<DataField> iter = col.iterator();
            int i = 0;
            while(iter.hasNext()){
                DataField dataField = iter.next();
                String dataType = PREFIXES.getPrefixModel().expandPrefix(dataField.rdfAnnotation().uri());
                i++;
                queryStr += " ?offering bigiot-core:hasOutput  ?outputData" + i +".\n"+
                        " ?outputData" + i + "  bigiot-core:rdfAnnotation <" + dataType + ">.\n";
                if(!(dataField.value() instanceof UndefinedType)){
                    String valueType = SCHEMA.TEXT.getURI();
                     if(dataField.value() instanceof TextType)
                         valueType = SCHEMA.TEXT.getURI();
                    else if(dataField.value() instanceof NumberType)
                         valueType = SCHEMA.NUMBER.getURI();
                    else if(dataField.value() instanceof IntegerType )
                         valueType = SCHEMA.INTEGER.getURI();
                    else if(dataField.value() instanceof DateTimeType)
                         valueType = SCHEMA.DATETIME.getURI();
                    else if(dataField.value() instanceof BooleanType)
                         valueType = SCHEMA.BOOLEAN.getURI();
                    else if(dataField.value() instanceof ObjectType) {
                         valueType = SCHEMA.OBJECT.getURI();
                    }
                    queryStr += " ?outputData" + i + " bigiot-core:value ?outputValue" + i + ".\n" +
                                " ?outputValue" + i + " bigiot-core:valueType <" + valueType + ">.\n";
                }
            }
        }
        if (!query.inputs().isEmpty()) {
            logger.info("build input data query {}",query.inputs());
            Collection<DataField> col = asJavaCollection(query.inputs());
            Iterator<DataField> iter = col.iterator();
            int i = 0;
            while(iter.hasNext()){
                DataField dataField = iter.next();
                String dataType = PREFIXES.getPrefixModel().expandPrefix(dataField.rdfAnnotation().uri());
                i++;
                queryStr += " ?offering bigiot-core:hasInput  ?inputData" + i +".\n"+
                        " ?inputData" + i + "  bigiot-core:rdfAnnotation <" + dataType + ">.\n";
                if(!(dataField.value() instanceof UndefinedType)){
                    String valueType = SCHEMA.TEXT.getURI();
                    if(dataField.value() instanceof TextType)
                        valueType = SCHEMA.TEXT.getURI();
                    else if(dataField.value() instanceof NumberType)
                        valueType = SCHEMA.NUMBER.getURI();
                    else if(dataField.value() instanceof IntegerType )
                        valueType = SCHEMA.INTEGER.getURI();
                    else if(dataField.value() instanceof DateTimeType)
                        valueType = SCHEMA.DATETIME.getURI();
                    else if(dataField.value() instanceof BooleanType)
                        valueType = SCHEMA.BOOLEAN.getURI();
                    else if(dataField.value() instanceof ObjectType) {
                        valueType = SCHEMA.OBJECT.getURI();
                    }
                    queryStr += " ?inputData" + i + " bigiot-core:value ?inputValue" + i + ".\n" +
                            " ?inputValue" + i + " bigiot-core:valueType <" + valueType + ">.\n";
                }
            }
        }

        logger.debug("check spatial extent:{}",query.spatialExtent());
        if((query.spatialExtent().isDefined()) && (query.spatialExtent().get().city().length()>0)){
            if(!query.spatialExtent().get().boundary().isDefined())
                queryStr += " ?offering schema:spatialCoverage ?area.\n" +
                        "  ?area rdfs:label \"" + query.spatialExtent().get().city() + "\"^^xsd:string.\n";
        }

        if(query.price().isDefined()){
            Option<price.Price> price = query.price();
            String defaultCurrency = "EUR";
            double amount = 0;
            if(price.get().money().isDefined()){
                logger.debug("price info {}",price);
                defaultCurrency = price.get().money().get().currency().value();
                amount = price.get().money().get().amount().doubleValue();
            }

            queryStr += "?offering schema:priceSpecification ?priceSpec.\n" +
                    "    ?priceSpec bigiot-core:pricingModel ?pModel.\n" +
                     "    VALUES ?pModel{\n" +
                    "       <" + BIGIOT.getPriceModel(price.get().pricingModel().toString()) + "> \n" +
                    "       bigiot-core:free_price\n" +
                    "     }\n" +
                    "           ?priceSpec  schema:priceCurrency \"" + defaultCurrency + "\"^^xsd:string;\n" +
                    "                       schema:price ?money.\n"+
                    "           FILTER(?money <=" + amount + ").\n" ;
            logger.debug("price query {}",price);
        }

        if(query.license().isDefined()){
            Option<license.License> license = query.license();
            queryStr+= "?offering schema:license ?licenseIndiv.\n" +
                    "   ?licenseIndiv bigiot-core:licenseType <" + BIGIOT.getLicenseType(license.get().value()) + ">. \n";
        }

        if(query.rdfAnnotation().isDefined()) {
            Option<semantics.RdfAnnotation> rdfAnnotation = query.rdfAnnotation();
            queryStr +=  " ?offering schema:category <" + PREFIXES.getPrefixModel().expandPrefix(rdfAnnotation.get().uri()) +">.\n" ;
        }

        //check expirationTime
        Date currentDate = new Date();
        queryStr+= "FILTER(?expirationTime >= \"" + currentDate.getTime() + "\"^^xsd:long).\n";
        queryStr = PREFIXES_str +
                "\n" + queryStr + "}";
        return queryStr;
    }

    public static String OfferingActivated(OfferingActivated event) {
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?off bigiot-core:isActivated ?status;" +
                "        bigiot-core:offeringExpirationTime ?expirationTime.} \n" +
                "INSERT{?off bigiot-core:isActivated \"true\"^^xsd:boolean;" +
                "       bigiot-core:offeringExpirationTime \"" + event.expirationTime()  + "\"^^<" + XSD.xlong.getURI() + ">.\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?off bigiot-core:isActivated ?status;\n" +
                "           bigiot-core:offeringExpirationTime ?expirationTime;\n" +
                "           bigiot-core:offeringId \"" + event.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
    }

    public static String OfferingDeactivated(OfferingDeactivated event) {
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?off bigiot-core:isActivated ?status.} \n" +
                "INSERT{?off bigiot-core:isActivated \"false\"^^xsd:boolean.} \n" +
                "WHERE{\n" +
                "      ?off bigiot-core:isActivated ?status;\n" +
                "           bigiot-core:offeringId \"" + event.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
    }

    public static String subscriptionDeleted(SubscriptionDeleted event) {
        String query = QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "      ?subscription a bigiot-core:Subscription;\n" +
                "           bigiot-core:subscriptionId ?id;\n" +
                "           bigiot-core:subscribedQuery ?offeringQuery;\n" +
                "           bigiot-core:subscribeTo ?offering." +
                "} \n" +
                "WHERE{\n" +
                "      ?subscription a bigiot-core:Subscription;\n" +
                "           bigiot-core:subscriptionId ?id;\n" +
                "           bigiot-core:subscribedQuery ?offeringQuery;\n" +
                "           bigiot-core:subscribeTo ?offering." +
                "      ?offering bigiot-core:offeringId \"" + event.subscribableId() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      ?offeringQuery bigiot-core:queryId \"" + event.subscriberId() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
        return query;
    }

    public static String offeringQueryNameChanged(OfferingQueryNameChanged ev) {
        logger.info("update offering query {} with new name {}", ev.id(), ev.name());
        String queryStr = QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?offQuery schema:name ?name.} \n" +
                "INSERT{?offQuery schema:name \"" + ev.name() + "\".} \n" +
                "WHERE{\n" +
                "      ?offQuery a bigiot-core:OfferingQuery;\n " +
                "           schema:name ?name;\n" +
                "           bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "}\n";
        return queryStr;
    }

    public static String offeringQueryCategoryChanged(OfferingQueryCategoryChanged ev) {
        logger.info("update offering query {} with new category {}", ev.id(), ev.rdfUri().get());
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "      ?offQuery schema:category ?category.\n" +
                "} \n" +
                "INSERT{" +
                "      ?offQuery schema:category <" + ev.rdfUri().get() + ">.} \n" +
                "WHERE{\n" +
                "      ?offQuery bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                "           schema:category ?category.\n" +
                "}\n";
    }

    public static String offeringQueryDeleted(OfferingQueryDeleted event) {
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE {\n" +
                "      ?offeringQuery bigiot-core:queryId \"" + event.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                "           schema:priceSpecification ?priceSpec;\n" +
                "           schema:license ?licenseIndiv;\n" +
                "           ?p ?o.\n" +
                "      ?offeringQuery schema:spatialCoverage ?area.\n" +
                "      ?area ?areaP ?areaO.\n" +
                "      ?priceSpec ?priceP ?priceO.\n" +
                "      ?licenseIndiv ?licenseIndivP ?licenseIndivO.\n" +
                "      ?offeringQuery bigiot-core:endpoint ?endpoint.\n" +
                "      ?endpoint ?endpointP ?endpointO.\n" +
                "}\n" +
                "WHERE{\n" +
                "      ?offeringQuery ?p ?o;\n" +
                "           bigiot-core:queryId \"" + event.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL{\n" +
                "         ?offeringQuery schema:license ?licenseIndiv.\n" +
                "         ?licenseIndiv ?licenseIndivP ?licenseIndivO.\n" +
                "      }\n" +
                "      OPTIONAL{\n" +
                "         ?offeringQuery schema:priceSpecification ?priceSpec.\n" +
                "         ?priceSpec ?priceP ?priceO.\n" +
                "      }\n" +
                "      OPTIONAL{\n" +
                "           ?offeringQuery schema:spatialCoverage ?area.\n" +
                "           ?area ?areaP ?areaO.\n" +
                "      }\n" +
                "      OPTIONAL {\n" +
                "           ?offeringQuery bigiot-core:endpoint ?endpoint.\n" +
                "           ?endpoint ?endpointP ?endpointO.\n" +
                "      }\n" +
                "}\n";
    }

    public static String offeringQueryPriceChanged(OfferingQueryPriceChanged ev) {
        logger.info("update offering query {} with new price {}", ev.id(), ev.price().get().pricingModel().toString());
        if(!ev.price().isDefined()) {
            logger.error("New Offering Query Price is not defined");
            return null;
        }
        if(ev.price().get().pricingModel().equals(QueryFactory.FREE)) {
            String priceSpec = PREFIXES.BIGIOT_BASE_NS + ev.id() + "Price";
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{" +
                    "    ?offQuery schema:priceSpecification ?priceSpec.\n" +
                    "    ?priceSpec bigiot-core:pricingModel ?priceModel. " +
                    "    ?priceSpec ?p ?o.\n" +
                    "} \n" +
                    "INSERT{" +
                    "    ?offQuery schema:priceSpecification <" + priceSpec +">.\n" +
                    "    <" + priceSpec + "> bigiot-core:pricingModel <" + BIGIOT.getPriceModel(ev.price().get().pricingModel().toString()) + ">;" +
                    "                  a bigiot-core:Price;\n" +
                    "                  schema:priceCurrency 'EUR';\n" +
                    "                  schema:price '0.0'^^http://www.w3.org/2001/XMLSchema#double.\n" +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?offQuery a bigiot-core:OfferingQuery;\n" +
                    "           bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                    "      OPTIONAL{\n " +
                    "           ?offQuery schema:priceSpecification ?priceSpec.\n" +
                    "           ?priceSpec bigiot-core:pricingModel ?priceModel.\n" +
                    "           ?priceSpec ?p ?o.\n" +
                    "      }\n " +
                    "}\n";
        }
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{?priceSpec bigiot-core:pricingModel ?priceModel;\n " +
                "                 schema:priceCurrency ?currency;\n" +
                "                 schema:price ?priceAmount.\n" +
                "} \n" +
                "INSERT{?priceSpec bigiot-core:pricingModel <" + BIGIOT.getPriceModel(ev.price().get().pricingModel().toString()) + ">;\n" +
                "                  schema:priceCurrency \"" + ev.price().get().money().get().currency().value() + "\"^^xsd:string;\n" +
                "                  schema:price " + ev.price().get().money().get().amount() + ".\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?offQuery a bigiot-core:OfferingQuery;\n" +
                "           bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL{\n " +
                "           ?offQuery schema:priceSpecification ?priceSpec.\n" +
                "           ?priceSpec bigiot-core:pricingModel ?priceModel;\n" +
                "                 schema:priceCurrency ?currency;\n" +
                "                 schema:price ?priceAmount.\n" +
                "      }\n " +
                "}\n";
    }

    public static String offeringQueryLicenseChanged(OfferingQueryLicenseChanged ev) {
        logger.info("update offering query {} with new license {}", ev.id(), ev);
        if(ev.license().isDefined()) {
            String licenseURI = PREFIXES.BIGIOT_BASE_NS + ev.id().value() + "License";
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{" +
                    "   ?offQuery schema:license ?license.\n" +
                    "   ?license bigiot-core:licenseType ?licenseType. " +
                    "} \n" +
                    "INSERT{" +
                    "   ?offQuery schema:license <" + licenseURI + ">.\n" +
                    "   <" + licenseURI + "> bigiot-core:licenseType <" + BIGIOT.getLicenseType(ev.license().get().toString()) + ">." +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?offQuery a bigiot-core:OfferingQuery;\n " +
                    "           bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                    "      OPTIONAL{\n " +
                    "           ?offQuery schema:license ?license.\n" +
                    "           ?license bigiot-core:licenseType ?licenseType. " +
                    "      }\n " +
                    "}\n";
        }else
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{ \n" +
                    "   ?offQuery schema:license ?license.\n" +
                    "   ?license bigiot-core:licenseType ?licenseType." +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?offQuery a bigiot-core:OfferingQuery;\n " +
                    "           bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                    "      OPTIONAL{\n " +
                    "           ?offQuery schema:license ?license.\n" +
                    "           ?license bigiot-core:licenseType ?licenseType. " +
                    "      }\n " +
                    "}\n";
    }

    public static String offeringQuerySpatialExtentChanged(OfferingQuerySpatialExtentChanged ev) {
        logger.info("update offering query {} with new spatial extent {}", ev.id(), ev.spatialExtent().get().city());
        if(ev.spatialExtent().isDefined()) {
            if(ev.spatialExtent().get().boundary().isDefined())
                return PREFIXES_str +
                        "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                        "DELETE{" +
                        "      ?offQuery schema:spatialCoverage ?area.\n" +
                        "      ?area ?p ?o.\n" +
                        "} \n" +
                        "INSERT{" +
                        "      ?offQuery schema:spatialCoverage ?area.\n" +
                        "      ?area rdfs:label \"" + ev.spatialExtent().get().city() + "\"^^xsd:string; \n" +
                        "            bigiot-core:lowerBoundLatitude \"" + ev.spatialExtent().get().boundary().get().l1().lat() + "\"^^xsd:double; \n" +
                        "            bigiot-core:lowerBoundLongitude \"" + ev.spatialExtent().get().boundary().get().l1().lng() + "\"^^xsd:double; \n" +
                        "            bigiot-core:upperBoundLatitude \"" + ev.spatialExtent().get().boundary().get().l2().lat() + "\"^^xsd:double; \n" +
                        "            bigiot-core:upperBoundLongitude \"" + ev.spatialExtent().get().boundary().get().l2().lng() + "\"^^xsd:double. \n" +
                        "} \n" +
                        "WHERE{\n" +
                        "      ?offQuery bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                        "      OPTIONAL{\n" +
                        "           ?offQuery schema:spatialCoverage ?area.\n" +
                        "           OPTIONAL{\n" +
                        "               ?area ?p ?o.\n" +
                        "           }\n" +
                        "      }\n" +
                        "}\n";
            else
                return PREFIXES_str +
                        "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                        "DELETE{" +
                        "      ?offQuery schema:spatialCoverage ?area.\n" +
                        "      ?area ?p ?o.\n" +
                        "} \n" +
                        "INSERT{" +
                        "      ?offQuery schema:spatialCoverage ?area.\n" +
                        "      ?area rdfs:label \"" + ev.spatialExtent().get().city() + "\"^^xsd:string. \n" +
                        "} \n" +
                        "WHERE{\n" +
                        "      ?offQuery bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                        "      OPTIONAL{\n" +
                        "           ?offQuery schema:spatialCoverage ?area.\n" +
                        "           OPTIONAL{\n" +
                        "               ?area ?p ?o.\n" +
                        "           }\n" +
                        "      }\n" +
                        "}\n";
        }else
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{" +
                    "      ?offQuery schema:spatialCoverage ?area.\n" +
                    "      ?area ?p ?o.\n" +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?offQuery bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                    "      OPTIONAL{\n" +
                    "           ?offQuery schema:spatialCoverage ?area.\n" +
                    "           OPTIONAL{\n" +
                    "               ?area ?p ?o.\n" +
                    "           }\n" +
                    "      }\n" +
                    "}\n";
    }

    public static String offeringQueryTemporalExtentChanged(OfferingQueryTemporalExtentChanged ev) {
        String deletePhrase =  "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                                "DELETE{"+
                                "      ?offQuery schema:validFrom ?fromTime.\n" +
                                "      ?offQuery schema:validThrough ?toTime.\n" +
                                "} \n" ;

        String insertPhrase = "INSERT{";
        String wherePhrase = "WHERE{\n" +
                                "      ?offQuery bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                                "      OPTIONAL{\n" +
                                "           ?offQuery schema:validFrom ?fromTime.\n" +
                                "      } \n" +
                                "      OPTIONAL{\n" +
                                "           ?offQuery schema:validThrough ?toTime.\n" +
                                "      }\n" +
                                "} \n";

        if(ev.temporalExtent().isDefined()) {
            if(ev.temporalExtent().get().from().isDefined()){
                insertPhrase+= " ?offQuery schema:validFrom \"" + ev.temporalExtent().get().from().get() + "\"^^<" + XSD.xlong.getURI() + ">.\n";
            }else
                insertPhrase+= " ?offQuery schema:validFrom \"" + 0 + "\"^^<" + XSD.xlong.getURI() + ">.\n";
            if(ev.temporalExtent().get().to().isDefined()){
                insertPhrase+= " ?offQuery schema:validThrough \"" + ev.temporalExtent().get().to().get() + "\"^^<" + XSD.xlong.getURI() + ">.\n";
            }else
                insertPhrase+= " ?offQuery schema:validThrough \"" + 0 + "\"^^<" + XSD.xlong.getURI() + ">.\n";
            return PREFIXES_str +
                    deletePhrase +
                    insertPhrase +
                    "} \n" +
                    wherePhrase;
        }else
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{" +
                    "      ?offQuery schema:validFrom ?fromTime.\n" +
                    "      ?offQuery schema:validThrough ?toTime.\n" +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?offQuery bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                    "      OPTIONAL{\n" +
                    "           ?offQuery schema:validFrom ?fromTime.\n" +
                    "      } \n" +
                    "      OPTIONAL{\n" +
                    "           ?offQuery schema:validThrough ?toTime.\n" +
                    "      } \n" +
                    "}\n";
    }



    public static String offeringQueryInputDataFieldsDeleted(OfferingQueryInputsChanged ev) {
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "     ?offQuery bigiot-core:hasInput  ?inputData.\n"+
                "     ?inputData  ?p ?o.\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?offQuery a bigiot-core:OfferingQuery;\n" +
                "           bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL{\n" +
                "           ?offQuery bigiot-core:hasInput  ?inputData.\n"+
                "           ?inputData  ?p ?o.\n" +
                "      } \n" +
                "} \n";
    }

    public static String offeringQueryOutputDataFieldsDeleted(OfferingQueryOutputsChanged ev) {
        return  QueryFactory.getPREFIXES() +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "    ?offQuery bigiot-core:hasOutput  ?outputData.\n"+
                "    ?outputData  ?p ?o.\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?offQuery a bigiot-core:OfferingQuery;\n" +
                "           bigiot-core:queryId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                "      OPTIONAL{\n" +
                "           ?offQuery bigiot-core:hasOutput  ?outputData.\n"+
                "           ?outputData  ?p ?o.\n" +
                "      } \n" +
                "} \n";
    }

    public static String offeringSpatialExtentChanged(OfferingSpatialExtentChanged ev) {
        logger.info("update offering {} with new spatial extent {}", ev.id(), ev.spatialExtent());

        if(ev.spatialExtent().isDefined()) {
            if(ev.spatialExtent().get().boundary().isDefined())
                return PREFIXES_str +
                        "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                        "DELETE{" +
                        "      ?off schema:spatialCoverage ?area.\n" +
                        "      ?area ?p ?o.\n" +
                        "} \n" +
                        "INSERT{" +
                        "      ?off schema:spatialCoverage ?area.\n" +
                        "      ?area rdfs:label \"" + ev.spatialExtent().get().city() + "\"^^xsd:string; \n" +
                        "            bigiot-core:lowerBoundLatitude \"" + ev.spatialExtent().get().boundary().get().l1().lat() + "\"^^xsd:double; \n" +
                        "            bigiot-core:lowerBoundLongitude \"" + ev.spatialExtent().get().boundary().get().l1().lng() + "\"^^xsd:double; \n" +
                        "            bigiot-core:upperBoundLatitude \"" + ev.spatialExtent().get().boundary().get().l2().lat() + "\"^^xsd:double; \n" +
                        "            bigiot-core:upperBoundLongitude \"" + ev.spatialExtent().get().boundary().get().l2().lng() + "\"^^xsd:double; \n" +
                        "            <" + BIGIOT.GEOMETRY.getURI() + "> \"BOX("+ ev.spatialExtent().get().boundary().get().l1().lng()
                                                                            +" "+ ev.spatialExtent().get().boundary().get().l1().lat() + ","
                                                                            + ev.spatialExtent().get().boundary().get().l2().lng()
                                                                            +" "+ ev.spatialExtent().get().boundary().get().l2().lat()+ ")" + "\"^^<http://www.openlinksw.com/schemas/virtrdf#Geometry>. \n" +
                        "} \n" +
                        "WHERE{\n" +
                        "      ?off bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                        "      OPTIONAL{\n" +
                        "           ?off schema:spatialCoverage ?area.\n" +
                        "           OPTIONAL{\n" +
                        "               ?area ?p ?o.\n" +
                        "           }\n" +
                        "      }\n" +
                        "}\n";
            else
                return PREFIXES_str +
                        "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                        "DELETE{" +
                        "      ?off schema:spatialCoverage ?area.\n" +
                        "      ?area ?p ?o.\n" +
                        "} \n" +
                        "INSERT{" +
                        "      ?off schema:spatialCoverage ?area.\n" +
                        "      ?area rdfs:label \"" + ev.spatialExtent().get().city() + "\"^^xsd:string. \n" +
                        "} \n" +
                        "WHERE{\n" +
                        "      ?off bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                        "      OPTIONAL{\n" +
                        "           ?off schema:spatialCoverage ?area.\n" +
                        "           OPTIONAL{\n" +
                        "               ?area ?p ?o.\n" +
                        "           }\n" +
                        "      }\n" +
                        "}\n";
        }else
            return PREFIXES_str +
                    "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                    "DELETE{" +
                    "      ?off schema:spatialCoverage ?area.\n" +
                    "      ?area ?p ?o.\n" +
                    "} \n" +
                    "WHERE{\n" +
                    "      ?off bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">.\n" +
                    "      OPTIONAL{\n" +
                    "           ?off schema:spatialCoverage ?area.\n" +
                    "           OPTIONAL{\n" +
                    "               ?area ?p ?o.\n" +
                    "           }\n" +
                    "      }\n" +
                    "}\n";

    }

    public static String offeringEndpointsDeleted(offering.OfferingEndpointsChanged ev) {
        String query = PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "           ?offering bigiot-core:endpoint ?endpoint.\n" +
                "           ?endpoint bigiot-core:accessInterfaceType ?accessInterfaceIndiv;\n" +
                "                     bigiot-core:endpointType ?endpointTypeIndiv;\n" +
                "                     schema:url ?endpointUri.\n" +
                "           ?accessInterfaceIndiv rdfs:label ?accessInterface.\n" +
                "           ?endpointTypeIndiv rdfs:label ?endpointType.\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?offering bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                "           bigiot-core:endpoint ?endpoint.\n" +
                "      ?endpoint bigiot-core:accessInterfaceType ?accessInterfaceIndiv;\n" +
                "           bigiot-core:endpointType ?endpointTypeIndiv;\n" +
                "           schema:url ?endpointUri.\n" +
                "      ?accessInterfaceIndiv rdfs:label ?accessInterface.\n" +
                "      ?endpointTypeIndiv rdfs:label ?endpointType.\n" +
                "}\n";
        return query;
    }

    public static String offeringCategoryChanged(offering.OfferingCategoryChanged ev) {
        logger.info("update offering {} with new category {}", ev.id(), ev.rdfUri());
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "      ?off schema:category ?category.\n" +
                "} \n" +
                "INSERT{" +
                "      ?off schema:category <" + ev.rdfUri() + ">.} \n" +
                "WHERE{\n" +
                "      ?off bigiot-core:offeringId \"" + ev.id().value() + "\"^^<" + XSD.xstring.getURI() + ">;\n" +
                "           schema:category ?category.\n" +
                "}\n";
    }

    public static String offeringCategoryNameChanged(OfferingCategoryNameChanged ev) {
        logger.info("update offering category {} with new name {}", ev.id(), ev.name());
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "      <" + ev.uri() + "> rdfs:label ?name.\n" +
                "} \n" +
                "INSERT{" +
                "      <" + ev.uri() + "> rdfs:label \"" + ev.name() + "\"^^<" + XSD.xstring.getURI() + ">.}\n" +
                "WHERE{\n" +
                "      <" + ev.uri() + "> rdfs:label ?name.\n" +
                "}\n";
    }

    public static String offeringCategoryParentChanged(OfferingCategoryParentChanged ev) {
        logger.info("update offering category {} with new parent {}", ev.id(), ev.parent());
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOfferingGraph() + "> \n" +
                "DELETE{" +
                "      ?parent skos:narrower <" + ev.uri() + ">.\n" +
                "      <" + ev.uri() + "> <" + BIGIOT.expectedAnnotation.getURI() + "> ?expectedDataType.\n" +
                "} \n" +
                "WHERE{\n" +
                "      ?parent skos:narrower <" + ev.uri() + ">.\n" +
                "      <" + ev.uri() + "> <" + BIGIOT.expectedAnnotation.getURI() + "> ?expectedDataType.\n" +
                "}\n";
    }

    public static String offeringCategoryDeprecated(OfferingCategoryDeprecated ev) {
        logger.info("Deprecate offering category {}");
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOntologyGraph() + "> \n" +
                "DELETE{" +
                "      <" + ev.uri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "} \n" +
                "INSERT{" +
                "      <" + ev.uri() + "> <" + BIGIOT.isDeprecated.getURI() + "> \"true\"^^<" + XSD.xboolean.getURI() + ">.}\n" +
                "WHERE{\n" +
                "      <" + ev.uri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "}\n";
    }

    public static String offeringCategoryUndeprecated(OfferingCategoryUndeprecated ev) {
        logger.info("Undeprecate offering category {}");
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOntologyGraph() + "> \n" +
                "DELETE{" +
                "      <" + ev.uri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "} \n" +
                "INSERT{" +
                "      <" + ev.uri() + "> <" + BIGIOT.isDeprecated.getURI() + "> \"false\"^^<" + XSD.xboolean.getURI() + ">.}\n" +
                "WHERE{\n" +
                "      <" + ev.uri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "}\n";
    }

    public static String inputTypeDeprecatedForOfferingCategory(InputTypeDeprecatedForOfferingCategory ev) {
        logger.info("Deprecate offering category {}");
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOntologyGraph() + "> \n" +
                "DELETE{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "} \n" +
                "INSERT{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> \"true\"^^<" + XSD.xboolean.getURI() + ">.}\n" +
                "WHERE{\n" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "}\n";
    }

    public static String inputTypeUndeprecatedForOfferingCategory(InputTypeUndeprecatedForOfferingCategory ev) {
        logger.info("Deprecate offering category {}");
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOntologyGraph() + "> \n" +
                "DELETE{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "} \n" +
                "INSERT{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> \"false\"^^<" + XSD.xboolean.getURI() + ">.}\n" +
                "WHERE{\n" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "}\n";
    }

    public static String outputTypeDeprecatedForOfferingCategory(OutputTypeDeprecatedForOfferingCategory ev) {
        logger.info("Deprecate offering category {}");
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOntologyGraph() + "> \n" +
                "DELETE{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "} \n" +
                "INSERT{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> \"true\"^^<" + XSD.xboolean.getURI() + ">.}\n" +
                "WHERE{\n" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "}\n";
    }

    public static String outputTypeUndeprecatedForOfferingCategory(OutputTypeUndeprecatedForOfferingCategory ev) {
        logger.info("Deprecate offering category {}");
        return PREFIXES_str +
                "WITH <" + QueryFactory.getOntologyGraph() + "> \n" +
                "DELETE{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "} \n" +
                "INSERT{" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> \"false\"^^<" + XSD.xboolean.getURI() + ">.}\n" +
                "WHERE{\n" +
                "      <" + ev.typeUri() + "> <" + BIGIOT.isDeprecated.getURI() + "> ?value.\n" +
                "}\n";
    }

    public static String createGraph(String graphName){
        return "CREATE GRAPH <" + graphName + ">";
    }

    public static String deleteGraph(String graphName){
        return "DROP SILENT GRAPH <" + graphName + ">";
    }

}
