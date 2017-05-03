package org.springframework.cloud.gcp.pubsub.converters.support;

import org.springframework.cloud.gcp.pubsub.converters.HeaderConverter;

/**
 * @author Vinicius Carvalho
 */
public class LongConverter implements HeaderConverter<Long> {
	@Override
	public String encode(Long value) {
		return value.toString();
	}

	@Override
	public Long decode(String value) {
		Long result = null;
		try{
			result = Long.decode(value);
		}catch (NumberFormatException nfe){}
		return result;
	}
}
