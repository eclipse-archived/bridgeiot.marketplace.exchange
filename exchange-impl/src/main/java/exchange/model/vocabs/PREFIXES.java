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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.HashMap;
import java.util.Map;

public class PREFIXES {

    public static final String DATEX_NS     = "http://vocab.datex.org/terms#";
    public static final String SCHEMA_NS    = "http://schema.org/";
    public static final String TD_NS        = "http://www.w3c.org/wot/td#";
    public static final String BIGIOT_CORE_NS   = "http://schema.big-iot.org/core/";
    public static final String SKOS_NS     = "http://www.w3.org/2004/02/skos/core#";
    public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String XDS_NS = "http://www.w3.org/2001/XMLSchema#";
    public static final String OWL_NS = "https://www.w3.org/2002/07/owl#";
    public static final String BIGIOT_BASE_NS = "http://schema.big-iot.org/resources#";
    public static final String BIGIOT_MOBILITY_NS = "http://schema.big-iot.org/mobility/";

    public static final String DATEX     = "datex";
    public static final String SCHEMA    = "schema";
    public static final String TD        = "td";
    public static final String BIGIOT_CORE   = "bigiot-core";
    public static final String SKOS     = "skos";
    public static final String RDF = "rdf";
    public static final String RDFS = "rdfs";
    public static final String XDS = "xsd";
    public static final String OWL = "owl";
    public static final String BIGIOT_BASE   = "bigiot-base";
    public static final String BIGIOT_MOBILITY   = "bigiot-mobility";

    private static HashMap<String,String> PREFIX_MAP;
    private static Model prefixModel;

    static {
        PREFIX_MAP = new HashMap<String,String>();
        PREFIX_MAP.put(DATEX, DATEX_NS);
        PREFIX_MAP.put(SCHEMA, SCHEMA_NS);
        PREFIX_MAP.put(TD, TD_NS);
        PREFIX_MAP.put(BIGIOT_CORE, BIGIOT_CORE_NS);
        PREFIX_MAP.put(SKOS, SKOS_NS);
        PREFIX_MAP.put(RDF, RDF_NS);
        PREFIX_MAP.put(RDFS, RDFS_NS);
        PREFIX_MAP.put(XDS, XDS_NS);
        PREFIX_MAP.put(OWL, OWL_NS);
        PREFIX_MAP.put(BIGIOT_BASE, BIGIOT_BASE_NS);
        PREFIX_MAP.put(BIGIOT_MOBILITY, BIGIOT_MOBILITY_NS);

        prefixModel = ModelFactory.createDefaultModel();
        prefixModel.setNsPrefixes(PREFIX_MAP);

    }

    public static Model getPrefixModel(){
        return prefixModel;
    }
    public static HashMap<String,String> getPrefixSet(){
        return PREFIX_MAP;
    }

    public static String getPrefixNS(String prefix){
        return PREFIX_MAP.get(prefix);
    }

    public static String prefixesToString(){
        String prefixes = "";

        for (Map.Entry<String, String> entry : PREFIX_MAP.entrySet())
            prefixes += " PREFIX " + entry.getKey() + ": <" + entry.getValue() +">\n";

        return prefixes;
    }

}
