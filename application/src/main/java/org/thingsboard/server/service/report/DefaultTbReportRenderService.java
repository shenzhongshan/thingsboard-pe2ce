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
package org.thingsboard.server.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTbReportRenderService implements TbReportRenderService {

    private final PdfReportRenderer pdfReportRenderer;
    private final CsvReportRenderer csvReportRenderer;

    @Override
    public ReportData generateReport(TenantId tenantId, ReportTemplate template, String timezone) throws Exception {
        if (template.getConfiguration() == null) {
            throw new ThingsboardException("Report template has no configuration!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        TbReportFormat format = template.getFormat() != null ? template.getFormat() : TbReportFormat.PDF;
        byte[] data = switch (format) {
            case PDF -> pdfReportRenderer.render(tenantId, template, timezone);
            case CSV -> csvReportRenderer.render(tenantId, template);
        };
        String namePattern = template.getConfiguration().path("namePattern").asText(null);
        String name = expandName(namePattern, template.getName()) + format.getExtension();
        return new ReportData(data, name, format.getContentType());
    }

    private String expandName(String namePattern, String templateName) {
        String base = namePattern != null && !namePattern.isEmpty() ? namePattern : templateName;
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
        return base.replace("${date}", timestamp);
    }

}
