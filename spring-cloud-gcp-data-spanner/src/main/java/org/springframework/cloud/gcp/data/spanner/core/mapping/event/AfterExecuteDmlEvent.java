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
import org.springframework.context.ApplicationEvent;

/**
 * This event is published immediately after a DML statement is executed. It contains the DML statement as well
 * as the number of rows affected.
 *
 * @author Chengyuan Zhao
 */
public class AfterExecuteDmlEvent extends ExecuteDmlEvent{

    private final long numberRowsAffected;

    /**
     * Constructor.
     * @param statement the DML statement that was executed.
     * @param numberRowsAffected the number of rows affected.
     */
    public AfterExecuteDmlEvent(Statement statement, long numberRowsAffected) {
        super(statement);
        this.numberRowsAffected = numberRowsAffected;
    }

    /**
     * Get the number of rows affected by the DML.
     * @return the number of rows affected.
     */
    public long getNumberRowsAffected() {
        return this.numberRowsAffected;
    }
}
