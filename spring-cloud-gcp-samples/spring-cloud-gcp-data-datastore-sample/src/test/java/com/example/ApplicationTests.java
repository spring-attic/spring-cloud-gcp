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

package com.example;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.output.TeeOutputStream;
import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

/**
 * These tests verifies that the datastore-sample works. In order to run it, use the
 *
 * -Dit.datastore=true -Dspring.cloud.gcp.sql.database-name=[...]
 * -Dspring.cloud.gcp.datastore.namespace=[...]
 *
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(classes = {
		DatastoreRepositoryExample.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationTests {

	private static PrintStream systemOut;

	private static ByteArrayOutputStream baos;

	final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private CommandLineRunner commandLineRunner;

	@Autowired
	private SingerRepository singerRepository;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Datastore-sample integration tests are disabled. Please use '-Dit.datastore=true' "
						+ "to enable them. ",
				System.getProperty("it.datastore"), is("true"));
		systemOut = System.out;
		baos = new ByteArrayOutputStream();
		TeeOutputStream out = new TeeOutputStream(systemOut, baos);
		System.setOut(new PrintStream(out));
	}

	@Test
	public void basicTest() throws Exception {
		Singer johnDoe = new Singer(null, "John", "Doe", null);
		Singer maryJane = new Singer(null, "Mary", "Jane", null);
		Singer scottSmith = new Singer(null, "Scott", "Smith", null);
		Singer frodoBaggins = new Singer(null, "Frodo", "Baggins", null);

		List<Singer> singersAsc = getSingers("/singers?sort=lastName,ASC");
		assertThat(singersAsc)
				.as("Verify ASC order")
				.containsExactly(johnDoe, maryJane, scottSmith);

		List<Singer> singersDesc = getSingers("/singers?sort=lastName,DESC");
		assertThat(singersDesc)
				.as("Verify DESC order")
				.containsExactly(scottSmith, maryJane, johnDoe);

		sendRequest("/singers", "{\"firstName\": \"Frodo\", \"lastName\": \"Baggins\"}",
				HttpMethod.POST);

		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> getSingers("/singers?sort=lastName,ASC").size() == 4);

		List<Singer> singersAfterInsertion = getSingers("/singers?sort=lastName,ASC");
		assertThat(singersAfterInsertion)
				.as("Verify post")
				.containsExactly(frodoBaggins, johnDoe, maryJane, scottSmith);

		sendRequest("/singers/singer1", null, HttpMethod.DELETE);

		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> getSingers("/singers?sort=lastName,ASC").size() == 3);

		List<Singer> singersAfterDeletion = getSingers("/singers?sort=lastName,ASC");
		assertThat(singersAfterDeletion)
				.as("Verify Delete")
				.containsExactly(frodoBaggins, maryJane, scottSmith);

		assertThat(baos.toString())
				.as("Verify relationships saved in transaction")
				.contains("Relationship links "
						+ "were saved between a singer, bands, and instruments in a single transaction: "
						+ "Singer{singerId='singer2', firstName='Mary', lastName='Jane', "
						+ "albums=[Album{albumName='a', date=2012-01-20}, Album{albumName='b', "
						+ "date=2018-02-12}], firstBand=band1, bands=band1,band2, "
						+ "personalInstruments=recorder,cow bell}");

		assertThat(
				this.singerRepository.findById("singer2").get().getPersonalInstruments()
						.stream().map(Instrument::getType).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("recorder", "cow bell");

		assertThat(
				this.singerRepository.findById("singer2").get().getBands().stream()
						.map(Band::getName).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("band1", "band2");

		assertThat(
				this.singerRepository.findById("singer3").get().getPersonalInstruments()
						.stream().map(Instrument::getType).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("triangle", "marimba");

		assertThat(
				this.singerRepository.findById("singer3").get().getBands().stream()
						.map(Band::getName).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("band3", "band2");

		assertThat(baos.toString()).contains("This concludes the sample.");
	}

	private String sendRequest(String url, String json, HttpMethod method) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("Content-Type", "application/json");

		HttpEntity<String> entity = new HttpEntity<>(json, map);
		ResponseEntity<String> response = this.restTemplate.exchange(url, method, entity,
				String.class);
		return response.getBody();
	}

	@SuppressWarnings("unchecked")
	private List<Singer> getSingers(String url) throws java.io.IOException {
		String response = this.restTemplate.getForObject(url, String.class);

		Map<String, Object> parsedResponse = this.mapper.readValue(response,
				new TypeReference<HashMap<String, Object>>() {
				});
		List<Map<String, Object>> singerMaps = (List<Map<String, Object>>) ((Map<String, Object>) parsedResponse
				.get("_embedded")).get("singers");

		return singerMaps.stream().map((som) -> new Singer(null,
				(String) som.get("firstName"), (String) som.get("lastName"), null))
				.collect(Collectors.toList());
	}
}
