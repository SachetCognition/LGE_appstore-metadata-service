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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lgi.appstore.metadata.api.error.ApplicationAlreadyExistsException;
import com.lgi.appstore.metadata.api.error.MaintainerAlreadyExistsException;
import com.lgi.appstore.metadata.api.error.MaintainerNotFoundException;
import com.lgi.appstore.metadata.api.service.BaseServiceTest;
import com.lgi.appstore.metadata.jooq.model.tables.records.MaintainerRecord;
import com.lgi.appstore.metadata.model.Maintainer;
import com.lgi.appstore.metadata.model.MaintainerForUpdate;
import com.lgi.appstore.metadata.model.MaintainerList;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistentMaintainersServiceTest extends BaseServiceTest {

    @Autowired
    private DSLContext dslContext;

    private PersistentMaintainersService maintainersService;

    @BeforeEach
    void setUp() {
        maintainersService = new PersistentMaintainersService(dslContext);
    }

    @Test
    void shouldGetMaintainerByCode() {
        MaintainerRecord maintainerRecord = createRandomMaintainerRecord();

        Maintainer result = maintainersService.getMaintainer(maintainerRecord.getCode());

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(maintainerRecord.getCode());
        assertThat(result.getName()).isEqualTo(maintainerRecord.getName());
        assertThat(result.getAddress()).isEqualTo(maintainerRecord.getAddress());
        assertThat(result.getEmail()).isEqualTo(maintainerRecord.getEmail());
        assertThat(result.getHomepage()).isEqualTo(maintainerRecord.getHomepage());
    }

    @Test
    void shouldThrowExceptionWhenMaintainerNotFound() {
        String nonExistentCode = randomUUID().toString();

        assertThatThrownBy(() -> maintainersService.getMaintainer(nonExistentCode))
                .isInstanceOf(MaintainerNotFoundException.class)
                .hasMessageContaining(nonExistentCode);
    }

    @Test
    void shouldCreateMaintainer() {
        String code = randomUUID().toString();
        Maintainer maintainer = new Maintainer()
                .code(code)
                .name("Test Maintainer")
                .address("Test Address")
                .email("test@example.com")
                .homepage("http://test.com");

        maintainersService.createMaintainer(maintainer);

        Maintainer result = maintainersService.getMaintainer(code);
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(code);
        assertThat(result.getName()).isEqualTo("Test Maintainer");
        assertThat(result.getAddress()).isEqualTo("Test Address");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getHomepage()).isEqualTo("http://test.com");
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateMaintainer() {
        MaintainerRecord existingMaintainer = createRandomMaintainerRecord();

        Maintainer duplicateMaintainer = new Maintainer()
                .code(existingMaintainer.getCode())
                .name("Duplicate Name")
                .address("Duplicate Address")
                .email("duplicate@example.com")
                .homepage("http://duplicate.com");

        assertThatThrownBy(() -> maintainersService.createMaintainer(duplicateMaintainer))
                .isInstanceOf(MaintainerAlreadyExistsException.class)
                .hasMessageContaining(existingMaintainer.getCode());
    }

    @Test
    void shouldUpdateMaintainer() {
        MaintainerRecord maintainerRecord = createRandomMaintainerRecord();

        MaintainerForUpdate updateData = new MaintainerForUpdate()
                .name("Updated Name")
                .address("Updated Address")
                .email("updated@example.com")
                .homepage("http://updated.com");

        boolean result = maintainersService.updateMaintainer(maintainerRecord.getCode(), updateData);

        assertThat(result).isTrue();

        Maintainer updatedMaintainer = maintainersService.getMaintainer(maintainerRecord.getCode());
        assertThat(updatedMaintainer.getName()).isEqualTo("Updated Name");
        assertThat(updatedMaintainer.getAddress()).isEqualTo("Updated Address");
        assertThat(updatedMaintainer.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedMaintainer.getHomepage()).isEqualTo("http://updated.com");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentMaintainer() {
        String nonExistentCode = randomUUID().toString();

        MaintainerForUpdate updateData = new MaintainerForUpdate()
                .name("Updated Name")
                .address("Updated Address")
                .email("updated@example.com")
                .homepage("http://updated.com");

        assertThatThrownBy(() -> maintainersService.updateMaintainer(nonExistentCode, updateData))
                .isInstanceOf(MaintainerNotFoundException.class)
                .hasMessageContaining(nonExistentCode);
    }

    @Test
    void shouldDeleteMaintainer() {
        MaintainerRecord maintainerRecord = createRandomMaintainerRecord();

        boolean result = maintainersService.deleteMaintainer(maintainerRecord.getCode());

        assertThat(result).isTrue();
        assertThatThrownBy(() -> maintainersService.getMaintainer(maintainerRecord.getCode()))
                .isInstanceOf(MaintainerNotFoundException.class);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentMaintainer() {
        String nonExistentCode = randomUUID().toString();

        assertThatThrownBy(() -> maintainersService.deleteMaintainer(nonExistentCode))
                .isInstanceOf(MaintainerNotFoundException.class)
                .hasMessageContaining(nonExistentCode);
    }

    @Test
    void shouldThrowExceptionWhenDeletingMaintainerWithApplications() throws JsonProcessingException {
        MaintainerRecord maintainerRecord = createRandomMaintainerRecord();
        createRandomApplicationRecord(maintainerRecord, "com.test.app", "1.0.0", true);

        assertThatThrownBy(() -> maintainersService.deleteMaintainer(maintainerRecord.getCode()))
                .isInstanceOf(ApplicationAlreadyExistsException.class)
                .hasMessageContaining(maintainerRecord.getCode());
    }

    @Test
    void shouldSearchMaintainersWithNoFilters() {
        MaintainerRecord maintainer1 = createRandomMaintainerRecord();
        MaintainerRecord maintainer2 = createRandomMaintainerRecord();

        MaintainerList result = maintainersService.searchMaintainers(null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMaintainers()).isNotEmpty();
        assertThat(result.getMeta()).isNotNull();
        assertThat(result.getMeta().getResultSet().getTotal()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldSearchMaintainersByName() {
        String uniquePrefix = "UniquePrefix" + randomUUID().toString().substring(0, 8);
        
        Maintainer maintainer = new Maintainer()
                .code(randomUUID().toString())
                .name(uniquePrefix + " Test Maintainer")
                .address("Test Address")
                .email("test@example.com")
                .homepage("http://test.com");
        maintainersService.createMaintainer(maintainer);

        MaintainerList result = maintainersService.searchMaintainers(uniquePrefix, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMaintainers()).hasSize(1);
        assertThat(result.getMaintainers().get(0).getName()).startsWith(uniquePrefix);
    }

    @Test
    void shouldSearchMaintainersByNameCaseInsensitive() {
        String uniquePrefix = "CaseTest" + randomUUID().toString().substring(0, 8);
        
        Maintainer maintainer = new Maintainer()
                .code(randomUUID().toString())
                .name(uniquePrefix.toUpperCase() + " Test Maintainer")
                .address("Test Address")
                .email("test@example.com")
                .homepage("http://test.com");
        maintainersService.createMaintainer(maintainer);

        MaintainerList result = maintainersService.searchMaintainers(uniquePrefix.toLowerCase(), null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMaintainers()).hasSize(1);
    }

    @Test
    void shouldSearchMaintainersWithLimit() {
        createRandomMaintainerRecord();
        createRandomMaintainerRecord();
        createRandomMaintainerRecord();

        MaintainerList result = maintainersService.searchMaintainers(null, 2, null);

        assertThat(result).isNotNull();
        assertThat(result.getMaintainers()).hasSizeLessThanOrEqualTo(2);
        assertThat(result.getMeta().getResultSet().getLimit()).isEqualTo(2);
    }

    @Test
    void shouldSearchMaintainersWithOffset() {
        createRandomMaintainerRecord();
        createRandomMaintainerRecord();

        MaintainerList resultWithoutOffset = maintainersService.searchMaintainers(null, null, null);
        MaintainerList resultWithOffset = maintainersService.searchMaintainers(null, null, 1);

        assertThat(resultWithOffset).isNotNull();
        assertThat(resultWithOffset.getMeta().getResultSet().getOffset()).isEqualTo(1);
        assertThat(resultWithOffset.getMaintainers().size()).isLessThanOrEqualTo(resultWithoutOffset.getMaintainers().size());
    }

    @Test
    void shouldSearchMaintainersWithLimitAndOffset() {
        createRandomMaintainerRecord();
        createRandomMaintainerRecord();
        createRandomMaintainerRecord();
        createRandomMaintainerRecord();

        MaintainerList result = maintainersService.searchMaintainers(null, 2, 1);

        assertThat(result).isNotNull();
        assertThat(result.getMaintainers()).hasSizeLessThanOrEqualTo(2);
        assertThat(result.getMeta().getResultSet().getLimit()).isEqualTo(2);
        assertThat(result.getMeta().getResultSet().getOffset()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyListWhenNoMaintainersMatchName() {
        String nonExistentName = "NonExistent" + randomUUID().toString();

        MaintainerList result = maintainersService.searchMaintainers(nonExistentName, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMaintainers()).isEmpty();
        assertThat(result.getMeta().getResultSet().getTotal()).isEqualTo(0);
        assertThat(result.getMeta().getResultSet().getCount()).isEqualTo(0);
    }

    @Test
    void shouldReturnCorrectMetaInSearchResults() {
        String uniquePrefix = "MetaTest" + randomUUID().toString().substring(0, 8);
        
        for (int i = 0; i < 5; i++) {
            Maintainer maintainer = new Maintainer()
                    .code(randomUUID().toString())
                    .name(uniquePrefix + " Maintainer " + i)
                    .address("Address " + i)
                    .email("test" + i + "@example.com")
                    .homepage("http://test" + i + ".com");
            maintainersService.createMaintainer(maintainer);
        }

        MaintainerList result = maintainersService.searchMaintainers(uniquePrefix, 3, 1);

        assertThat(result).isNotNull();
        assertThat(result.getMeta().getResultSet().getTotal()).isEqualTo(5);
        assertThat(result.getMeta().getResultSet().getLimit()).isEqualTo(3);
        assertThat(result.getMeta().getResultSet().getOffset()).isEqualTo(1);
        assertThat(result.getMeta().getResultSet().getCount()).isEqualTo(3);
    }
}
