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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Elena Felder
 */
@RunWith(MockitoJUnitRunner.class)
public class AudienceValidatorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	Jwt mockJwt;

	@Test
	public void testNullAudienceDisallowedInConstructor() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Audience cannot be null");

		new AudienceValidator(null);
	}

	@Test
	public void testNullAudienceDisallowedInSetter() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Audience cannot be null");

		AudienceValidator validator = new AudienceValidator();
		validator.setAudience(null);
	}

	@Test
	public void testConstructorPassedAudienceMatchesCorrectly() {

		AudienceValidator validator = new AudienceValidator("cats");

		when(this.mockJwt.getAudience()).thenReturn(ImmutableList.of("cats"));
		assertFalse(validator.validate(this.mockJwt).hasErrors());

		when(this.mockJwt.getAudience()).thenReturn(ImmutableList.of("dogs"));
		assertTrue(validator.validate(this.mockJwt).hasErrors());
	}

	@Test
	public void testSetterPassedAudienceMatchesCorrectly() {

		AudienceValidator validator = new AudienceValidator();
		validator.setAudience("cats");

		when(this.mockJwt.getAudience()).thenReturn(ImmutableList.of("cats"));
		assertFalse(validator.validate(this.mockJwt).hasErrors());

		when(this.mockJwt.getAudience()).thenReturn(ImmutableList.of("dogs"));
		assertTrue(validator.validate(this.mockJwt).hasErrors());
	}
}
