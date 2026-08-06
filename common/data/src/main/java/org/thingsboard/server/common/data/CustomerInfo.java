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
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerInfo extends Customer {

    private static final long serialVersionUID = 4325762332601165726L;

    @Schema(description = "Comma-separated names of the Entity Groups this customer belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
    private String groups;

    @Schema(description = "Title of the parent Customer, if this is a sub-customer.", accessMode = Schema.AccessMode.READ_ONLY)
    private String parentCustomerName;

    public CustomerInfo() {
        super();
    }

    public CustomerInfo(CustomerId customerId) {
        super(customerId);
    }

    public CustomerInfo(Customer customer) {
        super(customer);
    }

    public CustomerInfo(Customer customer, String groups) {
        super(customer);
        this.groups = groups;
    }
}
