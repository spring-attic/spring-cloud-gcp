/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.List;

import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.cloud.gcp.data.datastore.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author Chengyuan Zhao
 */
public interface TestEntityRepository extends DatastoreRepository<TestEntity, Long> {

	@Query("select * from  test_entities_ci where id = @id_val")
	List<TestEntity> findEntitiesWithCustomQuery(@Param("id_val") long id);

	@Query(value = "select size from  test_entities_ci where size <= @size", count = true)
	int countEntitiesWithCustomQuery(@Param("size") long size);

	@Query(value = "select * from  test_entities_ci where id = @id_val", exists = true)
	boolean existsByEntitiesWithCustomQuery(@Param("id_val") long id);

	@Query("select id from  test_entities_ci where id <= @id_val ")
	List<TestEntity> findEntitiesWithCustomProjectionQuery(@Param("id_val") long id);

	long countBySizeAndColor(long size, String color);

	List<TestEntity> findTop3BySizeAndColor(long size, String color);
}
