# Bridge.IoT Exchange
The Exchange is the central component of the Bridge.IoT Marketplace. 

Offering Providers register Offerings with semantic description to the Marketplace so that Offering Consumers can query for and subscribe to matching Offerings.

To start the Exchange server enter

    sbt run

The Exchange server opens an endpoint on http://localhost:8080/graphql. It accepts queries and mutations defined by [GraphQL](http://graphql.org).

## Interactive Exploration of Exchange API
[GraphiQL](https://github.com/graphql/graphiql) is an in-browser IDE for exploring GraphQL APIs. For the Exchange you can access it by opening 
http://localhost:8080 in a browser. Please use auto-completion (ctrl-space) in the left panel to show and describe all allowed queries and mutations with all
parameters. Any Errors (invalid queries or mutation) are underlined in red and when you hover your mouse over it you get a description of the error(s). 

To execute a query or mutation you can use the play button in the top left (and select the query or mutation if you have defined multiple) or postion the 
cursur in the query or mutation you want to execute and press ctrl-enter.

## API documentation
When you click _Docs_ in the top right of GraphiQL you can browse the full (generated) documentation for the Exchange API. All queries, mutations with parameters and 
return types are described there. Parameters and return types with exclamation mark "!" are mandatory, all others are optional. 
On http://localhost:8080/schema you can get the full API definition in one page as described on [GraphQL Type System](http://graphql.org/docs/typesystem).

## GraphQL Queries
Here are some examples of the queries you can make:
```javascript
query q1 {
  allOfferings { 
    offerings {
      id
      name
    }
  }
}
```
this gives back a list of all registered offerings with id and name:
```json
{
  "data": {
    "allOfferings": {
      "offerings": [
        {
          "id": "Barcelona_City-provider3-offering3",
          "name": "Barcelona Parking Sensors"
        },
        {
          "id": "CSI-provider1-offering2",
          "name": "Montalto Dora Parking Sensors"
        },
        {
          "id": "CSI-provider1-offering1",
          "name": "Montalto Dora Traffic counter"
        }
      ]
    }
  }
}
```
Please note that all GraphQL Query results are wrapped at least twice:
* an object always called _data_
* an object named by the query name
* if the query returns a list of items then it is wrapped in an additional object (called _offerings_ in this example).

To see all registered Organzations, Providers, Consumers, Offerings and OfferingQueries you have to enter this query:
```javascript
query all { allOrganizations { organisations {
  id name license {licenseType {name}} price {accountingModel {name} money {amount currency {name}}}
  providers {id name offerings {id name rdfType {uri} price {accountingModel {name} money {amount currency {name}}}}}
  consumers {id name queries {id name rdfType {uri}}}
}}}
```

## GraphQL Mutations
Here is an example for a mutation to create a new Offering on the Exchange:

```javascript
mutation createOffering {
  createOffering(input: {
    providerId:"Barcelona_City-provider3"
    localId:"newOffering"
    name:"name" 
    rdfUri: "bigiot:Parking"
    licenseType:"CREATIVE_COMMONS"
    price: {pricingModel: "PER_ACCESS" money:{amount:10 currency:"EUR"}}
  }) {offering { id name } }
}
```

The result should be something like this:
```json
{
  "data": {
    "createOffering": {
      "offering": {
        "id": "Barcelona_City-provider3-newOffering",
        "name": "name"
      }
    }
  }
}
```
The ids are created in a hierarchical way, that is by combining the parent id and the child id with a separator "-" in between. The child id
for an entity to be created is either given explicitly as "localId" or calculated from the name (by replacing spaces and special characters with "_"). If neither
localId nor name is given a unique id is created.

This example shows how to create a new OfferingQuery:
```javascript
mutation createOfferingQuery {
  createOfferingQuery(input: {
    consumerId:"Barcelona_City-Example_Service"
    name:"Smart Parking"
    rdfUri: "bigiot:Parking"
  }) {offeringQuery { id name } }
}
```

## Using the GraphQL endpoint
Here is an example query by accessing the http://localhost:8080/graphql endpoint using curl:
```
curl -X POST localhost:8080/graphql -H "Content-Type:application/json" -d "{\"query\": \"{ allOfferings { offerings {name} } } \"}"
```
