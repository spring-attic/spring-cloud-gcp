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

package org.springframework.cloud.gcp.pubsub.support;

import com.google.pubsub.v1.ProjectTopicName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PubSubTopicUtils}.
 *
 * @author Mike Eltsufin
 */
public class PubSubTopicUtilsTests {

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Test
	public void testParseTopic_canonical() {
		String project = "projectA";
		String topic = "topicA";
		String fqn = "projects/" + project + "/topics/" + topic;

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.parseTopic(project, topic);

		assertThat(parsedProjectTopicName).isEqualTo(ProjectTopicName.of(project, topic));
		assertThat(parsedProjectTopicName.toString()).isEqualTo(fqn);
	}

	@Test
	public void testParseTopic_no_topic() {
		expectedEx.expect(IllegalArgumentException.class);
		expectedEx.expectMessage("The topic can't be null.");

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.parseTopic("topicA", null);
	}

	@Test
	public void testParseTopic_canonical_no_project() {
		expectedEx.expect(IllegalArgumentException.class);
		expectedEx.expectMessage("The project ID can't be null when using canonical topic name.");

		String project = "projectA";
		String topic = "topicA";
		String fqn = "projects/" + project + "/topics/" + topic;

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.parseTopic(null, topic);

		assertThat(parsedProjectTopicName).isEqualTo(ProjectTopicName.of(project, topic));
		assertThat(parsedProjectTopicName.toString()).isEqualTo(fqn);
	}

	@Test
	public void testParseTopic_fqn() {
		String project = "projectA";
		String topic = "topicA";
		String fqn = "projects/" + project + "/topics/" + topic;

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.parseTopic(project, fqn);

		assertThat(parsedProjectTopicName).isEqualTo(ProjectTopicName.of(project, topic));
		assertThat(parsedProjectTopicName.toString()).isEqualTo("projects/" + project + "/topics/" + topic);
	}

	@Test
	public void testParseTopic_fqn_no_project() {
		String project = "projectA";
		String topic = "topicA";
		String fqn = "projects/" + project + "/topics/" + topic;

		ProjectTopicName parsedProjectTopicName = PubSubTopicUtils.parseTopic(null, fqn);

		assertThat(parsedProjectTopicName).isEqualTo(ProjectTopicName.of(project, topic));
		assertThat(parsedProjectTopicName.toString()).isEqualTo("projects/" + project + "/topics/" + topic);
	}
}
