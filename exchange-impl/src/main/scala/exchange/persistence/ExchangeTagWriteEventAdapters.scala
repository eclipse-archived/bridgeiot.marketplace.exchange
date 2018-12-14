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

import microservice.persistence.TagWriteEventAdapter

import exchange.api.consumer.ConsumerEvent
import exchange.api.offering.OfferingEvent
import exchange.api.offeringquery.OfferingQueryEvent
import exchange.api.organization.OrganizationEvent
import exchange.api.provider.ProviderEvent
import exchange.api.semantics.SemanticsEvent
import exchange.api.subscription.SubscriptionEvent
import exchange.model._
import exchange.server.Exchange

class SemanticsTagWriteEventAdapter extends TagWriteEventAdapter[SemanticsEvent](OfferingCategory.tag, Exchange.tag)
class OrganizationTagWriteEventAdapter extends TagWriteEventAdapter[OrganizationEvent](Organization.tag, Exchange.tag)
class ProviderTagWriteEventAdapter extends TagWriteEventAdapter[ProviderEvent](Provider.tag, Exchange.tag)
class ConsumerTagWriteEventAdapter extends TagWriteEventAdapter[ConsumerEvent](Consumer.tag, Exchange.tag)
class OfferingTagWriteEventAdapter extends TagWriteEventAdapter[OfferingEvent](Offering.tag, Exchange.tag)
class OfferingQueryTagWriteEventAdapter extends TagWriteEventAdapter[OfferingQueryEvent](OfferingQuery.tag, Exchange.tag)
class SubscriptionTagWriteEventAdapter extends TagWriteEventAdapter[SubscriptionEvent](Subscription.tag, Exchange.tag)
