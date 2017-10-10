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

package org.springframework.cloud.gcp.storage;

import java.io.IOException;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Artem Bilan
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.cloud.gcp.storage.auto-create-files=false")
@RunWith(SpringRunner.class)
public class GoogleStorageAutoConfigurationTests {

	@LocalServerPort
	private int port;

	@Value("gs://test-spring/images/spring.png")
	private Resource googleStorageResource;

	@Test
	public void testValidObject() throws Exception {
		TestRestTemplate testRestTemplate = new TestRestTemplate();
		Long actual = testRestTemplate.getForObject("http://localhost:" + this.port + "/resource", Long.class);
		assertEquals(new Long(4096L), actual);
	}

	@Test
	public void testAutoCreateFilesFalse() throws IOException {
		assertFalse(((GoogleStorageResource) this.googleStorageResource)
				.isCreateBlobIfNotExists());
	}

	@SpringBootApplication(exclude = GcpContextAutoConfiguration.class)
	@RestController
	static class StorageApplication {

		@Value("gs://test-spring/images/spring.png")
		private Resource remoteResource;

		@GetMapping("/resource")
		public long getResource() throws IOException {
			return this.remoteResource.contentLength();
		}

		@Bean
		public static Storage mockStorage() throws Exception {
			Storage storage = Mockito.mock(Storage.class);
			BlobId validBlob = BlobId.of("test-spring", "images/spring.png");
			Blob mockedBlob = Mockito.mock(Blob.class);
			Mockito.when(mockedBlob.exists()).thenReturn(true);
			Mockito.when(mockedBlob.getSize()).thenReturn(4096L);
			Mockito.when(storage.get(Mockito.eq(validBlob))).thenReturn(mockedBlob);
			return storage;
		}
	}

}
