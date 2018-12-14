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

import exchange.api.consumer.Consumer;
import exchange.api.extent;
import exchange.api.extent.BoundingBox;
import exchange.api.extent.SpatialExtent;
import exchange.api.extent.TemporalExtent;
import exchange.api.license.License;
import exchange.api.offeringquery.OfferingQuery;
import exchange.api.offeringquery.OfferingQueryId;
import exchange.api.offeringquery.QueryToOfferingSubscription;
import exchange.api.organization.Organization;
import exchange.api.organization.OrganizationId;
import exchange.api.price.Currency;
import exchange.api.price.Money;
import exchange.api.price.Price;
import exchange.api.price.PricingModel;
import exchange.api.provider.Provider;
import exchange.api.semantics;
import exchange.api.semantics.*;
import exchange.model.vocabs.BIGIOT;
import exchange.model.vocabs.SCHEMA;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.Some;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;
import scala.math.BigDecimal;

import javax.xml.crypto.Data;
import java.util.ArrayList;

public class RDFSerializer {

    final static Logger logger = LoggerFactory.getLogger(RDFSerializer.class);

    public OfferingCategory allOfferingCategories(Model m) {
        ResIterator it = m.listSubjectsWithProperty(RDF.type, BIGIOT.OfferingCategory);

            Resource res = m.getResource(BIGIOT.RootCategory.getURI());
            logger.debug("find root offering {}",res.getURI());
            // hierarchy category root
            return findOfferingCategory(res.getURI(), m).get();

    }

    public Option<OfferingCategory> findOfferingCategory(String uri, Model m) {
        uri = m.expandPrefix(uri);
        if(!m.containsResource(ResourceFactory.createResource(uri))){
            logger.debug("category {} does not exists",uri);
            return Option.empty();
        }
        boolean isDeprecated = false;

        Resource res = m.getResource(uri);
        boolean isProposed = false;

        String name = uri;
        if (res.hasProperty(RDFS.label)) {
            name = res.getProperty(RDFS.label).getObject().asLiteral().getLexicalForm();
        }

        if(res.hasProperty(RDF.type,BIGIOT.ProposedOfferingCategory))
            isProposed = true;

        if(res.hasProperty(BIGIOT.isDeprecated)) {
            isDeprecated = res.getProperty(BIGIOT.isDeprecated).getBoolean();
        }
        RdfAnnotation parent = new RdfAnnotation(uri, name, isProposed, isDeprecated);

        ArrayList<OfferingCategory> c = new ArrayList<OfferingCategory>();
        StmtIterator it = res.listProperties(SKOS.narrower);
        while (it.hasNext()) {
            Resource childRes = it.next().getResource();
            Option<OfferingCategory> child = findOfferingCategory(childRes.getURI(), m);
            if(child.isDefined())
                c.add(child.get());
        }
        List<OfferingCategory> children = JavaConverters.asScalaBuffer(c).toList();

        ArrayList<RdfAnnotation> t = new ArrayList<RdfAnnotation>();
        it = res.listProperties(BIGIOT.expectedAnnotation);
        while (it.hasNext()) {
            String typeUri = it.next().getResource().getURI();
            RdfAnnotation type = findRdfAnnotation(typeUri, m);
            if (type!=null) t.add(type);
        }
        List<RdfAnnotation> types = JavaConverters.asScalaBuffer(t).toList();

        OfferingCategory category = new OfferingCategory(parent, children, types, types);

        return new Some(category);
    }

    public List<RdfAnnotation> allRdfAnnotations(Model m) {
        ArrayList<RdfAnnotation> types = new ArrayList<RdfAnnotation>();

        ResIterator it = m.listResourcesWithProperty(RDF.type, BIGIOT.DatatypeAnnotation);
        while (it.hasNext()) {
            String uri = it.next().getURI();
            RdfAnnotation type = findRdfAnnotation(uri, m);
            types.add(type);
        }

        return JavaConverters.asScalaBuffer(types).toList();
    }

    public RdfAnnotation findRdfAnnotation(String uri, Model m) {
        uri = m.expandPrefix(uri);
        boolean isProposed = false;
        boolean isDeprecated = false;

        if(!m.containsResource(ResourceFactory.createResource(uri))){
            logger.debug("adding new proposed data type {}", uri);
            isProposed = true;
        }
        Resource res = m.getResource(uri);
        String label = uri;
        if (res.hasProperty(RDFS.label)) {
            label = res.getProperty(RDFS.label).getObject().asLiteral().getLexicalForm();
        }
        if(res.hasProperty(RDF.type,BIGIOT.ProposedDatatypeAnnotation))
            isProposed = true;

        if(res.hasProperty(BIGIOT.isDeprecated)) {
            isDeprecated = res.getProperty(BIGIOT.isDeprecated).getBoolean();
        }

        return new RdfAnnotation(uri, label, isProposed, isDeprecated); // TODO fix deprecated property
    }

    public Option<DataField> findDataField(String uri, Model m) {
        uri = m.expandPrefix(uri);
        boolean isProposed = false;
        boolean isDeprecated = false;

        if(!m.containsResource(ResourceFactory.createResource(uri))){
            logger.debug("adding new proposed data type {}", uri);
            isProposed = true;
        }
        Resource res = m.getResource(uri);
        String label = uri;
        if (res.hasProperty(RDFS.label)) {
            label = res.getProperty(RDFS.label).getObject().asLiteral().getLexicalForm();
        }
        if(res.hasProperty(RDF.type,BIGIOT.ProposedDatatypeAnnotation))
            isProposed = true;

        if(res.hasProperty(BIGIOT.isDeprecated)) {
            isDeprecated = res.getProperty(BIGIOT.isDeprecated).getBoolean();
        }

        RdfAnnotation rdfAnnotation = new RdfAnnotation(uri,label, isProposed, isDeprecated); // TODO fix deprecated property
        Resource valueTypeRes = null;
        ValueType valueType  = new TextType();
        ArrayList<DataField> arr = new ArrayList<>();

        try {
            if (res.hasProperty(SCHEMA.rangeIncludes)) {
                valueTypeRes = res.getProperty(SCHEMA.rangeIncludes).getObject().asResource();
                logger.info("value type of {} is {}",res.getURI(),valueTypeRes.getURI());
            }
            if (valueTypeRes.getURI().equals(SCHEMA.INTEGER.getURI()))
                valueType = new IntegerType(Option.empty(), Option.empty());
            else if (valueTypeRes.getURI().equals(SCHEMA.TEXT.getURI()))
                valueType = new TextType();
            else if (valueTypeRes.getURI().equals(SCHEMA.NUMBER.getURI()))
                valueType = new NumberType(Option.empty(), Option.empty());
            else if (valueTypeRes.getURI().equals(SCHEMA.DATETIME.getURI()))
                valueType = new DateTimeType(Option.empty(), Option.empty());
            else if (valueTypeRes.getURI().equals(SCHEMA.BOOLEAN.getURI()))
                valueType = new BooleanType();
            else {
                ArrayList<DataField> c = new ArrayList();
                StmtIterator it = res.listProperties(BIGIOT.hasMember);
                while (it.hasNext()) {
                    Resource childRes = it.next().getResource();
                    Option<DataField> child = findDataField(childRes.getURI(), m);
                    if(child.isDefined())
                        c.add(child.get());
                }
                List<DataField> children = JavaConverters.asScalaBuffer(c).toList();
                if(c.isEmpty())
                    valueType = new TextType();
                else
                    valueType = new ObjectType(children);
//                valueType = new ObjectType(JavaConverters.asScalaBuffer(arr).toList());
            }
        }catch (Exception e){
            logger.error("can not retrieve complex type for :" + res.getURI(),e.getMessage());
            valueType = new TextType();
        }

        return new Some(new DataField(label, rdfAnnotation,valueType, "",false));
//        return new Some(new DataField(label, rdfAnnotation,new TextType(), "",false));
    }


    public Option<Organization> findOrganization(String id, Model m) {
        Option<Organization> opt = Option.empty();

        Literal l = m.createTypedLiteral(id);
        ResIterator it = m.listResourcesWithProperty(BIGIOT.organizationId, l);
        if (it.hasNext()) {
            Resource res = it.next();
            String name = res.getProperty(SCHEMA.name).getObject().asLiteral().getLexicalForm();

            Option<RdfContext> ctxOpt = Option.empty(); // TODO
            Option<License> licenseOpt = findLicense(res);
            Option<Price> priceOpt = findPrice(res);

            List<Provider> providers = nil(Provider.class);
            List<Consumer> consumers =  nil(Consumer.class);

            Organization org = new Organization(new OrganizationId(id), name, providers, consumers);
            opt = new Some<Organization>(org);
        }

        return opt;
    }


    public Option<OfferingQuery> findOfferingQuery(String id, Model m) {
        Option<OfferingQuery> opt = Option.empty();

        Literal l = m.createTypedLiteral(id);
        ResIterator it = m.listResourcesWithProperty(BIGIOT.queryId, l);
        if (it.hasNext()) {
            Resource res = it.next();
            String name = res.getProperty(SCHEMA.name).getObject().asLiteral().getLexicalForm();

            Option<Consumer> consumerOpt = Option.empty();

            Option<RdfAnnotation> catOpt = findCategory(res);
            Option<RdfContext> ctxOpt = Option.empty();

            List<DataField> input = findInputData(res);
            List<DataField> output = findOutputData(res);

            Option<SpatialExtent> spatialExtentOpt = findSpatialExtent(res);
            Option<TemporalExtent> temporalExtentOpt = findTemporalExtent(res);

            Option<License> licenseOpt = findLicense(res);
            Option<Price> priceOpt = findPrice(res);

            List<QueryToOfferingSubscription> subs = nil(QueryToOfferingSubscription.class);

            OfferingQuery query = new OfferingQuery(new OfferingQueryId(id), name, consumerOpt, ctxOpt, catOpt, output, input,
                    spatialExtentOpt, temporalExtentOpt, licenseOpt, priceOpt, subs);
            opt = new Some<OfferingQuery>(query);
        }

        return opt;
    }

    private Option<RdfAnnotation> findCategory(Resource res) {
        Option<RdfAnnotation> opt = Option.empty();

        if (res.hasProperty(SCHEMA.category)) {
            Resource category = res.getPropertyResourceValue(SCHEMA.category);
            String uri = category.getURI();

            String name = uri;
            if (category.hasProperty(RDFS.label)) {
                name = category.getProperty(RDFS.label).getObject().asLiteral().getLexicalForm();
            }

            RdfAnnotation type = new RdfAnnotation(uri, name, true, false); // TODO fix proposed & deprecated properties
            opt = new Some(type);
        }

        return opt;
    }

    private List<DataField> findInputData(Resource res) {
        ArrayList<DataField> fields = new ArrayList<DataField>();

        StmtIterator it = res.listProperties(BIGIOT.hasInput);
        while (it.hasNext()) {
            Resource dataRes = it.next().getObject().asResource();

            String name = dataRes.getProperty(SCHEMA.name).getObject().asLiteral().getLexicalForm();
            RdfAnnotation type = findRdfAnnotation(dataRes);

            Resource valueTypeRes = null;
            ValueType valueType  = new UndefinedType();
            ArrayList<DataField> arr = new ArrayList<>();

            try {
                if (dataRes.hasProperty(BIGIOT.value)) {
                    Resource value = dataRes.getProperty(BIGIOT.value).getObject().asResource();
                    valueTypeRes = value.getProperty(BIGIOT.valueType).getObject().asResource();
                }
                if(valueTypeRes == null)
                    valueType  = new UndefinedType();
                else if (valueTypeRes.getURI().equals(SCHEMA.INTEGER.getURI()))
                    valueType = new IntegerType(Option.empty(), Option.empty());
                else if (valueTypeRes.getURI().equals(SCHEMA.TEXT.getURI()))
                    valueType = new TextType();
                else if (valueTypeRes.getURI().equals(SCHEMA.NUMBER.getURI()))
                    valueType = new NumberType(Option.empty(), Option.empty());
                else if (valueTypeRes.getURI().equals(SCHEMA.DATETIME.getURI()))
                    valueType = new DateTimeType(Option.empty(), Option.empty());
                else if (valueTypeRes.getURI().equals(SCHEMA.BOOLEAN.getURI()))
                    valueType = new BooleanType();
                else if (valueTypeRes.getURI().equals(SCHEMA.OBJECT.getURI()))
                    valueType = new ObjectType(JavaConverters.asScalaBuffer(arr).toList());
            }catch (Exception e){
                logger.error("can not retrieve value type:",e.getMessage());
                valueType  = new UndefinedType();
            }

            DataField data = new DataField(name, type, valueType, "", false);
//            DataField data = new DataField(name, type, new NumberType(Option.empty(), Option.empty()), "", false); // TODO: fix value type
            fields.add(data);
        }

        return JavaConverters.asScalaBuffer(fields).toList();
    }

    private List<DataField> findOutputData(Resource res) {
        ArrayList<DataField> fields = new ArrayList<DataField>();

        StmtIterator it = res.listProperties(BIGIOT.hasOutput);
        while (it.hasNext()) {
            Resource dataRes = it.next().getObject().asResource();

            String name = dataRes.getProperty(SCHEMA.name).getObject().asLiteral().getLexicalForm();
            RdfAnnotation type = findRdfAnnotation(dataRes);

            Resource valueTypeRes = null;
            ValueType valueType  = new UndefinedType();
            ArrayList<DataField> arr = new ArrayList<>();

            try {
                if (dataRes.hasProperty(BIGIOT.value)) {
                    Resource value = dataRes.getProperty(BIGIOT.value).getObject().asResource();
                    valueTypeRes = value.getProperty(BIGIOT.valueType).getObject().asResource();
                }
                if(valueTypeRes == null)
                    valueType  = new UndefinedType();
                else if (valueTypeRes.getURI().equals(SCHEMA.INTEGER.getURI()))
                    valueType = new IntegerType(Option.empty(), Option.empty());
                else if (valueTypeRes.getURI().equals(SCHEMA.TEXT.getURI()))
                    valueType = new TextType();
                else if (valueTypeRes.getURI().equals(SCHEMA.NUMBER.getURI()))
                    valueType = new NumberType(Option.empty(), Option.empty());
                else if (valueTypeRes.getURI().equals(SCHEMA.DATETIME.getURI()))
                    valueType = new DateTimeType(Option.empty(), Option.empty());
                else if (valueTypeRes.getURI().equals(SCHEMA.BOOLEAN.getURI()))
                    valueType = new BooleanType();
                else if (valueTypeRes.getURI().equals(SCHEMA.OBJECT.getURI()))
                    valueType = new ObjectType(JavaConverters.asScalaBuffer(arr).toList());
            }catch (Exception e){
                logger.error("can not retrieve value type:",e.getMessage());
                valueType  = new UndefinedType();
            }

            DataField data = new DataField(name, type, valueType, "", false);
//            DataField data = new DataField(name, type, new NumberType(Option.empty(), Option.empty()), "", false); // TODO: fix value type
            fields.add(data);
        }

        return JavaConverters.asScalaBuffer(fields).toList();
    }

    private RdfAnnotation findRdfAnnotation(Resource res) {
        String uri = res.getPropertyResourceValue(BIGIOT.rdfAnnotation).getURI();
        String name = res.getProperty(SCHEMA.name).getObject().asLiteral().getLexicalForm();

        return new RdfAnnotation(uri, name, true, false); // TODO check proposed & deprecated properties
    }

    private Option<License> findLicense(Resource res) {
        Option<License> opt = Option.empty();

        if (res.hasProperty(SCHEMA.license)) {
            Statement label = res.getPropertyResourceValue(SCHEMA.license)
                                 .getPropertyResourceValue(BIGIOT.licenseType)
                                 .getProperty(RDFS.label);
            String val = label.getObject().asLiteral().getLexicalForm();
            License license = new License(val);
            opt = new Some(license);
        }

        return opt;
    }

    private Option<SpatialExtent> findSpatialExtent(Resource res) {
        Option<SpatialExtent> opt = Option.empty();

        if(res.hasProperty(SCHEMA.spatialCoverage)){
            Resource region = res.getPropertyResourceValue(SCHEMA.spatialCoverage);
            String city = region.getProperty(RDFS.label).getObject().asLiteral().getLexicalForm();
            Option<BoundingBox> boxOpt = Option.empty();
            logger.debug("check if bounding box is found");
            if(region.hasProperty(BIGIOT.LOWER_BOUND_LATITUDE)){
                double lower_lat = region.getProperty(BIGIOT.LOWER_BOUND_LATITUDE).getObject().asLiteral().getDouble();
                double lower_lng = region.getProperty(BIGIOT.LOWER_BOUND_LONGITUDE).getObject().asLiteral().getDouble();
                double upper_lat = region.getProperty(BIGIOT.UPPER_BOUND_LATITUDE).getObject().asLiteral().getDouble();
                double upper_lng = region.getProperty(BIGIOT.UPPER_BOUND_LONGITUDE).getObject().asLiteral().getDouble();
                BoundingBox box = new BoundingBox(new extent.Location(lower_lat,lower_lng),new extent.Location(upper_lat,upper_lng));
                boxOpt = new Some<BoundingBox>(box);
                logger.debug("bounding box is found:{}",boxOpt);
            }
            SpatialExtent spatialExtent = new SpatialExtent(city, boxOpt);
            opt = new Some(spatialExtent);
        }

        return opt;
    }

    private Option<TemporalExtent> findTemporalExtent(Resource res) {
        Option<TemporalExtent> opt = Option.empty();
        long from = -1, to = -1;
        // TODO handle temporal extent
        if(res.hasProperty(SCHEMA.validFrom))
            from = res.getProperty(SCHEMA.validFrom).getObject().asLiteral().getLong();
        if(res.hasProperty(SCHEMA.validThrough))
            to = res.getProperty(SCHEMA.validThrough).getObject().asLiteral().getLong();
        if(from>-1 && to>-1) {
            TemporalExtent temporalExtent = new TemporalExtent(new Some(from), new Some(to));
            opt = new Some(temporalExtent);
        }
        return opt;
    }

    private Option<Price> findPrice(Resource res) {
        Option<Price> opt = Option.empty();

        if (res.hasProperty(SCHEMA.priceSpecification)) {
            Resource spec = res.getPropertyResourceValue(SCHEMA.priceSpecification);

            Statement modelStmt = spec.getPropertyResourceValue(BIGIOT.pricingModel)
                                      .getProperty(RDFS.label);
            String modelLabel = modelStmt.getObject().asLiteral().getLexicalForm();
            PricingModel model = new PricingModel(modelLabel); // TODO use converters

            Option<Money> moneyOpt = Option.empty();
            if (!modelLabel.equals("FREE")) { // FIXME compare PricingModel objects
                double amountFloat = spec.getProperty(SCHEMA.price).getObject().asLiteral().getDouble();
                BigDecimal amount = new BigDecimal(new java.math.BigDecimal(amountFloat));

                String curLabel = spec.getProperty(SCHEMA.priceCurrency).getObject().asLiteral().getString();
                Currency currency = new Currency(curLabel);

                Money money = new Money(amount, currency);
                moneyOpt = new Some<Money>(money);
            }

            Price price = new Price(model, moneyOpt);
            opt = new Some(price);
        }

        return opt;
    }

    private <T> List<T> nil(Class<T> c) {
        return JavaConverters.asScalaBuffer(new ArrayList<T>()).toList();
    }


//    public static void main(String[] args){
//        RDFSerializer rdf = new RDFSerializer();
//        Model model = RDFDataMgr.loadModel("/Volumes/Data/Projects/BigIoT/source_code/bigiot/exchange/exchange-impl/src/main/resources/test.ttl");
//        RDFDataMgr.write(System.out, model, RDFFormat.TURTLE);
//        System.out.println(rdf.allOfferings(model));
//        System.out.println("hello");
//    }
}
