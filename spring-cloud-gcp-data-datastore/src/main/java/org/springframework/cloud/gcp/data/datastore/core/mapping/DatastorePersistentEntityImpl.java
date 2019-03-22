/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.type.filter.AssignableTypeFilter;
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
import org.springframework.util.StringUtils;

/**
 * Metadata class for entities stored in Datastore.
 * @param <T> the type of the persistent entity
 *
 * @author Chengyuan Zhao
 * @since 1.1
 */
public class DatastorePersistentEntityImpl<T>
		extends BasicPersistentEntity<T, DatastorePersistentProperty>
		implements DatastorePersistentEntity<T> {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private final Expression kindNameExpression;

	private final String classBasedKindName;

	private final Entity kind;

	private final DiscriminatorField discriminatorField;

	private final DiscriminatorValue discriminatorValue;

	private final DatastoreMappingContext datastoreMappingContext;

	private StandardEvaluationContext context;

	/**
	 * Constructor.
	 * @param information type information about the underlying entity type.
	 * @param datastoreMappingContext a mapping context used to get metadata for related
	 *     persistent entities.
	 */
	public DatastorePersistentEntityImpl(TypeInformation<T> information,
			DatastoreMappingContext datastoreMappingContext) {
		super(information);

		Class<?> rawType = information.getType();

		this.datastoreMappingContext = datastoreMappingContext;
		this.context = new StandardEvaluationContext();
		this.kind = findAnnotation(Entity.class);
		this.discriminatorField = findAnnotation(DiscriminatorField.class);
		this.discriminatorValue = findAnnotation(DiscriminatorValue.class);
		this.classBasedKindName = this.hasTableName() ? this.kind.name()
				: StringUtils.uncapitalize(rawType.getSimpleName());
		this.kindNameExpression = detectExpression();
	}

	protected boolean hasTableName() {
		return this.kind != null && StringUtils.hasText(this.kind.name());
	}

	@Nullable
	private Expression detectExpression() {
		if (!hasTableName()) {
			return null;
		}

		Expression expression = PARSER.parseExpression(this.kind.name(),
				ParserContext.TEMPLATE_EXPRESSION);

		return (expression instanceof LiteralExpression) ? null : expression;
	}

	@Override
	public String kindName() {
		if (this.discriminatorValue != null && this.discriminatorField == null) {
			throw new DatastoreDataException(
					"This class expects a discrimination field but none are designated: " + getType());
		}
		else if (this.discriminatorValue != null && getType().getSuperclass() != Object.class) {
			return ((DatastorePersistentEntityImpl) (this.datastoreMappingContext
					.getPersistentEntity(getType().getSuperclass()))).getDiscriminationSuperclassPersistentEntity()
							.kindName();
		}
		return (this.kindNameExpression == null) ? this.classBasedKindName
				: this.kindNameExpression.getValue(this.context, String.class);
	}

	@Override
	public DatastorePersistentProperty getIdPropertyOrFail() {
		if (!hasIdProperty()) {
			throw new DatastoreDataException(
					"An ID property was required but does not exist for the type: "
							+ getType());
		}
		return getIdProperty();
	}

	@Override
	public void verify() {
		super.verify();
		initializeSubclassEntities();
		addEntityToDiscriminationFamily();
		checkDiscriminationValues();
	}

	private void checkDiscriminationValues() {
		Set<Class> otherMembers = DatastoreMappingContext.getDiscriminationFamily(getType());
		if (otherMembers != null) {
			for (Class other : otherMembers) {
				DatastorePersistentEntity persistentEntity = this.datastoreMappingContext.getPersistentEntity(other);
				if (getDiscriminatorValue() != null
						&& getDiscriminatorValue().equals(persistentEntity.getDiscriminatorValue())) {
					throw new DatastoreDataException(
							"More than one class in an inheritance hierarchy has the same DiscriminatorValue: "
									+ getType() + " and " + other);
				}
			}
		}

	}

	private void addEntityToDiscriminationFamily() {
		Class parentClass = getType().getSuperclass();
		DatastorePersistentEntity parentEntity = parentClass != Object.class
				? this.datastoreMappingContext.getPersistentEntity(parentClass)
				: null;
		if (parentEntity != null && parentEntity.getDiscriminationFieldName() != null) {
			if (!parentEntity.getDiscriminationFieldName().equals(getDiscriminationFieldName())) {
				throw new DatastoreDataException(
						"This class and its super class both have discrimination fields but they are different fields: "
								+ getType() + " and " + parentClass);
			}
			DatastoreMappingContext.addDiscriminationClassConnection(parentClass, getType());
		}
	}

	@Override
	public String getDiscriminationFieldName() {
		return this.discriminatorField == null ? null : this.discriminatorField.field();
	}

	@Override
	public List<String> getCompatibleDiscriminationValues() {
		if (this.discriminatorValue == null) {
			return Collections.emptyList();
		}
		else {
			List<String> compatibleValues = new LinkedList<>();
			compatibleValues.add(this.discriminatorValue.value());
			DatastorePersistentEntity<?> persistentEntity = this.datastoreMappingContext
					.getPersistentEntity(getType().getSuperclass());
			if (persistentEntity != null) {
				List<String> compatibleDiscriminationValues = persistentEntity.getCompatibleDiscriminationValues();
				compatibleValues.addAll(compatibleDiscriminationValues);
			}
			return compatibleValues;
		}
	}

	@Override
	public String getDiscriminatorValue() {
		return this.discriminatorValue == null ? null : this.discriminatorValue.value();
	}

	@Override
	public void doWithColumnBackedProperties(
			PropertyHandler<DatastorePersistentProperty> handler) {
		doWithProperties(
				(PropertyHandler<DatastorePersistentProperty>) (datastorePersistentProperty) -> {
					if (datastorePersistentProperty.isColumnBacked()) {
						handler.doWithPersistentProperty(datastorePersistentProperty);
					}
				});
	}

	@Override
	public void doWithDescendantProperties(
			PropertyHandler<DatastorePersistentProperty> handler) {
		doWithProperties(
				(PropertyHandler<DatastorePersistentProperty>) (datastorePersistentProperty) -> {
					if (datastorePersistentProperty.isDescendants()) {
						handler.doWithPersistentProperty(datastorePersistentProperty);
					}
				});
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context.addPropertyAccessor(new BeanFactoryAccessor());
		this.context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		this.context.setRootObject(applicationContext);
	}

	/* This method is used by subclass persistent entities to get the superclass Kind name. */
	private DatastorePersistentEntity getDiscriminationSuperclassPersistentEntity() {
		if (this.discriminatorField != null) {
			return this;
		}
		return ((DatastorePersistentEntityImpl) (this.datastoreMappingContext
				.getPersistentEntity(getType().getSuperclass()))).getDiscriminationSuperclassPersistentEntity();
	}

	private void initializeSubclassEntities() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(getType()));
		for (BeanDefinition component : provider.findCandidateComponents(getType().getPackage().getName())) {
			try {
				this.datastoreMappingContext.getPersistentEntity(Class.forName(component.getBeanClassName()));
			}
			catch (ClassNotFoundException ex) {
				throw new DatastoreDataException("Could not find expected subclass for this entity: " + getType(), ex);
			}
		}
	}
}
