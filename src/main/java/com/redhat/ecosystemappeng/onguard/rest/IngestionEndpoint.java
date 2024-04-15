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
package com.redhat.ecosystemappeng.onguard.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.ecosystemappeng.onguard.service.IngestionService;

import io.quarkus.vertx.http.ManagementInterface;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
public class IngestionEndpoint {
  
  @Inject
  IngestionService svc;

  @Inject
  ObjectMapper mapper;

  public void registerManagementRoutes(@Observes ManagementInterface mi) {
    mi.router().get("/admin/status").handler(ctx -> {
      svc.getStatus().subscribe().with(i -> {
        try {
          ctx.response().setStatusCode(200).putHeader("Content-Type", MediaType.APPLICATION_JSON).end(mapper.writeValueAsString(i));
        } catch (JsonProcessingException e) {
          ctx.response().setStatusCode(500).end(e.getMessage());
        }
      });
    });
  }
}
