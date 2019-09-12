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

package org.springframework.cloud.gcp.pubsub.support;

import com.google.pubsub.v1.ProjectSubscriptionName;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PubSubSubscriptionUtils}.
 *
 * @author Mike Eltsufin
 */
public class PubSubSubscriptionUtilsTests {

	@Test
	public void testToProjectSubscriptionName_canonical() {
		String project = "projectA";
		String subscription = "subscriptionA";
		String fqn = "projects/" + project + "/subscriptions/" + subscription;

		ProjectSubscriptionName parsedProjectSubscriptionName = PubSubSubscriptionUtils
				.toProjectSubscriptionName(subscription, project);

		assertThat(parsedProjectSubscriptionName).isEqualTo(ProjectSubscriptionName.of(project, subscription));
		assertThat(parsedProjectSubscriptionName.toString()).isEqualTo(fqn);
	}

	@Test
	public void testToProjectSubscriptionName_no_subscription() {
		assertThatThrownBy(() -> PubSubSubscriptionUtils.toProjectSubscriptionName(null, "subscriptionA"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The subscription can't be null.");
	}

	@Test
	public void testToProjectSubscriptionName_canonical_no_project() {
		assertThatThrownBy(() -> PubSubSubscriptionUtils.toProjectSubscriptionName("subscriptionA", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The project ID can't be null when using canonical subscription name.");
	}

	@Test
	public void testToProjectSubscriptionName_fqn() {
		String project = "projectA";
		String subscription = "subscriptionA";
		String fqn = "projects/" + project + "/subscriptions/" + subscription;

		ProjectSubscriptionName parsedProjectSubscriptionName = PubSubSubscriptionUtils.toProjectSubscriptionName(fqn,
				project);

		assertThat(parsedProjectSubscriptionName).isEqualTo(ProjectSubscriptionName.of(project, subscription));
		assertThat(parsedProjectSubscriptionName.toString()).isEqualTo(fqn);
	}

	@Test
	public void testToProjectSubscriptionName_fqn_no_project() {
		String project = "projectA";
		String subscription = "subscriptionA";
		String fqn = "projects/" + project + "/subscriptions/" + subscription;

		ProjectSubscriptionName parsedProjectSubscriptionName = PubSubSubscriptionUtils.toProjectSubscriptionName(fqn,
				null);

		assertThat(parsedProjectSubscriptionName).isEqualTo(ProjectSubscriptionName.of(project, subscription));
		assertThat(parsedProjectSubscriptionName.toString()).isEqualTo(fqn);
	}
}
