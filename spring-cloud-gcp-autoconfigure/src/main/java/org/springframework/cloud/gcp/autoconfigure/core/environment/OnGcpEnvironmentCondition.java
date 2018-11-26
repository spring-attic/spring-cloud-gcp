/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.core.environment;

import java.util.Map;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cloud.gcp.core.GcpEnvironment;
import org.springframework.cloud.gcp.core.GcpEnvironmentProvider;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@Condition} that determines which GCP environment the application is running on.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public class OnGcpEnvironmentCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {

		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnGcpEnvironment.class.getName());
		GcpEnvironment targetEnvironment = (GcpEnvironment) attributes.get("value");

		try {
			GcpEnvironmentProvider environmentProvider = context.getBeanFactory().getBean(GcpEnvironmentProvider.class);
			if (!environmentProvider.isCurrentEnvironment(targetEnvironment)) {
				return new ConditionOutcome(false, "Application is not running on " + targetEnvironment);
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			return new ConditionOutcome(
					false, "GcpEnvironmentProvider not found; environment-specific checks disabled.");
		}

		return new ConditionOutcome(true, "Application is running on " + targetEnvironment);
	}
}
