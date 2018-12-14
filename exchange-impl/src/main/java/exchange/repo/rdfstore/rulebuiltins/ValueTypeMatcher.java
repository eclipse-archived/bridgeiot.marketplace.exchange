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
package exchange.repo.rdfstore.rulebuiltins;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.BindingEnvironment;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

public class ValueTypeMatcher extends BaseBuiltin {

	@Override
	public String getName() {
		return "valueType";
	}

	@Override
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		return setValueType(args, length, context);
	}

	@Override
	public void headAction(Node[] args, int length, RuleContext context) {
		setValueType(args, length, context);
	}

	protected boolean setValueType(Node[] args, int length, RuleContext context) {
		checkArgs(length, context);
		BindingEnvironment env = context.getEnv();

		Node rangeType = getArg(0, args, context);

		return (env.bind(args[1], getValueType(rangeType)));

	}

	protected Node getValueType(Node rangeType) {
		switch (rangeType.getURI()) {
		case "http://schema.org/Boolean":
			return NodeFactory.createURI("http://schema.big-iot.org/core/BooleanSchema");
		case "http://schema.org/Date":
		case "http://schema.org/DateTime":
		case "http://schema.org/Time":
			return NodeFactory.createURI("http://schema.big-iot.org/core/DateTimeSchema");
		case "http://schema.org/Number":
		case "http://schema.org/Integer":
		case "http://schema.org/Float":
			return NodeFactory.createURI("http://schema.big-iot.org/core/NumberSchema");
		case "http://schema.org/Text":
		case "http://schema.org/URL":
			return NodeFactory.createURI("http://schema.big-iot.org/core/StringSchema");
		default:
			return NodeFactory.createURI("http://schema.big-iot.org/core/ObjectSchema");
		}
		//TODO invalid input handling
		//TODO array schema handling
	}

}
