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
package org.thingsboard.server.dao.menu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;
import org.thingsboard.server.dao.sql.user.UserRepository;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseCustomMenuService implements CustomMenuService {

    private final CustomMenuDao customMenuDao;
    private final CustomerService customerService;
    private final UserService userService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CustomMenu createCustomMenu(TenantId tenantId, CustomMenuInfo info, List<EntityId> assignToList, boolean force) {
        if (StringUtils.isBlank(info.getName())) {
            throw new DataValidationException("Custom menu name should be specified!");
        }
        if (info.getScope() == null) {
            throw new DataValidationException("Custom menu scope should be specified!");
        }
        if (info.getScope() == CMScope.SYSTEM && !tenantId.isSysTenantId()) {
            throw new DataValidationException("Only system administrator can create a SYSTEM scope custom menu!");
        }
        CMAssigneeType assigneeType = info.getAssigneeType() == null ? CMAssigneeType.NO_ASSIGN : info.getAssigneeType();
        if (assigneeType == CMAssigneeType.ALL) {
            demoteExistingDefault(tenantId, info.getScope(), force);
        }
        CustomMenuInfo toSave = new CustomMenuInfo(info);
        toSave.setId(null);
        toSave.setTenantId(tenantId);
        toSave.setAssigneeType(assigneeType);
        CustomMenu menu = new CustomMenu(toSave, new CustomMenuConfig());
        CustomMenu saved = customMenuDao.save(tenantId, menu);
        if (assigneeType == CMAssigneeType.CUSTOMERS || assigneeType == CMAssigneeType.USERS) {
            assignEntities(tenantId, saved.getId(), assigneeType, assignToList);
        }
        return saved;
    }

    @Override
    @Transactional
    public CustomMenu updateCustomMenuConfig(TenantId tenantId, CustomMenuId id, CustomMenuConfig config) {
        CustomMenu menu = getExisting(tenantId, id);
        menu.setConfig(config != null ? config : new CustomMenuConfig());
        return customMenuDao.save(tenantId, menu);
    }

    @Override
    @Transactional
    public CustomMenu updateCustomMenuName(TenantId tenantId, CustomMenuId id, String name) {
        if (StringUtils.isBlank(name)) {
            throw new DataValidationException("Custom menu name should be specified!");
        }
        CustomMenu menu = getExisting(tenantId, id);
        menu.setName(name);
        return customMenuDao.save(tenantId, menu);
    }

    @Override
    @Transactional
    public void updateAssigneeList(TenantId tenantId, CustomMenuId id, CMAssigneeType newAssigneeType, List<EntityId> newList, boolean force) {
        CustomMenu menu = getExisting(tenantId, id);
        CMAssigneeType resolvedType = newAssigneeType == null ? CMAssigneeType.NO_ASSIGN : newAssigneeType;
        if (resolvedType == CMAssigneeType.ALL && menu.getAssigneeType() != CMAssigneeType.ALL) {
            demoteExistingDefault(tenantId, menu.getScope(), force);
        }
        clearAssignees(tenantId, id);
        menu.setAssigneeType(resolvedType);
        customMenuDao.save(tenantId, menu);
        if (resolvedType == CMAssigneeType.CUSTOMERS || resolvedType == CMAssigneeType.USERS) {
            assignEntities(tenantId, id, resolvedType, newList);
        }
    }

    @Override
    public CustomMenu findCustomMenuById(TenantId tenantId, CustomMenuId id) {
        return getExisting(tenantId, id);
    }

    @Override
    public CustomMenuInfo findCustomMenuInfoById(TenantId tenantId, CustomMenuId id) {
        CustomMenuInfo info = customMenuDao.findInfoById(tenantId, id);
        if (info == null || !info.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Custom menu with id [" + id + "] is not found!");
        }
        return info;
    }

    @Override
    public PageData<CustomMenuInfo> findCustomMenuInfos(TenantId tenantId, CMScope scope, PageLink pageLink) {
        ensureDefaultMenus(tenantId);
        return customMenuDao.findInfosByTenantId(tenantId, scope, pageLink);
    }

    /**
     * PE ships with a pre-seeded default menu row per scope ("System default menu",
     * "Tenant default menu"). CE seeds them lazily on first list access: the system tenant gets
     * the SYSTEM default plus the TENANT-scope fallback inherited by all tenants; every other
     * tenant gets its own TENANT default. (PE's "Customer default menu" is dropped — no CUSTOMER
     * scope in the CE port.) Untouched (empty-config) defaults don't override inheritance — see
     * the empty-config fall-through in the resolution methods.
     */
    private void ensureDefaultMenus(TenantId tenantId) {
        if (tenantId.isSysTenantId()) {
            ensureDefault(tenantId, CMScope.SYSTEM, "System default menu");
            ensureDefault(tenantId, CMScope.TENANT, "Tenant default menu");
        } else {
            ensureDefault(tenantId, CMScope.TENANT, "Tenant default menu");
        }
    }

    private void ensureDefault(TenantId tenantId, CMScope scope, String name) {
        if (customMenuDao.findDefaultByTenantIdAndScope(tenantId, scope).isEmpty()) {
            CustomMenuInfo info = new CustomMenuInfo();
            info.setTenantId(tenantId);
            info.setName(name);
            info.setScope(scope);
            info.setAssigneeType(CMAssigneeType.ALL);
            customMenuDao.save(tenantId, new CustomMenu(info, new CustomMenuConfig()));
        }
    }

    @Override
    public List<EntityInfo> findCustomMenuAssigneeList(TenantId tenantId, CustomMenuId id) {
        CustomMenu menu = getExisting(tenantId, id);
        return findAssignees(tenantId, menu);
    }

    @Override
    @Transactional
    public CustomMenuDeleteResult deleteCustomMenu(TenantId tenantId, CustomMenuId id, boolean force) {
        CustomMenu menu = getExisting(tenantId, id);
        List<EntityInfo> assignees = findAssignees(tenantId, menu);
        if (!assignees.isEmpty() && !force) {
            return CustomMenuDeleteResult.blocked(menu.getAssigneeType(), assignees);
        }
        clearAssignees(tenantId, id);
        customMenuDao.removeById(tenantId, id);
        return CustomMenuDeleteResult.success();
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        customMenuDao.removeByTenantId(tenantId);
    }

    @Override
    public CustomMenuConfig resolveSystemAdminMenu() {
        return customMenuDao.findDefaultByTenantIdAndScope(TenantId.SYS_TENANT_ID, CMScope.SYSTEM)
                .map(CustomMenu::getConfig)
                .orElseGet(CustomMenuConfig::new);
    }

    // Untouched (empty-config) menus don't terminate resolution: an auto-seeded default row that
    // was never edited must not shadow the configured menu of the next fallback level.
    private boolean hasItems(CustomMenu menu) {
        return menu != null && menu.getConfig() != null
                && menu.getConfig().getItems() != null && !menu.getConfig().getItems().isEmpty();
    }

    @Override
    public CustomMenuConfig resolveUserMenu(TenantId tenantId, CustomerId customerId, UserId userId) {
        if (userId != null) {
            User user = userService.findUserById(tenantId, userId);
            if (user != null && user.getCustomMenuId() != null) {
                CustomMenu menu = customMenuDao.findById(tenantId, user.getCustomMenuId());
                if (hasItems(menu)) {
                    return menu.getConfig();
                }
            }
        }
        if (customerId != null && !customerId.isNullUid()) {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer != null && customer.getCustomMenuId() != null) {
                CustomMenu menu = customMenuDao.findById(tenantId, customer.getCustomMenuId());
                if (hasItems(menu)) {
                    return menu.getConfig();
                }
            }
        }
        CustomMenu tenantDefault = customMenuDao.findDefaultByTenantIdAndScope(tenantId, CMScope.TENANT).orElse(null);
        if (hasItems(tenantDefault)) {
            return tenantDefault.getConfig();
        }
        CustomMenu systemFallback = customMenuDao.findDefaultByTenantIdAndScope(TenantId.SYS_TENANT_ID, CMScope.TENANT).orElse(null);
        if (hasItems(systemFallback)) {
            return systemFallback.getConfig();
        }
        return new CustomMenuConfig();
    }

    private CustomMenu getExisting(TenantId tenantId, CustomMenuId id) {
        CustomMenu menu = customMenuDao.findById(tenantId, id);
        if (menu == null || !menu.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Custom menu with id [" + id + "] is not found!");
        }
        return menu;
    }

    private void demoteExistingDefault(TenantId tenantId, CMScope scope, boolean force) {
        Optional<CustomMenu> existingDefault = customMenuDao.findDefaultByTenantIdAndScope(tenantId, scope);
        if (existingDefault.isEmpty()) {
            return;
        }
        if (!force) {
            throw new DataValidationException("A default custom menu already exists for this scope: [" + existingDefault.get().getName() + "]. Use force to replace it.");
        }
        CustomMenu existing = existingDefault.get();
        existing.setAssigneeType(CMAssigneeType.NO_ASSIGN);
        customMenuDao.save(tenantId, existing);
    }

    private void assignEntities(TenantId tenantId, CustomMenuId menuId, CMAssigneeType assigneeType, List<EntityId> assignToList) {
        if (assignToList == null || assignToList.isEmpty()) {
            return;
        }
        if (assigneeType == CMAssigneeType.CUSTOMERS) {
            for (EntityId entityId : assignToList) {
                Customer customer = customerService.findCustomerById(tenantId, new CustomerId(entityId.getId()));
                if (customer != null) {
                    customer.setCustomMenuId(menuId);
                    customerService.saveCustomer(customer);
                }
            }
        } else if (assigneeType == CMAssigneeType.USERS) {
            for (EntityId entityId : assignToList) {
                User user = userService.findUserById(tenantId, new UserId(entityId.getId()));
                if (user != null) {
                    user.setCustomMenuId(menuId);
                    userService.saveUser(tenantId, user);
                }
            }
        }
    }

    private void clearAssignees(TenantId tenantId, CustomMenuId menuId) {
        List<Customer> customers = customerRepository.findByTenantIdAndCustomMenuId(tenantId.getId(), menuId.getId())
                .stream().map(entity -> entity.toData()).toList();
        for (Customer customer : customers) {
            customer.setCustomMenuId(null);
            customerService.saveCustomer(customer);
        }
        List<User> users = userRepository.findByTenantIdAndCustomMenuId(tenantId.getId(), menuId.getId())
                .stream().map(entity -> entity.toData()).toList();
        for (User user : users) {
            user.setCustomMenuId(null);
            userService.saveUser(tenantId, user);
        }
    }

    private List<EntityInfo> findAssignees(TenantId tenantId, CustomMenu menu) {
        if (menu.getAssigneeType() == CMAssigneeType.CUSTOMERS) {
            return customerRepository.findByTenantIdAndCustomMenuId(tenantId.getId(), menu.getId().getId())
                    .stream().map(entity -> {
                        Customer customer = entity.toData();
                        return new EntityInfo(customer.getId(), customer.getName());
                    }).toList();
        } else if (menu.getAssigneeType() == CMAssigneeType.USERS) {
            return userRepository.findByTenantIdAndCustomMenuId(tenantId.getId(), menu.getId().getId())
                    .stream().map(entity -> {
                        User user = entity.toData();
                        return new EntityInfo(user.getId(), user.getName());
                    }).toList();
        }
        return Collections.emptyList();
    }

}
