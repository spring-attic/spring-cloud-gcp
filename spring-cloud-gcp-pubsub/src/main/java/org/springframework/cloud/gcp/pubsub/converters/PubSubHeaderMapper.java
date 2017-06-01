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

package org.springframework.cloud.gcp.pubsub.converters;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.converters.support.BooleanConverter;
import org.springframework.cloud.gcp.pubsub.converters.support.DateConverter;
import org.springframework.cloud.gcp.pubsub.converters.support.DoubleConverter;
import org.springframework.cloud.gcp.pubsub.converters.support.FloatConverter;
import org.springframework.cloud.gcp.pubsub.converters.support.IntegerConverter;
import org.springframework.cloud.gcp.pubsub.converters.support.LongConverter;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;

/**
 * @author Vinicius Carvalho
 */
public class PubSubHeaderMapper
		implements HeaderMapper<Map<String, String>>, InitializingBean {

	private final Map<Class<?>, HeaderConverter<?>> converterMap = new LinkedHashMap<>();

	private String datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public String getDatePattern() {
		return this.datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.converterMap.put(Boolean.class, new BooleanConverter());
		this.converterMap.put(Integer.class, new IntegerConverter());
		this.converterMap.put(Long.class, new LongConverter());
		this.converterMap.put(Float.class, new FloatConverter());
		this.converterMap.put(Double.class, new DoubleConverter());
		this.converterMap.put(Date.class, new DateConverter(this.datePattern));
	}

	@Override
	public void fromHeaders(MessageHeaders headers, Map<String, String> target) {
		for (Map.Entry<String, Object> entry : headers.entrySet()) {
			target.put(entry.getKey(), encode(entry.getValue()));
		}
	}

	@Override
	public MessageHeaders toHeaders(Map<String, String> source) {
		Map<String, Object> headerMap = new HashMap<>();
		for (Map.Entry<String, String> entry : source.entrySet()) {
			headerMap.put(entry.getKey(), decode(entry.getValue()));
		}
		return new MessageHeaders(headerMap);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String encode(Object value) {
		if (value instanceof String) {
			return (String) value;
		}
		HeaderConverter converter = this.converterMap.get(value.getClass());
		return (converter != null) ? converter.encode(value) : value.toString();
	}

	@SuppressWarnings("rawtypes")
	private Object decode(String value) {
		Object result = null;
		for (HeaderConverter converter : this.converterMap.values()) {
			result = converter.decode(value);
			if (result != null) {
				break;
			}
		}
		if (result == null) {
			result = value;
		}
		return result;
	}
}
