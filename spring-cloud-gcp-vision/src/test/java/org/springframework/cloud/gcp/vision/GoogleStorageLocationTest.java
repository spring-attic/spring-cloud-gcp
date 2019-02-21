/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.vision;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class GoogleStorageLocationTest {

	@Test
	public void testUriValidateProtocol() {
		assertThatThrownBy(() -> new GoogleStorageLocation("gs:/bucket"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("A Google Storage URI must start with gs://");
	}

	@Test
	public void testUriHasBucket() {
		assertThatThrownBy(() -> new GoogleStorageLocation("gs:///asdf"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The Google Storage URI is missing a bucket: gs:///asdf");
	}
}
