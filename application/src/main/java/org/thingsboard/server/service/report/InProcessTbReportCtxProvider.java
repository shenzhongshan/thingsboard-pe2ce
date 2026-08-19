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
import org.springframework.stereotype.Component;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.TbReportCtxProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Builds the per-render context.
 * <p>
 * PE's counterpart runs inside a separate {@code tb-report} process and therefore carries an access
 * token so it can call the platform back over REST. Here the renderer runs in the platform, so the
 * token is left empty and {@link InProcessReportDataService} reaches the services directly — see
 * that class for how the report user's permissions are still honoured.
 */
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class InProcessTbReportCtxProvider implements TbReportCtxProvider {

    private static final String CREATED_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final TbelInvokeService tbelInvokeService;

    @Override
    public TbReportCtx newContext(ReportTask task) {
        String timezone = task.getTimezone();
        SimpleDateFormat createdTimeFormat = new SimpleDateFormat(CREATED_TIME_PATTERN);
        if (timezone != null && !timezone.isEmpty()) {
            createdTimeFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        }
        return InProcessTbReportCtx.builder()
                .tenantId(task.getTenantId())
                .configuration(task.getReportTemplateConfig())
                .timeZone(timezone)
                .userId(task.getUserId())
                .userOwnerId(task.getUserOwnerId())
                .accessToken(task.getAccessToken())
                .accessTokenExpTs(task.getAccessTokenExpirationTs())
                .reportCreatedTime(createdTimeFormat.format(new Date()))
                .tbelInvokeService(tbelInvokeService)
                .build();
    }

}
