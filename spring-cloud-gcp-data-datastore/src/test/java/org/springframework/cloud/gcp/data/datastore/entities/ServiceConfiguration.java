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

package org.springframework.cloud.gcp.data.datastore.entities;

import java.util.Objects;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

@Entity
public class ServiceConfiguration {
	@Id
	private String serviceName;

	private CustomMap customMap;

	public ServiceConfiguration(String serviceName, CustomMap customMap) {
		this.serviceName = serviceName;
		this.customMap = customMap;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ServiceConfiguration that = (ServiceConfiguration) o;
		return Objects.equals(serviceName, that.serviceName) &&
				Objects.equals(customMap, that.customMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceName, customMap);
	}
}
