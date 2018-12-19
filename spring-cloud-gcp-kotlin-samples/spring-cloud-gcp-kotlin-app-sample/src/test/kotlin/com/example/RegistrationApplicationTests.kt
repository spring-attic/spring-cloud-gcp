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

package com.example

import com.example.app.Application
import com.example.data.Person
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.hamcrest.Matchers.`is`
import org.junit.Assume.assumeThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.util.concurrent.TimeUnit

/**
 * Tests to verify the Kotlin sample app.
 *
 * @author Daniel Zou
 *
 * @since 1.1
 */
@RunWith(SpringRunner::class)
@AutoConfigureMockMvc
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = [Application::class],
		properties = [
			"spring.datasource.password=test",
			"spring.cloud.gcp.sql.instance-connection-name=spring-cloud-gcp-ci:us-central1:testmysql",
			"spring.cloud.gcp.sql.database-name=code_samples_test_db",
			"spring.test.mockmvc.print=none"
		]
)
class RegistrationApplicationTests {

	private val REGISTER_USER_URL = "/registerPerson?firstName=Bob&lastName=Blob&email=bob@bob.com"

	private val REGISTRANTS_URL = "/registrants"

	@Autowired
	private lateinit var mockMvc: MockMvc

	companion object {

		@BeforeClass
		@JvmStatic
		fun prepare() {
			assumeThat<String>(
					"Kotlin Sample app tests are disabled. Please use '-Dit.kotlin=true' to enable them.",
					System.getProperty("it.kotlin"), `is`("true"))
		}
	}

	@Test
	fun testRegisterPersonSuccessful() {
		this.mockMvc.perform(post(REGISTER_USER_URL))

		await().atMost(60, TimeUnit.SECONDS).untilAsserted {
			val mvcResult = this.mockMvc
					.perform(get(REGISTRANTS_URL))
					.andReturn()

			val resultParams = mvcResult.modelAndView.modelMap
			val personsList = resultParams.get("personsList") as List<Person>
			assertThat(personsList).hasSize(1)

			val person = personsList[0]
			assertThat(person.firstName).isEqualTo("Bob")
			assertThat(person.lastName).isEqualTo("Blob")
			assertThat(person.email).isEqualTo("bob@bob.com")
		}
	}
}
