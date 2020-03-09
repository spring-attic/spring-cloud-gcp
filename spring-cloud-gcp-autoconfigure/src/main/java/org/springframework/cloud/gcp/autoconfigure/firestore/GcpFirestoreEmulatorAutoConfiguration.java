package org.springframework.cloud.gcp.autoconfigure.firestore;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.datastore.GcpDatastoreAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.firestore.GcpFirestoreAutoConfiguration.FirestoreReactiveAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides autoconfiguration to use the Firestore emulator if enabled.
 *
 * @since 1.2.3
 * @author Daniel Zou
 */
@Configuration
@ConditionalOnProperty("spring.cloud.gcp.firestore.emulator.enabled")
@AutoConfigureBefore({
		FirestoreReactiveAutoConfiguration.class, GcpFirestoreAutoConfiguration.class})
@EnableConfigurationProperties(GcpFirestoreProperties.class)
public class GcpFirestoreEmulatorAutoConfiguration {

	private final String projectId;

	private final String hostPort;

	GcpFirestoreEmulatorAutoConfiguration(
			GcpFirestoreProperties properties,
			GcpProjectIdProvider projectIdProvider) throws IOException {
		this.projectId = projectIdProvider.getProjectId();
		this.hostPort = properties.getHostPort();
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreOptions firestoreOptions() {
		return FirestoreOptions.newBuilder()
				.setCredentials(fakeCredentials())
				.setProjectId(this.projectId)
				.setChannelProvider(
						InstantiatingGrpcChannelProvider.newBuilder()
								.setEndpoint(this.hostPort)
								.setChannelConfigurator(input -> input.usePlaintext())
								.build())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreGrpc.FirestoreStub firestoreGrpcStub() throws IOException {
		ManagedChannel channel = ManagedChannelBuilder
				.forAddress("localhost", 8080)
				.usePlaintext()
				.build();

		return FirestoreGrpc.newStub(channel)
				.withCallCredentials(MoreCallCredentials.from(fakeCredentials()))
				.withExecutor(Runnable::run);
	}

	private static Credentials fakeCredentials() {
		final Map<String, List<String>> headerMap = new HashMap<>();
		headerMap.put("Authorization", Collections.singletonList("Bearer owner"));
		headerMap.put("google-cloud-resource-prefix", Collections.singletonList("projects/my-project/databases/(default)"));

		return new Credentials() {
			@Override
			public String getAuthenticationType() {
				return null;
			}

			@Override
			public Map<String, List<String>> getRequestMetadata(URI uri) {
				return headerMap;
			}

			@Override
			public boolean hasRequestMetadata() {
				return true;
			}

			@Override
			public boolean hasRequestMetadataOnly() {
				return true;
			}

			@Override
			public void refresh() {
				// no-op
			}
		};
	}
}
