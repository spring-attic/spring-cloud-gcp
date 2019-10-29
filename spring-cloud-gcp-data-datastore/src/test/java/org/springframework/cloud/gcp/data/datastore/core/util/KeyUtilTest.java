/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.core.util;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyUtilTest {

	@Test
	public void testRemoveAncestors_NamedKeys() {
		Key namedKey = Key.newBuilder("project", "person", "Smith")
				.addAncestor(PathElement.of("person", "GrandParent"))
				.addAncestor(PathElement.of("person", "Parent"))
				.build();

		Key processedKey = KeyUtil.getKeyWithoutAncestors(namedKey);
		assertThat(processedKey.getAncestors()).isEmpty();
	}

	@Test
	public void testRemoveAncestors_IdKeys() {
		Key idKey = Key.newBuilder("project", "person", 46L)
				.addAncestor(PathElement.of("person", 22L))
				.addAncestor(PathElement.of("person", 18L))
				.build();

		Key processedKey = KeyUtil.getKeyWithoutAncestors(idKey);
		assertThat(processedKey.getAncestors()).isEmpty();
	}
}
