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

import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Sample application.
 *
 * @author Joao Andre Martins
 */
@SpringBootApplication
public class DemoApplication {

	private static final Log LOGGER = LogFactory.getLog(DemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner houses(HouseRepository houseRepository) {
		return (args) -> {
			houseRepository.deleteAll();

			Stream.of(new House("111 8th Av., NYC"),
					new House("636 Avenue of the Americas, NYC"),
					new House("White House"),
					new House("Pentagon"))
					.forEach(houseRepository::save);

			LOGGER.info("Number of houses is " + houseRepository.count());
			houseRepository.findAll().forEach((house) -> LOGGER.info(house.getAddress()));
		};
	}
}
