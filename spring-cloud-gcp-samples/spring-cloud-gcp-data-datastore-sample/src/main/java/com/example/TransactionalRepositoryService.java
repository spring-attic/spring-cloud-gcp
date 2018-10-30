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

package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * A transactional service that provides methods run as transactions.
 * @author Chengyuan Zhao
 */
public class TransactionalRepositoryService {

  @Autowired
  private SingerRepository singerRepository;

  @Transactional
  public void testSaveAndStateConstantInTransaction(List<TestEntity> testEntities,
      long waitMillisecondsForConfirmation)
      throws InterruptedException {

    for (TestEntity testEntity : testEntities) {
      assertNull(this.datastoreTemplate.findById(testEntity.getId(),
          TestEntity.class));
    }

    this.datastoreTemplate.saveAll(testEntities);

    // Because these saved entities should NOT appear when we subsequently check, we
    // must wait a period of time that would see a non-transactional save go through.
    Thread.sleep(waitMillisecondsForConfirmation);

    // Datastore transactions always see the state at the start of the transaction. Even
    // after waiting these entities should not be found.
    for (TestEntity testEntity : testEntities) {
      assertNull(this.datastoreTemplate.findById(testEntity.getId(),
          TestEntity.class));
    }
  }

  @Transactional
  public void testSaveInTransactionFailed(List<TestEntity> testEntities) {
    this.datastoreTemplate.saveAll(testEntities);
    throw new RuntimeException("Intentional failure to cause rollback.");
  }
}
