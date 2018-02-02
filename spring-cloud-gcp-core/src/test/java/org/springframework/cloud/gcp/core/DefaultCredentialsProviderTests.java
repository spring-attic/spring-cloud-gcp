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

package org.springframework.cloud.gcp.core;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class DefaultCredentialsProviderTests {

	@Test
	public void testResolveScopesDefaultScopes() throws IOException {
		List<String> scopes = DefaultCredentialsProvider.resolveScopes(null);
		assertTrue(scopes.size() > 1);
		assertTrue(scopes.contains(GcpScope.PUBSUB.getUrl()));
	}

	@Test
	public void testResolveScopesOverrideScopes() throws IOException {
		List<String> scopes = DefaultCredentialsProvider.resolveScopes(ImmutableList.of("myscope"));
		assertEquals(scopes.size(), 1);
		assertTrue(scopes.contains("myscope"));
	}

	@Test
	public void testResolveScopesStarterScopesPlaceholder() {
		List<String> scopes = DefaultCredentialsProvider.resolveScopes(ImmutableList.of("DEFAULT_SCOPES", "myscope"));
		assertTrue(scopes.size() == GcpScope.values().length + 1);
		assertTrue(scopes.contains(GcpScope.PUBSUB.getUrl()));
		assertTrue(scopes.contains("myscope"));
	}

}
