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
package org.thingsboard.server.common.data.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;

/** Lightweight listing projection of {@link ReportTemplate} — omits the (potentially large) configuration JSON. */
@Schema
@EqualsAndHashCode(callSuper = true)
public class ReportTemplateInfo extends BaseData<ReportTemplateId> implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 1L;

    @Getter @Setter
    private TenantId tenantId;

    @Getter @Setter
    private CustomerId customerId;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private TbReportFormat format;

    @Getter @Setter
    private ReportTemplateType type;

    @Getter @Setter
    private String description;

    @Getter @Setter
    @Schema(description = "Owning customer's title, if this template is customer-owned", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    public ReportTemplateInfo() {
        super();
    }

    public ReportTemplateInfo(ReportTemplateId id) {
        super(id);
    }

    @Override
    public String getName() {
        return name;
    }

}
