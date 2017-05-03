package org.springframework.cloud.gcp.pubsub.converters.support;

import org.springframework.cloud.gcp.pubsub.converters.HeaderConverter;

/**
 * @author Vinicius Carvalho
 */
public class DoubleConverter implements HeaderConverter<Double> {
	@Override
	public String encode(Double value) {
		return value.toString();
	}

	@Override
	public Double decode(String value) {
		Double result = null;
		try{
			result = Double.valueOf(value);
		}catch (NumberFormatException nfe){}
		return result;
	}
}
