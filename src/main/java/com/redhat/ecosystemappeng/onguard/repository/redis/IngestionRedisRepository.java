/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.ecosystemappeng.onguard.repository.redis;

import java.io.InputStream;

import com.redhat.ecosystemappeng.onguard.model.Ingestion;
import com.redhat.ecosystemappeng.onguard.repository.IngestionRepository;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IngestionRedisRepository implements IngestionRepository {

  private static final String SYNCS = "ingestions:updates";
  private static final int MAX_HISTORY = 100;

  private final ReactiveListCommands<String, Ingestion> syncCommands;

  public IngestionRedisRepository(ReactiveRedisDataSource ds) {
    this.syncCommands = ds.list(String.class, Ingestion.class);
  }

  @Override
  public Uni<Ingestion> getSync() {
    return syncCommands.lindex(SYNCS, 0);
  }

  @Override
  public Uni<Ingestion> saveSync(Ingestion ingestion) {
    return syncCommands.llen(SYNCS).chain(len -> {
      Uni<Ingestion> res = Uni.createFrom().item(ingestion);
      if(len == MAX_HISTORY) {
        res = syncCommands.rpop(SYNCS);
      }
      return res.chain(i -> syncCommands.lpush(SYNCS, ingestion)).replaceWith(ingestion);
    });
  }

  @Override
  public Uni<Ingestion> updateSync(Ingestion ingestion) {
    return syncCommands.lset(SYNCS, 0, ingestion).replaceWith(ingestion);
  }

  @Override
  public Uni<Void> deleteAll() {
    return syncCommands.getDataSource().flushall();
  }

}
