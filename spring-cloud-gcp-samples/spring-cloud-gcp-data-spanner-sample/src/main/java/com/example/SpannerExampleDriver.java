/*
 *  Copyright 2018 original author or authors.
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

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class SpannerExampleDriver {

	private static final Log LOGGER = LogFactory.getLog(SpannerExampleDriver.class);

	@Autowired
	private SpannerRepositoryExample spannerRepositoryExample;

	@Autowired
	private SpannerTemplateExample spannerTemplateExample;

	public static void main(String[] args) {
		System.out.println(Arrays.toString(args));
		SpringApplication.run(SpannerExampleDriver.class, args);
	}

	@Bean
	@Profile("!test")
	ApplicationRunner applicationRunner() {
		return args -> {
			if (!args.containsOption("spanner_repository") && !args.containsOption("spanner_template")) {
				throw new IllegalArgumentException("To run the Spanner example, please specify "
						+ " -Dspring-boot.run.arguments=--spanner_repository to run the Spanner repository"
						+ " example or -Dspring-boot.run.arguments=--spanner_template to"
						+ " run the Spanner template example.");
			}

			if (args.containsOption("spanner_repository")) {
				LOGGER.info("Running the Spanner Repository Example.");
				this.spannerRepositoryExample.runExample();
			}
			else if (args.containsOption("spanner_template")) {
				LOGGER.info("Running the Spanner Template Example.");
				this.spannerTemplateExample.runExample();
			}
		};
	}
}
