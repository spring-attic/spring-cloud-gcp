import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.GcsSpringIntegrationApplication;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * This test uploads a file to Google Cloud Storage and verifies that it was received in
 * the local directory specified in application-test.properties.
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(classes = { GcsSpringIntegrationApplication.class })
public class GcsSpringIntegrationTests {
	private static final String TEST_FILE_NAME = "test_file";

	@Value("${gcs-read-bucket}")
	private String cloudInputBucket;

	@Value("${local-directory}")
	private String outputFolder;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Spanner Google Cloud Storage integration tests are disabled. "
						+ "Please use '-Dit.storage=true' to enable them. ",
				System.getProperty("it.storage"), is("true"));
	}

	@Before
	public void setupTestEnvironment() {
		cleanupCloudStorage();
	}

	@After
	public void teardownTestEnvironment() throws IOException {
		cleanupCloudStorage();
		cleanupLocalDirectory();
	}

	@Test
	public void testBucketPropagatesFile() {
		Storage storage = StorageOptions.getDefaultInstance().getService();
		BlobId blobId = BlobId.of("gcp-storage-bucket-sample-input", TEST_FILE_NAME);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
		Blob blob = storage.create(blobInfo, "Hello World!".getBytes(UTF_8));

		await().atMost(15, TimeUnit.SECONDS)
				.untilAsserted(() -> {
					Path outputFile = Paths.get(outputFolder + "/" + TEST_FILE_NAME);
					assertThat(Files.exists(outputFile)).isTrue();
					assertThat(Files.isRegularFile(outputFile)).isTrue();

					String firstLine = Files.lines(outputFile).findFirst().get();
					assertThat(firstLine).isEqualTo("Hello World!");
				});
	}

	private void cleanupCloudStorage() {
		Storage storage = StorageOptions.getDefaultInstance().getService();
		Page<Blob> blobs = storage.list(cloudInputBucket);
		for (Blob blob : blobs.iterateAll()) {
			blob.delete();
		}
	}

	private void cleanupLocalDirectory() throws IOException {
		Path localDirectory = Paths.get(outputFolder);
		List<Path> files = Files.list(localDirectory).collect(Collectors.toList());
		for (Path file : files) {
			Files.delete(file);
		}
		Files.deleteIfExists(Paths.get(outputFolder));
	}
}
