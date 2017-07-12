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

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
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
		OutputStream os = writableResource.getOutputStream();
		Assert.assertTrue(os instanceof GoogleStorageOutputStream);
	}

	@Test
	public void testGcsOutputStream() throws Exception {
		Blob blob = Mockito.mock(Blob.class);
		WriteChannel writeChannel = Mockito.mock(WriteChannel.class);
		Mockito.when(blob.writer()).thenReturn(writeChannel);

		GoogleStorageOutputStream os = new GoogleStorageOutputStream(blob);

		// test single byte
		os.write(99);
		Mockito.verify(writeChannel).write(ByteBuffer.wrap(new byte[] { 99 }));

		// test multiple bytes
		os.write("test".getBytes());
		Mockito.verify(writeChannel).write(ByteBuffer.wrap("test".getBytes()));
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
