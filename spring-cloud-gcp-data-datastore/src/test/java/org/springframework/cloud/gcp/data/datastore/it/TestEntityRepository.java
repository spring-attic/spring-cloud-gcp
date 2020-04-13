/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.cloud.datastore.Key;

import org.springframework.cloud.gcp.data.datastore.it.TestEntity.Shape;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.cloud.gcp.data.datastore.repository.query.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

/**
 * A repository for testing Query Methods that uses many advanced features.
 *
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
public interface TestEntityRepository extends DatastoreRepository<TestEntity, Long> {

	@Query("select * from  test_entities_ci where size = @size ")
	LinkedList<TestEntity> findEntitiesWithCustomQuery(@Param("size") long size);

	@Query("select * from  test_entities_ci where size = @size and __key__ = @id")
	LinkedList<TestEntity> findEntitiesWithCustomQueryWithId(@Param("size") long size, @Param("id") Key id);

	@Query("select * from  test_entities_ci where color = @color")
	Slice<TestEntity> findEntitiesWithCustomQuerySlice(@Param("color") String color, Pageable pageable);

	@Query("select * from  test_entities_ci where color = @color")
	Page<TestEntity> findEntitiesWithCustomQueryPage(@Param("color") String color, Pageable pageable);

	@Query("select * from  test_entities_ci")
	List<TestEntity> findEntitiesWithCustomQuerySort(Sort sort);

	@Query(value = "select size from  test_entities_ci where size <= @size", count = true)
	int countEntitiesWithCustomQuery(@Param("size") long size);

	int countBySize(long size);

	@Nullable
	List<TestEntity> getByColorAndIdGreaterThanEqualOrderById(String color, Long id);

	int deleteBySize(long size);

	void deleteBySizeEquals(long size);

	List<TestEntity> removeByColor(String color);

	List<TestEntity> findByShape(Shape shape);

	List<TestEntity> findByEmbeddedEntityStringField(String val);

	@Query("select * from test_entities_ci where shape = @enum_val")
	List<TestEntity> findByEnumQueryParam(@Param("enum_val") Shape shape);

	@Query(value = "select __key__ from |org.springframework.cloud.gcp.data.datastore.it.TestEntity| "
			+ "where size = :#{#size}", exists = true)
	boolean existsByEntitiesWithCustomQuery(@Param("size") long size);

	@Query("select size from  test_entities_ci where size <= @size ")
	TestEntity[] findEntitiesWithCustomProjectionQuery(@Param("size") long size);

	@Query("select __key__ from test_entities_ci")
	Set<Key> getKeys();

	@Query("select color from test_entities_ci")
	Page<String> getColorsPage(Pageable p);

	@Query("select __key__ from test_entities_ci limit 1")
	Key getKey();

	// Also involves conversion from long id to String
	@Query("select size from  test_entities_ci where size <= @size ")
	long[] getSizes(@Param("size") long size);

	// Also involves conversion from long id to String
	@Query("select size from  test_entities_ci where size <= @size and size >= @size")
	long getOneSize(@Param("size") long size);

	@Query("select * from  test_entities_ci where size= @size")
	TestEntity getOneTestEntity(@Param("size") long size);

	long countBySizeAndColor(long size, String color);

	LinkedList<TestEntity> findTop3BySizeAndColor(long size, String color);

	@Query("select * from  test_entities_ci where size = @size")
	TestEntityProjection getBySize(@Param("size") long size);

	@Query("select * from test_entities_ci where size = @size")
	Slice<TestEntityProjection> getBySizeSlice(@Param("size") long size, Pageable pageable);

	@Query("select * from test_entities_ci where size = @size")
	Page<TestEntityProjection> getBySizePage(@Param("size") long size, Pageable pageable);

	@Query("select color from test_entities_ci where size = @size")
	Slice<String> getSliceStringBySize(@Param("size") long size, Pageable pageable);

	Slice<TestEntityProjection> findBySize(long size, Pageable pageable);

	Page<TestEntity> findByShape(Shape shape, Pageable pageable);

	Slice<TestEntity> findByColor(String color, Pageable pageable);

	Optional<TestEntity> findFirstByColor(String color);

	@Nullable
	TestEntity getByColor(String color);
}
