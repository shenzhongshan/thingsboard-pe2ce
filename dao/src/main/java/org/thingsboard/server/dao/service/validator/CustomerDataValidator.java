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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.customer.CustomerServiceImpl;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.List;

@Component
public class CustomerDataValidator extends DataValidator<Customer> {

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private TenantService tenantService;

    @Override
    protected void validateCreate(TenantId tenantId, Customer customer) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.CUSTOMER);
    }

    @Override
    protected Customer validateUpdate(TenantId tenantId, Customer customer) {
        Customer old = customerDao.findById(customer.getTenantId(), customer.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing customer!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Customer customer) {
        validateString("Customer title", customer.getTitle());
        if (customer.getTitle().equals(CustomerServiceImpl.PUBLIC_CUSTOMER_TITLE)) {
            throw new DataValidationException("'Public' title for customer is system reserved!");
        }
        if (!StringUtils.isEmpty(customer.getEmail())) {
            validateEmail(customer.getEmail());
        }
        if (customer.getTenantId() == null) {
            throw new DataValidationException("Customer should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(customer.getTenantId())) {
                throw new DataValidationException("Customer is referencing to non-existent tenant!");
            }
        }
        if (customer.isSubCustomer()) {
            if (customer.getId() != null && customer.getParentCustomerId().equals(customer.getId())) {
                throw new DataValidationException("Customer cannot be its own parent!");
            }
            Customer parent = customerDao.findById(tenantId, customer.getParentCustomerId().getId());
            if (parent == null) {
                throw new DataValidationException("Customer is referencing a non-existent parent customer!");
            }
            if (!parent.getTenantId().equals(customer.getTenantId())) {
                throw new DataValidationException("Parent customer must belong to the same tenant!");
            }
            if (customer.getId() != null) {
                List<Customer> ancestors = customerDao.findAncestors(tenantId.getId(), customer.getParentCustomerId().getId());
                boolean cycle = customer.getParentCustomerId().equals(customer.getId())
                        || ancestors.stream().anyMatch(a -> a.getId().equals(customer.getId()));
                if (cycle) {
                    throw new DataValidationException("Cannot set parent customer to one of its own descendants!");
                }
            }
        }
    }
}
