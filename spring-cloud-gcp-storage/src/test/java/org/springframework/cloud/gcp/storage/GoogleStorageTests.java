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
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

	@Value("gs://test_spring/images/spring.png")
	private Resource remoteResourceWithUnderscore;

	@Value("gs://test-spring")
	private Resource bucketResource;

	@Value("gs://test-spring/")
	private Resource bucketResourceTrailingSlash;

	@Value("gs://")
	private Resource emptyPathResource;

	@Value("gs:///")
	private Resource slashPathResource;

	@Test
	public void testNullPaths() {
		Assert.assertFalse(this.emptyPathResource instanceof GoogleStorageResourceObject);
		Assert.assertFalse(this.emptyPathResource instanceof GoogleStorageResourceBucket);
		Assert.assertFalse(this.slashPathResource instanceof GoogleStorageResourceObject);
		Assert.assertFalse(this.slashPathResource instanceof GoogleStorageResourceBucket);
		Assert.assertTrue(this.remoteResource instanceof GoogleStorageResourceObject);
		Assert.assertTrue(this.bucketResource instanceof GoogleStorageResourceBucket);
		Assert.assertTrue(this.bucketResourceTrailingSlash instanceof GoogleStorageResourceBucket);
	}

	@Test
	public void testValidObject() throws Exception {
		Assert.assertTrue(this.remoteResource.exists());
		Assert.assertEquals(4096L, this.remoteResource.contentLength());
	}

	@Test
	public void testValidObjectWithUnderscore() throws Exception {
		Assert.assertTrue(this.remoteResourceWithUnderscore.exists());
	}

	@Test
	public void testValidBucket() throws IOException {
		Assert.assertEquals("test-spring", this.bucketResource.getDescription());
		Assert.assertEquals(this.bucketResource.getDescription(),
				this.bucketResourceTrailingSlash.getDescription());

		Assert.assertEquals("test-spring", this.bucketResource.getFilename());
		Assert.assertEquals(this.bucketResource.getFilename(),
				this.bucketResourceTrailingSlash.getFilename());

		Assert.assertEquals("gs://test-spring", this.bucketResource.getURI().toString());
		Assert.assertEquals(this.bucketResource.getURI().toString(),
				this.bucketResourceTrailingSlash.getURI().toString());


		String relative = this.bucketResource.createRelative("aaa/bbb").getURI()
				.toString();
		Assert.assertEquals("gs://test-spring/aaa/bbb", relative);
		Assert.assertEquals(relative,
				this.bucketResource.createRelative("/aaa/bbb").getURI().toString());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testBucketOutputStream() throws IOException {
		((WritableResource) this.bucketResource).getOutputStream();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testBucketInputStream() throws IOException {
		this.bucketResource.getInputStream();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testBucketContentLength() throws IOException {
		this.bucketResource.contentLength();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testBucketFile() throws IOException {
		this.bucketResource.getFile();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testBucketLastModified() throws IOException {
		this.bucketResource.lastModified();
	}

	@Test
	public void testBucketResourceStatuses() throws IOException {
		Assert.assertFalse(this.bucketResource.isOpen());
		Assert.assertFalse(this.bucketResource.isReadable());
		Assert.assertFalse(((WritableResource) this.bucketResource).isWritable());
		Assert.assertTrue(this.bucketResource.exists());
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
		Storage storage = mock(Storage.class);
		Blob blob = mock(Blob.class);
		WriteChannel writeChannel = mock(WriteChannel.class);
		when(blob.writer()).thenReturn(writeChannel);
		when(blob.exists()).thenReturn(true);
		when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(blob);

		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location);
		OutputStream os = resource.getOutputStream();
		Assert.assertNotNull(os);
	}

	@Test(expected = FileNotFoundException.class)
	public void testWritableOutputStreamNoAutoCreateOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = mock(Storage.class);
		when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(null);

		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location,
				false);
		resource.getOutputStream();
	}

	@Test
	public void testWritableOutputStreamWithAutoCreateOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = mock(Storage.class);
		BlobId blobId = BlobId.of("test-spring", "test");
		when(storage.get(blobId)).thenReturn(null);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
		WriteChannel writeChannel = mock(WriteChannel.class);
		Blob blob = mock(Blob.class);
		when(blob.writer()).thenReturn(writeChannel);
		when(storage.create(blobInfo)).thenReturn(blob);

		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location);
		GoogleStorageResourceObject spyResource = spy(resource);
		doReturn(this.bucketResource).when(spyResource).getResourceBucket();
		OutputStream os = spyResource.getOutputStream();
		Assert.assertNotNull(os);
	}

	@Test
	public void testWritableOutputStreamWithAutoCreateOnNonExistantBlob()
			throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = mock(Storage.class);
		BlobId blobId = BlobId.of("test-spring", "test");
		Blob nonExistantBlob = mock(Blob.class);
		when(nonExistantBlob.exists()).thenReturn(false);
		when(storage.get(blobId)).thenReturn(nonExistantBlob);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
		WriteChannel writeChannel = mock(WriteChannel.class);
		Blob blob = mock(Blob.class);
		when(blob.writer()).thenReturn(writeChannel);
		when(storage.create(blobInfo)).thenReturn(blob);


		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location);
		GoogleStorageResourceObject spyResource = spy(resource);
		doReturn(this.bucketResource).when(spyResource).getResourceBucket();
		OutputStream os = spyResource.getOutputStream();
		Assert.assertNotNull(os);
	}

	@Test(expected = FileNotFoundException.class)
	public void testGetInputStreamOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = mock(Storage.class);
		when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(null);

		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location,
				false);
		resource.getInputStream();
	}

	@Test
	public void testGetFilenameOnNullBlob() throws Exception {
		String location = "gs://test-spring/test";
		Storage storage = mock(Storage.class);
		when(storage.get(BlobId.of("test-spring", "test"))).thenReturn(null);

		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location,
				false);
		Assert.assertNull(resource.getFilename());
	}

	@Test
	public void testCreateBlobIfNotExistingGetterSetter() {
		String location = "gs://test-spring/test";
		Storage storage = mock(Storage.class);
		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location);
		Assert.assertTrue(resource.isCreateBlobIfNotExists());
	}

	@Test
	public void testCreateRelative() throws IOException {
		String location = "gs://test-spring/test.png";
		Storage storage = mock(Storage.class);
		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location);
		GoogleStorageResourceObject relative = (GoogleStorageResourceObject) resource.createRelative("relative.png");
		Assert.assertEquals("gs://test-spring/relative.png", relative.getURI().toString());
	}

	@Test
	public void testCreateRelativeSubdirs() throws IOException {
		String location = "gs://test-spring/t1/test.png";
		Storage storage = mock(Storage.class);
		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location);
		GoogleStorageResourceObject relative = (GoogleStorageResourceObject) resource.createRelative("r1/relative.png");
		Assert.assertEquals("gs://test-spring/t1/r1/relative.png", relative.getURI().toString());
	}

	@Test
	public void nullSignedUrlForNullBlob() throws IOException {
		String location = "gs://test-spring/t1/test.png";
		Storage storage = mock(Storage.class);
		GoogleStorageResourceObject resource =
				new GoogleStorageResourceObject(storage, location, false);
		when(storage.get(any(BlobId.class))).thenReturn(null);
		Assert.assertNull(resource.createSignedUrl(TimeUnit.DAYS, 1));
	}

	@Test
	public void signedUrlFunctionCalled() throws IOException {
		String location = "gs://test-spring/t1/test.png";
		Storage storage = mock(Storage.class);
		Blob blob = mock(Blob.class);
		when(blob.getBucket()).thenReturn("fakeBucket");
		when(blob.getName()).thenReturn("fakeObject");
		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(storage, location);
		when(storage.get(any(BlobId.class))).thenReturn(blob);
		Storage.SignUrlOption option = Storage.SignUrlOption.httpMethod(HttpMethod.PUT);
		resource.createSignedUrl(TimeUnit.DAYS, 1L, option);
		verify(storage, times(1)).signUrl(any(), eq(1L), eq(TimeUnit.DAYS), eq(option));
	}

	@Configuration
	@Import(GoogleStorageProtocolResolver.class)
	static class StorageApplication {

		@Bean
		public static Storage mockStorage() throws Exception {
			Storage storage = mock(Storage.class);
			BlobId validBlob = BlobId.of("test-spring", "images/spring.png");
			BlobId validBlobWithUnderscore = BlobId.of("test_spring", "images/spring.png");
			Bucket mockedBucket = mock(Bucket.class);
			Blob mockedBlob = mock(Blob.class);
			WriteChannel writeChannel = mock(WriteChannel.class);
			when(mockedBlob.exists()).thenReturn(true);
			when(mockedBucket.exists()).thenReturn(true);
			when(mockedBlob.getSize()).thenReturn(4096L);
			when(storage.get(eq(validBlob))).thenReturn(mockedBlob);
			when(storage.get(eq(validBlobWithUnderscore))).thenReturn(mockedBlob);
			when(storage.get("test-spring")).thenReturn(mockedBucket);
			when(mockedBucket.getName()).thenReturn("test-spring");
			when(mockedBlob.writer()).thenReturn(writeChannel);
			return storage;
		}
	}

}
