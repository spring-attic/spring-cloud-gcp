package org.springframework.cloud.gcp.autoconfigure.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.storage.GoogleStorageProtocolResolver;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * @author Vinicius Carvalho
 */
@Configuration
@ConditionalOnClass(Storage.class)
public class StorageAutoConfiguration implements ResourceLoaderAware{

	private ResourceLoader defaultResourceLoader;

	@Bean
	@ConditionalOnMissingBean(Storage.class)
	public Storage storage(GoogleCredentials credentials) throws Exception{
		return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
	}

	@Bean
	public GoogleStorageProtocolResolver googleStorageProtocolResolver(Storage storage){
		return new GoogleStorageProtocolResolver(this.defaultResourceLoader,storage);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.defaultResourceLoader =resourceLoader;
	}
}
