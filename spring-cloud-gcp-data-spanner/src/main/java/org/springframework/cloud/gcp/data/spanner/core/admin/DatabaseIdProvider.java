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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.util.function.Supplier;

import com.google.cloud.spanner.DatabaseId;

/**
 * A provider of {@code DatabaseId} that is used to determine the target Cloud Spanner
 * databases on which to act.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public interface DatabaseIdProvider extends Supplier<DatabaseId> {
}
