/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.storage;

import java.io.IOException;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoogleStorageResourceTest {

	@Test
	public void testConstructorValidation() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new GoogleStorageResource(null, "gs://foo", false))
				.withMessageContaining("Storage object can not be null");

		Storage mockStorage = mock(Storage.class);
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new GoogleStorageResource(mockStorage, (String) null, false));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new GoogleStorageResource(mockStorage, "", false));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new GoogleStorageResource(mockStorage, "foo", false));

		assertThat(new GoogleStorageResource(mockStorage, "gs://foo", true)).isNotNull();
		assertThat(new GoogleStorageResource(mockStorage, "gs://foo/bar", true)).isNotNull();
	}

	@Test
	public void getURL_Bucket() throws IOException {
		Storage mockStorage = mock(Storage.class);
		Bucket mockBucket = mock(Bucket.class);

		when(mockStorage.get("my-bucket")).thenReturn(mockBucket);
		when(mockBucket.getSelfLink()).thenReturn("https://www.googleapis.com/storage/v1/b/my-bucket");

		GoogleStorageResource gsr = new GoogleStorageResource(mockStorage, "gs://my-bucket");
		assertThat(gsr.getURL()).isNotNull();
	}

	@Test
	public void getURL_Object() throws IOException {
		Storage mockStorage = mock(Storage.class);
		Blob mockBlob = mock(Blob.class);

		when(mockStorage.get(BlobId.of("my-bucket", "my-object"))).thenReturn(mockBlob);
		when(mockBlob.getSelfLink()).thenReturn("https://www.googleapis.com/storage/v1/b/my-bucket/o/my-object");

		GoogleStorageResource gsr = new GoogleStorageResource(mockStorage, "gs://my-bucket/my-object");
		assertThat(gsr.getURL()).isNotNull();

	}
}
