/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.test;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Helper class to resolve table name from the SpEL expression, simulating an
 * applicationContext containing a bean definition of <code>tablePostfix</code>.
 * <p>
 * For example {@link Trade} has the expression
 * <code>#{'trades_'.concat(tablePostfix)}</code>
 * </p>
 * @author Balint Pato
 */
public class TablePostfixResolver {

	public static final ExpressionParser PARSER = new SpelExpressionParser();

	public static String getTableName(Class<?> tradeClass, String tablePostfix) {
		String exp = tradeClass.getAnnotation(SpannerTable.class).name();
		Expression expression = PARSER.parseExpression(exp, ParserContext.TEMPLATE_EXPRESSION);
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(
				new TablePostfixContextRoot(tablePostfix));
		return expression.getValue(evaluationContext, String.class);
	}

	private static final class TablePostfixContextRoot {
		public final String tablePostfix;

		private TablePostfixContextRoot(String tablePostfix) {
			this.tablePostfix = tablePostfix;
		}
	}

}
