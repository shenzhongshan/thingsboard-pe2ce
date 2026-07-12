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
package org.thingsboard.rule.engine.report;

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

/**
 * CE port of PE's "generate report" (v2) node - wired to this port's synchronous Report Engine
 * (ReportGenerationService) instead of PE's async Job Manager (this build has no report job
 * queue/task-manager entry for report generation; see ReportGenerationService javadoc).
 */
@RuleNode(
        type = ComponentType.ACTION,
        name = "generate report",
        configClazz = TbGenerateReportV2NodeConfiguration.class,
        nodeDescription = "Generates report",
        nodeDetails = "Generates report from the selected report template. On success, sets a \"reports\" metadata field " +
                "with the generated report id.",
        icon = "description"
)
public class TbGenerateReportV2Node extends TbAbstractExternalNode {

    private static final String REPORTS_METADATA_KEY = "reports";

    private TbGenerateReportV2NodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbGenerateReportV2NodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg tbMsg) {
        ReportConfig reportConfig = config.isUseConfigFromMessage()
                ? JacksonUtil.fromString(tbMsg.getData(), ReportConfig.class)
                : config.getConfig();
        if (reportConfig == null) {
            ctx.tellFailure(tbMsg, new IllegalArgumentException("Report configuration is missing"));
            return;
        }
        TbMsg msg = ackIfNeeded(ctx, tbMsg);
        ctx.generateReport(reportConfig,
                report -> {
                    TbMsgMetaData metaData = msg.getMetaData().copy();
                    metaData.putValue(REPORTS_METADATA_KEY, report.getId().toString());
                    tellSuccess(ctx, msg.transform().metaData(metaData).build());
                },
                t -> tellFailure(ctx, msg, t));
    }

}
