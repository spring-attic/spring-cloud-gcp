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

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.ModelMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Code sample that shows how Spring Cloud GCP can be leveraged to use Google Cloud Client
 * Libraries.
 *
 * <p>
 * This uses the Cloud Vision API with the {@link ImageAnnotatorClient}, which is
 * configured and provided by the spring-cloud-gcp-starter-vision module.
 *
 * @author João André Martins
 * @author Daniel Zou
 */
@RestController
public class VisionController {

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private ImageAnnotatorClient imageAnnotatorClient;

	/**
	 * This method downloads an image from a URL and sends its contents to the Vision API for label detection.
	 *
	 * @param imageUrl the URL of the image
	 * @return a string with the list of labels and percentage of certainty
	 * @throws Exception if the Vision API call produces an error
	 */
	@GetMapping("/vision")
	public ModelAndView uploadImage(String imageUrl, ModelMap map) throws Exception {
		// Copies the content of the image to memory.
		byte[] imageBytes = StreamUtils.copyToByteArray(this.resourceLoader.getResource(imageUrl).getInputStream());

		BatchAnnotateImagesResponse responses;

		Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();

		// Sets the type of request to label detection, to detect broad sets of categories in an image.
		Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
		AnnotateImageRequest request =
				AnnotateImageRequest.newBuilder().setImage(image).addFeatures(feature).build();
		responses = this.imageAnnotatorClient.batchAnnotateImages(Collections.singletonList(request));

		// We're only expecting one response.
		if (responses.getResponsesCount() == 1) {
			AnnotateImageResponse response = responses.getResponses(0);

			ImmutableMap.Builder<String, Float> annotations = ImmutableMap.builder();
			for (EntityAnnotation annotation : response.getLabelAnnotationsList()) {
				annotations.put(annotation.getDescription(), annotation.getScore());
			}
			map.addAttribute("annotations", annotations.build());
		}

		map.addAttribute("imageUrl", imageUrl);

		return new ModelAndView("result", map);
	}
}
