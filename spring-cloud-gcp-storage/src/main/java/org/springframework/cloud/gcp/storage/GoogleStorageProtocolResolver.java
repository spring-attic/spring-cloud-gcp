package org.springframework.cloud.gcp.storage;

import com.google.cloud.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * @author Vinicius Carvalho
 */
public class GoogleStorageProtocolResolver implements ProtocolResolver, InitializingBean {

	private ResourceLoader delegate;

	private Storage storage;

	private Logger logger = LoggerFactory.getLogger(GoogleStorageProtocolResolver.class);

	public GoogleStorageProtocolResolver(ResourceLoader delegate, Storage storage) {
		Assert.notNull(delegate,"Parent resource loader can not be null");
		Assert.notNull(storage,"Storage client can not be null");
		this.delegate = delegate;
		this.storage = storage;

	}

	@Override
	public Resource resolve(String location, ResourceLoader resourceLoader) {
		if(location.startsWith("gs://")){
			return new GoogleStorageResource(storage,location);
		}
		return delegate.getResource(location);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(DefaultResourceLoader.class.isAssignableFrom(this.delegate.getClass())){
			((DefaultResourceLoader)delegate).addProtocolResolver(this);
		}else{
			logger.warn("The provided delegate resource loader is not an implementation of DefaultResourceLoader. Custom Protocol using gs:// suffix will not be enabled");
		}
	}
}
