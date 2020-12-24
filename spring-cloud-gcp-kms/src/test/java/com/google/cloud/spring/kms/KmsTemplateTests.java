/*
 * Copyright 2017-2020 the original author or authors.
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

package com.google.cloud.spring.kms;

import java.util.Base64;

import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.kms.v1.DecryptRequest;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptRequest;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import com.google.protobuf.Int64Value;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KmsTemplateTests {

	private KeyManagementServiceClient client;

	private KmsTemplate kmsTemplate;

	@Before
	public void setupMocks() {
		this.client = mock(KeyManagementServiceClient.class);
		this.kmsTemplate = new KmsTemplate(this.client, () -> "my-project");
	}

	@Test
	public void testEncryptDecrypt() {
		EncryptResponse encryptResponse = createEncryptResponse();
		DecryptResponse decryptResponse = createDecryptResponse();

		when(this.client.encrypt(any(EncryptRequest.class))).thenReturn(encryptResponse);
		when(this.client.decrypt(any(DecryptRequest.class))).thenReturn(decryptResponse);

		String cryptoKeyNameStr = "test-project/europe-west2/key-ring-id/key-id";

		byte[] encryptedBytes = kmsTemplate.encryptText(cryptoKeyNameStr, "1234");
		String decryptedText = kmsTemplate.decryptText(cryptoKeyNameStr, encryptedBytes);

		Assert.assertEquals("1234", decryptedText);
	}

	@Test(expected = com.google.cloud.spring.kms.KmsException.class)
	public void testEncryptCorrupt() {
		EncryptResponse encryptResponse = EncryptResponse.newBuilder()
				.setCiphertext(ByteString.copyFromUtf8("invalid"))
				.setCiphertextCrc32C(Int64Value.newBuilder().setValue(0L).build())
				.build();

		when(this.client.encrypt(any(EncryptRequest.class))).thenReturn(encryptResponse);

		String cryptoKeyNameStr = "test-project/europe-west2/key-ring-id/key-id";
		kmsTemplate.encryptText(cryptoKeyNameStr, "1234");
	}

	@Test(expected = com.google.cloud.spring.kms.KmsException.class)
	public void testDecryptCorrupt() {
		DecryptResponse decryptResponse = DecryptResponse.newBuilder()
				.setPlaintext(ByteString.copyFromUtf8("1234"))
				.setPlaintextCrc32C(Int64Value.newBuilder().setValue(0L).build())
				.build();

		when(this.client.decrypt(any(DecryptRequest.class))).thenReturn(decryptResponse);

		String cryptoKeyNameStr = "test-project/europe-west2/key-ring-id/key-id";
		kmsTemplate.decryptText(cryptoKeyNameStr, "1234".getBytes());
	}

	@Test(expected = com.google.api.gax.rpc.InvalidArgumentException.class)
	public void testEncryptDecryptMissMatch() {
		EncryptResponse encryptResponse = createEncryptResponse();

		when(this.client.encrypt(any(EncryptRequest.class))).thenReturn(encryptResponse);
		when(this.client.decrypt(any(DecryptRequest.class))).thenThrow(InvalidArgumentException.class);

		String cryptoKeyNameStr = "test-project/europe-west2/key-ring-id/key-id";

		byte[] encryptedBytes = kmsTemplate.encryptText(cryptoKeyNameStr, "1234");
		kmsTemplate.decryptText(cryptoKeyNameStr, encryptedBytes);
	}

	@Test(expected = com.google.api.gax.rpc.PermissionDeniedException.class)
	public void testEncryptPermissionDenied() {
		when(this.client.encrypt(any(EncryptRequest.class))).thenThrow(PermissionDeniedException.class);

		String cryptoKeyNameStr = "test-project/europe-west2/no-access/key-id";

		byte[] encryptedBytes = kmsTemplate.encryptText(cryptoKeyNameStr, "1234");
		kmsTemplate.decryptText(cryptoKeyNameStr, encryptedBytes);
	}

	@Test(expected = com.google.api.gax.rpc.NotFoundException.class)
	public void testEncryptNotFound() {
		when(this.client.encrypt(any(EncryptRequest.class))).thenThrow(NotFoundException.class);

		String cryptoKeyNameStr = "test-project/europe-west2/key-ring-id/not-found";

		byte[] encryptedBytes = kmsTemplate.encryptText(cryptoKeyNameStr, "1234");
		kmsTemplate.decryptText(cryptoKeyNameStr, encryptedBytes);
	}

	private DecryptResponse createDecryptResponse() {
		return DecryptResponse.newBuilder()
					.setPlaintext(ByteString.copyFromUtf8("1234"))
					.setPlaintextCrc32C(Int64Value.newBuilder().setValue(4131058926L).build())
					.build();
	}

	private EncryptResponse createEncryptResponse() {
		String kmsEncryptedText = "CiQAIVnQi1mE20Me07FF9myX9IUDmx1HmvUre6/VrVbTcJYY9L4SLQAe63KkBSKEz+TmOTXyMxeS/M1DtaPk3rVIBWyshVeZ+FhUHMoOAu6Fx6Shkw==";
		byte[] encryptedBytes = Base64.getDecoder().decode(kmsEncryptedText.getBytes());

		return EncryptResponse.newBuilder()
				.setCiphertext(ByteString.copyFrom(encryptedBytes))
				.setCiphertextCrc32C(Int64Value.newBuilder().setValue(1171937405L).build())
				.build();
	}

}
