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
package com.redhat.ecosystemappeng.onguard.service.nvd;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.ecosystemappeng.onguard.model.Vulnerability;
import com.redhat.ecosystemappeng.onguard.model.nvd.Metrics;
import com.redhat.ecosystemappeng.onguard.model.nvd.NvdResponse;
import com.redhat.ecosystemappeng.onguard.model.nvd.NvdVulnerability;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NvdServiceImpl implements NvdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NvdServiceImpl.class);

    @RestClient
    NvdApi nvdApi;

    @Override
    public List<Vulnerability> bulkLoad(Integer index, Integer pageSize, LocalDateTime since) {
        NvdResponse response;
        if(since == null) {
            response = nvdApi.list(index, pageSize, null, null);
        } else {
            response = nvdApi.list(index, pageSize, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(since.atZone(ZoneId.systemDefault())), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(LocalDateTime.now().atZone(ZoneId.systemDefault())));
        }
        if(response == null) {
            return Collections.emptyList();
        }
        return response.vulnerabilities().stream().map(this::toVulnerability).toList();
    }
    
    @Override
    public Metrics getCveMetrics(String cveId) {
        try {
            var response = nvdApi.get(cveId);
            if (response.totalResults() == 0 || response.vulnerabilities() == null
                    || response.vulnerabilities().isEmpty()) {
                LOGGER.warn("Not found vulnerabilities for CVE: {} in NVD", cveId);
                return null;
            }
            if (response.vulnerabilities().size() == 1) {
                var cve = response.vulnerabilities().get(0).cve();
                return cve.metrics();
            } else {
                LOGGER.warn("Found multiple vulnerabilities for the same CVE: {}, found {}", cveId,
                        response.vulnerabilities().size());
            }
        } catch (ClientWebApplicationException e) {
            if (e.getResponse() != null) {
                switch (e.getResponse().getStatus()) {
                    case 404:
                        LOGGER.info("Not found vulnerability: {} in NVD", cveId);
                        break;
                    case 403:
                        LOGGER.info(
                                "Unable to retrieve vulnerability: {} from NVD. Rate limit reached. Wait for async loading or retry after 30 seconds",
                                cveId);
                        break;
                    default:
                        LOGGER.error("Error retrieving NVD vulnerability for {}", cveId, e);
                        break;
                }
            } else {
                LOGGER.error("Error retrieving NVD vulnerability for {}", cveId, e);
            }
        } catch (CircuitBreakerOpenException e) {
            LOGGER.info("Waiting for the circuit breaker to close. CVE: {}", cveId, e.getMessage());
        }
        return null;
    }

    private Vulnerability toVulnerability(NvdVulnerability nvdVuln) {
        return Vulnerability.builder()
            .created(new Date())
            .cveId(nvdVuln.cve().id())
            .metrics(nvdVuln.cve().metrics())
            .build();
    }


}
