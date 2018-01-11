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

package com.example;

import java.util.Collections;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Code sample that shows how Spring Cloud GCP can be leveraged to use Google Cloud Client Libraries.
 *
 * In this case, spring-cloud-gcp-starter-core autowires a {@link CredentialsProvider} object that provides the GCP
 * credentials, required to authenticate and authorize Vision API calls.
 *
 * @author João André Martins
 */
@RestController
public class VisionController {

	@Autowired
	private CredentialsProvider credentialsProvider;

	@Autowired
	private ResourceLoader resourceLoader;

	/**
	 * This method downloads an image from a URL and sends its contents to the Vision API for label detection.
	 *
	 * @param imageUrl the URL of the image
	 * @return a string with the list of labels and percentage of certainty
	 * @throws Exception if the Vision API call produces an error
	 */
	@GetMapping("/vision")
	public String uploadImage(String imageUrl) throws Exception {
		// Copies the content of the image to memory.
		byte[] imageBytes = StreamUtils.copyToByteArray(this.resourceLoader.getResource(imageUrl).getInputStream());

		ImageAnnotatorSettings clientSettings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.build();

		BatchAnnotateImagesResponse responses;

		// Initializes the API client and sends the request.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create(clientSettings)) {
			Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();
			// Sets the type of request to label detection, to detect broad sets of categories in an image.
			Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
			AnnotateImageRequest request =
					AnnotateImageRequest.newBuilder().setImage(image).addFeatures(feature).build();
			responses = client.batchAnnotateImages(Collections.singletonList(request));
		}

		StringBuilder responseBuilder = new StringBuilder("<table border=\"1\">");

		responseBuilder.append("<tr><th>description</th><th>score</th></tr>");

		// We're only expecting one response.
		if (responses.getResponsesCount() == 1) {
			AnnotateImageResponse response = responses.getResponses(0);

			if (response.hasError()) {
				throw new Exception(response.getError().getMessage());
			}

			for (EntityAnnotation annotation : response.getLabelAnnotationsList()) {
				responseBuilder.append("<tr><td>")
						.append(annotation.getDescription())
						.append("</td><td>")
						.append(annotation.getScore())
						.append("</td></tr>");
			}
		}

		responseBuilder.append("</table>");

		responseBuilder.append("<p><img src='" + imageUrl + "'/></p>");

		return responseBuilder.toString();
	}
}
