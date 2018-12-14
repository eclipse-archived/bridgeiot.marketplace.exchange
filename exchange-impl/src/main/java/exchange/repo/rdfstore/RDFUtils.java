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

import com.github.jsonldjava.core.RDFDataset;
import exchange.api.semantics.DataField;
import exchange.model.vocabs.PREFIXES;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;

import java.lang.reflect.Array;
import java.util.*;

import static exchange.repo.rdfstore.RDFServer.*;

class RDFUtils {
    private static RDFServer rdfServer;

    public static OntModel prefixModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    private static Model categoryOnlyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    private static Model allOfferingCategoryModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    private static Model allDataTypeAnnotationModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    static {
        try {
            rdfServer = get();
        } catch (Exception e) {

        }
        prefixModel.setNsPrefixes(PREFIXES.getPrefixSet());
        updateAllOfferingCategoriesModel();
        updateCategoryModel();
        updateDataTypeAnnotationModel();
    }

    public static Model toJenaModel(RDFDataset dataset, String graphName) {
        Model m = ModelFactory.createDefaultModel();

        for (RDFDataset.Quad triple : dataset.getQuads(graphName)) {
            // TODO handle blank nodes
            Resource s = ResourceFactory.createResource(triple.getSubject().getValue());
            Property p = ResourceFactory.createProperty(triple.getPredicate().getValue());
            RDFNode o = triple.getObject().isIRI() ?
                    ResourceFactory.createResource(triple.getObject().getValue()) :
                    ResourceFactory.createPlainLiteral(triple.getObject().getValue());
            Statement t = ResourceFactory.createStatement(s, p, o);
            m.add(t);
        }

        return m;
    }

    public static RDFDataset toJsonLdDataset(Model m) {
        RDFDataset object = new RDFDataset();

        StmtIterator it = m.listStatements();
        while (it.hasNext()) {
            Statement t = it.next();
            String subjectString = t.getSubject().toString();
            String predicateString = t.getPredicate().toString();
            if (t.getObject().isLiteral()) {
                Literal lit = t.getObject().asLiteral();
                object.addTriple(subjectString, predicateString,
                        lit.getValue().toString(),
                        lit.getDatatypeURI().isEmpty() ? null : lit.getDatatypeURI(),
                        lit.getLanguage().isEmpty() ? null : lit.getLanguage());
            } else {
                String objectString = t.getObject().toString();
                object.addTriple(subjectString, predicateString, objectString);
            }
        }

        return object;
    }

    /**
     * removes triples that point to orphan blank nodes
     */
    public static void pruneModel(Model m) {
        List<Statement> toRemove = new ArrayList<Statement>();

        StmtIterator it = m.listStatements();
        while (it.hasNext()) {
            Statement t = it.next();
            if (t.getObject().isAnon()) {
                List<Statement> desc = t.getObject().asResource().listProperties().toList();
                if (desc.isEmpty()) {
                    toRemove.add(t);
                }
            }
        }

        m.remove(toRemove);
    }

    private static Model allOfferingCategoriesModel(boolean isProposed) {
        String q = QueryFactory.allOfferingCategories(isProposed);
        Model m = rdfServer.executeConstructQuery(q);
        m.setNsPrefixes(PREFIXES.getPrefixSet());
        allOfferingCategoryModel = m;
        return m;
    }

    private static Model allDataTypeAnnotationsModel() {
        String q = QueryFactory.getDataTypeAnnotationTree();
        Model m = rdfServer.executeConstructQuery(q);
        m.setNsPrefixes(PREFIXES.getPrefixSet());
        allDataTypeAnnotationModel = m;
        return m;
    }

    public static Model prefixModel() {
        return prefixModel;
    }

    private static Model categoriesTreeOnlyModel(boolean isProposed) {
        String q = QueryFactory.getCategoriesTree(isProposed);
        Model m = rdfServer.executeConstructQuery(q);
        m.setNsPrefixes(PREFIXES.getPrefixSet());
        categoryOnlyModel = m;
        return m;
    }

    public static Model getCategoryOnlyModel() {
        return categoryOnlyModel;
    }

    public static Model getAllDataTypeAnnotationModel() {
        return allDataTypeAnnotationModel;
    }

    public static void updateCategoryModel(){
        categoryOnlyModel = categoriesTreeOnlyModel(true);
    }

    public static void updateAllOfferingCategoriesModel(){
        allOfferingCategoryModel = allOfferingCategoriesModel(true);
    }

    public static void updateDataTypeAnnotationModel(){
        allDataTypeAnnotationModel = allDataTypeAnnotationsModel();
    }

    public static Model getAllOfferingCategoryModel(){return allOfferingCategoryModel;}

    public static RDFServer getRdfServer() {
        return rdfServer;
    }

    public static List<DataField> flatten(DataField object) {
        return (List<DataField>) recursiveFlatten(object, true);
    }

    public static Set<DataField> uniqFlatten(DataField object) {
        return (Set<DataField>) recursiveFlatten(object, false);
    }

    private static Collection<DataField> recursiveFlatten(DataField object, Boolean allowDuplicates) {
        Collection<DataField> setOrList;
        if (allowDuplicates) {
            setOrList = new ArrayList<DataField>();
        } else {
            setOrList = new LinkedHashSet<DataField>();
        }
        if (object.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(object); i++) {
                setOrList.addAll(recursiveFlatten((DataField) Array.get(object, i), allowDuplicates));
            }
        } else if (object instanceof Map) {
            for (DataField element : ((Map<DataField, DataField>) object).values()) {
                setOrList.addAll(recursiveFlatten(element, allowDuplicates));
            }
        } else if (object instanceof Iterable) {
            for (DataField element : (Iterable<DataField>) object) {
                setOrList.addAll(recursiveFlatten(element, allowDuplicates));
            }
        } else {
            setOrList.add(object);
        }
        return setOrList;
    }
}
