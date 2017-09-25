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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author João André Martins
 */
@RestController
public class WebController {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@GetMapping("/getTuples")
	public List getTuples() {
		return this.jdbcTemplate.queryForList("SELECT * FROM users").stream()
				.map(Map::values)
				.collect(Collectors.toList());
	}

	@GetMapping("/populateDatabase")
	public String populateDatabase() {
		this.jdbcTemplate.execute("DROP TABLE users;");
		this.jdbcTemplate.execute("CREATE TABLE users ("
				+ "id int NOT NULL AUTO_INCREMENT,"
				+ "email VARCHAR(255),"
				+ "first_name VARCHAR(255),"
				+ "last_name VARCHAR(255),"
				+ "PRIMARY KEY (id));");
		this.jdbcTemplate.execute("INSERT INTO users (email, first_name, last_name) VALUES "
				+ "(\"luisao@gmail.com\", \"Anderson\", \"Silva\"),"
				+ "(\"jonas@gmail.com\", \"Jonas\", \"Gonçalves\"),"
				+ "(\"fejsa@gmail.com\", \"Ljubomir\", \"Fejsa\")");

		return "Database populated!";
	}
}
