/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.repository.it;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.test.IntegrationTestConfiguration;
import org.springframework.cloud.gcp.data.spanner.test.domain.Singer;
import org.springframework.cloud.gcp.data.spanner.test.domain.SingerRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Integration tests for Spanner Repository.
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { IntegrationTestConfiguration.class })
public class SpannerRepositoryInsertIntegrationTests {

	@Autowired
	SingerRepository singerRepository;

	@Autowired
	SpannerTemplate spannerTemplate;

	@Autowired
	protected SpannerSchemaUtils spannerSchemaUtils;

	@Autowired
	SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(System.getProperty("it.spanner"))
						.as("Spanner integration tests are disabled. "
										+ "Please use '-Dit.spanner=true' to enable them. ")
						.isEqualTo("true");
	}

	@Before
	public void setUp() {
		if (!this.spannerDatabaseAdminTemplate.tableExists("singers_list")) {
			this.spannerDatabaseAdminTemplate.executeDdlStrings(
							Collections.singleton(this.spannerSchemaUtils.getCreateTableDdlString(Singer.class)),
							true);
		}
		this.singerRepository.deleteAll();
	}

	@After
	public void clearData() {
		this.singerRepository.deleteAll();
	}

	@Test
	public void insertTest() {
		singerRepository.insert(1, "Cher", null);
		Iterable<Singer> singers = singerRepository.findAll();
		assertThat(singers).containsExactly(new Singer(1, "Cher", null));
	}
}
