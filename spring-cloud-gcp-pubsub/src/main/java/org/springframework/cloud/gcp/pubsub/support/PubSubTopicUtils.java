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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Various utility methods for dealing with Pub/Sub topics.
 *
 * @author Mike Eltsufin
 * @since 1.2
 */
public final class PubSubTopicUtils {

	private PubSubTopicUtils() {
	}

	/**
	 * Create a {@link ProjectTopicName} based on a topic name within a project or the
	 * fully-qualified topic name. If the specified topic is in the
	 * {@code projects/<project_name>/topics/<topic_name>} format, then the {@code projectId} is
	 * ignored}
	 * @param topic the topic name in the project or the fully-qualified project name
	 * @param projectId the project ID to use if the topic is not a fully-qualified name
	 * @return the Pub/Sub object representing the topic name
	 */
	public static ProjectTopicName toProjectTopicName(String topic, @Nullable String projectId) {
		Assert.notNull(topic, "The topic can't be null.");

		ProjectTopicName projectTopicName = null;

		if (ProjectTopicName.isParsableFrom(topic)) {
			// Fully-qualified topic name in the "projects/<project_name>/topics/<topic_name>" format
			projectTopicName = ProjectTopicName.parse(topic);
		}
		else {
			Assert.notNull(projectId, "The project ID can't be null when using canonical topic name.");
			projectTopicName = ProjectTopicName.of(projectId, topic);
		}

		return projectTopicName;
	}
}
