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

import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import com.google.cloud.spanner.Type.Code;
import com.google.spanner.v1.TypeCode;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * Represents an implementation for {@link SpannerPersistentProperty}, which is a property
 * of a {@link SpannerPersistentEntity} stored in a Cloud Spanner table.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerPersistentPropertyImpl
		extends AnnotationBasedPersistentProperty<SpannerPersistentProperty>
		implements SpannerPersistentProperty {

	private FieldNamingStrategy fieldNamingStrategy;

	/**
	 * Creates a new {@link SpannerPersistentPropertyImpl}
	 *
	 * @param property the property to store
	 * @param owner the entity to which this property belongs
	 * @param simpleTypeHolder the type holder
	 * @param fieldNamingStrategy the naming strategy used to get the column name of this
	 * property
	 */
	SpannerPersistentPropertyImpl(Property property,
			PersistentEntity<?, SpannerPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder, FieldNamingStrategy fieldNamingStrategy) {
		super(property, owner, simpleTypeHolder);
		this.fieldNamingStrategy = fieldNamingStrategy == null
				? PropertyNameFieldNamingStrategy.INSTANCE
				: fieldNamingStrategy;
	}

	/**
	 * Only provides types that are also annotated with {@link Table}.
	 */
	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypes() {
		return StreamUtils
				.createStreamFromIterator(super.getPersistentEntityTypes().iterator())
				.filter(typeInfo -> typeInfo.getType().isAnnotationPresent(Table.class))
				.collect(Collectors.toList());
	}

	@Override
	protected Association<SpannerPersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	/**
	 * Gets the name of the column in the Cloud Spanner table mapped to this property. The
	 * column name is resolved using the {@link FieldNamingStrategy} passed in to the
	 * {@link SpannerPersistentPropertyImpl#SpannerPersistentPropertyImpl(Property, PersistentEntity,
	 * SimpleTypeHolder, FieldNamingStrategy)}
	 * constructor. This is by default the by default
	 *
	 * @return the name of the column.
	 * @throws MappingException if the resolution fails
	 */
	@Override
	public String getColumnName() {
		if (isInterleaved()) {
			throw new SpannerDataException(
					"This property is a one-to-many child collection and "
							+ "does not correspond to a column: " + getName());
		}
		if (StringUtils.hasText(getAnnotatedColumnName())) {
			return getAnnotatedColumnName();
		}

		String fieldName = this.fieldNamingStrategy.getFieldName(this);

		if (!StringUtils.hasText(fieldName)) {
			throw new MappingException(String.format(
					"Invalid (null or empty) field name returned for property %s by %s!",
					this, this.fieldNamingStrategy.getClass()));
		}

		return fieldName;
	}

	@Override
	public Class<?> getColumnInnerType() {
		TypeInformation<?> ti = getTypeInformation();
		List<TypeInformation<?>> typeParams = ti.getTypeArguments();
		if (typeParams.size() != 1) {
			throw new SpannerDataException("in field '" + getColumnName()
					+ "': Unsupported number of type parameters found: " + typeParams.size()
					+ " Only collections of exactly 1 type parameter are supported.");
		}
		return typeParams.get(0).getType();
	}

	@Override
	public OptionalInt getPrimaryKeyOrder() {
		PrimaryKey annotation = findAnnotation(
				PrimaryKey.class);
		if (annotation == null) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(annotation.keyOrder());
	}

	@Override
	public boolean isMapped() {
		return findAnnotation(NotMapped.class) == null;
	}

	@Override
	public boolean isEmbedded() {
		return findAnnotation(Embedded.class) != null;
	}

	@Override
	public boolean isInterleaved() {
		return findAnnotation(Interleaved.class) != null;
	}

	@Override
	public OptionalLong getMaxColumnLength() {
		Column annotation = findAnnotation(Column.class);
		if (annotation == null || annotation.spannerTypeMaxLength() < 0) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(annotation.spannerTypeMaxLength());
	}

	@Override
	public boolean isGenerateSchemaNotNull() {
		Column annotation = findAnnotation(Column.class);
		return annotation != null && !annotation.nullable();
	}

	@Override
	public Code getAnnotatedColumnItemType() {
		Column annotation = findAnnotation(Column.class);
		if (annotation == null
				|| annotation.spannerType() == TypeCode.TYPE_CODE_UNSPECIFIED) {
			return null;
		}
		return Code.valueOf(annotation.spannerType().name());
	}

	@Override
	public boolean isIdProperty() {
		return false;
	}

	private String getAnnotatedColumnName() {

		Column annotation = findAnnotation(Column.class);

		if (annotation != null && StringUtils.hasText(annotation.name())) {
			return annotation.name();
		}

		return null;
	}
}
