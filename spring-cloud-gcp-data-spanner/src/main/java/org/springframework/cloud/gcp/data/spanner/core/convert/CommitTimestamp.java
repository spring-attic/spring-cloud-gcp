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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Value;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.springframework.core.GenericTypeResolver.resolveTypeArgument;

/**
 * The factory method {@link #of(Class)} should be used to create a specific value of a pointed class
 * that will be converted then to the {@link Value#COMMIT_TIMESTAMP} by {@link CommitTimestampSupplier}.
 * It should be used when a database column has type of Timestamp with an option "allow_commit_timestamp"
 * and the business logic needs to manage in runtime when the PENDING_COMMIT_TIMESTAMP should be stored into this field.
 * The method {@link #register(CommitTimestampSupplier)} should be used to add a custom "to timestamp" converter
 * to the list of converters that support this "CommitTimestamp" feature.
 *
 * @author Roman Solodovnichenko
 *
 * @since 1.3.0
 */
public final class CommitTimestamp {
	private static final Map<Object, Object> VALUES = new ConcurrentHashMap<>();

	static {
		VALUES.put(com.google.cloud.Timestamp.class, Value.COMMIT_TIMESTAMP);
	}

	private CommitTimestamp() {
	}

	/**
	 * Creates a value for {@link Value#COMMIT_TIMESTAMP}.
	 * @param timestampClass a class to be presented as a Timestamp in the database column.
	 * @param <T> type of the class. Supported by-default types are:
	 * 	{@link com.google.cloud.Timestamp}, {@link java.sql.Timestamp}, {@link java.time.LocalDateTime},
	 * 	{@link java.time.Instant}, {@link java.util.Date}.
	 * @return a value that will be converted to {@link Value#COMMIT_TIMESTAMP} by {@link SpannerConverters}
	 * @throws IllegalArgumentException when the {@code timestampClass} was not registered
	 * 	 with the method {@link #register(CommitTimestampSupplier)}.
	 * @see #register(CommitTimestampSupplier)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T of(Class<T> timestampClass) throws IllegalArgumentException {
		return (T) VALUES.computeIfAbsent(timestampClass, key -> {
			throw new IllegalArgumentException(
					String.format("\"CommitTimestamp\" is not supported for the type %s", key));
		});
	}

	/**
	 * The method should be used when we need to register a custom "to timestamp" converter as {@link CommitTimestampSupplier}.
	 * @param converter the {@link Converter} to register as CommitTimestampSupplier
	 * @param <S> the "source" type of {@link Converter}
	 * @param <C> the type if {@link CommitTimestampSupplier}
	 * @return registered converter.
	 * @throws IllegalStateException when the typed argument of the {@code converter} already registered.
	 *   It is impossible to overwrite existing registrations because it could affect the core converters
	 *   of the {@link SpannerConverters} class.
	 */
	@SuppressWarnings("unchecked")
	public static <S, C extends CommitTimestampSupplier<S>> C register(C converter) throws IllegalStateException {
		final Class<S> argument = (Class<S>) resolveTypeArgument(converter.getClass(), CommitTimestampSupplier.class);
		VALUES.compute(argument, (key, old) -> {
			if (old == null) {
				return converter.commitTimestamp;
			}
			throw new IllegalStateException(
					String.format("The value %s already registered as \"CommitTimestamp\" for the type %s", old, key));
		});
		return converter;
	}

	/**
	 * A specific template of "to timestamp" converter that any custom converter should follow
	 * to support "CommitTimestamp" feature.
	 * @param <S> a source type of converter.
	 */
	public static abstract class CommitTimestampSupplier<S> implements Converter<S, Timestamp> {

		final S commitTimestamp;
		private final Function<S, Timestamp> converter;

		protected CommitTimestampSupplier(S commitTimestamp, Function<S, Timestamp> converter) {
			this.commitTimestamp = commitTimestamp;
			this.converter = converter;
		}

		@Nullable
		public final Timestamp convert(@NonNull S source) {
			return commitTimestamp == source ? Value.COMMIT_TIMESTAMP : converter.apply(source);
		}

	}

}
