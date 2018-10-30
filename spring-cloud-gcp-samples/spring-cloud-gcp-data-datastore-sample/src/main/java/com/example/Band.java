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

import java.util.List;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Reference;
import org.springframework.data.annotation.Id;

/**
 * A group of singers. {@link Singer}s and bands can have complicated relationships
 * where singers can be part of many bands and bands can have many singers.
 *
 * @author Chengyuan
 */
@Entity
public class Band {

  @Id
  private String name;

  @Reference
  private List<Singer> singerList;

  public Band(String name, List<Singer> singers){
    this.name = name;
    this.singerList = singers;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Singer> getSingerList() {
    return this.singerList;
  }

  public void setSingerList(List<Singer> singerList) {
    this.singerList = singerList;
  }
}
