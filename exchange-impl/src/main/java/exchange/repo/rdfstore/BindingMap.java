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

import java.util.HashMap;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;

public class BindingMap extends HashMap<String, RDFNode> {

    public BindingMap withLiteral(String key, String value) {
        Literal l = ResourceFactory.createTypedLiteral(value);
        this.put(key, l);
        return this;
    }

    public BindingMap withResource(String key, String uri) {
        Resource r = ResourceFactory.createResource(RDFUtils.prefixModel().expandPrefix(uri));
        this.put(key, r);
        return this;
    }

}
