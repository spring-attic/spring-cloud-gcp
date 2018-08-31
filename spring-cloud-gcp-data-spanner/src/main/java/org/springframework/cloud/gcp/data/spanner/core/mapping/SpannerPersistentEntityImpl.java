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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a Cloud Spanner table and its columns' mapping to fields within an entity
 * type.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerPersistentEntityImpl<T>
		extends BasicPersistentEntity<T, SpannerPersistentProperty>
		implements SpannerPersistentEntity<T> {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private static final Pattern TABLE_NAME_ILLEGAL_CHAR_PATTERN = Pattern
			.compile("[^a-zA-Z0-9_]");

	private final Class rawType;

	private final Set<String> columnNames = new HashSet<>();

	private final Expression tableNameExpression;

	private final Table table;

	private final Map<Integer, SpannerPersistentProperty> primaryKeyParts = new HashMap<>();

	private final SpannerMappingContext spannerMappingContext;

	private StandardEvaluationContext context;

	private SpannerCompositeKeyProperty idProperty;

	private String tableName;

	/**
	 * Creates a {@link SpannerPersistentEntityImpl}
	 * @param information type information about the underlying entity type.
	 */
	public SpannerPersistentEntityImpl(TypeInformation<T> information) {
		this(information, new SpannerMappingContext());
	}

	/**
	 * Creates a {@link SpannerPersistentEntityImpl}
	 * @param information type information about the underlying entity type.
	 * @param spannerMappingContext a mapping context that can be used to create
	 * persistent entities from properties of this entity
	 */
	public SpannerPersistentEntityImpl(TypeInformation<T> information,
			SpannerMappingContext spannerMappingContext) {
		super(information);

		Assert.notNull(spannerMappingContext,
				"A valid SpannerMappingContext is required.");
		this.spannerMappingContext = spannerMappingContext;

		this.rawType = information.getType();

		this.context = new StandardEvaluationContext();

		this.table = this.findAnnotation(Table.class);
		this.tableNameExpression = detectExpression();
	}

	protected boolean hasAnnotatedTableName() {
		return this.table != null && StringUtils.hasText(this.table.name());
	}

	@Nullable
	private Expression detectExpression() {
		if (!hasAnnotatedTableName()) {
			return null;
		}

		Expression expression = PARSER.parseExpression(this.table.name(),
				ParserContext.TEMPLATE_EXPRESSION);

		return expression instanceof LiteralExpression ? null : expression;
	}

	@Override
	public void addPersistentProperty(SpannerPersistentProperty property) {
		if (!property.isMapped()) {
			return;
		}
		addPersistentPropertyToPersistentEntity(property);

		if (property.isEmbedded()) {
			this.columnNames.addAll(this.spannerMappingContext
					.getPersistentEntity(property.getType()).columns());
		}
		else if (!property.isInterleaved()) {
			this.columnNames.add(property.getColumnName());
		}

		if (property.getPrimaryKeyOrder() != null
				&& property.getPrimaryKeyOrder().isPresent()) {
			int order = property.getPrimaryKeyOrder().getAsInt();
			if (this.primaryKeyParts.containsKey(order)) {
				throw new SpannerDataException(
						"Two properties were annotated with the same primary key order: "
								+ property.getColumnName() + " and "
								+ this.primaryKeyParts.get(order).getColumnName()
								+ " in " + getType().getSimpleName() + ".");
			}
			this.primaryKeyParts.put(order, property);
		}
	}

	private void addPersistentPropertyToPersistentEntity(
			SpannerPersistentProperty property) {
		super.addPersistentProperty(property);
	}

	@Override
	public SpannerCompositeKeyProperty getIdProperty() {
		return this.idProperty;
	}

	@Override
	public void doWithInterleavedProperties(
			PropertyHandler<SpannerPersistentProperty> handler) {
		doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					if (spannerPersistentProperty.isInterleaved()) {
						handler.doWithPersistentProperty(spannerPersistentProperty);
					}
				});
	}

	@Override
	public void doWithColumnBackedProperties(
			PropertyHandler<SpannerPersistentProperty> handler) {
		doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					if (!spannerPersistentProperty.isInterleaved()) {
						handler.doWithPersistentProperty(spannerPersistentProperty);
					}
				});
	}

	@Override
	public boolean hasIdProperty() {
		return this.idProperty != null;
	}

	@Override
	public void verify() {
		super.verify();
		verifyPrimaryKeysConsecutive();
		verifyInterleavedProperties();
		verifyEmbeddedColumnNameOverlap(new HashSet<>(), this);
	}

	private void verifyInterleavedProperties() {
		doWithInterleavedProperties(spannerPersistentProperty -> {
			// getting the inner type will throw an exception if the property isn't a
			// collection.
			Class childType = spannerPersistentProperty.getColumnInnerType();
			SpannerPersistentEntityImpl childEntity = (SpannerPersistentEntityImpl)
					this.spannerMappingContext.getPersistentEntity(childType);
			List<SpannerPersistentProperty> primaryKeyProperties = getFlattenedPrimaryKeyProperties();
			List<SpannerPersistentProperty> childKeyProperties = childEntity
					.getFlattenedPrimaryKeyProperties();
			if (primaryKeyProperties.size() >= childKeyProperties.size()) {
				throw new SpannerDataException(
						"A child table (" + childEntity.getType().getSimpleName() + ")"
								+ " must contain the primary key columns of its "
								+ "parent (" + childEntity.getType().getSimpleName() + ")"
								+ " in the same order starting the first "
								+ "column with additional key columns after.");

			}
			for (int i = 0; i < primaryKeyProperties.size(); i++) {
				SpannerPersistentProperty parentKey = primaryKeyProperties.get(i);
				SpannerPersistentProperty childKey = childKeyProperties.get(i);
				if (!parentKey.getColumnName().equals(childKey.getColumnName())
						|| !parentKey.getType().equals(childKey.getType())) {
					throw new SpannerDataException(
							"The child primary key column ("
									+ childEntity.getType().getSimpleName() + "." + childKey.getColumnName()
									+ ") at position " + (i + 1)
									+ " does not match that of its parent ("
									+ getType().getSimpleName() + "." + parentKey.getColumnName() + ").");
				}
			}
		});
	}

	private void verifyEmbeddedColumnNameOverlap(Set<String> seen,
			SpannerPersistentEntity spannerPersistentEntity) {
		spannerPersistentEntity.doWithColumnBackedProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					if (spannerPersistentProperty.isEmbedded()) {
						if (ConversionUtils.isIterableNonByteArrayType(
								spannerPersistentProperty.getType())) {
							throw new SpannerDataException(
									"Embedded properties cannot be collections: "
											+ spannerPersistentProperty);
						}
						verifyEmbeddedColumnNameOverlap(seen,
								this.spannerMappingContext.getPersistentEntity(
										spannerPersistentProperty.getType()));
					}
					else {
						String columnName = spannerPersistentProperty.getColumnName();
						if (seen.contains(columnName)) {
							throw new SpannerDataException(
									"Two properties resolve to the same column name: "
											+ columnName + " in " + getType().getSimpleName());
						}
						seen.add(columnName);
					}
				});
	}

	private void verifyPrimaryKeysConsecutive() {
		for (int i = 1; i <= this.primaryKeyParts.size(); i++) {
			SpannerPersistentProperty keyPart = this.primaryKeyParts.get(i);
			if (keyPart == null) {
				throw new SpannerDataException(
						"The primary key columns were not given a consecutive order. "
								+ "There is no property annotated with order "
								+ i + " in " + this.getType().getSimpleName() + ".");
			}
		}
		this.idProperty = new SpannerCompositeKeyProperty(this, getPrimaryKeyProperties());
	}

	@Override
	public SpannerPersistentProperty[] getPrimaryKeyProperties() {
		SpannerPersistentProperty[] primaryKeyColumns = new SpannerPersistentProperty[this.primaryKeyParts
				.size()];
		for (int i = 1; i <= this.primaryKeyParts.size(); i++) {
			primaryKeyColumns[i - 1] = this.primaryKeyParts.get(i);
		}
		return primaryKeyColumns;
	}

	@Override
	public List<SpannerPersistentProperty> getFlattenedPrimaryKeyProperties() {
		List<SpannerPersistentProperty> primaryKeyColumns = new ArrayList<>();
		for (SpannerPersistentProperty property : getPrimaryKeyProperties()) {
			if (property.isEmbedded()) {
				primaryKeyColumns
						.addAll(((SpannerPersistentEntityImpl) this.spannerMappingContext
								.getPersistentEntity(property.getType()))
										.getFlattenedPrimaryKeyProperties());
			}
			else {
				primaryKeyColumns.add(property);
			}
		}
		return primaryKeyColumns;
	}

	@Override
	public SpannerMappingContext getSpannerMappingContext() {
		return this.spannerMappingContext;
	}

	@Override
	public String tableName() {
		if (this.tableName == null) {
			if (this.hasAnnotatedTableName()) {
				try {
					this.tableName = validateTableName(
							this.tableNameExpression == null ? this.table.name()
									: this.tableNameExpression.getValue(this.context,
											String.class));
				}
				catch (RuntimeException e) {
					throw new SpannerDataException(
							"Error getting table name for " + getType().getSimpleName(),
							e);
				}
			}
			else {
				this.tableName = StringUtils.uncapitalize(this.rawType.getSimpleName());
			}
		}
		return this.tableName;
	}

	// Because SpEL expressions in table name definitions are allowed, validation is
	// required.
	private String validateTableName(String name) {
		if (TABLE_NAME_ILLEGAL_CHAR_PATTERN.matcher(name).find()) {
			throw new SpannerDataException("Only letters, numbers, and underscores are "
					+ "allowed in table names: " + name);
		}
		return name;
	}

	@Override
	public Set<String> columns() {
		return Collections.unmodifiableSet(this.columnNames);
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context.addPropertyAccessor(new BeanFactoryAccessor());
		this.context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		this.context.setRootObject(applicationContext);
	}

	@Override
	public PersistentPropertyAccessor getPropertyAccessor(Object object) {
		PersistentPropertyAccessor delegatedAccessor = super.getPropertyAccessor(object);
		return new PersistentPropertyAccessor() {

			@Override
			public void setProperty(PersistentProperty property,
					@Nullable Object value) {
				if (property.isIdProperty()) {
					throw new SpannerDataException(
							"Setting the primary key directly via the Key ID property is not supported. "
									+ "Please set the underlying column properties.");
				}
				else {
					delegatedAccessor.setProperty(property, value);
				}
			}

			@Nullable
			@Override
			public Object getProperty(PersistentProperty property) {
				if (property.isIdProperty()) {
					return ((SpannerCompositeKeyProperty) property).getId(getBean());
				}
				else {
					return delegatedAccessor.getProperty(property);
				}
			}

			@Override
			public Object getBean() {
				return delegatedAccessor.getBean();
			}
		};
	}
}
