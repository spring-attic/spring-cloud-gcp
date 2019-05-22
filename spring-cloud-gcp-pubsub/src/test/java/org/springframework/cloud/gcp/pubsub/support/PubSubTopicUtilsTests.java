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

import com.google.pubsub.v1.ProjectTopicName;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PubSubTopicUtils}.
 *
 * @author Mike Eltsufin
 */
public class PubSubTopicUtilsTests {

	@Test
	public void testToProjectTopicName_canonical() {
		String project = "projectA";
		String topic = "topicA";
		String fqn = "projects/" + project + "/topics/" + topic;

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.toProjectTopicName(topic, project);

		assertThat(parsedProjectTopicName).isEqualTo(ProjectTopicName.of(project, topic));
		assertThat(parsedProjectTopicName.toString()).isEqualTo(fqn);
	}

	@Test
	public void testToProjectTopicName_no_topic() {
		assertThatThrownBy(() -> PubSubTopicUtils.toProjectTopicName(null, "topicA"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The topic can't be null.");
	}

	@Test
	public void testToProjectTopicName_canonical_no_project() {
		assertThatThrownBy(() -> PubSubTopicUtils.toProjectTopicName("topicA", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The project ID can't be null when using canonical topic name.");
	}

	@Test
	public void testToProjectTopicName_fqn() {
		String project = "projectA";
		String topic = "topicA";
		String fqn = "projects/" + project + "/topics/" + topic;

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.toProjectTopicName(fqn, project);

		assertThat(parsedProjectTopicName).isEqualTo(ProjectTopicName.of(project, topic));
		assertThat(parsedProjectTopicName.toString()).isEqualTo(fqn);
	}

	@Test
	public void testToProjectTopicName_fqn_no_project() {
		String project = "projectA";
		String topic = "topicA";
		String fqn = "projects/" + project + "/topics/" + topic;

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.toProjectTopicName(fqn, null);

		assertThat(parsedProjectTopicName).isEqualTo(ProjectTopicName.of(project, topic));
		assertThat(parsedProjectTopicName.toString()).isEqualTo(fqn);
	}
}
