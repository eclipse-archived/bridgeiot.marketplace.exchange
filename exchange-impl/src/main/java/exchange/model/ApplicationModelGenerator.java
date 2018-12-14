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
package exchange.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import exchange.model.vocabs.PREFIXES;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import exchange.model.vocabs.BIGIOT;
import exchange.model.vocabs.SOSA;

public class ApplicationModelGenerator {
	
	public static final String NS = "urn:big-iot:";

	public static Model generate(Model domains) {
		Model app = ModelFactory.createDefaultModel();
		
		Resource root = getSKOSCategory("allOfferings", "all offerings (root category)", null, app);
		root.addProperty(RDF.type, BIGIOT.OfferingCategory);
		
		Map<OntClass, Resource> categoryMap = new HashMap<>();
		
		// to include the hierarchical closure of sosa:FeatureOfInterest
		OntModel inf = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF);
		inf.add(domains);
		
		ExtendedIterator<OntClass> it = inf.listClasses();
		while (it.hasNext()) {
			OntClass cl = it.next();
			
			if (cl.hasSuperClass(SOSA.FeatureOfInterest) && !cl.equals(SOSA.FeatureOfInterest)) {
				Resource cat = app.createResource(NS + cl.getLocalName() + "Category");
				cat.addProperty(RDF.type, SKOS.Concept);
				
				String label = cl.getLocalName();
				if (cl.hasProperty(RDFS.label)) {
					label = cl.getProperty(RDFS.label).getObject().asLiteral().getLexicalForm().trim();
				}
				cat.addProperty(RDFS.label, normalize(label));
				
				categoryMap.put(cl, cat);
			}
		}
		
		for (OntClass foi : categoryMap.keySet()) {
			ExtendedIterator<OntClass> scit = foi.listSuperClasses(true).filterKeep(sc -> sc.hasSuperClass(SOSA.FeatureOfInterest));
			Resource parent = categoryMap.getOrDefault(scit.next(), root);
			
			Resource cat = categoryMap.get(foi);
			
			app.add(parent, SKOS.narrower, cat);
			app.add(cat, BIGIOT.refersTo, foi);
		}
		
		return app;
	}
	
	private static Resource getSKOSCategory(String localName, String label, Resource superCategory, Model m) {
		Resource cat = m.createResource(NS + localName + "Category");
		cat.addProperty(RDFS.label, label);

		cat.addProperty(RDF.type, SKOS.Concept);
		
		if (superCategory != null) {
			cat.addProperty(SKOS.broader, superCategory);
		}
		
		return cat;
	}
	
	private static String normalize(String camelCase) {
		return camelCase.replaceAll("([A-Z])([a-z]+)", " $1$2").trim();
	}
	
}
