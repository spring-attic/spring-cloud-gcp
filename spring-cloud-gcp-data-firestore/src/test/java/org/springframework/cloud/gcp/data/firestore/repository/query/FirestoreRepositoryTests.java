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

package org.springframework.cloud.gcp.data.firestore.repository.query;

import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.StructuredQuery.Direction;
import com.google.firestore.v1.StructuredQuery.FieldReference;
import com.google.firestore.v1.StructuredQuery.Order;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.entities.User;
import org.springframework.cloud.gcp.data.firestore.entities.UserRepository;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreDefaultClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.cloud.gcp.data.firestore.repository.query.FirestoreRepositoryTests.FirestoreRepositoryTestsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FirestoreRepositoryTestsConfiguration.class)
public class FirestoreRepositoryTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FirestoreTemplate template;

	@Test
	public void testSortQuery() {
		userRepository.findByAgeGreaterThan(0, Sort.by("name")).blockLast();

		ArgumentCaptor<StructuredQuery.Builder> captor =
				ArgumentCaptor.forClass(StructuredQuery.Builder.class);
		verify(template).execute(captor.capture(), eq(User.class));

		StructuredQuery.Builder result = captor.getValue();
		assertThat(result.getOrderByList()).containsExactly(
				Order.newBuilder()
						.setDirection(Direction.ASCENDING)
						.setField(
								FieldReference.newBuilder().setFieldPath("name"))
						.build());
	}

	@Configuration
	@EnableReactiveFirestoreRepositories(basePackageClasses = UserRepository.class)
	static class FirestoreRepositoryTestsConfiguration {
		private static final String DEFAULT_PARENT = "projects/my-project/databases/(default)/documents";

		@Bean
		public FirestoreMappingContext firestoreMappingContext() {
			return new FirestoreMappingContext();
		}

		@Bean
		public FirestoreTemplate firestoreTemplate(
				FirestoreClassMapper classMapper, FirestoreMappingContext firestoreMappingContext) {
			FirestoreTemplate template = Mockito.mock(FirestoreTemplate.class);
			Mockito.when(template.getClassMapper()).thenReturn(classMapper);
			Mockito.when(template.getMappingContext()).thenReturn(firestoreMappingContext);
			Mockito.when(template.execute(any(), any())).thenReturn(Flux.empty());
			return template;
		}

		@Bean
		@ConditionalOnMissingBean
		public FirestoreClassMapper getClassMapper() {
			return new FirestoreDefaultClassMapper();
		}
	}
}
