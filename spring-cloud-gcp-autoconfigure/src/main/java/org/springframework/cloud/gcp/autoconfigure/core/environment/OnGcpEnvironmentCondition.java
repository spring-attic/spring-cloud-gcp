/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.core.environment;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cloud.gcp.core.GcpEnvironment;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determine whether the application is running inside of a Compute Engine or GKE environment by attempting to query
 * the metadata server.
 *
 * @author Elena Felder
 */
public class OnGcpEnvironmentCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {

		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnGcpEnvironment.class.getName());
		GcpEnvironment targetEnvironment = (GcpEnvironment) attributes.get("value");

		boolean match = targetEnvironment.matches();
		String message = (match ? "Application is " : "Application is not")
				+ " running on " + targetEnvironment.toString();

		return new ConditionOutcome(match, message);
	}
}
