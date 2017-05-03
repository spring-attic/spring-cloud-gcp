package org.springframework.cloud.gcp.pubsub.converters.support;

import org.springframework.cloud.gcp.pubsub.converters.HeaderConverter;

/**
 * @author Vinicius Carvalho
 */
public class FloatConverter implements HeaderConverter<Float> {
	@Override
	public String encode(Float value) {
		return value.toString();
	}

	@Override
	public Float decode(String value) {
		Float result = null;
		try{
			result = Float.parseFloat(value);
			result = result.isInfinite() ? null : result;
		} catch (NumberFormatException nfe){}
		return result;
	}
}
