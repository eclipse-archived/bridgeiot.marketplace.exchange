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
package exchange.persistence

import org.scalatest.{FlatSpec, Matchers}

class EventSerializerTest extends FlatSpec with Matchers {
  "ProviderEventSerializer" should "decode ProviderCreated (with no field to rename)" in {
    val serializer = new ProviderEventSerializer
    val rawEvent = """{
     |   "ProviderCreated" : {
     |     "id" : "Atos-p1",
     |     "organizationId" : "Atos",
     |     "name" : "p1",
     |     "secret" : "DK8FgdRDSeu8d6uzbaoHfw==",
     |     "meta" : {
     |       "requesterId" : "github_1234",
     |       "organizationId" : "Atos",
     |       "requestId" : "9b3eb290_278a_44cf_8476_e3be6f3d2549",
     |       "time" : 1511171584000,
     |       "requestTime" : 1511171584000,
     |       "delay" : 0,
     |       "finished" : true
     |     }
     |   }
     | }""".stripMargin.getBytes

    val event = serializer.fromBinary(rawEvent, "ProviderEvent")
  }

  "ProviderEventSerializer" should "decode OfferingAdded (with fields to rename)" in {
    val serializer = new ProviderEventSerializer
    val rawEvent = """{
     |   "OfferingAdded":{
     |     "id":"Atos-p1",
     |     "name":"o1",
     |     "localId":null,
     |     "rdfUri":"bigiot:Parking",
     |     "rdfContext":null,
     |     "endpoints":[],
     |     "outputData":[{"name":"","rdfUri":"datex:parkingNumberOfVacantSpaces"}],
     |     "inputData":[{"name":"","rdfUri":"schema:latitude"},{"name":"","rdfUri":"schema:longitude"}],
     |     "spatialExtent":{"city":""},
     |     "license":"CREATIVE_COMMONS",
     |     "price":{"pricingModel":"FREE","money":null},
     |     "activation":{"status":false,"expirationTime":0},
     |     "meta":{
     |       "requesterId":"github_1234",
     |       "organizationId":"Atos",
     |       "requestId":"5316af43_2833_438e_be29_e6cb825edf6f",
     |       "time":1511171612000,
     |       "requestTime":1511171612000,
     |       "delay":1,
     |       "finished":true
     |     }
     |   }
     | }""".stripMargin.getBytes

    val event = serializer.fromBinary(rawEvent, "ProviderEvent")
  }

  "OfferingEventSerializer" should "decode ChangeOfferingOutputData (with event name and fields to rename)" in {
    val serializer = new OfferingEventSerializer
    val rawEvent = """{
     |   "OfferingOutputDataChanged":{
     |     "id":"Atos-p1-o1",
     |     "outputData":[{"name":"", "rdfAnnotation":{"uri":"schema:latitude", "label":"Latitude"}, "value":{"type":"number"}},
     |                   {"name":"", "rdfAnnotation":{"uri":"schema:longitude", "label":"Longitude"}, "value":{"type":"number"}}],
     |     "meta":{
     |       "requesterId":"github_1234",
     |       "organizationId":"Atos",
     |       "requestId":"5316af43_2833_438e_be29_e6cb825edf6f",
     |       "time":1511171612000,
     |       "requestTime":1511171612000,
     |       "delay":1,
     |       "finished":true
     |     }
     |   }
     | }""".stripMargin.getBytes

    val event = serializer.fromBinary(rawEvent, "OfferingEvent")
  }

}
