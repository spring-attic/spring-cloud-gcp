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

package org.springframework.cloud.gcp.core;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Determine whether this is running inside of an App Engine environment by looking into
 * the System properties {@value APPENGINE_RUNTIME_PROPERTY}.
 *
 * @author Ray Tsang
 */
public class AppEngineCondition implements Condition {
	public static final String APPENGINE_RUNTIME_PROPERTY = "com.google.appengine.runtime.version";

	public static final String APPENGINE_RUNTIME_PREFIX = "Google App Engine/";

	@Override
	public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
		String appEngineVersion = System.getProperty(APPENGINE_RUNTIME_PROPERTY);
		return !StringUtils.isEmpty(appEngineVersion)
				&& appEngineVersion.startsWith(APPENGINE_RUNTIME_PREFIX);

	}
}
