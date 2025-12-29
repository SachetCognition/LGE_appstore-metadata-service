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
package com.lgi.appstore.metadata.api.maintainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgi.appstore.metadata.api.error.GlobalExceptionHandler;
import com.lgi.appstore.metadata.api.error.MaintainerAlreadyExistsException;
import com.lgi.appstore.metadata.api.error.MaintainerNotFoundException;
import com.lgi.appstore.metadata.model.Maintainer;
import com.lgi.appstore.metadata.model.MaintainerForUpdate;
import com.lgi.appstore.metadata.model.MaintainerList;
import com.lgi.appstore.metadata.model.Meta;
import com.lgi.appstore.metadata.model.ResultSetMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@ExtendWith(MockitoExtension.class)
class MaintainersControllerTest {

    private MockMvc mvc;

    @Mock
    private MaintainersService maintainersService;

    @InjectMocks
    private MaintainersController maintainersController;

    private JacksonTester<Maintainer> jsonMaintainer;
    private JacksonTester<MaintainerForUpdate> jsonMaintainerForUpdate;
    private JacksonTester<MaintainerList> jsonMaintainerList;

    private static final String MAINTAINER_CODE = "lgi";

    private static final Maintainer SAMPLE_MAINTAINER = new Maintainer()
            .code(MAINTAINER_CODE)
            .name("Liberty Global")
            .address("Liberty Global B.V., Boeing Avenue 53, 1119 PE Schiphol Rijk, The Netherlands")
            .homepage("www.libertyglobal.com")
            .email("developer@libertyglobal.com");

    private static final MaintainerForUpdate SAMPLE_MAINTAINER_FOR_UPDATE = new MaintainerForUpdate()
            .name("Liberty Global Updated")
            .address("New Address")
            .homepage("www.libertyglobal-updated.com")
            .email("updated@libertyglobal.com");

    private static final MaintainerList EMPTY_MAINTAINER_LIST = new MaintainerList()
            .maintainers(List.of())
            .meta(new Meta()
                    .resultSet(new ResultSetMeta()
                            .limit(0)
                            .offset(0)
                            .count(0)
                            .total(0)
                    )
            );

    private static final MaintainerList NON_EMPTY_MAINTAINER_LIST = new MaintainerList()
            .maintainers(List.of(SAMPLE_MAINTAINER))
            .meta(new Meta()
                    .resultSet(new ResultSetMeta()
                            .limit(10)
                            .offset(0)
                            .count(1)
                            .total(1)
                    )
            );

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, new ObjectMapper());

        mvc = MockMvcBuilders.standaloneSetup(maintainersController)
                .addFilter(((request, response, chain) -> {
                    response.setCharacterEncoding("UTF-8");
                    chain.doFilter(request, response);
                }))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void canGetMaintainerByCode() throws Exception {
        given(maintainersService.getMaintainer(MAINTAINER_CODE))
                .willReturn(SAMPLE_MAINTAINER);

        MockHttpServletResponse response = mvc
                .perform(get("/maintainers/{maintainerCode}", MAINTAINER_CODE)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(jsonMaintainer.write(SAMPLE_MAINTAINER).getJson());
    }

    @Test
    void cannotGetNonExistingMaintainer() throws Exception {
        given(maintainersService.getMaintainer("nonexistent"))
                .willThrow(new MaintainerNotFoundException("nonexistent"));

        MockHttpServletResponse response = mvc
                .perform(get("/maintainers/{maintainerCode}", "nonexistent")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void canCreateMaintainer() throws Exception {
        doNothing().when(maintainersService).createMaintainer(any(Maintainer.class));

        MockHttpServletResponse response = mvc
                .perform(post("/maintainers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMaintainer.write(SAMPLE_MAINTAINER).getJson()))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        verify(maintainersService).createMaintainer(any(Maintainer.class));
    }

    @Test
    void cannotCreateDuplicateMaintainer() throws Exception {
        doThrow(new MaintainerAlreadyExistsException(MAINTAINER_CODE))
                .when(maintainersService).createMaintainer(any(Maintainer.class));

        MockHttpServletResponse response = mvc
                .perform(post("/maintainers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMaintainer.write(SAMPLE_MAINTAINER).getJson()))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void canUpdateMaintainer() throws Exception {
        given(maintainersService.updateMaintainer(eq(MAINTAINER_CODE), any(MaintainerForUpdate.class)))
                .willReturn(true);

        MockHttpServletResponse response = mvc
                .perform(put("/maintainers/{maintainerCode}", MAINTAINER_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMaintainerForUpdate.write(SAMPLE_MAINTAINER_FOR_UPDATE).getJson()))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void cannotUpdateNonExistingMaintainer() throws Exception {
        given(maintainersService.updateMaintainer(eq("nonexistent"), any(MaintainerForUpdate.class)))
                .willThrow(new MaintainerNotFoundException("nonexistent"));

        MockHttpServletResponse response = mvc
                .perform(put("/maintainers/{maintainerCode}", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMaintainerForUpdate.write(SAMPLE_MAINTAINER_FOR_UPDATE).getJson()))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void updateMaintainerReturnsNotFoundWhenNoRowsAffected() throws Exception {
        given(maintainersService.updateMaintainer(eq(MAINTAINER_CODE), any(MaintainerForUpdate.class)))
                .willReturn(false);

        MockHttpServletResponse response = mvc
                .perform(put("/maintainers/{maintainerCode}", MAINTAINER_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMaintainerForUpdate.write(SAMPLE_MAINTAINER_FOR_UPDATE).getJson()))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void canDeleteMaintainer() throws Exception {
        given(maintainersService.deleteMaintainer(MAINTAINER_CODE))
                .willReturn(true);

        MockHttpServletResponse response = mvc
                .perform(delete("/maintainers/{maintainerCode}", MAINTAINER_CODE)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void cannotDeleteNonExistingMaintainer() throws Exception {
        given(maintainersService.deleteMaintainer("nonexistent"))
                .willThrow(new MaintainerNotFoundException("nonexistent"));

        MockHttpServletResponse response = mvc
                .perform(delete("/maintainers/{maintainerCode}", "nonexistent")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void deleteMaintainerReturnsNotFoundWhenNoRowsAffected() throws Exception {
        given(maintainersService.deleteMaintainer(MAINTAINER_CODE))
                .willReturn(false);

        MockHttpServletResponse response = mvc
                .perform(delete("/maintainers/{maintainerCode}", MAINTAINER_CODE)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void canSearchMaintainersWithNoFilters() throws Exception {
        given(maintainersService.searchMaintainers(null, null, null))
                .willReturn(NON_EMPTY_MAINTAINER_LIST);

        MockHttpServletResponse response = mvc
                .perform(get("/maintainers")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(jsonMaintainerList.write(NON_EMPTY_MAINTAINER_LIST).getJson());
    }

    @Test
    void canSearchMaintainersByName() throws Exception {
        given(maintainersService.searchMaintainers("Liberty", null, null))
                .willReturn(NON_EMPTY_MAINTAINER_LIST);

        MockHttpServletResponse response = mvc
                .perform(get("/maintainers?name=Liberty")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(jsonMaintainerList.write(NON_EMPTY_MAINTAINER_LIST).getJson());
    }

    @Test
    void canSearchMaintainersWithPagination() throws Exception {
        given(maintainersService.searchMaintainers(null, 10, 5))
                .willReturn(EMPTY_MAINTAINER_LIST);

        MockHttpServletResponse response = mvc
                .perform(get("/maintainers?limit=10&offset=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void canSearchMaintainersWithAllFilters() throws Exception {
        given(maintainersService.searchMaintainers("Liberty", 10, 0))
                .willReturn(NON_EMPTY_MAINTAINER_LIST);

        MockHttpServletResponse response = mvc
                .perform(get("/maintainers?name=Liberty&limit=10&offset=0")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(jsonMaintainerList.write(NON_EMPTY_MAINTAINER_LIST).getJson());
    }

    @Test
    void searchMaintainersReturnsEmptyListWhenNoMatch() throws Exception {
        given(maintainersService.searchMaintainers("NonExistent", null, null))
                .willReturn(EMPTY_MAINTAINER_LIST);

        MockHttpServletResponse response = mvc
                .perform(get("/maintainers?name=NonExistent")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(jsonMaintainerList.write(EMPTY_MAINTAINER_LIST).getJson());
    }
}
