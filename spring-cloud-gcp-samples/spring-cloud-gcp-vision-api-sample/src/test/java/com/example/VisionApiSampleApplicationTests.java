/*
 * Copyright 2017-2018 the original author or authors.
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

package com.example;

import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.vision.v1.EntityAnnotation;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * This test sends images to the GCP Vision API and verifies the returned image
 * annotations.
 *
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class VisionApiSampleApplicationTests {

	private static final String LABEL_IMAGE_URL = "/extractLabels?imageUrl=classpath:static/boston-terrier.jpg";

	private static final String TEXT_IMAGE_URL = "/extractText?imageUrl=classpath:static/stop-sign.jpg";

	@Autowired
	private MockMvc mockMvc;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"Vision Sample integration tests are disabled. Please use '-Dit.vision=true' "
						+ "to enable them.",
				System.getProperty("it.vision"), is("true"));
	}

	@Test
	public void testExtractTextFromImage() throws Exception {
		this.mockMvc.perform(get(TEXT_IMAGE_URL))
				.andDo((response) -> {
					String textImageResponse = response.getResponse().getContentAsString();
					assertThat(textImageResponse).isEqualTo("Text from image: STOP\n");
				});
	}

	@Test
	public void testClassifyImageLabels() throws Exception {
		this.mockMvc.perform(get(LABEL_IMAGE_URL))
				.andDo((response) -> {
					ModelAndView result = response.getModelAndView();
					List<EntityAnnotation> annotations = (List<EntityAnnotation>) result.getModelMap().get("annotations");

					List<String> annotationNames = annotations.stream()
							.map(annotation -> annotation.getDescription().toLowerCase().trim())
							.collect(Collectors.toList());

					assertThat(annotationNames).contains("boston terrier");
				});
	}
}
