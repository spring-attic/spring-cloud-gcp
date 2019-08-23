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

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
public final class SliceUtil {

	private SliceUtil() {
	}

	/**
	 * Cut array into slices of a given size and call consumer on each of them.
	 * @param <T> the type of the elements.
	 * @param elements the array to be sliced.
	 * @param sliceSize the max size of a slice.
	 * @param consumer the consumer to be called on every slice.
	 */
	public static <T> void sliceAndExecute(T[] elements, int sliceSize, Consumer<T[]> consumer) {
		int num_slices = (int) (Math.ceil((double) elements.length / sliceSize));
		for (int i = 0; i < num_slices; i++) {
			int start = i * sliceSize;
			int end = Math.min(start + sliceSize, elements.length);

			T[] slice = Arrays.copyOfRange(elements, start, end);
			consumer.accept(slice);
		}
	}
}
