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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample REST Controller to demonstrate Spring Cloud Sleuth & Stackdriver Trace
 *
 * @author Ray Tsang
 */
@RestController
public class ExampleController {
	private static final Log LOGGER = LogFactory.getLog(ExampleController.class);

	private final WorkService workService;

	public ExampleController(WorkService workService) {
		this.workService = workService;
	}

	@RequestMapping("/")
	public String work() {
		this.workService.busyWork();
		return "finished";
	}

	@RequestMapping("/meet")
	public String meet() {
		long duration = 200L + (long) (Math.random() * 800L);
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException e) {
		}
		LOGGER.info("meeting took " + duration + "ms");
		return "meeting finished in " + duration + "ms";
	}
}
