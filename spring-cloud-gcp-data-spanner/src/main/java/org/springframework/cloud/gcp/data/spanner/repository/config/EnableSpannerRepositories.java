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

package org.springframework.cloud.gcp.data.spanner.repository.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.gcp.data.spanner.repository.support.SpannerRepositoryFactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;

/**
 * @author Chengyuan Zhao
 * @author Ray Tsang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SpannerRepositoriesRegistrar.class)
public @interface EnableSpannerRepositories {

	String[] value() default {};

	Filter[] includeFilters() default {};

	Filter[] excludeFilters() default {};

	String[] basePackages() default {};

	Class[] basePackageClasses() default {};

	Class repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	boolean considerNestedRepositories() default false;

	Class repositoryFactoryBeanClass() default SpannerRepositoryFactoryBean.class;

	String namedQueriesLocation() default "";

	String repositoryImplementationPostfix() default "";

	String spannerOperationsRef() default "spannerOperations";

	String spannerMappingContextRef() default "spannerMappingContext";
}
