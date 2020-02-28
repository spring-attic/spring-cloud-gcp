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

package org.springframework.cloud.gcp.security.firebase;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import sun.security.provider.X509Factory;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;



/**
 * Test utility class to generate a pair of Public/Private keys. Used for testing JWT signing.
 *
 * @author Vinicius Carvalho
 * @since 1.2.2
 */
public class RSAKeyGeneratorUtils {

	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private X509Certificate certificate;

	public RSAKeyGeneratorUtils() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(null, null);
		CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
		X500Name x500Name = new X500Name("www.springframework.org", "dev", "cloud", "NYC", "NY", "US");
		keypair.generate(2048);
		this.privateKey =  keypair.getPrivateKey();
		X509Certificate[] chain = new X509Certificate[1];
		chain[0] = keypair.getSelfCertificate(x500Name, new Date(), (long) 1096 * 24 * 60 * 60);
		this.certificate = chain[0];
		this.publicKey = this.certificate.getPublicKey();
	}

	/**
	 *
	 * @return A PEM encoded string for the public key of the certificate.
	 * @throws CertificateEncodingException if certificate can't be encoded.
	 */
	public String getPublicKeyCertificate() throws CertificateEncodingException {
		final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
		StringBuilder builder = new StringBuilder();
		builder.append(X509Factory.BEGIN_CERT + LINE_SEPARATOR);
		builder.append(encoder.encodeToString(certificate.getEncoded()) + LINE_SEPARATOR);
		builder.append(X509Factory.END_CERT);
		return builder.toString();
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}
}
