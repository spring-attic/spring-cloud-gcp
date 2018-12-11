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

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTests {

  @Autowired
  private lateinit var testRestTemplate: TestRestTemplate

  @Before
  fun prepare() {
    assumeThat<String>(
        "Kotlin Sample app tests are disabled. Please use '-Dit.kotlin=true' to enable them.",
        System.getProperty("it.kotlin"), `is`("true"))
  }
  @Test
  fun testStartupSuccessful() {
    val response = testRestTemplate.getForObject("/", String::class.java)
    assertThat(response).isEqualTo("Hello World!")
  }
}
