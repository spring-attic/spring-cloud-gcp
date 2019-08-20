/*
 * Copyright 2018-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.core.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.gcp.data.datastore.core.util.SliceUtil.sliceAndExecute;

/**
 * @author Dmitry Solomakha
 */
public class SliceUtilTest {
	@Test
	public void sliceAndExecuteTest() {
		Integer[] elements = getIntegers(7);
		List<Integer[]> slices = new ArrayList<>();
		sliceAndExecute(elements, 3, slice -> {
			slices.add(slice);
		});
		assertThat(slices).containsExactly(new Integer[] { 0, 1, 2 }, new Integer[] { 3, 4, 5 }, new Integer[] { 6 });
	}

	@Test
	public void sliceAndExecuteEvenTest() {
		Integer[] elements = getIntegers(6);
		List<Integer[]> slices = new ArrayList<>();
		sliceAndExecute(elements, 3, slice -> {
			slices.add(slice);
		});
		assertThat(slices).containsExactly(new Integer[] { 0, 1, 2 }, new Integer[] { 3, 4, 5 });
	}

	@Test
	public void sliceAndExecuteEmptyTest() {
		Integer[] elements = getIntegers(0);
		List<Integer[]> slices = new ArrayList<>();
		sliceAndExecute(elements, 3, slice -> {
			slices.add(slice);
		});
		assertThat(slices).isEmpty();
	}

	private Integer[] getIntegers(Integer inputSize) {
		Integer[] elements = new Integer[inputSize];
		for (int i = 0; i < inputSize; i++) {
			elements[i] = i;
		}
		return elements;
	}

}
