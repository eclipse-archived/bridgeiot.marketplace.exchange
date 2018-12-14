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
package exchange.model.vocabs;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class BIGIOT {

    public static final Resource Organization = ResourceFactory.createResource("http://schema.org/Organization");

    public static final Resource Provider = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "Provider");

    public static final Resource Consumer = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "Consumer");

    public static final Resource Offering = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "Offering");

    public static final Resource OfferingCategory = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "OfferingCategory");

    public static final Resource ProposedOfferingCategory = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "ProposedOfferingCategory");

    public static final Resource DatatypeAnnotation = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "DatatypeAnnotation");

    public static final Resource ProposedDatatypeAnnotation = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "ProposedDatatypeAnnotation");

    public static final Resource User = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "User");

    public static final Resource Currency = ResourceFactory.createResource("http://purl.org/linked-data/sdmx/2009/code#Currency");

    public static Resource OfferingQuery = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "OfferingQuery");

    public static Resource Subscription  = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "Subscription");

    public static Resource DataField  = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "Data");

    public static Resource DataValue  = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "DataValue");

    public static final Resource RootCategory = ResourceFactory.createResource("urn:big-iot:allOfferingsCategory");

    public static final Resource DATASCHEMA = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "DataSchema");

    public static final Resource OBJECTSCHEMA = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "ObjectSchema");

    //----------------------------Property--------------------------------------
    public static final Property organizationId = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "organizationId");

    public static final Property offeringId = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "offeringId");

    public static final Property providerId = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "providerId");

    public static final Property consumerId = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "consumerId");

    public static final Property queryId = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "queryId");

    public static final Property licenseType = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "licenseType");

    public static final Property pricingModel = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "pricingModel");

    public static final Property isProvidedBy = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "isProvidedBy");

    public static final Property offering = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "offering");

    public static final Property isActivated = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "isActivated");

    public static final Property expirationTime = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "offeringExpirationTime");

    public static final Property endpoint = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "endpoint");

    public static final Property endpointType = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "endpointType");

    public static final Property accessInterfaceType = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "accessInterfaceType");

    public static final Property rdfAnnotation = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "rdfAnnotation");

    public static final Property isRegisteredBy = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "isRegisteredBy");

    public static Property subscriptionId = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "subscriptionId");

    public static Property subscribeTo = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "subscribeTo");

    public static Property subscribedQuery = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "subscribedQuery");

    public static Property expectedAnnotation = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "expectedAnnotation");

    public static Property hasInput = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "hasInput");

    public static Property hasOutput = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "hasOutput");

    public static final Property refersTo = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "refersTo");

    public static final Property narrower = ResourceFactory.createProperty(PREFIXES.SKOS_NS + "narrower");

    public static final Property isAccessedBy = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "isAccessedBy");

    public static final Property value = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "value");

    public static final Property valueType = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "valueType");

    public static final Property hasMember = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "hasMember");

    public static final Property hasFlattenedInput = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "hasFlattenedInput");

    public static final Property hasFlattenedOutput = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "hasFlattenedOutput");

    public static final Property isDeprecated = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "isDeprecated");

    //----------------------------License type--------------------------------------

    public static final Resource CREATIVE_COMMONS = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "creative_commons");

    public static final Resource OPEN_DATA_LICENSE = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "open_data_license");

    public static final Resource NON_COMMERCIAL_DATA_LICENSE = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "non_commercial_data_lc");

    public static final Resource PROJECT_INTERNAL_USE_LICENSE = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "project_internal_use_only");

    //----------------------------Endpoint type--------------------------------------

    public static final Resource HTTP_GET = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "http_get");

    public static final Resource HTTP_POST = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "http_post");

    public static final Resource WEBSOCKET = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "endpoint_socket");

    //----------------------------Access interface type--------------------------------------

    public static final Resource BIGIOT_LIB = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "ac_bigiot_lib");

    public static final Resource EXTERNAL = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "ac_external_lib");

    //----------------------------Pricing model--------------------------------------

    public static final Resource PER_ACCESS = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "per_access_price");

    public static final Resource PER_MONTH = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "per_month_price");

    public static final Resource FREE = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "free_price");

    public static final Resource PER_BYTE = ResourceFactory.createResource(PREFIXES.BIGIOT_CORE_NS + "per_byte_price");

    //----------------------------Spatial Extent--------------------------------------

    public static final Property GEOMETRY = ResourceFactory.createProperty(Wgs84.NS + "geometry");

    public static final Property LOWER_BOUND_LATITUDE = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "lowerBoundLatitude");

    public static final Property LOWER_BOUND_LONGITUDE = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "lowerBoundLongitude");

    public static final Property UPPER_BOUND_LATITUDE = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "upperBoundLatitude");

    public static final Property UPPER_BOUND_LONGITUDE = ResourceFactory.createProperty(PREFIXES.BIGIOT_CORE_NS + "upperBoundLongitude");


    //----------------------------Reasoning rules--------------------------------------

    public static final String OFFERING_SUB_CATEGORY_RULES =
            "[offeringSubCategory_rules: (?offering rdf:type <" + PREFIXES.BIGIOT_CORE_NS + "Offering>) "
            +"(?offering <http://schema.org/category> ?category) "
            + "(?parentCategory <http://www.w3.org/2004/02/skos/core#narrower> ?category) "
            + "-> (?offering <http://schema.org/category> ?parentCategory) ]";

    public static final String OFFERING_FREE_PRICE_RULES = "[offeringFreePrice_rules: (?offering rdf:type <" + PREFIXES.BIGIOT_CORE_NS + "Offering>) "
            + "(?offering <http://schema.org/priceSpecification> ?priceSpec) "
            + "(?priceSpec <" + PREFIXES.BIGIOT_CORE_NS + "pricingModel> <"+ PREFIXES.BIGIOT_CORE_NS + "free_price>) "
            + "-> (?priceSpec <http://schema.org/priceCurrency> 'EUR') "
            + "(?priceSpec <http://schema.org/price> '0.0'^^http://www.w3.org/2001/XMLSchema#double)]";

    public static final String APPLICATION_INFERRED_RULES =
            "[application_inferred_rules: (?category <http://www.w3.org/2004/02/skos/core#narrower> ?subcategory),\n" +
                    "(?category rdf:type <http://schema.big-iot.org/core/OfferingCategory>)\n" +
                    "    -> (?subcategory rdf:type <http://schema.big-iot.org/core/OfferingCategory>) .\n" +
                    "\n" +
                    "(?category rdf:type <http://schema.big-iot.org/core/OfferingCategory>),\n" +
                    "(?category <http://schema.big-iot.org/core/refersTo> ?class),\n" +
                    "(?prop <http://schema.org/domainIncludes> ?class), \n" +
                    "(?prop <http://schema.org/rangeIncludes> ?range), \n" +
                    "    -> (?prop rdf:type <http://schema.big-iot.org/core/DatatypeAnnotation>),\n" +
                    "       (?category <http://schema.big-iot.org/core/expectedAnnotation> ?prop). \n" +
                    "\n" +
                    "(?category <http://schema.big-iot.org/core/expectedAnnotation> ?prop),\n" +
                    "(?prop rdf:type <http://schema.big-iot.org/core/DatatypeAnnotation>),\n" +
                    "(?prop <http://schema.org/rangeIncludes> ?class),\n" +
                    "(?otherprop <http://schema.org/domainIncludes> ?class)\n" +
                    "   -> (?otherprop rdf:type <http://schema.big-iot.org/core/DatatypeAnnotation>),\n" +
                    "      (?category <http://schema.big-iot.org/core/expectedAnnotation> ?otherprop) .\n" +
                    "\n" +
                    "(?prop rdf:type <http://schema.big-iot.org/core/DatatypeAnnotation>), \n" +
                    "(?prop <http://schema.org/rangeIncludes> ?range), \n" +
                    "isSimple(?range, ?simple), \n" +
//                    "valueType(?range, ?valueType) \n" +
                    "    -> (?prop <http://schema.big-iot.org/core/isSimpleValueType> ?simple). \n" +
//                    "       (?prop <http://schema.big-iot.org/core/valueType> ?valueType). \n" +
                    "\n" +
                    "(?prop rdf:type <http://schema.big-iot.org/core/DatatypeAnnotation>),\n" +
                    "(?prop <http://schema.org/rangeIncludes> ?range),\n" +
                    "(?member <http://schema.org/domainIncludes> ?range),\n" +
                    "(?member rdf:type <http://schema.big-iot.org/core/DatatypeAnnotation>),\n" +
                    "   -> (?prop <http://schema.big-iot.org/core/hasMember> ?member). \n"+
                    "\n" +
                    "(?supercategory <http://www.w3.org/2004/02/skos/core#narrower> ?category),\n" +
                    "(?category rdf:type <http://schema.big-iot.org/core/OfferingCategory>),\n" +
                    "(?supercategory <http://schema.big-iot.org/core/expectedAnnotation> ?prop)\n" +
                    "  -> (?category <http://schema.big-iot.org/core/expectedAnnotation> ?prop)]";

    public static final String PROPOSED_DATATYPE_SUB_CATEGORY_RULES =
            "[application_inferred_rules: (?supercategory <http://www.w3.org/2004/02/skos/core#narrower> ?category),\n" +
                    "(?category rdf:type <http://schema.big-iot.org/core/OfferingCategory>),\n" +
                    "(?supercategory <http://schema.big-iot.org/core/expectedAnnotation> ?prop)\n" +
                    "(?prop rdf:type <http://schema.big-iot.org/core/ProposedDatatypeAnnotation>),\n" +
                    "  -> (?category  <http://schema.big-iot.org/core/expectedAnnotation> ?prop)]";



    public static String getLicenseType(String lc) {
        if(lc.contains("CREATIVE_COMMONS")) {
            return CREATIVE_COMMONS.getURI();
        } else if(lc.contains("OPEN_DATA_LICENSE")) {
            return OPEN_DATA_LICENSE.getURI();
        } else if(lc.contains("NON_COMMERCIAL_DATA_LICENSE")) {
            return NON_COMMERCIAL_DATA_LICENSE.getURI();
        } else if(lc.contains("PROJECT_INTERNAL_USE_ONLY")) {
            return PROJECT_INTERNAL_USE_LICENSE.getURI();
        }
        return null;
    }

    public static String getPriceModel(String pm) {
        if(pm.contains("PER_ACCESS")) {
            return PER_ACCESS.getURI();
        } else if(pm.contains("PER_MONTH")) {
            return PER_MONTH.getURI();
        } else if(pm.contains("FREE")) {
            return FREE.getURI();
        } else if(pm.contains("PER_BYTE")) {
            return PER_BYTE.getURI();
        }
        return null;
    }

    public static String getEndpointType(String pm) {
        if(pm.contains("HTTP_GET")) {
            return HTTP_GET.getURI();
        } else if(pm.contentEquals("HTTP_POST")) {
            return HTTP_POST.getURI();
        } else if(pm.contentEquals("WEBSOCKET")) {
            return WEBSOCKET.getURI();
        }
        return null;
    }

    public static String getAccessInterfaceType(String pm) {
        if(pm.contains("BIGIOT_LIB")) {
            return BIGIOT_LIB.getURI();
        } else if(pm.contains("EXTERNAL")) {
            return EXTERNAL.getURI();
        }
        return null;
    }
}
