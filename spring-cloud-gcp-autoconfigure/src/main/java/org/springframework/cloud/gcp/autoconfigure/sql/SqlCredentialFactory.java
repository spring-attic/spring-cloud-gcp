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

package org.springframework.cloud.gcp.autoconfigure.sql;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloud.sql.CredentialFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.core.GcpScope;

/**
 * Returns the credentials that are written to a system property by the Cloud SQL starter.
 *
 * <p>Since the sockets factory creates an instance of this class by reflection and without any
 * arguments, the credential location must be in a place that this class knows without any context.
 *
 * @author João André Martins
 */
public class SqlCredentialFactory implements CredentialFactory {

	public static final String CREDENTIAL_LOCATION_PROPERTY_NAME =
			"GOOGLE_CLOUD_SQL_CREDS_LOCATION";

	private static final Log LOGGER = LogFactory.getLog(SqlCredentialFactory.class);

	@Override
	public Credential create() {
		// TODO(joaomartins): Consider supporting Spring Resources as credential locations. There
		// would need to be a way to createBlob or inject a Spring context here statically, to load
		// the resource from there.
		String credentialResourceLocation = System.getProperty(CREDENTIAL_LOCATION_PROPERTY_NAME);
		if (credentialResourceLocation == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(CREDENTIAL_LOCATION_PROPERTY_NAME + " property does not exist. "
						+ "Socket factory will use application default credentials.");
			}
			return null;
		}

		try {
			return GoogleCredential.fromStream(new FileInputStream(credentialResourceLocation))
					.createScoped(Collections.singleton(GcpScope.SQLADMIN.getUrl()));
		}
		catch (IOException ioe) {
			LOGGER.warn("There was an error loading Cloud SQL credential.", ioe);
			return null;
		}
	}
}
