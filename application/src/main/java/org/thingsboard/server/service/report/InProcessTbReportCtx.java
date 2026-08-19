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

import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.report.context.TbReportCtx;

/**
 * The in-platform report context.
 * <p>
 * {@link TbReportCtx} is abstract because PE's only concrete subclass carries a REST client back to
 * the platform. Running in the platform there is nothing extra to hold, so this subclass exists
 * purely to satisfy {@link TbReportCtx#createSubReportCxt(ReportTemplateConfig)} — a sub-report
 * renders with its own template configuration but inherits everything else, including the
 * accumulated params.
 * <p>
 * The builder shape mirrors what Lombok's {@code @SuperBuilder} generates for the parent, which is
 * why it looks heavier than it is.
 */
public class InProcessTbReportCtx extends TbReportCtx {

    protected InProcessTbReportCtx(InProcessTbReportCtxBuilder<?, ?> b) {
        super(b);
    }

    public static InProcessTbReportCtxBuilder<?, ?> builder() {
        return new InProcessTbReportCtxBuilderImpl();
    }

    @Override
    public TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig) {
        TbReportCtx copy = builder()
                .tenantId(getTenantId())
                .tbelInvokeService(getTbelInvokeService())
                .configuration(reportTemplateConfig)
                .userId(getUserId())
                .userOwnerId(getUserOwnerId())
                .timeZone(getTimeZone())
                .accessToken(getAccessToken())
                .accessTokenExpTs(getAccessTokenExpTs())
                .reportCreatedTime(getReportCreatedTime())
                .build();
        copy.getParams().putAll(getParams());
        return copy;
    }

    public static abstract class InProcessTbReportCtxBuilder<C extends InProcessTbReportCtx, B extends InProcessTbReportCtxBuilder<C, B>>
            extends TbReportCtx.TbReportCtxBuilder<C, B> {
    }

    private static final class InProcessTbReportCtxBuilderImpl
            extends InProcessTbReportCtxBuilder<InProcessTbReportCtx, InProcessTbReportCtxBuilderImpl> {

        @Override
        protected InProcessTbReportCtxBuilderImpl self() {
            return this;
        }

        @Override
        public InProcessTbReportCtx build() {
            return new InProcessTbReportCtx(this);
        }
    }

}
