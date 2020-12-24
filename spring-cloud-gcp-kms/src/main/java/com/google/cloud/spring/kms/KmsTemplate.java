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

import java.nio.charset.StandardCharsets;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptRequest;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptRequest;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import com.google.protobuf.Int64Value;

/**
 * Offers convenience methods for performing common operations on KMS including
 * encrypting and decrypting text.
 *
 * @author Emmanouil Gkatziouras
 */
public class KmsTemplate implements KmsOperations {

	private final KeyManagementServiceClient client;

	private final GcpProjectIdProvider projectIdProvider;

	public KmsTemplate(
			KeyManagementServiceClient keyManagementServiceClient,
			GcpProjectIdProvider projectIdProvider) {
		this.client = keyManagementServiceClient;
		this.projectIdProvider = projectIdProvider;
	}

	@Override
	public byte[] encryptText(String cryptoKey, String text) {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		return encryptBytes(cryptoKey, bytes);
	}

	@Override
	public byte[] encryptBytes(String cryptoKey, byte[] bytes) {
		CryptoKeyName cryptoKeyName = KmsPropertyUtils.getCryptoKeyName(cryptoKey, projectIdProvider);

		long crc32c = longCrc32c(bytes);

		EncryptRequest request = EncryptRequest.newBuilder()
				.setName(cryptoKeyName.toString())
				.setPlaintext(ByteString.copyFrom(bytes))
				.setPlaintextCrc32C(
						Int64Value.newBuilder().setValue(crc32c).build())
				.build();

		EncryptResponse response = client.encrypt(request);
		assertCrcMatch(response);
		return response.getCiphertext().toByteArray();
	}

	@Override
	public String decryptText(String cryptoKey, byte[] cipherText) {
		byte[] decryptedBytes = decryptBytes(cryptoKey, cipherText);
		return new String(decryptedBytes, StandardCharsets.UTF_8);
	}

	@Override
	public byte[] decryptBytes(String cryptoKey, byte[] cipherText) {
		CryptoKeyName cryptoKeyName = KmsPropertyUtils.getCryptoKeyName(cryptoKey, projectIdProvider);

		ByteString encryptedByteString = ByteString.copyFrom(cipherText);
		long crc32c = longCrc32c(encryptedByteString);

		DecryptRequest request =
				DecryptRequest.newBuilder()
						.setName(cryptoKeyName.toString())
						.setCiphertext(encryptedByteString)
						.setCiphertextCrc32C(
								Int64Value.newBuilder().setValue(crc32c).build())
						.build();

		DecryptResponse response = client.decrypt(request);
		assertCrcMatch(response);
		return response.getPlaintext().toByteArray();
	}

	private long longCrc32c(ByteString plaintextByteString) {
		return longCrc32c(plaintextByteString.toByteArray());
	}

	private long longCrc32c(byte[] bytes) {
		return Hashing.crc32c().hashBytes(bytes).padToLong();
	}

	private void assertCrcMatch(EncryptResponse response) {
		long expected = response.getCiphertextCrc32C().getValue();
		long received = longCrc32c(response.getCiphertext());

		if (expected != received) {
			throw new KmsException("Encryption: response from server corrupted");
		}
	}

	private void assertCrcMatch(DecryptResponse response) {
		long expected = response.getPlaintextCrc32C().getValue();
		long received = longCrc32c(response.getPlaintext());

		if (expected != received) {
			throw new KmsException("Decryption : response from server corrupted");
		}
	}

}
