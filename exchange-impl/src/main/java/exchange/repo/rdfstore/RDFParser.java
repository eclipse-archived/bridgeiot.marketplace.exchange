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

import exchange.api.access.Endpoint;
import exchange.api.consumer.ConsumerCreated;
import exchange.api.offering.*;
import exchange.api.offeringquery.OfferingQueryCreated;
import exchange.api.offeringquery.OfferingQueryInputsChanged;
import exchange.api.offeringquery.OfferingQueryOutputsChanged;
import exchange.api.organization.OrganizationCreated;
import exchange.api.provider.ProviderCreated;
import exchange.api.semantics.*;
import exchange.api.subscription.SubscriptionCreated;
import exchange.model.vocabs.BIGIOT;
import exchange.model.vocabs.PREFIXES;
import exchange.model.vocabs.SCHEMA;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.Iterator;

import java.util.List;

import static exchange.api.extent.BoundingBox;

public class RDFParser {

    String SCHEMA_PREFIX = PREFIXES.SCHEMA_NS;
    String BIGIOT_PREFIX = PREFIXES.BIGIOT_CORE_NS;
    String BASE_PREFIX = PREFIXES.BIGIOT_BASE_NS;

    final static Logger logger = LoggerFactory.getLogger(RDFParser.class);

    public Model offeringCreated(OfferingCreated event) {

        try {
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            model.setNsPrefixes(PREFIXES.getPrefixSet());
            Model allOfferingCategoryModel = RDFUtils.getAllOfferingCategoryModel();
            boolean isUpdateCategoryTree = false;

            logger.info("Offering is creating {}",event);
            //create Offering
            OntClass offeringClass = model.createClass(BIGIOT_PREFIX + "Offering");
            Individual offeringIdv = model.createIndividual(BASE_PREFIX + event.id().value(), offeringClass);
            offeringIdv.addLiteral(BIGIOT.offeringId, event.id().value());

            logger.info("link offering to category");
            //link offering to category
            offeringIdv.addProperty(SCHEMA.category, model.createResource(model.expandPrefix(event.rdfUri())));
            if(!allOfferingCategoryModel.containsResource(ResourceFactory.createResource(model.expandPrefix(event.rdfUri())))){
                logger.debug("Offering category {} not found",model.expandPrefix(event.rdfUri()));
                return null;
            }

            logger.info("link offering to status");
            offeringIdv.addLiteral(BIGIOT.isActivated, event.activation().status());

            logger.info("link offering to expirationTime");
            offeringIdv.addLiteral(BIGIOT.expirationTime, model.createTypedLiteral(event.activation().expirationTime(), XSDDatatype.XSDlong));

            //add name to offering
            offeringIdv.addLiteral(SCHEMA.name, event.name());

            //link offering to provider
            OntClass providerClass = model.createClass(BIGIOT_PREFIX + "Provider");
            Individual providerIns = model.createIndividual(BASE_PREFIX + event.providerId().value(), providerClass);
            offeringIdv.addProperty(BIGIOT.isProvidedBy, providerIns);
            providerIns.addProperty(BIGIOT.offering, offeringIdv);

            logger.info("Parse endpoint");
            if (!event.endpoints().isEmpty() && event.endpoints().size() > 0) {
                //create Endpoint
                OntClass endpointClass = model.createClass(BIGIOT_PREFIX + "Endpoint");
                Iterator<Endpoint> iter = event.endpoints().iterator();
                while (iter.hasNext()) {
                    Endpoint endpoint = iter.next();
                    Individual endpointIns = model.createIndividual(offeringIdv.getURI() + "Endpoint", endpointClass);
                    //set url property for endpoint
                    endpointIns.addLiteral(SCHEMA.url, endpoint.uri());
                    //                System.out.println(endpoint.copy$default$3().toString());
                    endpointIns.addProperty(BIGIOT.accessInterfaceType,
                            model.createResource(BIGIOT.getAccessInterfaceType(endpoint.accessInterfaceType().toString())));
                    endpointIns.addProperty(BIGIOT.endpointType,
                            model.createResource(BIGIOT.getEndpointType(endpoint.endpointType().toString())));

                    //link offering to endpoint
                    offeringIdv.addProperty(BIGIOT.endpoint, endpointIns);
                }
            }

            logger.info("Parse license");
            //create License
            OntClass licenseClass = model.createClass(BIGIOT_PREFIX + "License");
            Individual openLicenseIns = model.createIndividual(offeringIdv.getURI() + "License", licenseClass);
            openLicenseIns.addProperty(BIGIOT.licenseType, model.createResource(BIGIOT.getLicenseType(event.license().toString())));

            //link license to offering
            offeringIdv.addProperty(SCHEMA.license, openLicenseIns);

            logger.info("Parse price");
            //create Price
            OntClass priceClass = model.createClass(BIGIOT_PREFIX + "Price");
            Individual priceIns = model.createIndividual(offeringIdv.getURI() + "Price", priceClass);
            if (event.price().money().isDefined()) {
                priceIns.addLiteral(SCHEMA.priceCurrency, event.price().money().get().currency().value());
                double amount = event.price().money().get().amount().doubleValue();
                priceIns.addLiteral(SCHEMA.price, model.createTypedLiteral(amount, XSDDatatype.XSDdouble));
            }
            //link accounting model to price
            priceIns.addProperty(model.createDatatypeProperty(BIGIOT_PREFIX + "pricingModel"), model.createResource(BIGIOT.getPriceModel(event.price().pricingModel().toString())));
            //link price to offering
            offeringIdv.addProperty(SCHEMA.priceSpecification, priceIns);

            logger.info("Parse spatial extent");
            //create region
            if (event.spatialExtent().isDefined()) {
                logger.info("Parse spatial extent {}",event.spatialExtent().isDefined());
                OntClass regionClass = model.createClass(SCHEMA_PREFIX + "Region");
                Individual regionIns = model.createIndividual(offeringIdv.getURI() + "Region", regionClass);
                if(event.spatialExtent().get().boundary().isDefined()) {
                    BoundingBox boundingBox = event.spatialExtent().get().boundary().get();
                    regionIns.addProperty(BIGIOT.GEOMETRY,model.createTypedLiteral("BOX("+boundingBox.l1().lng()+" "+ boundingBox.l1().lat() + ","
                            + boundingBox.l2().lng()+" "+ boundingBox.l2().lat()+ ")","http://www.openlinksw.com/schemas/virtrdf#Geometry"));
                    regionIns.addLiteral(BIGIOT.LOWER_BOUND_LATITUDE,model.createTypedLiteral(boundingBox.l1().lat(),XSDDatatype.XSDdouble));
                    regionIns.addLiteral(BIGIOT.LOWER_BOUND_LONGITUDE,model.createTypedLiteral(boundingBox.l1().lng(),XSDDatatype.XSDdouble));
                    regionIns.addLiteral(BIGIOT.UPPER_BOUND_LATITUDE,model.createTypedLiteral(boundingBox.l2().lat(),XSDDatatype.XSDdouble));
                    regionIns.addLiteral(BIGIOT.UPPER_BOUND_LONGITUDE,model.createTypedLiteral(boundingBox.l2().lng(),XSDDatatype.XSDdouble));
                }
                regionIns.addLabel(model.createTypedLiteral(event.spatialExtent().get().city()));
                //link region to offering
                offeringIdv.addProperty(SCHEMA.spatialCoverage, regionIns);
            }


            // TODO handle temporalExtent
            if (event.temporalExtent().isDefined()) {
                if (event.temporalExtent().get().from().isDefined())
                    offeringIdv.addLiteral(SCHEMA.validFrom, model.createTypedLiteral(event.temporalExtent().get().from().get(), XSDDatatype.XSDlong));
                else
                    offeringIdv.addLiteral(SCHEMA.validFrom, model.createTypedLiteral(0, XSDDatatype.XSDlong));
                if (event.temporalExtent().get().to().isDefined())
                    offeringIdv.addLiteral(SCHEMA.validThrough, model.createTypedLiteral(event.temporalExtent().get().to().get(), XSDDatatype.XSDlong));
                else
                    offeringIdv.addLiteral(SCHEMA.validThrough, model.createTypedLiteral(0, XSDDatatype.XSDlong));
            }
            else{
                offeringIdv.addLiteral(SCHEMA.validFrom, model.createTypedLiteral(0, XSDDatatype.XSDlong));
                offeringIdv.addLiteral(SCHEMA.validThrough, model.createTypedLiteral(0, XSDDatatype.XSDlong));
            }

            logger.info("Parse input data");
            if (!event.inputs().isEmpty()) {
                OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
                scala.collection.Iterator<DataField> iter = event.inputs().iterator();
                while (iter.hasNext()) {
                    DataField dataField = iter.next();
                    Individual inputData = DataClass.createIndividual();
                    inputData.addLiteral(SCHEMA.name, dataField.name());
                    inputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));
                    Individual value = model.createIndividual(BIGIOT.DataValue);
                    if(dataField.value() instanceof TextType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                    else if(dataField.value() instanceof NumberType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                    else if(dataField.value() instanceof IntegerType )
                        value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                    else if(dataField.value() instanceof DateTimeType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                    else if(dataField.value() instanceof BooleanType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                    else if(dataField.value() instanceof ObjectType) {
                        value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                        inputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                    }

                    inputData.addProperty(BIGIOT.value,value);

                    Model dataModel = dataTypeAddedToOfferingCategory(event.rdfUri(),dataField.rdfAnnotation().uri(),allOfferingCategoryModel);
                    if(dataModel!=null) {
//                        model.add(dataModel);
                        RDFUtils.getRdfServer().executeUpdateQuery(dataModel,QueryFactory.getOntologyGraph());
                        isUpdateCategoryTree = true;
                    }

                    //link offering to input data
                    offeringIdv.addProperty(BIGIOT.hasInput, inputData);
                    offeringIdv = addFlattenMember(offeringIdv,BIGIOT.hasFlattenedInput,dataField);
                }
            }

            logger.info("Parse output data");
            //add Output Data
            if (!event.outputs().isEmpty()) {
                OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
                scala.collection.Iterator<DataField> iter = event.outputs().iterator();
                while (iter.hasNext()) {
                    DataField dataField = iter.next();
                    Individual outputData = DataClass.createIndividual();
                    outputData.addLiteral(SCHEMA.name, dataField.name());
                    outputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));

                    Individual value = model.createIndividual(BIGIOT.DataValue);
                    if(dataField.value() instanceof TextType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                    else if(dataField.value() instanceof NumberType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                    else if(dataField.value() instanceof IntegerType )
                        value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                    else if(dataField.value() instanceof DateTimeType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                    else if(dataField.value() instanceof BooleanType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                    else if(dataField.value() instanceof ObjectType) {
                        value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                        outputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                    }

                    outputData.addProperty(BIGIOT.value,value);

                    Model dataModel = dataTypeAddedToOfferingCategory(event.rdfUri(),dataField.rdfAnnotation().uri(),allOfferingCategoryModel);
                    if(dataModel!=null) {
                        RDFUtils.getRdfServer().executeUpdateQuery(dataModel, QueryFactory.getOntologyGraph());
                        isUpdateCategoryTree = true;
                    }
                    //link offering to input data
                    offeringIdv.addProperty(BIGIOT.hasOutput, outputData);
                    offeringIdv = addFlattenMember(offeringIdv,BIGIOT.hasFlattenedOutput,dataField);
                }
            }

            //add accessWhitelist organisation
            if (!event.accessWhiteList().isEmpty()) {
                scala.collection.Iterator<String> iter = event.accessWhiteList().iterator();
                while (iter.hasNext()) {
                    String orgId = iter.next();
                    offeringIdv.addProperty(BIGIOT.isAccessedBy, model.createResource(BASE_PREFIX + orgId));
                }
            }

            logger.info("infer offering category");
            //infer offering category
            List<Rule> rules = Rule.parseRules(BIGIOT.OFFERING_SUB_CATEGORY_RULES);
            rules.add(Rule.parseRule(BIGIOT.OFFERING_FREE_PRICE_RULES));
            model.add(RDFUtils.getCategoryOnlyModel());
            Reasoner engine = new GenericRuleReasoner(rules);
            InfModel inf = ModelFactory.createInfModel(engine, model);

            //trigger update category
            if(isUpdateCategoryTree){
                RDFUtils.updateAllOfferingCategoriesModel();
            }

            return inf;
        }catch (Exception e){
            logger.error("Can not create offering {} ",e.toString());
        }
        return null;
    }

    private Individual addFlattenMember(Individual offering, Property flattenProperty, DataField dataField){
        if(dataField.value() instanceof ObjectType) {
            offering.addProperty(flattenProperty, ResourceFactory.createResource(dataField.rdfAnnotation().uri()));
            ObjectType ob = (ObjectType) dataField.value();
            scala.collection.Iterator<DataField> iter = ob.members().iterator();
            while (iter.hasNext()) {
                DataField member = iter.next();
                offering = addFlattenMember(offering,flattenProperty,member);
            }
        }else
            offering.addProperty(flattenProperty,ResourceFactory.createResource(dataField.rdfAnnotation().uri()));
        return offering;
    }

    public Model providerCreated(ProviderCreated event) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        OntClass providerClass = model.createClass(BIGIOT_PREFIX + "Provider");
        Individual providerIns = model.createIndividual(BASE_PREFIX + event.id().value(),providerClass);
        //add provider Id
        providerIns.addLiteral(BIGIOT.providerId, event.id().value());
        providerIns.addLiteral(SCHEMA.name, event.name());
        providerIns.addProperty(SCHEMA.sourceOrganization, model.createResource(BASE_PREFIX + event.organizationId().value()));

        return model;
    }

    public Model organizationCreated(OrganizationCreated event) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        OntClass orgClass = model.createClass(SCHEMA_PREFIX + "Organization");
        Individual orgIns = model.createIndividual(BASE_PREFIX + event.id().value(), orgClass);


//        orgIns.addLiteral(model.createProperty(BIGIOT_PREFIX+"organizationId"), model.createLiteral(event.id()));
        model.add(orgIns, BIGIOT.organizationId,event.id().value());
        orgIns.addProperty(model.createDatatypeProperty(SCHEMA_PREFIX+"name"),event.name());

        return model;
    }

    public Model consumerCreated(ConsumerCreated event) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        OntClass consumerClass = model.createClass(BIGIOT_PREFIX + "Consumer");
        Individual consInd = consumerClass.createIndividual(BASE_PREFIX + event.id().value());
        consInd.addLiteral(SCHEMA.name,event.name());
        consInd.addLiteral(BIGIOT.consumerId, event.id().value());
        consInd.addProperty(SCHEMA.sourceOrganization, model.createResource(BASE_PREFIX + event.organizationId().value()));
        return model;
    }

    public Model offeringQueryCreated(OfferingQueryCreated event) {
        logger.info("OfferingQuery is creating {}",event);
        try {
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            model.setNsPrefixes(PREFIXES.getPrefixSet());

            OntClass offQueryClass = model.createClass(BIGIOT_PREFIX + "OfferingQuery");
            Individual offQueryInd = offQueryClass.createIndividual(BASE_PREFIX + event.id().value());
            offQueryInd.addLiteral(BIGIOT.queryId, event.id().value());
            offQueryInd.addLiteral(SCHEMA.name, event.name());
            offQueryInd.addProperty(BIGIOT.isRegisteredBy, model.createResource(BASE_PREFIX + event.consumerId().value()));
            if (event.rdfUri().isDefined())
                offQueryInd.addProperty(SCHEMA.category, model.createResource(model.expandPrefix(event.rdfUri().get())));

            //add Input Data
            if (!event.inputs().isEmpty()) {
                OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
                scala.collection.Iterator<DataField> iter = event.inputs().iterator();
                while (iter.hasNext()) {
                    DataField dataField = iter.next();
                    Individual inputData = DataClass.createIndividual();
                    inputData.addLiteral(SCHEMA.name, dataField.name());
                    inputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));

                    Individual value = model.createIndividual(BIGIOT.DataValue);
                    if(dataField.value() instanceof TextType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                    else if(dataField.value() instanceof NumberType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                    else if(dataField.value() instanceof IntegerType )
                        value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                    else if(dataField.value() instanceof DateTimeType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                    else if(dataField.value() instanceof BooleanType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                    else if(dataField.value() instanceof ObjectType) {
                        value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                        inputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                    }

                    inputData.addProperty(BIGIOT.value,value);
                    //link offering to input data
                    offQueryInd.addProperty(BIGIOT.hasInput, inputData);
                }
            }

            //add Output Data
            if (!event.outputs().isEmpty()) {
                OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
                scala.collection.Iterator<DataField> iter = event.outputs().iterator();
                while (iter.hasNext()) {
                    DataField dataField = iter.next();
                    Individual outputData = DataClass.createIndividual();
                    outputData.addLiteral(SCHEMA.name, dataField.name());
                    outputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));

                    Individual value = model.createIndividual(BIGIOT.DataValue);
                    if(dataField.value() instanceof TextType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                    else if(dataField.value() instanceof NumberType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                    else if(dataField.value() instanceof IntegerType )
                        value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                    else if(dataField.value() instanceof DateTimeType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                    else if(dataField.value() instanceof BooleanType)
                        value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                    else if(dataField.value() instanceof ObjectType) {
                        value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                        outputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                    }

                    outputData.addProperty(BIGIOT.value,value);
                    //link offering to input data
                    offQueryInd.addProperty(BIGIOT.hasOutput, outputData);
                }
            }

            if (event.license() != null && event.license().toString() != "None") {
                OntClass licenseClass = model.createClass(BIGIOT_PREFIX + "License");
                Individual openLicenseIns = model.createIndividual(offQueryInd.getURI() + "License", licenseClass);
                openLicenseIns.addProperty(BIGIOT.licenseType, model.createResource(BIGIOT.getLicenseType(event.license().toString())));

                //link license to offering
                offQueryInd.addProperty(SCHEMA.license, openLicenseIns);
            }

            //create region
            if (event.spatialExtent().isDefined() && event.spatialExtent().toString() != "None") {
                OntClass regionClass = model.createClass(SCHEMA_PREFIX + "Region");
                Individual regionIns = model.createIndividual(offQueryInd.getURI() + "Region", regionClass);
                regionIns.addLabel(model.createTypedLiteral(event.spatialExtent().get().city()));
                if(event.spatialExtent().get().boundary().isDefined()) {
                    BoundingBox boundingBox = event.spatialExtent().get().boundary().get();
                    regionIns.addLiteral(BIGIOT.LOWER_BOUND_LATITUDE,model.createTypedLiteral(boundingBox.l1().lat(),XSDDatatype.XSDdouble));
                    regionIns.addLiteral(BIGIOT.LOWER_BOUND_LONGITUDE,model.createTypedLiteral(boundingBox.l1().lng(),XSDDatatype.XSDdouble));
                    regionIns.addLiteral(BIGIOT.UPPER_BOUND_LATITUDE,model.createTypedLiteral(boundingBox.l2().lat(),XSDDatatype.XSDdouble));
                    regionIns.addLiteral(BIGIOT.UPPER_BOUND_LONGITUDE,model.createTypedLiteral(boundingBox.l2().lng(),XSDDatatype.XSDdouble));
                }
                //link region to offering
                offQueryInd.addProperty(SCHEMA.spatialCoverage, regionIns);
            }


            if (event.price().isDefined() && event.price() != null && event.price().toString() != "None") {
                //create Price
                OntClass priceClass = model.createClass(BIGIOT_PREFIX + "Price");
                Individual priceIns = model.createIndividual(offQueryInd.getURI() + "Price", priceClass);
                if (event.price().get().money().isDefined()) {
                    priceIns.addLiteral(SCHEMA.priceCurrency, event.price().get().money().get().currency().value());
                    double amount = event.price().get().money().get().amount().doubleValue();
                    priceIns.addLiteral(SCHEMA.price, model.createTypedLiteral(amount, XSDDatatype.XSDdouble));
                }else{
                    priceIns.addLiteral(SCHEMA.priceCurrency, "EUR");
                    priceIns.addLiteral(SCHEMA.price, model.createTypedLiteral(0, XSDDatatype.XSDdouble));
                }
                //link accounting model to price
                priceIns.addProperty(model.createDatatypeProperty(BIGIOT_PREFIX + "pricingModel"),
                        model.createResource(BIGIOT.getPriceModel(event.price().get().pricingModel().toString())));
                //link price to offering
                offQueryInd.addProperty(SCHEMA.priceSpecification, priceIns);
            }

            // TODO handle temporalExtent
            if (event.temporalExtent().isDefined()) {
                if (event.temporalExtent().get().from().isDefined())
                    offQueryInd.addLiteral(SCHEMA.validFrom, model.createTypedLiteral(event.temporalExtent().get().from().get(), XSDDatatype.XSDlong));
                else
                    offQueryInd.addLiteral(SCHEMA.validFrom, model.createTypedLiteral(0, XSDDatatype.XSDlong));
                if (event.temporalExtent().get().to().isDefined())
                    offQueryInd.addLiteral(SCHEMA.validThrough, model.createTypedLiteral(event.temporalExtent().get().to().get(), XSDDatatype.XSDlong));
                else
                    offQueryInd.addLiteral(SCHEMA.validThrough, model.createTypedLiteral(0, XSDDatatype.XSDlong));
            }
//            else{
//                offQueryInd.addLiteral(SCHEMA.validFrom, model.createTypedLiteral(0, XSDDatatype.XSDlong));
//                offQueryInd.addLiteral(SCHEMA.validThrough, model.createTypedLiteral(0, XSDDatatype.XSDlong));
//            }

            return model;
        }catch(Exception e){
            logger.debug("Can not create offering query " + e.toString());
        }
        return null;
    }

    public Model subscriptionCreated(SubscriptionCreated event) {
        // TODO
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        OntClass subClass = model.createClass(BIGIOT_PREFIX + "Subscription");
        Individual subInd = subClass.createIndividual(BASE_PREFIX + event.id().value());
        subInd.addLiteral(BIGIOT.subscriptionId, event.id().value());

        OntClass offeringClass = model.createClass(BIGIOT_PREFIX + "Offering");
        Individual offeringIdv = model.createIndividual(BASE_PREFIX + event.subscribableId(),offeringClass);
        subInd.addProperty(model.createProperty(BIGIOT_PREFIX + "subscribeTo"),offeringIdv);

        OntClass offQueryClass = model.createClass(BIGIOT_PREFIX + "OfferingQuery");
        Individual offQueryInd = offQueryClass.createIndividual(BASE_PREFIX + event.subscriberId());
        subInd.addProperty(model.createProperty(BIGIOT_PREFIX + "subscribedQuery"),offQueryInd);

        return model;
    }

    public Model offeringEndpointsChanged(OfferingEndpointsChanged ev){
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        OntClass offeringClass = model.createClass(BIGIOT_PREFIX + "Offering");
        Individual offeringIdv = model.createIndividual(BASE_PREFIX + ev.id().value(),offeringClass);

        if(!ev.endpoints().isEmpty()) {
            //create Endpoint
            OntClass endpointClass = model.createClass(BIGIOT_PREFIX + "Endpoint");
            Iterator<Endpoint> iter = ev.endpoints().iterator();
            while (iter.hasNext()) {
                Endpoint endpoint = iter.next();
                Individual endpointIns = model.createIndividual(offeringIdv.getURI() + "Endpoint", endpointClass);
                //set url property for endpoint
                endpointIns.addLiteral(SCHEMA.url, endpoint.uri());
                endpointIns.addProperty(BIGIOT.accessInterfaceType,
                        model.createResource(BIGIOT.getAccessInterfaceType(endpoint.accessInterfaceType().toString())));
                endpointIns.addProperty(BIGIOT.endpointType,
                        model.createResource(BIGIOT.getEndpointType(endpoint.endpointType().toString())));
                //link offering to endpoint
                offeringIdv.addProperty(BIGIOT.endpoint, endpointIns);
            }
        }
        return model;
    }

    public Model offeringInputDataFieldsChanged(OfferingInputsChanged ev) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.setNsPrefixes(PREFIXES.getPrefixSet());
        boolean isUpdateCategory = false;
        OntClass offeringClass = model.createClass(BIGIOT_PREFIX + "Offering");
        Individual offeringIdv = model.createIndividual(BASE_PREFIX + ev.id().value(),offeringClass);

        if(!ev.inputs().isEmpty()) {
            Model allOfferingModel = RDFUtils.getAllOfferingCategoryModel();
            OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
            scala.collection.Iterator<DataField> iter = ev.inputs().iterator();
            while (iter.hasNext()) {
                DataField dataField = iter.next();
                Model dataModel = dataTypeAddedToOfferingCategory(ev.categoryUri(),dataField.rdfAnnotation().uri(),allOfferingModel);
                if(dataModel!=null) {
                    isUpdateCategory = true;
                    RDFUtils.getRdfServer().executeUpdateQuery(dataModel, QueryFactory.getOntologyGraph());
//                    model.add(dataModel);
                }
                Individual inputData = DataClass.createIndividual();
                inputData.addLiteral(SCHEMA.name, dataField.name());
                inputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));

                Individual value = model.createIndividual(BIGIOT.DataValue);
                if(dataField.value() instanceof TextType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                else if(dataField.value() instanceof NumberType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                else if(dataField.value() instanceof IntegerType )
                    value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                else if(dataField.value() instanceof DateTimeType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                else if(dataField.value() instanceof BooleanType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                else if(dataField.value() instanceof ObjectType) {
                    value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                    inputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                }

                inputData.addProperty(BIGIOT.value,value);
                //link offering to input data
                offeringIdv.addProperty(BIGIOT.hasInput, inputData);
            }
        }
        if(isUpdateCategory){
            RDFUtils.updateAllOfferingCategoriesModel();
            RDFUtils.updateDataTypeAnnotationModel();
        }
        return model;
    }

    public Model offeringOutputDataFieldsChanged(OfferingOutputsChanged ev) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.setNsPrefixes(PREFIXES.getPrefixSet());
        boolean isUpdateCategory = false;
        OntClass offeringClass = model.createClass(BIGIOT_PREFIX + "Offering");
        Individual offeringIdv = model.createIndividual(BASE_PREFIX + ev.id().value(),offeringClass);

        if(!ev.outputs().isEmpty()) {
            Model allOfferingModel = RDFUtils.getAllOfferingCategoryModel();
            OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
            scala.collection.Iterator<DataField> iter = ev.outputs().iterator();
            while (iter.hasNext()) {
                DataField dataField = iter.next();
                Model dataModel = dataTypeAddedToOfferingCategory(ev.categoryUri(),dataField.rdfAnnotation().uri(),allOfferingModel);
                if(dataModel!=null) {
//                    model.add(dataModel);
                    isUpdateCategory = true;
                    RDFUtils.getRdfServer().executeUpdateQuery(dataModel, QueryFactory.getOntologyGraph());
                }

                Individual outputData = DataClass.createIndividual();
                outputData.addLiteral(SCHEMA.name, dataField.name());
                outputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));

                Individual value = model.createIndividual(BIGIOT.DataValue);
                if(dataField.value() instanceof TextType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                else if(dataField.value() instanceof NumberType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                else if(dataField.value() instanceof IntegerType )
                    value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                else if(dataField.value() instanceof DateTimeType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                else if(dataField.value() instanceof BooleanType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                else if(dataField.value() instanceof ObjectType) {
                    value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                    outputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                }

                outputData.addProperty(BIGIOT.value,value);
                //link offering to input data
                offeringIdv.addProperty(BIGIOT.hasOutput, outputData);
            }
        }
        if(isUpdateCategory){
            RDFUtils.updateAllOfferingCategoriesModel();
            RDFUtils.updateDataTypeAnnotationModel();
        }
        return model;
    }

    public Model offeringQueryInputDataFieldsChanged(OfferingQueryInputsChanged ev) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.setNsPrefixes(PREFIXES.getPrefixSet());

        OntClass offeringClass = model.createClass(BIGIOT_PREFIX + "OfferingQuery");
        Individual offeringIdv = model.createIndividual(BASE_PREFIX + ev.id().value(),offeringClass);

        if(!ev.inputs().isEmpty()) {
            //create Endpoint
            OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
            scala.collection.Iterator<DataField> iter = ev.inputs().iterator();
            while (iter.hasNext()) {
                DataField dataField = iter.next();
                Individual inputData = DataClass.createIndividual();
                inputData.addLiteral(SCHEMA.name, dataField.name());
                inputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));

                Individual value = model.createIndividual(BIGIOT.DataValue);
                if(dataField.value() instanceof TextType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                else if(dataField.value() instanceof NumberType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                else if(dataField.value() instanceof IntegerType )
                    value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                else if(dataField.value() instanceof DateTimeType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                else if(dataField.value() instanceof BooleanType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                else if(dataField.value() instanceof ObjectType) {
                    value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                    inputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                }

                inputData.addProperty(BIGIOT.value,value);
                //link offering to input data
                offeringIdv.addProperty(BIGIOT.hasInput, inputData);
            }
        }
        return model;
    }

    public Model offeringQueryOutputDataFieldsChanged(OfferingQueryOutputsChanged ev) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.setNsPrefixes(PREFIXES.getPrefixSet());

        OntClass offeringClass = model.createClass(BIGIOT_PREFIX + "OfferingQuery");
        Individual offeringIdv = model.createIndividual(BASE_PREFIX + ev.id().value(),offeringClass);

        if(!ev.outputs().isEmpty()) {
            //create Endpoint
            OntClass DataClass = model.createClass(BIGIOT_PREFIX + "Data");
            scala.collection.Iterator<DataField> iter = ev.outputs().iterator();
            while (iter.hasNext()) {
                DataField dataField = iter.next();
                Individual outputData = DataClass.createIndividual();
                outputData.addLiteral(SCHEMA.name, dataField.name());
                outputData.addProperty(BIGIOT.rdfAnnotation, model.createResource(model.expandPrefix(dataField.rdfAnnotation().uri())));

                Individual value = model.createIndividual(BIGIOT.DataValue);
                if(dataField.value() instanceof TextType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.TEXT);
                else if(dataField.value() instanceof NumberType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.NUMBER);
                else if(dataField.value() instanceof IntegerType )
                    value.addProperty(BIGIOT.valueType,SCHEMA.INTEGER);
                else if(dataField.value() instanceof DateTimeType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.DATETIME);
                else if(dataField.value() instanceof BooleanType)
                    value.addProperty(BIGIOT.valueType,SCHEMA.BOOLEAN);
                else if(dataField.value() instanceof ObjectType) {
                    value.addProperty(BIGIOT.valueType, SCHEMA.OBJECT);
                    outputData.addProperty(RDF.type,BIGIOT.OBJECTSCHEMA);
                }

                outputData.addProperty(BIGIOT.value,value);
                //link offering to input data
                offeringIdv.addProperty(BIGIOT.hasOutput, outputData);
            }
        }
        return model;
    }


    public Model offeringCategoryChanged(OfferingCategoryChanged ev) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.setNsPrefixes(PREFIXES.getPrefixSet());
        OntClass offeringClass = model.createClass(PREFIXES.BIGIOT_CORE_NS + "Offering");
        Individual offeringIdv = model.createIndividual(PREFIXES.BIGIOT_BASE_NS + ev.id().value(), offeringClass);

        //link offering to category
        offeringIdv.addProperty(SCHEMA.category, model.createResource(model.expandPrefix(ev.rdfUri())));
        model.add(RDFUtils.getCategoryOnlyModel());

        List<Rule> rules = Rule.parseRules(BIGIOT.OFFERING_SUB_CATEGORY_RULES);
        Reasoner engine = new GenericRuleReasoner(rules);
        InfModel inf = ModelFactory.createInfModel(engine, model);

        Model entails = ModelFactory.createDefaultModel();
        entails = inf.difference(RDFServer.domainModel);
        return entails;
    }

    public Model offeringCategoryCreated(OfferingCategoryCreated ev) {
        logger.debug("create new offering category {} ",ev);
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.setNsPrefixes(PREFIXES.getPrefixSet());

        Individual parentOfferingCategoryIdv = model.createIndividual(model.expandPrefix(ev.parent()), BIGIOT.OfferingCategory);

        Individual proposedOfferingCategoryIdv = model.createIndividual(ev.uri(),BIGIOT.OfferingCategory);

        Model allOfferingModel = RDFUtils.getAllOfferingCategoryModel();
        if(allOfferingModel.containsResource(proposedOfferingCategoryIdv)) {
            logger.debug("Proposed category exists",ev);
            return null;
        }

        proposedOfferingCategoryIdv.addRDFType(BIGIOT.ProposedOfferingCategory);

        parentOfferingCategoryIdv.addProperty(model.createProperty(PREFIXES.SKOS_NS + "narrower"),proposedOfferingCategoryIdv);

        proposedOfferingCategoryIdv.addLabel(model.createLiteral(ev.name()));

        proposedOfferingCategoryIdv.addLiteral(BIGIOT.isDeprecated,false);


        NodeIterator iter = allOfferingModel.listObjectsOfProperty(allOfferingModel.getResource(parentOfferingCategoryIdv.getURI()),
                            BIGIOT.expectedAnnotation);
        while(iter.hasNext()){
            RDFNode node = iter.next();
            model.add(proposedOfferingCategoryIdv,BIGIOT.expectedAnnotation,node);
        }

        return model;
    }

    public Model offeringCategoryParentChanged(OfferingCategoryParentChanged ev) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.setNsPrefixes(PREFIXES.getPrefixSet());

        Individual parentOfferingCategoryIdv = model.createIndividual(model.expandPrefix(ev.parent()), BIGIOT.OfferingCategory);

        Individual proposedOfferingCategoryIdv = model.createIndividual(ev.uri(),BIGIOT.OfferingCategory);

        parentOfferingCategoryIdv.addProperty(model.createProperty(PREFIXES.SKOS_NS + "narrower"),proposedOfferingCategoryIdv);

        Model allOfferingModel = RDFUtils.getAllOfferingCategoryModel();

        NodeIterator iter = allOfferingModel.listObjectsOfProperty(allOfferingModel.getResource(parentOfferingCategoryIdv.getURI()),
                BIGIOT.expectedAnnotation);
        while(iter.hasNext()){
            RDFNode node = iter.next();
            model.add(proposedOfferingCategoryIdv,BIGIOT.expectedAnnotation,node);
        }

        return model;
    }

    public Model inputTypeAddedToOfferingCategory(InputTypeAddedToOfferingCategory event) {
        try {
            Model allOfferingModel = RDFUtils.getAllOfferingCategoryModel();
            String dataTypeURI = allOfferingModel.expandPrefix(event.rdfAnnotation().uri());
            if (allOfferingModel.containsResource(ResourceFactory.createResource(dataTypeURI))) {
                Resource res = allOfferingModel.getResource(dataTypeURI);
                if (res.hasProperty(RDF.type, BIGIOT.DatatypeAnnotation) || res.hasProperty(RDF.type, BIGIOT.OfferingCategory)) {
                    logger.debug("Proposed type is not valid or already exists {}", event.rdfAnnotation().uri());
                    return null;
                }
            }
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            model.setNsPrefixes(PREFIXES.getPrefixSet());

            Individual proposedInputType = model.createIndividual(dataTypeURI, BIGIOT.DatatypeAnnotation);
            proposedInputType.addRDFType(BIGIOT.ProposedDatatypeAnnotation);
            proposedInputType.addLabel(model.createLiteral(event.rdfAnnotation().label()));
            proposedInputType.addLiteral(BIGIOT.isDeprecated,false);

            Individual offeringCategoryIdv = model.createIndividual(event.uri(), BIGIOT.OfferingCategory);
            offeringCategoryIdv.addProperty(BIGIOT.expectedAnnotation, proposedInputType);

            NodeIterator iter = allOfferingModel.listObjectsOfProperty(allOfferingModel.getResource(offeringCategoryIdv.getURI()),
                    BIGIOT.narrower);
            while (iter.hasNext()) {
                RDFNode node = iter.next();
                model.add(node.asResource(), BIGIOT.expectedAnnotation, proposedInputType);
            }

            return model;
        }catch (Exception e){
            logger.error("can not create data type " + e.getMessage());
            return null;
        }
    }

    public Model outputTypeAddedToOfferingCategory(OutputTypeAddedToOfferingCategory event) {
        try {
            Model allOfferingModel = RDFUtils.getAllOfferingCategoryModel();
            String dataTypeURI = allOfferingModel.expandPrefix(event.rdfAnnotation().uri());
            if (allOfferingModel.containsResource(ResourceFactory.createResource(dataTypeURI))) {
                Resource res = allOfferingModel.getResource(allOfferingModel.expandPrefix(event.rdfAnnotation().uri()));
                if (res.hasProperty(RDF.type, BIGIOT.DatatypeAnnotation) || res.hasProperty(RDF.type, BIGIOT.OfferingCategory)) {
                    logger.debug("Proposed type is not valid or already exists {}", event.rdfAnnotation().uri());
                    return null;
                }
            }
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            model.setNsPrefixes(PREFIXES.getPrefixSet());

            Individual proposedInputType = model.createIndividual(dataTypeURI, BIGIOT.DatatypeAnnotation);

            proposedInputType.addRDFType(BIGIOT.ProposedDatatypeAnnotation);
            proposedInputType.addLabel(model.createLiteral(event.rdfAnnotation().label()));
            proposedInputType.addLiteral(BIGIOT.isDeprecated,false);

            Individual offeringCategoryIdv = model.createIndividual(event.uri(), BIGIOT.OfferingCategory);
            offeringCategoryIdv.addProperty(BIGIOT.expectedAnnotation, proposedInputType);

            NodeIterator iter = allOfferingModel.listObjectsOfProperty(allOfferingModel.getResource(offeringCategoryIdv.getURI()),
                    BIGIOT.narrower);
            while (iter.hasNext()) {
                RDFNode node = iter.next();
                model.add(node.asResource(), BIGIOT.expectedAnnotation, proposedInputType);
            }

            return model;
        }catch(Exception e){
            logger.error("can not create data type " + e.getMessage());
            return null;
        }
    }

    public Model dataTypeAddedToOfferingCategory(String categoryURI, String dataTypeURI, Model allOfferingModel) {
        try{
            allOfferingModel.setNsPrefixes(PREFIXES.getPrefixSet());
            if (allOfferingModel.containsResource(ResourceFactory.createResource(allOfferingModel.expandPrefix(dataTypeURI)))) {
                Resource res = allOfferingModel.getResource(dataTypeURI);
                if (res.hasProperty(RDF.type, BIGIOT.DatatypeAnnotation) || res.hasProperty(RDF.type, BIGIOT.OfferingCategory)) {
                    logger.debug("Proposed type is not valid or already exists {}", dataTypeURI);
                    return null;
                }
            }
            logger.debug("Creating proposed type {}", dataTypeURI);
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            model.setNsPrefixes(PREFIXES.getPrefixSet());

            Individual proposedInputType = model.createIndividual(model.expandPrefix(dataTypeURI), BIGIOT.DatatypeAnnotation);
            proposedInputType.addRDFType(BIGIOT.ProposedDatatypeAnnotation);
            proposedInputType.addLiteral(BIGIOT.isDeprecated,false);

            Individual offeringCategoryIdv = model.createIndividual(categoryURI, BIGIOT.OfferingCategory);
            offeringCategoryIdv.addProperty(BIGIOT.expectedAnnotation, proposedInputType);

            NodeIterator iter = allOfferingModel.listObjectsOfProperty(allOfferingModel.getResource(offeringCategoryIdv.getURI()),
                    BIGIOT.narrower);
            while (iter.hasNext()) {
                RDFNode node = iter.next();
                model.add(node.asResource(), BIGIOT.expectedAnnotation, proposedInputType);
            }

            return model;
        }catch (Exception e){
            logger.error("can not create data type " + e.getMessage());
            return null;
        }
    }
}
