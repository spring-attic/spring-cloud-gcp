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

package org.springframework.cloud.gcp.config;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

/**
 * Expected response format for Google Runtime Configurator API response.
 *
 * @author Jisha Abubaker
 */
class GoogleConfigEnvironment {

	private List<Variable> variables;

	GoogleConfigEnvironment() {
	}

	List<Variable> getVariables() {
		return this.variables;
	}

	void setVariables(List<Variable> variables) {
		this.variables = variables;
	}

	Map<String, Object> getConfig() {
		Map<String, Object> config = new HashMap<>();
		for (Variable variable : this.variables) {
			Object value = (variable.getText() != null) ? variable.getText() : variable.getValue();
			config.put(variable.getName(), value);
		}
		return config;
	}

	private static class Variable {

		private String name;

		private String text;

		private String value;

		private String updateTime;

		Variable() {
		}

		String getName() {
			return this.name;
		}

		public void setName(String name) {
			if (name != null) {
				// use short variable name instead of {project/config/variable} path
				String[] variableNameSplit = name.split("/");
				if (variableNameSplit.length > 0) {
					this.name = variableNameSplit[variableNameSplit.length - 1];
				}
			}
		}

		String getText() {
			return this.text;
		}

		void setText(String text) {
			this.text = text;
		}

		String getValue() {
			return this.value;
		}

		void setValue(String value) {
			if (value != null) {
				this.value = decode(value);
			}
		}

		String getUpdateTime() {
			return this.updateTime;
		}

		void setUpdateTime(String updateTime) {
			this.updateTime = updateTime;
		}

		private String decode(String value) {
			byte[] decodedValue = DatatypeConverter.parseBase64Binary(value);
			return new String(decodedValue, StandardCharsets.UTF_8);
		}
	}
}
