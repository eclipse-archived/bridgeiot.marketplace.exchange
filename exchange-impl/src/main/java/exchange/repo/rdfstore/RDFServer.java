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

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import exchange.model.ApplicationModelGenerator;
import exchange.model.vocabs.BIGIOT;

import exchange.repo.rdfstore.rulebuiltins.SimpleTypeChecker;
import exchange.repo.rdfstore.rulebuiltins.ValueTypeMatcher;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.BuiltinRegistry;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.web.HttpOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtuoso.jena.driver.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static exchange.repo.rdfstore.QueryFactory.*;

public class RDFServer {

    final static Logger logger = LoggerFactory.getLogger(RDFServer.class);

    private String DB_USERNAME ;
    private String DB_PASS ;
    private String endpointURL ;
    private String JDBC_URL ;
    private String coreModelURL, envModelURL, commonModelURL, schemaURL, mobilityModelURL;

    private static RDFServer instance = null;

    public static Model coreModel, domainModel, exModel, environmentModel, schemaModel, commonModel;

    public static InfModel inf;

    public static RDFServer get() throws Exception {
        if (instance == null) {
            instance = new RDFServer();
        }
        return instance;
    }

    private RDFServer() throws Exception{
        this.JDBC_URL = System.getenv("RDF_JDBC");
        this.DB_USERNAME = System.getenv("RDF_USERNAME");
        this.DB_PASS = System.getenv("RDF_PASS");
        this.endpointURL = System.getenv("RDF_ENDPOINT");

        this.coreModelURL = System.getenv("CORE_MODEL");
        this.envModelURL = System.getenv("ENVIRONMENT_MODEL");
        this.commonModelURL = System.getenv("COMMON_MODEL");
        this.mobilityModelURL = System.getenv("MOBILITY_MODEL");
        this.schemaURL = System.getenv("SCHEMA");

        QueryFactory.setOfferingGraph(System.getenv("OFFERING_GRAPH"));
        QueryFactory.setOntologyGraph(System.getenv("MODEL_GRAPH"));
        logger.info("Connecting to RDF repo:{}", this.DB_USERNAME + "," + this.DB_PASS + "," + this.endpointURL);
        init();
    }

    public String getEndpoint() {
        return endpointURL;
    }

    private void init(){
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        Credentials credentials = new UsernamePasswordCredentials(DB_USERNAME, DB_PASS);
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        HttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpOp.setDefaultHttpClient(httpclient);

        initModel();

        //create offering graph if not exsits
        String askQuery = "ASK FROM <" + getOfferingGraph()
                            + "> WHERE { ?s ?p ?o. }";
        logger.info("check if offering graph exits");
        try {
            if(!executeASKQuery(askQuery)){
                logger.info("creating offering graph");
                executeUpdateQuery(createGraph(getOfferingGraph()));
                String initDataQuery = "INSERT DATA { GRAPH <" + getOfferingGraph() + "> {<test1> <test2> <test3>.}}";
                executeUpdateQuery(initDataQuery);
            }

            logger.info("check if ontology graph exsits");
            askQuery = "ASK FROM <" + getOntologyGraph() + "> WHERE { ?s ?p ?o. }";
            //create ontology graph
            if(!executeASKQuery(askQuery)){
                logger.info("creating ontology graph");
                executeUpdateQuery(createGraph(getOntologyGraph()));

                logger.info("loading bigiot core model to ontology graph");
                executeUpdateQuery(inf, getOntologyGraph());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initModel(){
    	
    	registerBuiltins();
    	
        logger.info("initialising bigiot models");
        coreModel = getModelFromURL(this.coreModelURL);

        exModel = getModelFromURL(this.mobilityModelURL);
        coreModel.add(exModel);

        environmentModel = getModelFromURL(this.envModelURL);
        coreModel.add(environmentModel);

        commonModel = getModelFromURL(this.commonModelURL);
        coreModel.add(commonModel);

        schemaModel = getModelFromURL(this.schemaURL);
        coreModel.add(schemaModel);

        // generated from domain models + core
        domainModel = ApplicationModelGenerator.generate(coreModel);
        coreModel.add(domainModel);

        logger.info("loading bigiot categories rules");
        List<Rule> rules = Rule.parseRules(BIGIOT.APPLICATION_INFERRED_RULES);
        Reasoner engine = new GenericRuleReasoner(rules);
        inf = ModelFactory.createInfModel(engine, coreModel);
        inf.rebind();
        clearGraph(getOntologyGraph());
        executeUpdateQuery(inf, getOntologyGraph());
    }

	private void registerBuiltins() {
		BuiltinRegistry.theRegistry.register(new SimpleTypeChecker());
    	BuiltinRegistry.theRegistry.register(new ValueTypeMatcher());
	}


    public Model getModelFromURL(String url){
        Model model = null ;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .header("Accept", "text/turtle")
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();

            String content = response.body().string();
            model = ModelFactory.createDefaultModel() ;
            model.read(new ByteArrayInputStream(content.getBytes()),url,"turtle");
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("error {}",e.getMessage());
        }finally {

        }
        return model;
    }

    public Model executeConstructQuery(String query) {
        Model constructModel = ModelFactory.createDefaultModel();
        try {
            String logStr = query;
            logStr = logStr.replace("\n", "").replace("\r", "");

            logger.info("executing query :{}", logStr);
            long startTime = System.nanoTime();

            VirtGraph set = new VirtGraph (JDBC_URL, DB_USERNAME, DB_PASS);
            VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (query, set);
            constructModel = vqe.execConstruct();

            long finishTime = System.nanoTime();
            double time = (finishTime - startTime) / 1.0e6;
            logger.info(String.format("FINISH - %.2fms", time));
            logger.info("Construct query result size:{}", constructModel.size());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("error {}",e.getMessage());
        }
        return constructModel;
    }

    public Model executeConstructQuery(Query query) {
        Model constructModel = ModelFactory.createDefaultModel();
        try {
            String logStr = query.toString();
            logStr = logStr.replace("\n", "").replace("\r", "");

            logger.info("executing query:{}", logStr);
            long startTime = System.nanoTime();

            VirtGraph set = new VirtGraph (JDBC_URL, DB_USERNAME, DB_PASS);
            VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (query, set);
            constructModel = vqe.execConstruct();

            long finishTime = System.nanoTime();
            double time = (finishTime - startTime) / 1.0e6;
            logger.info(String.format("FINISH - %.2fms", time));
            vqe.close();

            logger.info("Construct query result size:{}", constructModel.size());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("error {}",e.getMessage());
        }
        return constructModel;
    }

    public VirtuosoQueryExecution executeSelectQuery(String queryStr) {
        String logStr = queryStr;
        logStr = logStr.replace("\n", "").replace("\r", "");

        logger.info("executing query select query:{}", logStr);
        long startTime = System.nanoTime();
        VirtuosoQueryExecution qexec = null;
        try {

            VirtGraph graph = new VirtGraph(JDBC_URL, DB_USERNAME, DB_PASS);
//            Query sparql = QueryFactory.create(queryStr);
            qexec = VirtuosoQueryExecutionFactory.create (queryStr, graph);

            long finishTime = System.nanoTime();
            double time = (finishTime - startTime) / 1.0e6;
            logger.info(String.format("FINISH - %.2fms", time));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("error {}",e.getMessage());
        }
        return qexec;
    }

    public void executeUpdateQuery(String query) {
        String logStr = query;
        logStr = logStr.replace("\n", "").replace("\r", "");

        logger.info("executing update query to {} {}", exchange.repo.rdfstore.QueryFactory.getOfferingGraph(), logStr);
        long startTime = System.nanoTime();

        VirtGraph graph = new VirtGraph(JDBC_URL, DB_USERNAME, DB_PASS);
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, graph);
        vur.exec();
        graph.close();
    }

    public void executeUpdateQuery(Model model) {
        logger.info("Updating model....");
        long startTime = System.nanoTime();
        if(model ==null){
            logger.debug("updated model is empty");
            return;
        }

        VirtModel vm = VirtModel.openDatabaseModel(getOfferingGraph(), JDBC_URL, DB_USERNAME, DB_PASS);
        vm.add(model.listStatements().toList());
        vm.close();
        long finishTime = System.nanoTime();
        double time = (finishTime - startTime) / 1.0e6;
        logger.info(String.format("Model update finished: %.2fms", time));
    }

    public void executeUpdateQuery(Model model, String graph) {
        logger.info("Insert model to graph {}",graph);
        if(model ==null){
            logger.debug("model is empty");
            return;
        }
        VirtModel vm = VirtModel.openDatabaseModel(graph, JDBC_URL, DB_USERNAME, DB_PASS);
        vm.add(model.listStatements().toList());
        vm.close();
    }

    public void clearGraph(String graph) {
        logger.info("clear graph {}",graph);
        VirtModel vm = VirtModel.openDatabaseModel(graph, JDBC_URL, DB_USERNAME, DB_PASS);
        vm.removeAll();
        vm.close();
    }

    public boolean executeASKQuery(String query){
        QueryExecution qexec = null;

        logger.info("executing ask query {}", query);
        long startTime = System.nanoTime();
        try {
            qexec = QueryExecutionFactory.sparqlService(endpointURL, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean rs = qexec.execAsk();
        logger.info(String.format("ASK query result: {}",rs));
        return  rs;
    }

    private String envOrElse(String key, String defaultValue) {
        if (System.getenv().containsKey(key)) {
            return System.getenv(key);
        } else {
            return defaultValue;
        }
    }


}
