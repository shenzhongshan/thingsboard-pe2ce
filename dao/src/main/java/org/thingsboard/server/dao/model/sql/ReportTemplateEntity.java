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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "report_template")
public class ReportTemplateEntity extends BaseVersionedEntity<ReportTemplate> implements BaseEntity<ReportTemplate> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "format")
    private TbReportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ReportTemplateType type;

    @Column(name = "description")
    private String description;

    @Convert(converter = JsonConverter.class)
    @Column(name = "configuration")
    private JsonNode configuration;

    @Column(name = "external_id")
    private UUID externalId;

    public ReportTemplateEntity() {
        super();
    }

    public ReportTemplateEntity(ReportTemplate reportTemplate) {
        super(reportTemplate);
        if (reportTemplate.getTenantId() != null) {
            this.tenantId = reportTemplate.getTenantId().getId();
        }
        if (reportTemplate.getCustomerId() != null) {
            this.customerId = reportTemplate.getCustomerId().getId();
        }
        this.name = reportTemplate.getName();
        this.format = reportTemplate.getFormat();
        this.type = reportTemplate.getType();
        this.description = reportTemplate.getDescription();
        this.configuration = toConfigurationJson(reportTemplate);
        if (reportTemplate.getExternalId() != null) {
            this.externalId = reportTemplate.getExternalId().getId();
        }
    }

    @Override
    public ReportTemplate toData() {
        ReportTemplate reportTemplate = new ReportTemplate(new ReportTemplateId(id));
        reportTemplate.setCreatedTime(createdTime);
        reportTemplate.setVersion(version);
        if (tenantId != null) {
            reportTemplate.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            reportTemplate.setCustomerId(new CustomerId(customerId));
        }
        reportTemplate.setName(name);
        reportTemplate.setFormat(format);
        reportTemplate.setType(type);
        reportTemplate.setDescription(description);
        reportTemplate.setConfiguration(toConfiguration());
        if (externalId != null) {
            reportTemplate.setExternalId(new ReportTemplateId(externalId));
        }
        return reportTemplate;
    }

    /**
     * {@link ReportTemplateConfig} is polymorphic on its own {@code format} property, but the format
     * also lives in its own column and the column is authoritative — the UI sets it there. Stamp it
     * into the JSON on write so the type id is always present and always agrees with the column.
     */
    private JsonNode toConfigurationJson(ReportTemplate reportTemplate) {
        ReportTemplateConfig config = reportTemplate.getConfiguration();
        if (config == null) {
            return null;
        }
        JsonNode json = JacksonUtil.valueToTree(config);
        if (json instanceof ObjectNode object && reportTemplate.getFormat() != null) {
            object.put("format", reportTemplate.getFormat().name());
        }
        return json;
    }

    private ReportTemplateConfig toConfiguration() {
        if (configuration == null || configuration.isNull()) {
            return null;
        }
        JsonNode json = configuration;
        // Rows written before the configuration became typed carry no type id; supply it from the
        // column rather than failing the read of the whole template.
        if (json instanceof ObjectNode object && !object.hasNonNull("format") && format != null) {
            object = object.deepCopy();
            object.put("format", format.name());
            json = object;
        }
        return JacksonUtil.treeToValue(json, ReportTemplateConfig.class);
    }

}
