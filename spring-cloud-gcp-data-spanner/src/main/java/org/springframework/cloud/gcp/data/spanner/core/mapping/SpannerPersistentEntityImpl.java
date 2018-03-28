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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Represents a Google Spanner table and its columns' mapping to fields within an entity
 * type.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerPersistentEntityImpl<T>
		extends BasicPersistentEntity<T, SpannerPersistentProperty>
		implements SpannerPersistentEntity<T> {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private final String tableName;

	private final Set<String> columnNames = new HashSet<>();

	private final Expression tableNameExpression;

	private StandardEvaluationContext context;

	private final Table table;

	/**
	 * Creates a {@link SpannerPersistentEntityImpl}
	 * @param information type information about the underlying entity type.
	 */
	public SpannerPersistentEntityImpl(TypeInformation<T> information) {
		super(information);

		Class<?> rawType = information.getType();
		String fallback = StringUtils.uncapitalize(rawType.getSimpleName());

		this.context = new StandardEvaluationContext();

		this.table = this.findAnnotation(Table.class);
		this.tableName = this.hasTableName() ? this.table.name() : fallback;
		this.tableNameExpression = detectExpression();
	}

	protected boolean hasTableName() {
		return this.table != null && StringUtils.hasText(this.table.name());
	}

	@Nullable
	private Expression detectExpression() {
		if (!hasTableName()) {
			return null;
		}

		Expression expression = PARSER.parseExpression(this.table.name(), ParserContext.TEMPLATE_EXPRESSION);

		return expression instanceof LiteralExpression ? null : expression;
	}

	@Override
	public void addPersistentProperty(SpannerPersistentProperty property) {
		addPersistentPropertyToPersistentEntity(property);
		this.columnNames.add(property.getColumnName());
	}

	private void addPersistentPropertyToPersistentEntity(SpannerPersistentProperty property) {
		super.addPersistentProperty(property);
	}

	@Override
	public String tableName() {
		return this.tableNameExpression == null
				? this.tableName
				: this.tableNameExpression.getValue(this.context, String.class);
	}

	@Override
	public Iterable<String> columns() {
		return Collections.unmodifiableSet(this.columnNames);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context.addPropertyAccessor(new BeanFactoryAccessor());
		this.context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		this.context.setRootObject(applicationContext);
	}

}
