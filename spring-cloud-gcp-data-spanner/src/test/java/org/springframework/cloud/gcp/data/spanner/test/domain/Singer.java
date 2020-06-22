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

package org.springframework.cloud.gcp.data.spanner.test.domain;

import java.util.Objects;

import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

@Table(name = "singers_list")
public class Singer {
	@PrimaryKey
	Integer singerId;

	String firstName;

	String lastName;

	public Singer(Integer singerId, String firstName, String lastName) {
		this.singerId = singerId;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Singer singer = (Singer) o;
		return Objects.equals(singerId, singer.singerId) &&
						Objects.equals(firstName, singer.firstName) &&
						Objects.equals(lastName, singer.lastName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(singerId, firstName, lastName);
	}
}
