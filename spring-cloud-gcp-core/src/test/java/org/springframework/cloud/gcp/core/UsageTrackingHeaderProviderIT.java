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

import java.util.regex.Pattern;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This needs to be an integration test because the JAR MANIFEST has to be available for
 * this.getClass().getPackage().getImplementationVersion() to work properly.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class UsageTrackingHeaderProviderIT {

	@Test
	public void testGetHeaders() {
		UsageTrackingHeaderProvider subject = new UsageTrackingHeaderProvider(this.getClass());

		String versionRegex = "\\d+\\.\\d+\\.\\d+\\.[BUILD-SNAPSHOT|M\\d+|RC\\d+|RELEASE]$";
		assertThat(subject.getHeaders()).containsKey("User-Agent");
		assertThat(subject.getHeaders().get("User-Agent")).matches(
				Pattern.compile("Spring/" + versionRegex + " spring-cloud-gcp-core/" + versionRegex));
		assertThat(subject.getHeaders().size()).isEqualTo(1);
	}
}
