package org.springframework.cloud.gcp.pubsub.converters.support;

import org.springframework.cloud.gcp.pubsub.converters.HeaderConverter;

/**
 * @author Vinicius Carvalho
 */
public class BooleanConverter implements HeaderConverter<Boolean> {

	@Override
	public String encode(Boolean value) {
		return value.toString();
	}

	@Override
	public Boolean decode(String value) {

		if(value.equalsIgnoreCase("true")){
			return Boolean.TRUE;
		}else if(value.equalsIgnoreCase("false")){
			return Boolean.FALSE;
		}

		return null;
	}

}
