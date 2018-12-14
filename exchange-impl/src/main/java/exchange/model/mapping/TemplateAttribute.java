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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.vocabulary.XSD;

public final class TemplateAttribute {

    private final String name;
    private final String type;
    private final String container;

    public TemplateAttribute(String name) {
        URI uri;
        try {
            uri = new URI(name);
        } catch (URISyntaxException e) {
            this.name = null;
            this.type = null;
            this.container = null;
            return;
        }

        String fullPath = uri.getPath();
        this.name = fullPath.substring(fullPath.lastIndexOf("/") + 1);
        Map<String, String> params = split(uri.getQuery());

        if (params.containsKey("type")) {
            if (params.get("type").equals("id")) {
                this.type = "@id";
            } else {
                this.type = XSD.getURI() + params.get("type");
            }
        } else {
            this.type = null;
        }

        if (params.containsKey("container") && params.get("container").equals("set")) {
            this.container = "@set";
        } else if (params.containsKey("container") && params.get("container").equals("set")) {
            this.container = "@list";
        } else {
            this.container = null;
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getContainer() {
        return container;
    }

    public boolean hasType() {
        return type != null;
    }

    public boolean definesContainer() {
        return container != null;
    }

    private Map<String, String> split(String query) {
        Map<String, String> params = new HashMap<String, String>();

        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=");
                params.put(kv[0], kv[1]);
            }
        }

        return params;
    }

}
