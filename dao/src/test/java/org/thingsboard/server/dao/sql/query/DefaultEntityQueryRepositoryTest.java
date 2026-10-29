/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.query;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.permission.QueryPermissions;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityFilter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DefaultEntityQueryRepository.class)
public class DefaultEntityQueryRepositoryTest {

    @MockBean
    NamedParameterJdbcTemplate jdbcTemplate;
    @MockBean
    TransactionTemplate transactionTemplate;
    @MockBean
    DefaultQueryLogComponent queryLog;

    @Autowired
    DefaultEntityQueryRepository repo;

    /*
     * This value has to be reasonable small to prevent infinite recursion as early as possible
     * */
    @Test
    public void givenDefaultMaxLevel_whenStaticConstant_thenEqualsTo() {
        assertThat(repo.getMaxLevelAllowed(), equalTo(50));
    }

    @Test
    public void givenMaxLevelZeroOrNegative_whenGetMaxLevel_thenReturnDefaultMaxLevel() {
        assertThat(repo.getMaxLevel(0), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-2), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MIN_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

    @Test
    public void givenMaxLevelPositive_whenGetMaxLevel_thenValueTheSame() {
        assertThat(repo.getMaxLevel(1), equalTo(1));
        assertThat(repo.getMaxLevel(2), equalTo(2));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed()), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed() + 1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MAX_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

    // Customer users who also hold a GROUP role grant get an owner-scope-OR-group-grant permission
    // clause (see DefaultEntityQueryRepository#defaultPermissionQuery). The device type/name filter
    // has to bind to that whole OR as a unit, not just to the group-grant disjunct - otherwise, since
    // SQL "and" binds tighter than "or", every entity in the user's owner scope would bypass the
    // filter entirely regardless of device type/name.
    @Test
    public void givenDeviceTypeFilterAndGroupGrant_whenBuildEntityWhere_thenFilterAppliesToWholePermissionClause() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        QueryPermissions queryPermissions = QueryPermissions.of(
                Set.of(EntityType.DEVICE), Set.of(UUID.randomUUID()), Set.of(), Set.of());
        EntityFilter filter = new DeviceTypeFilter(List.of("sensor"), "Device A");

        SqlQueryContext ctx = new SqlQueryContext(new QueryContext(tenantId, customerId, EntityType.DEVICE, false, null, queryPermissions));

        String permissionQuery = invokePrivate(String.class, "buildPermissionQuery", ctx, filter);
        String entityFilterQuery = invokePrivate(String.class, "buildEntityFilterQuery", ctx, filter);
        String actual = invokePrivate(String.class, "buildEntityWhere", ctx, filter, Collections.emptyList());

        // The permission clause (whatever boolean shape it has) must be parenthesized as one unit
        // before the filter is ANDed onto it.
        String expected = "(" + permissionQuery + ")" + " and (" + entityFilterQuery + ")";
        assertThat(actual, equalTo(expected));
    }

    private <T> T invokePrivate(Class<T> returnType, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        paramTypes[0] = SqlQueryContext.class;
        paramTypes[1] = EntityFilter.class;
        if (args.length > 2) {
            paramTypes[2] = List.class;
        }
        Method method = DefaultEntityQueryRepository.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return returnType.cast(method.invoke(repo, args));
    }

}
