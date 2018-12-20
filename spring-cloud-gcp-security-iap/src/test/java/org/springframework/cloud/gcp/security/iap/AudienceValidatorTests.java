/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.security.iap;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Spring context needed to exercise {@link AudienceValidator}'s {@code afterPropertiesSet()}.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public class AudienceValidatorTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestConfiguration.class));

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testNullAudienceDisallowedInConstructor() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Audience Provider cannot be null");

		new AudienceValidator(null);
	}

	@Test
	public void testCorrectAudienceMatches() {
		Jwt mockJwt = Mockito.mock(Jwt.class);
		when(mockJwt.getAudience()).thenReturn(ImmutableList.of("cats"));

		this.contextRunner.run((context) -> {
			AudienceValidator validator = context.getBean(AudienceValidator.class);
			assertFalse(validator.validate(mockJwt).hasErrors());
		});
	}

	@Test
	public void testWrongAudienceDoesNotMatch() {
		Jwt mockJwt = Mockito.mock(Jwt.class);
		when(mockJwt.getAudience()).thenReturn(ImmutableList.of("dogs"));

		this.contextRunner.run((context) -> {
			AudienceValidator validator = context.getBean(AudienceValidator.class);
			assertTrue(validator.validate(mockJwt).hasErrors());
		});
	}

	@Configuration
	static class TestConfiguration {
		@Bean
		AudienceValidator audienceValidator() {
			return new AudienceValidator(() -> "cats");
		}
	}
}
