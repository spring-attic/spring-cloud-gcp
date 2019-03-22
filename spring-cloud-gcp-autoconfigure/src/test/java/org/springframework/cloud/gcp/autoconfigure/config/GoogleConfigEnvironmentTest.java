/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.config;

import java.util.Base64;

import org.junit.Test;

import org.springframework.cloud.gcp.autoconfigure.config.GoogleConfigEnvironment.Variable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the environment config.
 *
 * @author Dmitry Solomakha
 */
public class GoogleConfigEnvironmentTest {

	@Test
	public void testSetVariabeValue() {
		GoogleConfigEnvironment.Variable var = new Variable();
		String value = "v a l u e";
		String encodedString = Base64.getEncoder().encodeToString(value.getBytes());
		var.setValue(encodedString);
		assertThat(var.getValue()).isEqualTo(value);
	}
}
