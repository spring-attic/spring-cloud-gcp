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

import java.io.FileNotFoundException;
import java.io.OutputStream;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Vinicius Carvalho
 * @author Artem Bilan
 * @author Mike Eltsufin
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class GoogleStorageTests {

	@Value("gs://test-spring/images/spring.png")
	private Resource remoteResource;

	@Test
	public void testValidObject() throws Exception {
		Assert.assertTrue(this.remoteResource.exists());
		Assert.assertEquals(4096L, this.remoteResource.contentLength());
	}

	@Test
	public void testWritable() throws Exception {
		Assert.assertTrue(this.remoteResource instanceof WritableResource);
		WritableResource writableResource = (WritableResource) this.remoteResource;
		Assert.assertTrue(writableResource.isWritable());
		writableResource.getOutputStream();
	}

	@Test
	public void testWritableOutputStream() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = Mockito.mock(Storage.class);
		Blob blob = Mockito.mock(Blob.class);
		WriteChannel writeChannel = Mockito.mock(WriteChannel.class);
		Mockito.when(blob.writer()).thenReturn(writeChannel);
		Mockito.when(blob.exists()).thenReturn(true);
		Mockito.when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(blob);

		GoogleStorageResource resource = new GoogleStorageResource(storage, location);
		OutputStream os = resource.getOutputStream();
		Assert.assertNotNull(os);
	}

	@Test(expected = FileNotFoundException.class)
	public void testWritableOutputStreamNoAutoCreateOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = Mockito.mock(Storage.class);
		Mockito.when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(null);

		GoogleStorageResource resource = new GoogleStorageResource(storage, location,
				false);
		resource.getOutputStream();
	}

	@Test
	public void testWritableOutputStreamWithAutoCreateOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = Mockito.mock(Storage.class);
		BlobId blobId = BlobId.of("test-spring", "test");
		Mockito.when(storage.get(blobId)).thenReturn(null);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
		WriteChannel writeChannel = Mockito.mock(WriteChannel.class);
		Blob blob = Mockito.mock(Blob.class);
		Mockito.when(blob.writer()).thenReturn(writeChannel);
		Mockito.when(storage.create(blobInfo)).thenReturn(blob);

		GoogleStorageResource resource = new GoogleStorageResource(storage, location);
		OutputStream os = resource.getOutputStream();
		Assert.assertNotNull(os);
	}

	@Test
	public void testWritableOutputStreamWithAutoCreateOnNonExistantBlob()
			throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = Mockito.mock(Storage.class);
		BlobId blobId = BlobId.of("test-spring", "test");
		Blob nonExistantBlob = Mockito.mock(Blob.class);
		Mockito.when(nonExistantBlob.exists()).thenReturn(false);
		Mockito.when(storage.get(blobId)).thenReturn(nonExistantBlob);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
		WriteChannel writeChannel = Mockito.mock(WriteChannel.class);
		Blob blob = Mockito.mock(Blob.class);
		Mockito.when(blob.writer()).thenReturn(writeChannel);
		Mockito.when(storage.create(blobInfo)).thenReturn(blob);

		GoogleStorageResource resource = new GoogleStorageResource(storage, location);
		OutputStream os = resource.getOutputStream();
		Assert.assertNotNull(os);
	}

	@Test(expected = FileNotFoundException.class)
	public void testGetInputStreamOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = Mockito.mock(Storage.class);
		Mockito.when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(null);

		GoogleStorageResource resource = new GoogleStorageResource(storage, location,
				false);
		resource.getInputStream();
	}

	@Test
	public void testGetFilenameOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = Mockito.mock(Storage.class);
		Mockito.when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(null);

		GoogleStorageResource resource = new GoogleStorageResource(storage, location,
				false);
		Assert.assertNull(resource.getFilename());
	}

	@Test
	public void testCreateBlobIfNotExistingGetterSetter() {
		String location = "gs://test-spring/test";
		Storage storage = Mockito.mock(Storage.class);
		GoogleStorageResource resource = new GoogleStorageResource(storage, location);
		Assert.assertTrue(resource.isCreateBlobIfNotExists());
	}

	@Configuration
	@Import(GoogleStorageProtocolResolver.class)
	static class StorageApplication {

		@Bean
		public static Storage mockStorage() throws Exception {
			Storage storage = Mockito.mock(Storage.class);
			BlobId validBlob = BlobId.of("test-spring", "images/spring.png");
			Blob mockedBlob = Mockito.mock(Blob.class);
			WriteChannel writeChannel = Mockito.mock(WriteChannel.class);
			Mockito.when(mockedBlob.exists()).thenReturn(true);
			Mockito.when(mockedBlob.getSize()).thenReturn(4096L);
			Mockito.when(storage.get(Mockito.eq(validBlob))).thenReturn(mockedBlob);
			Mockito.when(mockedBlob.writer()).thenReturn(writeChannel);
			return storage;
		}
	}

}
