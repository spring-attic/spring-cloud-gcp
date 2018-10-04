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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Key.Builder;
import com.google.cloud.spanner.Type.Code;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Represents an persistent property just to represent Cloud Spanner primary keys, and does not
 * correspond to actual properties of POJOs, as it might even be a composite, multi-column key.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerCompositeKeyProperty implements SpannerPersistentProperty {

	private final SpannerPersistentEntity<?> spannerPersistentEntity;

	private final SpannerPersistentProperty[] primaryKeyColumns;

	public SpannerCompositeKeyProperty(SpannerPersistentEntity spannerPersistentEntity,
			SpannerPersistentProperty[] primaryKeyColumns) {
		Assert.notNull(spannerPersistentEntity,
				"A valid Cloud Spanner persistent entity is required.");
		Assert.notNull(primaryKeyColumns,
				"A valid array of primary key properties is required.");
		this.primaryKeyColumns = primaryKeyColumns;
		this.spannerPersistentEntity = spannerPersistentEntity;
	}

	Key getId(Object entity) {
		PersistentPropertyAccessor accessor = getOwner().getPropertyAccessor(entity);
		Builder keyBuilder = Key.newBuilder();
		for (SpannerPersistentProperty spannerPersistentProperty : this.primaryKeyColumns) {
			if (spannerPersistentProperty.isEmbedded()) {
				Key embeddedKeyParts = this.spannerPersistentEntity
						.getSpannerMappingContext()
						.getPersistentEntity(spannerPersistentProperty.getType())
						.getIdProperty()
						.getId(accessor.getProperty(spannerPersistentProperty));
				for (Object keyPart : embeddedKeyParts.getParts()) {
					keyBuilder.appendObject(keyPart);
				}
			}
			else {
				keyBuilder.appendObject(accessor.getProperty(spannerPersistentProperty));
			}
		}
		return keyBuilder.build();
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public Class getColumnInnerType() {
		return null;
	}

	@Override
	public OptionalInt getPrimaryKeyOrder() {
		return null;
	}

	@Override
	public boolean isMapped() {
		return false;
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}

	@Override
	public boolean isInterleaved() {
		return false;
	}

	@Override
	public boolean isGenerateSchemaNotNull() {
		return false;
	}

	@Override
	public Code getAnnotatedColumnItemType() {
		return null;
	}

	@Override
	public OptionalLong getMaxColumnLength() {
		return OptionalLong.empty();
	}

	@Override
	public PersistentEntity<?, SpannerPersistentProperty> getOwner() {
		return this.spannerPersistentEntity;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Class<?> getType() {
		return Key.class;
	}

	@Override
	public TypeInformation<?> getTypeInformation() {
		return ClassTypeInformation.from(getType());
	}

	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypes() {
		return Collections.emptySet();
	}

	@Nullable
	@Override
	public Method getGetter() {
		return null;
	}

	@Nullable
	@Override
	public Method getSetter() {
		return null;
	}

	@Nullable
	@Override
	public Field getField() {
		return null;
	}

	@Nullable
	@Override
	public String getSpelExpression() {
		return null;
	}

	@Nullable
	@Override
	public Association<SpannerPersistentProperty> getAssociation() {
		return null;
	}

	@Override
	public boolean isEntity() {
		return false;
	}

	@Override
	public boolean isIdProperty() {
		return true;
	}

	@Override
	public boolean isVersionProperty() {
		return false;
	}

	@Override
	public boolean isCollectionLike() {
		return false;
	}

	@Override
	public boolean isMap() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Nullable
	@Override
	public Class<?> getComponentType() {
		return null;
	}

	@Override
	public Class<?> getRawType() {
		return getType();
	}

	@Nullable
	@Override
	public Class<?> getMapValueType() {
		return null;
	}

	@Override
	public Class<?> getActualType() {
		return getType();
	}

	@Nullable
	@Override
	public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
		return null;
	}

	@Nullable
	@Override
	public <A extends Annotation> A findPropertyOrOwnerAnnotation(
			Class<A> annotationType) {
		return null;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return false;
	}

	@Override
	public boolean usePropertyAccess() {
		return false;
	}

	/**
	 * @since 1.1
	 */
	@Override
	public Class<?> getAssociationTargetType() {
		return null;
	}

	/**
	 * @since 1.1
	 */
	@Override
	public boolean isImmutable() {
		return false;
	}

	/**
	 * @since 1.1
	 */
	@Override
	public Method getWither() {
		return null;
	}
}
