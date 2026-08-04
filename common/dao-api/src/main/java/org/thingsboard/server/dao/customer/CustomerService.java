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
package org.thingsboard.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface CustomerService extends EntityDaoService {

    Customer findCustomerById(TenantId tenantId, CustomerId customerId);

    Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title);

    ListenableFuture<Optional<Customer>> findCustomerByTenantIdAndTitleAsync(TenantId tenantId, String title);

    ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId);

    Customer saveCustomer(Customer customer);

    Customer saveCustomer(Customer customer, NameConflictStrategy nameConflictStrategy);

    void deleteCustomer(TenantId tenantId, CustomerId customerId);

    Customer findOrCreatePublicCustomer(TenantId tenantId);

    Customer findPublicCustomer(TenantId tenantId);

    PageData<Customer> findCustomersByTenantId(TenantId tenantId, PageLink pageLink);

    /** Converts a page of customers to a page of {@link CustomerInfo}, enriching each with its Entity Group names. */
    PageData<CustomerInfo> toCustomerInfos(PageData<Customer> customers);

    void deleteCustomersByTenantId(TenantId tenantId);

    List<Customer> findCustomersByTenantIdAndIds(TenantId tenantId, List<CustomerId> customerIds);

    /** Direct children of the given customer (sub-customer hierarchy). */
    PageData<Customer> findCustomersByTenantIdAndParentCustomerId(TenantId tenantId, CustomerId parentCustomerId, PageLink pageLink);

    /** Top-level customers (no parent) for the tenant. */
    PageData<Customer> findTopLevelCustomersByTenantId(TenantId tenantId, PageLink pageLink);

    /** All descendants (any depth) of the given customer. */
    PageData<Customer> findCustomerDescendants(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /** The ancestor chain of the given customer, root-first (excludes the customer itself). */
    List<Customer> findCustomerAncestors(TenantId tenantId, CustomerId customerId);

}
