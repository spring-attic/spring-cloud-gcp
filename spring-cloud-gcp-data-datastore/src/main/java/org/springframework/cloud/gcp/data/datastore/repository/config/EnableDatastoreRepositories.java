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

package org.springframework.cloud.gcp.data.datastore.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.gcp.data.datastore.repository.support.DatastoreRepositoryFactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;

/**
 * Annotation that enables the instantiation of Datastore repositories.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(DatastoreRepositoriesRegistrar.class)
public @interface EnableDatastoreRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
	 * declarations e.g.: {@code @EnableDatastoreRepositories("org.my.pkg")} instead of
	 * {@code @EnableDatastoreRepositories(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set
	 * of candidate components from everything in {@link #basePackages()} to everything in
	 * the base packages that matches the given filter or filters.
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for
	 * (and mutually exclusive with) this attribute. Use {@link #basePackageClasses()} for
	 * a type-safe alternative to String-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to
	 * scan for annotated components. The package of each class specified will be scanned.
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 */
	Class[] basePackageClasses() default {};

	/**
	 * Configure the repository base class to be used to create repository proxies for
	 * this particular configuration.
	 *
	 * @return the base repository class
	 */
	Class repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes)
	 * should be discovered by the repositories infrastructure.
	 */
	boolean considerNestedRepositories() default false;

	/**
	 * Returns the {@link org.springframework.beans.factory.FactoryBean} class to be used
	 * for each repository instance. Defaults to {@link DatastoreRepositoryFactoryBean}.
	 *
	 * @return the factory bean class used to create factories
	 */
	Class repositoryFactoryBeanClass() default DatastoreRepositoryFactoryBean.class;

	/**
	 * Configures the location of where to read the Spring Data named queries properties
	 * file. Will default to {@code META-INF/datastore-named-queries.properties}
	 *
	 * @return the location of the file holding named queries' strings.
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the postfix to be used when looking up custom repository implementations.
	 * Defaults to {@literal Impl}. So for a repository named {@code PersonRepository} the
	 * corresponding implementation class will be looked up scanning for
	 * {@code PersonRepositoryImpl}.
	 *
	 * @return the default suffix that will cause classes to be assumed to be
	 * implementations
	 */
	String repositoryImplementationPostfix() default "";

	/**
	 * Configures the name of the
	 * {@link org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate} bean to
	 * be used by default with the repositories detected.
	 *
	 * @return the name of the Datastore template class
	 */
	String datastoreTemplateRef() default "datastoreTemplate";

	/**
	 * Configures the name of the
	 * {@link org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext}
	 * bean to be used by default with the repositories detected.
	 *
	 * @return the name of the Datastore mapping context class
	 */
	String datastoreMappingContextRef() default "datastoreMappingContext";
}
