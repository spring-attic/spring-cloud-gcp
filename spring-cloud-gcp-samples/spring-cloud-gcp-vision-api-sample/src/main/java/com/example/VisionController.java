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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

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
import com.google.protobuf.Descriptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author João André Martins
 */
@RestController
public class VisionController {

	@Autowired
	private CredentialsProvider credentialsProvider;

	@GetMapping("/vision")
	public String uploadImage(String path) throws Exception {
		String result = "";

		ImageAnnotatorSettings clientSettings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.build();

		Path imagePath = Paths.get(path);
		BatchAnnotateImagesResponse responses;

		try (ImageAnnotatorClient client = ImageAnnotatorClient.create(clientSettings)) {
			Image image = Image.newBuilder().setContent(
					ByteString.copyFrom(Files.readAllBytes(imagePath))).build();
			Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
			AnnotateImageRequest request =
					AnnotateImageRequest.newBuilder().setImage(image).addFeatures(feature).build();
			responses = client.batchAnnotateImages(Collections.singletonList(request));
		}

		for (AnnotateImageResponse response : responses.getResponsesList()) {
			if (response.hasError()) {
				result += response.getError().getMessage();
			}
			else {
				for (EntityAnnotation annotation : response.getLabelAnnotationsList()) {
					for (Map.Entry<Descriptors.FieldDescriptor, Object> entry :
							annotation.getAllFields().entrySet()) {
						result += entry.getKey().getName() + " " + entry.getValue().toString() + "<br/>";
					}
				}
			}
		}

		return result;
	}
}
