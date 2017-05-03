package org.springframework.cloud.gcp.pubsub.converters;

/**
 * @author Vinicius Carvalho
 */
public interface HeaderConverter<T>{

	String encode(T value);
	T decode(String value);

}
