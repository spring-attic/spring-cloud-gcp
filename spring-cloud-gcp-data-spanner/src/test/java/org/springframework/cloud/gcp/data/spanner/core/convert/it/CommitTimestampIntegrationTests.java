/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.core.convert.it;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.UUID;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactory;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.convert.CommitTimestamp;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverters;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.CommitTimestamps;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.GenericTypeResolver.resolveTypeArguments;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.getField;
import static org.springframework.util.ReflectionUtils.setField;

/**
 * Integration tests for the {@link CommitTimestamp} feature.
 *
 * @author Roman Solodovnichenko
 */
@RunWith(SpringRunner.class)
public class CommitTimestampIntegrationTests extends AbstractSpannerIntegrationTest {

	@Autowired
	private SpannerOperations spannerOperations;
	@Autowired
	private DatabaseClient databaseClient;
	@Autowired
	private SpannerMutationFactory mutationFactory;

	@Test
	public void testCommitTimestamp() {

		final CommitTimestamps entity = new CommitTimestamps();
		final String id = UUID.randomUUID().toString();
		entity.id = id;

		doWithFields(CommitTimestamps.class,
				f -> setField(f, entity, CommitTimestamp.of(f.getType())),
				ff -> !ff.isSynthetic() && Objects.isNull(ff.getAnnotation(PrimaryKey.class)));

		final Timestamp committedAt = databaseClient.write(mutationFactory.insert(entity));

		final CommitTimestamps fetched = spannerOperations.read(CommitTimestamps.class, Key.of(id));
		doWithFields(CommitTimestamps.class,
				f -> assertThat(getField(f, fetched))
						.describedAs("Test of the field %s has tailed", f)
						.isEqualTo(getConverter(f).convert(committedAt)),
				ff -> !ff.isSynthetic() && isNull(ff.getAnnotation(PrimaryKey.class)));
	}

	@SuppressWarnings("unchecked")
	private Converter<Timestamp, ?> getConverter(final Field field) {
		return Objects.equals(Timestamp.class, field.getType()) ? t -> t
				: SpannerConverters.DEFAULT_SPANNER_READ_CONVERTERS.stream()
				.filter(c -> {
					Class<?>[] typeArguments = resolveTypeArguments(c.getClass(), Converter.class);
					return Objects.equals(Timestamp.class, typeArguments[0])
							&& Objects.equals(field.getType(), typeArguments[1]);
				})
				.findFirst().orElseThrow(() -> new IllegalStateException(
						String.format("No Converter from Timestamp to %s found for the field %s", field.getType(), field)));
	}

}
