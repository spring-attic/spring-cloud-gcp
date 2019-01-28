/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.core.mapping.event;

import com.google.cloud.spanner.Statement;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DML after-execution event.
 *
 * @author Chengyuan Zhao
 */
public class AfterExecuteDmlEventTest {

	@Test
	public void equalsHashcodeTest() {
		AfterExecuteDmlEvent afterExecuteDmlEventa1 = new AfterExecuteDmlEvent(Statement.of("a"), 1L);
		AfterExecuteDmlEvent afterExecuteDmlEventa1x = new AfterExecuteDmlEvent(Statement.of("a"), 1L);

		AfterExecuteDmlEvent afterExecuteDmlEventa2 = new AfterExecuteDmlEvent(Statement.of("a"), 2L);

		AfterExecuteDmlEvent afterExecuteDmlEventb1 = new AfterExecuteDmlEvent(Statement.of("b"), 1L);
		AfterExecuteDmlEvent afterExecuteDmlEventb2 = new AfterExecuteDmlEvent(Statement.of("b"), 2L);

		assertThat(afterExecuteDmlEventa1).isEqualTo(afterExecuteDmlEventa1);
		assertThat(afterExecuteDmlEventa1).isEqualTo(afterExecuteDmlEventa1x);
		assertThat(afterExecuteDmlEventa1).isNotEqualTo(afterExecuteDmlEventa2);
		assertThat(afterExecuteDmlEventb1).isNotEqualTo(afterExecuteDmlEventb2);
		assertThat(afterExecuteDmlEventa1).isNotEqualTo(afterExecuteDmlEventb2);
		assertThat(afterExecuteDmlEventa1).isNotEqualTo(null);
		assertThat(afterExecuteDmlEventa1).isNotEqualTo(new Object());

		assertThat(afterExecuteDmlEventa1.hashCode()).isEqualTo(afterExecuteDmlEventa1.hashCode());
		assertThat(afterExecuteDmlEventa1.hashCode()).isEqualTo(afterExecuteDmlEventa1x.hashCode());
		assertThat(afterExecuteDmlEventa1.hashCode()).isNotEqualTo(afterExecuteDmlEventa2.hashCode());
		assertThat(afterExecuteDmlEventb1.hashCode()).isNotEqualTo(afterExecuteDmlEventb2.hashCode());
		assertThat(afterExecuteDmlEventa1.hashCode()).isNotEqualTo(afterExecuteDmlEventb2.hashCode());
	}

}
