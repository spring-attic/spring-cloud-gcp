/*
 * Copyright 2017-2019 the original author or authors.
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

package com.example;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminationValue;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;

/**
 * An example band type that is stored in the same Kind with its parent type.
 *
 * @author Chengyuan Zhao
 */
@Entity
@DiscriminationValue("bluegrass")
public class BluegrassBand extends Band {
	public BluegrassBand(String name) {
		super(name);
	}

	@Override
	public String toString() {
		return this.name + " is into progressive bluegrass and folk-country.";
	}
}
