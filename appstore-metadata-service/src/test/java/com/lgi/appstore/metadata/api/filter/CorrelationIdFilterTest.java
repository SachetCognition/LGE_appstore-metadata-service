/*
 * If not stated otherwise in this file or this component's LICENSE file the
 * following copyright and licenses apply:
 *
 * Copyright 2022 Liberty Global Technology Services BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lgi.appstore.metadata.api.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private static final String X_REQUEST_ID_HEADER = "x-request-id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private CorrelationIdFilter correlationIdFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        MDC.clear();
    }

    @Test
    void shouldUseProvidedCorrelationIdFromHeader() throws ServletException, IOException {
        String providedCorrelationId = "test-correlation-id-123";
        request.addHeader(X_REQUEST_ID_HEADER, providedCorrelationId);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(X_REQUEST_ID_HEADER)).isEqualTo(providedCorrelationId);
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderNotProvided() throws ServletException, IOException {
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String generatedCorrelationId = response.getHeader(X_REQUEST_ID_HEADER);
        assertThat(generatedCorrelationId).isNotNull();
        assertThat(generatedCorrelationId).isNotEmpty();
        assertThat(isValidUUID(generatedCorrelationId)).isTrue();
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsBlank() throws ServletException, IOException {
        request.addHeader(X_REQUEST_ID_HEADER, "   ");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String generatedCorrelationId = response.getHeader(X_REQUEST_ID_HEADER);
        assertThat(generatedCorrelationId).isNotNull();
        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(isValidUUID(generatedCorrelationId)).isTrue();
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsEmpty() throws ServletException, IOException {
        request.addHeader(X_REQUEST_ID_HEADER, "");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String generatedCorrelationId = response.getHeader(X_REQUEST_ID_HEADER);
        assertThat(generatedCorrelationId).isNotNull();
        assertThat(generatedCorrelationId).isNotEmpty();
        assertThat(isValidUUID(generatedCorrelationId)).isTrue();
    }

    @Test
    void shouldClearMdcAfterFilterExecution() throws ServletException, IOException {
        String providedCorrelationId = "test-correlation-id-456";
        request.addHeader(X_REQUEST_ID_HEADER, providedCorrelationId);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldAddCorrelationIdToResponseHeader() throws ServletException, IOException {
        String providedCorrelationId = "response-header-test-id";
        request.addHeader(X_REQUEST_ID_HEADER, providedCorrelationId);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.containsHeader(X_REQUEST_ID_HEADER)).isTrue();
        assertThat(response.getHeader(X_REQUEST_ID_HEADER)).isEqualTo(providedCorrelationId);
    }

    @Test
    void shouldContinueFilterChain() throws ServletException, IOException {
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertThat(filterChain.getRequest()).isEqualTo(request);
        assertThat(filterChain.getResponse()).isEqualTo(response);
    }

    @Test
    void shouldGenerateUniqueCorrelationIdsForDifferentRequests() throws ServletException, IOException {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockFilterChain filterChain1 = new MockFilterChain();

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        MockFilterChain filterChain2 = new MockFilterChain();

        correlationIdFilter.doFilterInternal(request1, response1, filterChain1);
        correlationIdFilter.doFilterInternal(request2, response2, filterChain2);

        String correlationId1 = response1.getHeader(X_REQUEST_ID_HEADER);
        String correlationId2 = response2.getHeader(X_REQUEST_ID_HEADER);

        assertThat(correlationId1).isNotEqualTo(correlationId2);
    }

    private boolean isValidUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
