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

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;


/**
 * Test utility class to generate a pair of Public/Private keys. Used for testing JWT signing.
 *
 * @author Vinicius Carvalho
 * @author Elena Felder
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
		KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
		kpGenerator.initialize(2048);
		KeyPair keyPair = kpGenerator.generateKeyPair();

		X500Name issuerName = new X500Name("OU=spring-cloud-gcp,CN=firebase-auth-integration-test");
		this.privateKey =  keyPair.getPrivate();

		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				issuerName,
				BigInteger.valueOf(System.currentTimeMillis()),
				Date.from(Instant.now()), Date.from(Instant.now().plusMillis(1096 * 24 * 60 * 60)),
				issuerName, keyPair.getPublic());
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
		X509CertificateHolder certHolder = builder.build(signer);
		this.certificate = new JcaX509CertificateConverter().getCertificate(certHolder);
		this.publicKey = this.certificate.getPublicKey();
	}

	/**
	 *
	 * @return A PEM encoded string for the public key of the certificate.
	 * @throws CertificateEncodingException if certificate can't be encoded.
	 */
	public String getPublicKeyCertificate() throws CertificateEncodingException {
		StringWriter sw = new StringWriter();
		try (PemWriter pw = new PemWriter(sw)) {
			PemObjectGenerator gen = new JcaMiscPEMGenerator(this.certificate);
			pw.writeObject(gen);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sw.toString();
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}
}
