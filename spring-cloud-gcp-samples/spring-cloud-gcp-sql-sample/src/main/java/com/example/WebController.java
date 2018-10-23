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

import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author João André Martins
 */
@RestController
public class WebController {

	private final JdbcTemplate jdbcTemplate;
	UserRepository repository;

	public WebController(JdbcTemplate jdbcTemplate, UserRepository repository) {
		this.jdbcTemplate = jdbcTemplate;
		this.repository = repository;
	}

	@GetMapping("/getTuples")
	public List getTuples() {
		/*return this.jdbcTemplate.queryForList("SELECT * FROM users").stream()
				.map(Map::values)
				.collect(Collectors.toList());
				*/
		List<Map> list = new ArrayList<>();
		repository.findAll().forEach( e -> {
			System.out.println("got an element: " + e);
			Map<String,String> m = new HashMap<>();
			m.put("first_name", e.getFirstName());
			m.put("last_name", e.getFirstName());
			m.put("id", e.getFirstName());
			list.add(m);
		});
		return list;
	}
}
