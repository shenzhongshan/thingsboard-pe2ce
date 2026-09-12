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
package org.thingsboard.server.dao.sql.report;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ReportTemplateFields;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ReportTemplateEntity;
import org.thingsboard.server.dao.report.ReportTemplateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Component
@SqlDao
public class JpaReportTemplateDao extends JpaAbstractDao<ReportTemplateEntity, ReportTemplate> implements ReportTemplateDao {

    @Autowired
    private ReportTemplateRepository reportTemplateRepository;

    @Override
    protected Class<ReportTemplateEntity> getEntityClass() {
        return ReportTemplateEntity.class;
    }

    @Override
    protected JpaRepository<ReportTemplateEntity, UUID> getRepository() {
        return reportTemplateRepository;
    }

    @Override
    public ReportTemplate findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(reportTemplateRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public ReportTemplate findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(reportTemplateRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<ReportTemplate> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(reportTemplateRepository.findByTenantId(
                tenantId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ReportTemplateId getExternalIdByInternal(ReportTemplateId internalId) {
        return Optional.ofNullable(reportTemplateRepository.getExternalIdById(internalId.getId()))
                .map(ReportTemplateId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

    /** Feeds {@code EdqsSyncService#syncTenantEntity} - see ObjectType#edqsTenantTypes. */
    @Override
    public List<ReportTemplateFields> findNextBatch(UUID id, int batchSize) {
        return reportTemplateRepository.findNextBatch(id, Limit.of(batchSize));
    }

}
