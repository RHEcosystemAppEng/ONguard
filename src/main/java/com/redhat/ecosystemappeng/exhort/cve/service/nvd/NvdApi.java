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
package com.redhat.ecosystemappeng.exhort.cve.service.nvd;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import com.redhat.ecosystemappeng.exhort.cve.model.nvd.NvdResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@Path("/rest/json/cves/2.0")
@RegisterRestClient(configKey = "nvd-api")
public interface NvdApi {

    static final long NVD_API_WINDOW_SECS = 30;

    @GET
    @ClientHeaderParam(name = "apiKey", value = "${api.nvd.apikey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Fallback(value = NvdFallbackService.class, applyOn = WebApplicationException.class, skipOn = ClientWebApplicationException.class)
    @CircuitBreaker(delay = NVD_API_WINDOW_SECS, delayUnit = ChronoUnit.SECONDS)
    NvdResponse getCve(@QueryParam("cveId") String cveId);

}
