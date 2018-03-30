package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.lang.reflect.Method;

import java.util.Optional;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SpannerQueryMethod extends QueryMethod {

	private final Method method;

	private final MappingContext<? extends SpannerPersistentEntity<?>, SpannerPersistentProperty> mappingContext;

	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct query
	 * to use for following invocations of the method given.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public SpannerQueryMethod(Method method,
			RepositoryMetadata metadata,
			ProjectionFactory factory,
			MappingContext<? extends SpannerPersistentEntity<?>, SpannerPersistentProperty> mappingContext) {
		super(method, metadata, factory);

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.method = method;
		this.mappingContext = mappingContext;
	}


  /**
   * Returns whether the method has an annotated query.
   *
   * @return
   */
  public boolean hasAnnotatedQuery() {
    return findAnnotatedQuery().isPresent();
  }

  private Optional<String> findAnnotatedQuery() {

    return Optional.ofNullable(getQueryAnnotation()) //
        .map(AnnotationUtils::getValue) //
        .map(it -> (String) it) //
        .filter(StringUtils::hasText);
  }

  /**
   * Returns the {@link Query} annotation that is applied to the method or {@code null} if none available.
   *
   * @return
   */
  @Nullable
  Query getQueryAnnotation() {
    return AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
  }



}
