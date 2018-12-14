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

import java.util.Arrays;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDbinary;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.BindingEnvironment;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

public class SimpleTypeChecker extends BaseBuiltin {

	private List<String> simpleTypes = Arrays.asList(
			"http://schema.org/Boolean",
			"http://schema.org/Date", "http://schema.org/DateTime", "http://schema.org/Time",
			"http://schema.org/Number", "http://schema.org/Integer", "http://schema.org/Float",
			"http://schema.org/Text", "http://schema.org/URL");

	@Override
	public String getName() {
		return "isSimple";
	}

	@Override
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		return checkWhetherIsASimpleType(args, length, context);
	}
	
	@Override
	public void headAction(Node[] args, int length, RuleContext context) {
		checkWhetherIsASimpleType(args, length, context);
	}

	private boolean checkWhetherIsASimpleType(Node[] args, int length, RuleContext context) {
		checkArgs(length, context);
		BindingEnvironment env = context.getEnv();

		Node rangeType = getArg(0, args, context);
		
		if (rangeType.isURI() && simpleTypes.contains(rangeType.getURI())) {
			return (env.bind(args[1], NodeFactory.createLiteralByValue(true, XSDbinary.XSDboolean)));
		}

		return (env.bind(args[1], NodeFactory.createLiteralByValue(false, XSDbinary.XSDboolean)));
	}

}
