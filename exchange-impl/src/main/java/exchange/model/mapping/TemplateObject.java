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
package exchange.model.mapping;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.graph.*;

public final class TemplateObject {

    private final String rdfClass;
    private final Object context;

    public TemplateObject(Template template) {
        this.rdfClass = findClass(template);
        this.context = buildContext(template);
    }

    public String getType() {
        return rdfClass;
    }

    public Object getContext() {
        return context;
    }

    private String findClass(Template template) {
        // TODO find the root instead: only var that is a subject and no object
        Iterator<Triple> it = template.getBGP().iterator();
        while (it.hasNext()) {
            Triple t = it.next();
            if (t.getPredicate().equals(RDF.type.asNode())) {
                return t.getObject().getURI();
            }
        }
        return null;
    }

    private Object buildContext(Template template) {
        Map<String, Object> ctx = new HashMap<String, Object>();

        Iterator<Triple> it = template.getBGP().iterator();
        while (it.hasNext()) {
            Triple t = it.next();
            TemplateAttribute var = new TemplateAttribute(t.getPredicate().getURI());
            Map<String, String> node = new HashMap<String, String>();
            node.put("@id", t.getPredicate().getURI());
            if (var.hasType()) {
                node.put("@type", var.getType());
            }
            if (var.definesContainer()) {
                node.put("@container", var.getContainer());
            }
            ctx.put(var.getName(), node);
        }

        return ctx;
    }

}
