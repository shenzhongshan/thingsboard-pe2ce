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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.menu.CustomMenuItem;
import org.thingsboard.server.common.data.menu.DefaultMenuItem;
import org.thingsboard.server.common.data.menu.MenuItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;
import org.thingsboard.server.dao.sql.user.UserRepository;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseCustomMenuService implements CustomMenuService {

    private static final CustomerId NO_CUSTOMER = new CustomerId(CustomerId.NULL_UUID);

    private final CustomMenuDao customMenuDao;
    private final CustomerService customerService;
    private final UserService userService;
    private final EntityGroupService entityGroupService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 把菜单变更增量下发给 edge。没有这一步，edge 只在首次全量同步时拿到菜单，
     * 之后的任何改动都不会到达（接收侧 CustomMenuEdgeProcessor 一直在等这个事件）。
     *
     * <p>entityId 填 tenantId 而不是菜单自身的 id：内容全在 body 里，这与全量同步
     * fetcher 的产物保持一致（它发的 entityId 就是 null，路由只看租户）。entityId 不能
     * 传 null —— EdgeEventSourcingListener 会直接解引用它的 EntityType。
     */
    private void publishEdgeEvent(TenantId tenantId, CustomMenu menu, ActionType actionType) {
        if (menu == null) {
            return;
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(tenantId)
                .edgeEventType(EdgeEventType.CUSTOM_MENU)
                .actionType(actionType)
                .body(JacksonUtil.toString(menu))
                .build());
    }

    @Override
    @Transactional
    public CustomMenu createCustomMenu(TenantId tenantId, CustomerId customerId, CustomMenuInfo info,
                                       List<EntityId> assignToList, boolean force) {
        CustomerId owner = normalize(customerId);
        CustomMenuInfo toSave = new CustomMenuInfo(info);
        toSave.setId(null);
        toSave.setTenantId(tenantId);
        toSave.setCustomerId(owner);
        toSave.setAssigneeType(info.getAssigneeType() == null ? CMAssigneeType.NO_ASSIGN : info.getAssigneeType());
        validate(toSave);
        if (toSave.getAssigneeType() == CMAssigneeType.ALL) {
            demoteExistingDefault(tenantId, owner, toSave.getScope(), force);
        }
        CustomMenu saved = customMenuDao.save(tenantId, new CustomMenu(toSave, new CustomMenuConfig()));
        if (toSave.getAssigneeType() == CMAssigneeType.CUSTOMERS || toSave.getAssigneeType() == CMAssigneeType.USERS) {
            assignEntities(tenantId, saved.getId(), toSave.getAssigneeType(), assignToList);
        }
        publishEdgeEvent(tenantId, saved, ActionType.ADDED);
        return saved;
    }

    @Override
    @Transactional
    public CustomMenu updateCustomMenuConfig(TenantId tenantId, CustomMenuId id, CustomMenuConfig config) {
        CustomMenu menu = getExisting(tenantId, id);
        menu.setConfig(config != null ? config : new CustomMenuConfig());
        CustomMenu saved = customMenuDao.save(tenantId, menu);
        publishEdgeEvent(tenantId, saved, ActionType.UPDATED);
        return saved;
    }

    @Override
    @Transactional
    public CustomMenu updateCustomMenuName(TenantId tenantId, CustomMenuId id, String name) {
        if (StringUtils.isBlank(name)) {
            throw new DataValidationException("Custom menu name should be specified!");
        }
        CustomMenu menu = getExisting(tenantId, id);
        menu.setName(name);
        CustomMenu saved = customMenuDao.save(tenantId, menu);
        publishEdgeEvent(tenantId, saved, ActionType.UPDATED);
        return saved;
    }

    @Override
    @Transactional
    public void updateAssigneeList(TenantId tenantId, CustomMenuId id, CMAssigneeType newAssigneeType,
                                   List<EntityId> newList, String[] newUserGroupNames, boolean force) {
        CustomMenu menu = getExisting(tenantId, id);
        CMAssigneeType resolvedType = newAssigneeType == null ? CMAssigneeType.NO_ASSIGN : newAssigneeType;
        menu.setAssigneeType(resolvedType);
        // Stale group names must not survive a switch away from USER_GROUPS — they would be inert
        // (ignored by resolution) but would still show up on the next GET as if still active.
        menu.setUserGroupNames(resolvedType == CMAssigneeType.USER_GROUPS ? newUserGroupNames : null);
        validate(menu);
        if (resolvedType == CMAssigneeType.ALL) {
            demoteExistingDefault(tenantId, menu.getCustomerId(), menu.getScope(), force, id);
        }
        clearAssignees(tenantId, id);
        CustomMenu saved = customMenuDao.save(tenantId, menu);
        if (resolvedType == CMAssigneeType.CUSTOMERS || resolvedType == CMAssigneeType.USERS) {
            assignEntities(tenantId, id, resolvedType, newList);
        }
        publishEdgeEvent(tenantId, saved, ActionType.UPDATED);
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
    public PageData<CustomMenuInfo> findCustomMenuInfos(TenantId tenantId, CustomerId customerId, CMScope scope, PageLink pageLink) {
        return customMenuDao.findInfosByOwner(tenantId, normalize(customerId), scope, pageLink);
    }

    /**
     * PE seeds one ALL-assignee default menu per scope at the system level during install, and
     * nothing at the tenant/customer level — a fresh tenant's menu list is empty and it inherits
     * the system default until it creates its own. Called from the install/upgrade data loader,
     * and idempotent so an already-seeded database is left alone.
     */
    @Override
    public void createDefaultCustomMenus() {
        ensureDefault(CMScope.SYSTEM, "System default menu");
        ensureDefault(CMScope.TENANT, "Tenant default menu");
        ensureDefault(CMScope.CUSTOMER, "Customer default menu");
    }

    private void ensureDefault(CMScope scope, String name) {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        if (customMenuDao.findDefaultByOwnerAndScope(tenantId, NO_CUSTOMER, scope).isPresent()) {
            return;
        }
        CustomMenuInfo info = new CustomMenuInfo();
        info.setTenantId(tenantId);
        info.setCustomerId(NO_CUSTOMER);
        info.setName(name);
        info.setScope(scope);
        info.setAssigneeType(CMAssigneeType.ALL);
        customMenuDao.save(tenantId, new CustomMenu(info, new CustomMenuConfig()));
        log.info("Created {} scope default custom menu [{}]", scope, name);
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
        // The three system defaults are the end of every resolution chain — deleting one would
        // leave tenants/customers with no inheritable menu at all.
        if (menu.getTenantId().isSysTenantId() && menu.getAssigneeType() == CMAssigneeType.ALL) {
            throw new DataValidationException("System default menu can not be deleted");
        }
        List<EntityInfo> assignees = findAssignees(tenantId, menu);
        if (!assignees.isEmpty() && !force) {
            return CustomMenuDeleteResult.blocked(menu.getAssigneeType(), assignees);
        }
        clearAssignees(tenantId, id);
        customMenuDao.removeById(tenantId, id);
        // 用删除前读到的 menu 作 body：edge 侧收到 DELETED 时已无法回查内容。
        publishEdgeEvent(tenantId, menu, ActionType.DELETED);
        return CustomMenuDeleteResult.success();
    }

    /**
     * Called from tenant deletion, which is not itself transactional — without @Transactional the
     * derived bulk delete throws "No EntityManager with actual transaction available" and aborts
     * the whole tenant removal with a 500.
     */
    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        customMenuDao.removeByTenantId(tenantId);
    }

    @Override
    public CustomMenuConfig resolveSystemAdminMenu() {
        return visibleItems(customMenuDao
                .findDefaultByOwnerAndScope(TenantId.SYS_TENANT_ID, NO_CUSTOMER, CMScope.SYSTEM)
                .orElse(null));
    }

    @Override
    public CustomMenuConfig resolveTenantUserMenu(TenantId tenantId, UserId userId) {
        CustomMenu result = findCustomMenuByUserId(tenantId, userId);
        if (result == null) {
            result = customMenuDao.findDefaultByOwnerAndScope(tenantId, NO_CUSTOMER, CMScope.TENANT).orElse(null);
        }
        if (result == null) {
            result = customMenuDao.findDefaultByOwnerAndScope(TenantId.SYS_TENANT_ID, NO_CUSTOMER, CMScope.TENANT).orElse(null);
        }
        return visibleItems(result);
    }

    @Override
    public CustomMenuConfig resolveCustomerUserMenu(TenantId tenantId, CustomerId customerId, UserId userId) {
        CustomMenu result = findCustomMenuByUserId(tenantId, userId);
        if (result == null) {
            result = findCustomerHierarchyCustomMenu(tenantId, customerId);
        }
        if (result == null) {
            result = customMenuDao.findDefaultByOwnerAndScope(tenantId, NO_CUSTOMER, CMScope.CUSTOMER).orElse(null);
        }
        if (result == null) {
            result = customMenuDao.findDefaultByOwnerAndScope(TenantId.SYS_TENANT_ID, NO_CUSTOMER, CMScope.CUSTOMER).orElse(null);
        }
        return visibleItems(result);
    }

    /**
     * Direct user override first, then a USER_GROUPS menu matching one of the groups the user
     * directly belongs to: at TENANT scope for a tenant user, or walking the customer hierarchy
     * (nearest customer up to the tenant's own CUSTOMER-scope menus) for a customer user —
     * mirrors PE's CustomMenuServiceImpl#findCustomMenuByUserId.
     */
    private CustomMenu findCustomMenuByUserId(TenantId tenantId, UserId userId) {
        if (userId == null) {
            return null;
        }
        User user = userService.findUserById(tenantId, userId);
        if (user == null) {
            return null;
        }
        if (user.getCustomMenuId() != null) {
            CustomMenu direct = customMenuDao.findById(tenantId, user.getCustomMenuId());
            if (direct != null) {
                return direct;
            }
        }
        Set<String> groupNames = findUserGroupNames(tenantId, userId, user.getCustomerId());
        if (groupNames.isEmpty()) {
            return null;
        }
        if (user.getCustomerId() == null || user.getCustomerId().isNullUid()) {
            return findFirstUserGroupMenu(tenantId, NO_CUSTOMER, CMScope.TENANT, groupNames);
        }
        for (CustomerId owner : ownerCustomerHierarchy(tenantId, user.getCustomerId())) {
            CustomMenu match = findFirstUserGroupMenu(tenantId, owner, CMScope.CUSTOMER, groupNames);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    // Names of the non-"All" USER-type groups the user directly belongs to (explicit membership
    // relations only — "All" matches everyone and is deliberately not an assignable target).
    private Set<String> findUserGroupNames(TenantId tenantId, UserId userId, CustomerId customerId) {
        return entityGroupService.findEntityGroupsForEntity(tenantId, userId, customerId).stream()
                .map(groupId -> entityGroupService.findEntityGroupById(tenantId, groupId))
                .filter(group -> group != null && group.getType() == EntityType.USER && !group.isGroupAll())
                .map(EntityGroup::getName)
                .collect(Collectors.toSet());
    }

    private CustomMenu findFirstUserGroupMenu(TenantId tenantId, CustomerId ownerCustomerId, CMScope scope, Set<String> groupNames) {
        return customMenuDao.findUserGroupMenusByOwnerAndScope(tenantId, ownerCustomerId, scope).stream()
                .filter(menu -> menu.getUserGroupNames() != null
                        && Arrays.stream(menu.getUserGroupNames()).anyMatch(groupNames::contains))
                .findFirst().orElse(null);
    }

    /**
     * Nearest-first owner chain for a CUSTOMER-scope lookup: the customer itself, each parent
     * customer in turn, and finally {@link #NO_CUSTOMER} standing in for the tenant's own
     * CUSTOMER-scope menus. Same ordering as {@code OwnerService.getOwnerHierarchy}
     * (application module) — re-derived here with {@code CustomerService.findCustomerAncestors}
     * directly, since dao cannot depend on application.
     */
    private List<CustomerId> ownerCustomerHierarchy(TenantId tenantId, CustomerId customerId) {
        List<CustomerId> chain = new ArrayList<>();
        chain.add(customerId);
        List<Customer> ancestors = new ArrayList<>(customerService.findCustomerAncestors(tenantId, customerId));
        Collections.reverse(ancestors);
        ancestors.forEach(ancestor -> chain.add(ancestor.getId()));
        chain.add(NO_CUSTOMER);
        return chain;
    }

    /**
     * Walks the customer chain upwards: at each level the customer's own CUSTOMER-scope default
     * menu wins, then a menu explicitly assigned to that customer, then its parent customer.
     */
    private CustomMenu findCustomerHierarchyCustomMenu(TenantId tenantId, CustomerId customerId) {
        if (customerId == null || customerId.isNullUid()) {
            return null;
        }
        CustomMenu result = customMenuDao.findDefaultByOwnerAndScope(tenantId, customerId, CMScope.CUSTOMER).orElse(null);
        if (result != null) {
            return result;
        }
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer == null) {
            return null;
        }
        if (customer.getCustomMenuId() != null) {
            result = customMenuDao.findById(tenantId, customer.getCustomMenuId());
            if (result != null) {
                return result;
            }
        }
        return customer.isSubCustomer() ? findCustomerHierarchyCustomMenu(tenantId, customer.getParentCustomerId()) : null;
    }

    // Hidden items never reach the client: the effective-menu endpoint is what the navigation is
    // built from, and "hidden" there is indistinguishable from "removed".
    private static CustomMenuConfig visibleItems(CustomMenu menu) {
        if (menu == null || menu.getConfig() == null || menu.getConfig().getItems() == null) {
            return new CustomMenuConfig();
        }
        return new CustomMenuConfig(filterVisible(menu.getConfig().getItems()));
    }

    private static <T extends MenuItem> List<T> filterVisible(List<T> items) {
        return items.stream().filter(MenuItem::isVisible)
                .map(BaseCustomMenuService::filterVisiblePages)
                .collect(Collectors.toList());
    }

    private static <T extends MenuItem> T filterVisiblePages(T item) {
        if (item instanceof DefaultMenuItem defaultItem) {
            if (defaultItem.getPages() != null) {
                defaultItem.setPages(filterVisible(defaultItem.getPages()));
            }
        } else if (item instanceof CustomMenuItem customItem) {
            if (customItem.getPages() != null) {
                customItem.setPages(filterVisible(customItem.getPages()));
            }
        }
        return item;
    }

    private CustomMenu getExisting(TenantId tenantId, CustomMenuId id) {
        CustomMenu menu = customMenuDao.findById(tenantId, id);
        if (menu == null || !menu.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Custom menu with id [" + id + "] is not found!");
        }
        return menu;
    }

    /**
     * PE's CustomMenuValidator rules, verbatim: only the system tenant may own SYSTEM scope, a
     * customer-owned menu may only be CUSTOMER scope, and a TENANT scope menu cannot be targeted
     * at customers (its items are the tenant-admin menu, which customer users never see).
     */
    private void validate(CustomMenuInfo info) {
        if (StringUtils.isBlank(info.getName())) {
            throw new DataValidationException("Custom menu name should be specified!");
        }
        if (info.getTenantId() == null) {
            throw new DataValidationException("Custom menu should be assigned to tenant");
        }
        if (info.getScope() == null) {
            throw new DataValidationException("Custom menu scope should be specified!");
        }
        if (info.getScope() == CMScope.TENANT && info.getAssigneeType() == CMAssigneeType.CUSTOMERS) {
            throw new DataValidationException("Tenant custom menu can not be assigned to customers");
        }
        if (!info.getTenantId().isSysTenantId() && info.getScope() == CMScope.SYSTEM) {
            throw new DataValidationException("Tenant custom menu can not have SYSTEM scope! Only TENANT and CUSTOMER are available for tenant");
        }
        if (info.getCustomerId() != null && !info.getCustomerId().isNullUid() && info.getScope() != CMScope.CUSTOMER) {
            throw new DataValidationException("Customer custom menu can have CUSTOMER scope only");
        }
        if (info.getAssigneeType() == CMAssigneeType.USER_GROUPS
                && (info.getUserGroupNames() == null || info.getUserGroupNames().length == 0)) {
            throw new DataValidationException("At least one user group name should be specified");
        }
    }

    private void demoteExistingDefault(TenantId tenantId, CustomerId customerId, CMScope scope, boolean force) {
        demoteExistingDefault(tenantId, customerId, scope, force, null);
    }

    private void demoteExistingDefault(TenantId tenantId, CustomerId customerId, CMScope scope, boolean force, CustomMenuId self) {
        Optional<CustomMenu> existingDefault = customMenuDao.findDefaultByOwnerAndScope(tenantId, normalize(customerId), scope);
        if (existingDefault.isEmpty() || existingDefault.get().getId().equals(self)) {
            return;
        }
        if (!force) {
            throw new DataValidationException("There is already default menu for scope " + scope);
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

    private static CustomerId normalize(CustomerId customerId) {
        return customerId == null || customerId.isNullUid() ? NO_CUSTOMER : customerId;
    }

}
