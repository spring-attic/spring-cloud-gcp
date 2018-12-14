/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.app

import com.example.data.Person
import com.example.data.PersonRepository
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Starts the Kotlin Spring Boot sample application.
 *
 * @author Daniel Zou
 *
 * @since 1.1
 */
@SpringBootApplication()
@EnableJpaRepositories(basePackageClasses = [PersonRepository::class])
@EntityScan(basePackageClasses = [Person::class])
class Application

fun main(args: Array<String>) {
	SpringApplication.run(Application::class.java, *args)
}
