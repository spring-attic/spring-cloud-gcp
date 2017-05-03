package org.springframework.cloud.gcp.pubsub.converters.support;

import org.springframework.cloud.gcp.pubsub.converters.HeaderConverter;

/**
 * @author Vinicius Carvalho
 */
public class IntegerConverter implements HeaderConverter<Integer> {
	@Override
	public String encode(Integer value) {
		return value.toString();
	}

	@Override
	public Integer decode(String value) {
		Integer result = null;
		try {
			result = Integer.decode(value);
		}catch (NumberFormatException nfe){
		}
		return result;
	}
}
