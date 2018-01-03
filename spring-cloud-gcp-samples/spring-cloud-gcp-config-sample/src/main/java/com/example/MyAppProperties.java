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

package com.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * Sample Configuration class with property values loaded using the Runtime Configurator API.
 *
 * @author Jisha Abubaker
 * @author Stephane Nicoll
 */
@RefreshScope
@ConfigurationProperties("myapp")
public class MyAppProperties {

	private int queueSize;

	private boolean isFeatureXEnabled;

	public int getQueueSize() {
		return this.queueSize;
	}

	public void setQueueSize(int size) {
		this.queueSize = size;
	}

	public boolean isFeatureXEnabled() {
		return this.isFeatureXEnabled;
	}

	public void setFeatureXEnabled(boolean featureXEnabled) {
		this.isFeatureXEnabled = featureXEnabled;
	}
}
