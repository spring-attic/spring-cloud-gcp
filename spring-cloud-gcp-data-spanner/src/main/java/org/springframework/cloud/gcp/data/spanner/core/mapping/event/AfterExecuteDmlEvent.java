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

package org.springframework.cloud.gcp.data.spanner.core.mapping.event;

import java.util.Objects;

import com.google.cloud.spanner.Statement;

/**
 * This event is published immediately after a DML statement is executed. It contains the
 * DML statement as well as the number of rows affected.
 *
 * @author Chengyuan Zhao
 */
public class AfterExecuteDmlEvent extends ExecuteDmlEvent {

	private final long numberOfRowsAffected;

	/**
	 * Constructor.
	 * @param statement the DML statement that was executed.
	 * @param numberOfRowsAffected the number of rows affected.
	 */
	public AfterExecuteDmlEvent(Statement statement, long numberOfRowsAffected) {
		super(statement);
		this.numberOfRowsAffected = numberOfRowsAffected;
	}

	/**
	 * Get the number of rows affected by the DML.
	 * @return the number of rows affected.
	 */
	public long getNumberOfRowsAffected() {
		return this.numberOfRowsAffected;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AfterExecuteDmlEvent that = (AfterExecuteDmlEvent) o;
		return getStatement().equals(that.getStatement()) && getNumberOfRowsAffected() == that.getNumberOfRowsAffected();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getStatement().hashCode(), getNumberOfRowsAffected());
	}
}
