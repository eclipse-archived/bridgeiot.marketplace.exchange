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

import exchange.api.access.AccessInterfaceType;
import exchange.api.access.EndpointType;
import exchange.api.consumer.*;
import exchange.api.extent;
import exchange.api.license.License;
import exchange.api.offering;
import exchange.api.offering.*;
import exchange.api.offeringquery.*;
import exchange.api.organization.*;
import exchange.api.price.Currency;
import exchange.api.price.PricingModel;
import exchange.api.provider.*;
import exchange.api.semantics.*;
import exchange.api.subscription.*;
import exchange.model.vocabs.BIGIOT;
import exchange.model.vocabs.PREFIXES;
import exchange.repo.ExchangeSemanticRepo;
import exchange.repo.ExchangeRepoMutations;
import org.apache.commons.collections4.ListUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.Some;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;
import virtuoso.jena.driver.VirtuosoQueryExecution;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class RDFExchangeRepo implements ExchangeRepoMutations, ExchangeSemanticRepo {

    public boolean isPersisting() {
      return true;
    }

    final static Logger logger = LoggerFactory.getLogger(RDFExchangeRepo.class);
    private final RDFSerializer rdfSerializer;
    private final RDFParser rdfParser;
    private RDFServer rdfServer;

    public RDFExchangeRepo() throws Exception {
        this.rdfServer = RDFServer.get();
        this.rdfSerializer = new RDFSerializer();
        this.rdfParser = new RDFParser();
    }

    public RDFExchangeRepo(String instanceType) throws Exception {
        logger.info("This deployment for environment {}" , instanceType);
        this.rdfServer = RDFServer.get();
        this.rdfSerializer = new RDFSerializer();
        this.rdfParser = new RDFParser();
    }

    public OfferingCategory allOfferingCategories() {
        Model m = RDFUtils.getAllOfferingCategoryModel();
        return rdfSerializer.allOfferingCategories(m);
    }

    public Option<OfferingCategory> offeringCategory(String rdfUri) {
        Model m = RDFUtils.getAllOfferingCategoryModel();
        return rdfSerializer.findOfferingCategory(rdfUri, m);
    }

    public List<String> allOfferingCategoryUris() {
        ArrayList<String> categoryUris = new ArrayList<String>();
        try {
            String query = QueryFactory.allCategoryUris();
            VirtuosoQueryExecution vqe = rdfServer.executeSelectQuery(query);
            ResultSet results = vqe.execSelect();
            while (results.hasNext()) {
                QuerySolution result = results.nextSolution();
                String category = result.getResource("category").getURI();
                categoryUris.add(category);
            }
            vqe.close();
        }catch (Exception e){
            logger.error("allOfferingCategoryUris error " + e);
        }
        logger.debug("allOfferingCategoryUris result size {}", categoryUris.size());
        return JavaConverters.asScalaBuffer(categoryUris).toList();
    }

    public List<RdfAnnotation> allDataTypes() {
        String q = QueryFactory.create(BIGIOT.DatatypeAnnotation);
        Model m = rdfServer.executeConstructQuery(q);
        m.setNsPrefixes(PREFIXES.getPrefixSet());
        return rdfSerializer.allRdfAnnotations(m);
    }

    public Option<DataField> outputDataField(String typeUri, String categoryUri) {
//        BindingMap bindings = new BindingMap().withResource("annotation", typeUri);
//        String q = QueryFactory.create(BIGIOT.DatatypeAnnotation, bindings);
//        Model m = rdfServer.executeConstructQuery(q);
        Model m = RDFUtils.getAllDataTypeAnnotationModel();
        m.setNsPrefixes(PREFIXES.getPrefixSet());

        Option<DataField> dataField = rdfSerializer.findDataField(typeUri,m);
        return dataField;
    }

    public Option<DataField> inputDataField(String typeUri, String categoryUri) {
        Model m = RDFUtils.getAllDataTypeAnnotationModel();
        m.setNsPrefixes(PREFIXES.getPrefixSet());

//        return Option.empty(); // new Some(new DataField("", rdfAnnotation, new TextType(), "", false)); // TODO fill in default name, value type and required property
        Option<DataField> dataField = rdfSerializer.findDataField(typeUri,m);
        return dataField;
    }

    private Option<OfferingQuery> findOfferingQuery(String id) {
        logger.debug("Offering query id to find:{}",id);
        BindingMap bindings = new BindingMap().withLiteral("id", id);
        String q = QueryFactory.create(BIGIOT.OfferingQuery, bindings);
        Model m = rdfServer.executeConstructQuery(q);
        m.setNsPrefixes(PREFIXES.getPrefixSet());
        return rdfSerializer.findOfferingQuery(id, m);
    }

    //////////////////////////////////////////////////////////////////// Mutations

    public List<String> matchingOfferingIds(OfferingQueryId queryId) {
        ArrayList<String> offeringIds = new ArrayList<String>();
        java.util.List<String> merged = new ArrayList<String>();
        logger.debug("find offering query:{}", queryId);
        Option<OfferingQuery> query = findOfferingQuery(queryId.value());
        logger.debug("offering query found:{}", query);
        try {
            if (query.isDefined()) {
                logger.debug("start matching offering process...");
                ArrayList<String> spatialFilterIds = new ArrayList<String>();
                try {
                    Option<extent.SpatialExtent> spatialExtentOption = new Some(query.get().spatialExtent());
                    if (spatialExtentOption!=null && spatialExtentOption.isDefined()) {
                        if (query.get().spatialExtent().get().boundary().isDefined()) {
                            logger.debug("start spatial filtering process...");
                            String spatialFilterQuery = QueryFactory.spatialFilterOfferings(query.get());
                            VirtuosoQueryExecution vqe = rdfServer.executeSelectQuery(spatialFilterQuery);
                            ResultSet results = vqe.execSelect();
                            while (results.hasNext()) {
                                QuerySolution result = results.nextSolution();
                                String id = result.getLiteral("id").getString();
                                spatialFilterIds.add(id);
                            }
                            logger.debug("spatial filtering result size {} and details:{}", spatialFilterIds.size(),
                                    spatialFilterIds.stream()
                                            .map(n -> n.toString())
                                            .collect(Collectors.joining(",")));
                            if (spatialFilterIds.isEmpty())
                                return JavaConverters.asScalaBuffer(spatialFilterIds).toList();
                        }
                    }
                }catch(Exception e){
                    logger.error("spatial matching offering error " + e);
                }
                ArrayList<String> temporalFilterIds = new ArrayList<String>();
                if(query.get().temporalExtent().isDefined()){
                    if(query.get().temporalExtent().get().from().isDefined() && query.get().temporalExtent().get().to().isDefined()){
                            logger.debug("start temporal filtering process...");
                            String temporalFilterQuery = QueryFactory.temporalFilterOfferings(query.get());
                            VirtuosoQueryExecution vqe = rdfServer.executeSelectQuery(temporalFilterQuery);
                            ResultSet results = vqe.execSelect();
                            while (results.hasNext()) {
                                QuerySolution result = results.nextSolution();
                                String id = result.getLiteral("id").getString();
                                temporalFilterIds.add(id);
                            }
                            logger.debug("temporal filtering result size {} and details:{}", temporalFilterIds.size(),
                                    temporalFilterIds.stream()
                                            .map(n -> n.toString())
                                            .collect(Collectors.joining(",")));
                            if (temporalFilterIds.isEmpty())
                                return JavaConverters.asScalaBuffer(temporalFilterIds).toList();
                    }

                }

                String q = QueryFactory.findMatchingOfferings(query.get());
                VirtuosoQueryExecution vqe = rdfServer.executeSelectQuery(q);
                ResultSet results = vqe.execSelect();
                while (results.hasNext()) {
                    QuerySolution result = results.nextSolution();
                    String id = result.getLiteral("id").getString();
                    offeringIds.add(id);
                }
                logger.debug("semantic matching result size {} and details:{}", offeringIds.size(),
                                                    offeringIds.stream().map( n -> n.toString() )
                                                    .collect( Collectors.joining( "," ) ));
                vqe.close();

                if(spatialFilterIds.isEmpty() && temporalFilterIds.isEmpty()) {
                    logger.debug("return semantic filter");
                    merged = offeringIds;
                }
                if(!spatialFilterIds.isEmpty() && temporalFilterIds.isEmpty()) {
                    logger.debug("join spatial and semantic filter");
                    merged = ListUtils.intersection(offeringIds,spatialFilterIds);
                }
                if(spatialFilterIds.isEmpty() && !temporalFilterIds.isEmpty()) {
                    logger.debug("join temporal and semantic filter");
                    merged = ListUtils.intersection(offeringIds,temporalFilterIds);
                }
                if(!spatialFilterIds.isEmpty() && !temporalFilterIds.isEmpty()) {
                    logger.debug("join temporal, spatial and semantic filter");
                    merged = ListUtils.intersection(offeringIds, spatialFilterIds);
                    merged = ListUtils.intersection(temporalFilterIds, merged);
                }
            }
        }catch (Exception e){
            logger.error("matching offering error " + e);
        }
        logger.debug("matching result size {} and details:{}", merged.size(),
                merged.stream().map( n -> n.toString() )
                        .collect( Collectors.joining( "," ) ));
        return JavaConverters.asScalaBuffer(merged).toList();
    }

    public void offeringCategoryCreated(OfferingCategoryCreated event) {
        if(event.proposed()) {
            Model m = rdfParser.offeringCategoryCreated(event);
            rdfServer.executeUpdateQuery(m,QueryFactory.getOntologyGraph());
            RDFUtils.updateAllOfferingCategoriesModel();
            RDFUtils.updateCategoryModel();
        }
    }

    public void offeringCategoryDeprecated(OfferingCategoryDeprecated ev) {
        String updateQuery = QueryFactory.offeringCategoryDeprecated(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringCategoryUndeprecated(OfferingCategoryUndeprecated ev) {
        String updateQuery = QueryFactory.offeringCategoryUndeprecated(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringCategoryNameChanged(OfferingCategoryNameChanged event) {
        String updateQuery = QueryFactory.offeringCategoryNameChanged(event);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringCategoryParentChanged(OfferingCategoryParentChanged event) {
        String updateQuery = QueryFactory.offeringCategoryParentChanged(event);
        rdfServer.executeUpdateQuery(updateQuery);

        Model m = rdfParser.offeringCategoryParentChanged(event);
        rdfServer.executeUpdateQuery(m);

        RDFUtils.updateAllOfferingCategoriesModel();
        RDFUtils.updateCategoryModel();
    }

    public void inputTypeAddedToOfferingCategory(InputTypeAddedToOfferingCategory event) {
        Model m = rdfParser.inputTypeAddedToOfferingCategory(event);
        rdfServer.executeUpdateQuery(m,QueryFactory.getOntologyGraph());
        RDFUtils.updateAllOfferingCategoriesModel();
        RDFUtils.updateDataTypeAnnotationModel();
    }

    public void inputTypeDeprecatedForOfferingCategory(InputTypeDeprecatedForOfferingCategory ev) {
        String updateQuery = QueryFactory.inputTypeDeprecatedForOfferingCategory(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void inputTypeUndeprecatedForOfferingCategory(InputTypeUndeprecatedForOfferingCategory ev) {
        String updateQuery = QueryFactory.inputTypeUndeprecatedForOfferingCategory(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void outputTypeAddedToOfferingCategory(OutputTypeAddedToOfferingCategory event) {
        Model m = rdfParser.outputTypeAddedToOfferingCategory(event);
        rdfServer.executeUpdateQuery(m,QueryFactory.getOntologyGraph());
        RDFUtils.updateAllOfferingCategoriesModel();
        RDFUtils.updateDataTypeAnnotationModel();
    }

    public void outputTypeDeprecatedForOfferingCategory(OutputTypeDeprecatedForOfferingCategory ev) {
        String updateQuery = QueryFactory.outputTypeDeprecatedForOfferingCategory(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void outputTypeUndeprecatedForOfferingCategory(OutputTypeUndeprecatedForOfferingCategory ev) {
        String updateQuery = QueryFactory.outputTypeUndeprecatedForOfferingCategory(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void organizationCreated(OrganizationCreated event) {
        Model m = rdfParser.organizationCreated(event);
        rdfServer.executeUpdateQuery(m);
    }

    public void organizationNameChanged(OrganizationNameChanged ev) {
        String updateQuery = QueryFactory.organizationNameChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void providerCreated(ProviderCreated event) {
        Model m = rdfParser.providerCreated(event);
        rdfServer.executeUpdateQuery(m);
    }

    public void providerDeleted(ProviderDeleted ev) {
        String updateQuery = QueryFactory.providerDeleted(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void providerNameChanged(ProviderNameChanged ev) {
        String updateQuery = QueryFactory.providerNameChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringCreated(OfferingCreated event) {
        Model m = rdfParser.offeringCreated(event);
        rdfServer.executeUpdateQuery(m);
    }

    public void offeringDeleted(OfferingDeleted event) {
        String updateQuery = QueryFactory.offeringDeleted(event);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringNameChanged(OfferingNameChanged ev) {
        String updateQuery = QueryFactory.offeringNameChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringCategoryChanged(OfferingCategoryChanged ev) {
        String updateQuery = QueryFactory.offeringCategoryChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);

        Model dataModel = rdfParser.offeringCategoryChanged(ev);
        rdfServer.executeUpdateQuery(dataModel);
    }

    public void offeringAccessWhiteListChanged(OfferingAccessWhiteListChanged ev) {
      //ignore
    }

    public void offeringEndpointsChanged(OfferingEndpointsChanged ev) {
        String updateQuery = QueryFactory.offeringEndpointsDeleted(ev);
        rdfServer.executeUpdateQuery(updateQuery);

        Model endpointModel = rdfParser.offeringEndpointsChanged(ev);
        rdfServer.executeUpdateQuery(endpointModel);
    }

    public void offeringInputDataChanged(OfferingInputsChanged ev) {
        String updateQuery = QueryFactory.offeringInputDataFieldsDeleted(ev);
        rdfServer.executeUpdateQuery(updateQuery);

        Model dataModel = rdfParser.offeringInputDataFieldsChanged(ev);
        rdfServer.executeUpdateQuery(dataModel);
    }

    public void offeringOutputDataChanged(OfferingOutputsChanged ev) {
        String updateQuery = QueryFactory.offeringOutputDataFieldsDeleted(ev);
        rdfServer.executeUpdateQuery(updateQuery);

        Model dataModel = rdfParser.offeringOutputDataFieldsChanged(ev);
        rdfServer.executeUpdateQuery(dataModel);
    }

    public void offeringSpatialExtentChanged(OfferingSpatialExtentChanged ev) {
        String updateQuery = QueryFactory.offeringSpatialExtentChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringTemporalExtentChanged(OfferingTemporalExtentChanged ev) {
        logger.info("update offering temporal extent {}",ev);
        String updateQuery = QueryFactory.offeringTemporalExtentChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringLicenseChanged(OfferingLicenseChanged ev) {
        String updateQuery = QueryFactory.offeringLicenseChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringPriceChanged(OfferingPriceChanged ev) {
        String updateQuery = QueryFactory.offeringPriceChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringExtension1Changed(OfferingExtension1Changed ev) {
        // ignore
    }

    public void offeringExtension2Changed(OfferingExtension2Changed ev) {
        // ignore
    }

    public void offeringExtension3Changed(OfferingExtension3Changed ev) {
        // ignore
    }

    public void offeringActivated(OfferingActivated event) {
        String updateQuery = QueryFactory.OfferingActivated(event);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringDeactivated(OfferingDeactivated event) {
        String updateQuery = QueryFactory.OfferingDeactivated(event);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void consumerCreated(ConsumerCreated event) {
        Model m = rdfParser.consumerCreated(event);
        rdfServer.executeUpdateQuery(m);
    }

    public void consumerDeleted(ConsumerDeleted event) {
        String updateQuery = QueryFactory.consumerDeleted(event);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void consumerNameChanged(ConsumerNameChanged ev) {
        String updateQuery = QueryFactory.consumerNameChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringQueryCreated(OfferingQueryCreated event) {
        Model m = rdfParser.offeringQueryCreated(event);
        rdfServer.executeUpdateQuery(m);
    }

    public void offeringQueryDeleted(OfferingQueryDeleted event) {
        String updateQuery = QueryFactory.offeringQueryDeleted(event);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringQueryNameChanged(OfferingQueryNameChanged ev) {
        String updateQuery = QueryFactory.offeringQueryNameChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringQueryCategoryChanged(OfferingQueryCategoryChanged ev) {
        String updateQuery = QueryFactory.offeringQueryCategoryChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringQueryInputDataChanged(OfferingQueryInputsChanged ev) {
        String updateQuery = QueryFactory.offeringQueryInputDataFieldsDeleted(ev);
        rdfServer.executeUpdateQuery(updateQuery);

        Model dataModel = rdfParser.offeringQueryInputDataFieldsChanged(ev);
        rdfServer.executeUpdateQuery(dataModel);
    }

    public void offeringQueryOutputDataChanged(OfferingQueryOutputsChanged ev) {
        String updateQuery = QueryFactory.offeringQueryOutputDataFieldsDeleted(ev);
        rdfServer.executeUpdateQuery(updateQuery);

        Model dataModel = rdfParser.offeringQueryOutputDataFieldsChanged(ev);
        rdfServer.executeUpdateQuery(dataModel);
    }

    public void offeringQuerySpatialExtentChanged(OfferingQuerySpatialExtentChanged ev) {
        String updateQuery = QueryFactory.offeringQuerySpatialExtentChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringQueryTemporalExtentChanged(OfferingQueryTemporalExtentChanged ev) {
        logger.info("update offering query temporal extent {}",ev);
        String updateQuery = QueryFactory.offeringQueryTemporalExtentChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringQueryLicenseChanged(OfferingQueryLicenseChanged ev) {
        String updateQuery = QueryFactory.offeringQueryLicenseChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringQueryPriceChanged(OfferingQueryPriceChanged ev) {
        String updateQuery = QueryFactory.offeringQueryPriceChanged(ev);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void subscriptionCreated(SubscriptionCreated event) {
        Model m = rdfParser.subscriptionCreated(event);
        rdfServer.executeUpdateQuery(m);
    }

    public void subscriptionDeleted(SubscriptionDeleted event) {
        String updateQuery = QueryFactory.subscriptionDeleted(event);
        rdfServer.executeUpdateQuery(updateQuery);
    }

    public void offeringGraphDeleted(){
        String deleteGraphQuery = QueryFactory.deleteGraph(QueryFactory.getOfferingGraph());
        rdfServer.executeUpdateQuery(deleteGraphQuery);
    }

    public boolean isOfferingConsistent(OfferingCreated ev) {
        return false;
    }

    public boolean isOfferingQueryConsistent(OfferingQueryCreated ev) {
        return false;
    }

    // synchronize enums
    public void currenciesUpdated(Currency[] currencies) {
    }

    public void pricingModelsUpdated(PricingModel[] pricingModels) {
    }

    public void licensesUpdated(License[] licenses) {
    }

    public void endpointTypesUpdated(EndpointType[] endpointTypes) {
    }

    public void accessInterfaceTypesUpdated(AccessInterfaceType[] accessInterfaceTypes) {
    }
}
