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

package org.springframework.cloud.gcp.pubsub.converters.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.cloud.gcp.pubsub.converters.HeaderConverter;

/**
 * @author Vinicius Carvalho
 */
public class DateConverter implements HeaderConverter<Date> {

	private String datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private DateFormat dateFormat;

	public DateConverter() {
		this(null);
	}

	public DateConverter(String datePattern) {
		if (datePattern != null) {
			this.datePattern = datePattern;
		}
		this.dateFormat = new SimpleDateFormat(this.datePattern);
	}

	@Override
	public String encode(Date value) {
		return this.dateFormat.format(value);
	}

	@Override
	public Date decode(String value) {
		Date result = null;
		try {
			result = this.dateFormat.parse(value);
		}
		catch (ParseException e) {
		}
		return result;
	}
}
