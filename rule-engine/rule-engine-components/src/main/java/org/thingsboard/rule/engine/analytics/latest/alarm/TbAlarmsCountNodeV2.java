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
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "alarms count",
        configClazz = TbAlarmsCountNodeV2Configuration.class,
        hasQueueName = true,
        nodeDescription = "Counts alarms by msg originator",
        nodeDetails = "Performs count of alarms for the message originator. Generates outgoing messages with alarm count " +
                "values for the originator. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages is controlled under \"<b>Output message type</b>\" configuration parameter.",
        icon = "functions"
)
public class TbAlarmsCountNodeV2 implements TbNode {

    private static final int MAX_ALARMS_PER_ENTITY = 1000;

    private TbAlarmsCountNodeV2Configuration config;
    private String queueName;
    private String outMsgType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAlarmsCountNodeV2Configuration.class);
        this.queueName = ctx.getQueueName();
        this.outMsgType = StringUtils.isNotBlank(config.getOutMsgType()) ? config.getOutMsgType() : TbMsgType.POST_TELEMETRY_REQUEST.name();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.isTypeOneOf(TbMsgType.ENTITY_CREATED, TbMsgType.ENTITY_UPDATED, TbMsgType.ALARM, TbMsgType.ALARM_CREATED,
                TbMsgType.ALARM_UPDATED, TbMsgType.ALARM_SEVERITY_UPDATED, TbMsgType.ALARM_ACK, TbMsgType.ALARM_CLEAR)) {
            process(ctx, msg);
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private void process(TbContext ctx, TbMsg msg) {
        EntityId entityId = msg.getOriginator();
        ObjectNode result = countAlarms(ctx, entityId);
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", Long.toString(System.currentTimeMillis()));
        TbMsg newMsg = TbMsg.newMsg().queueName(queueName).type(outMsgType).originator(entityId).copyMetaData(metaData)
                .data(JacksonUtil.toString(result)).build();
        ctx.enqueueForTellNext(newMsg, "Success");
        ctx.ack(msg);
    }

    private ObjectNode countAlarms(TbContext ctx, EntityId entityId) {
        ObjectNode obj = JacksonUtil.newObjectNode();
        for (AlarmsCountMapping mapping : config.getAlarmsCountMappings()) {
            Long startTime = mapping.getLatestInterval() > 0 ? System.currentTimeMillis() - mapping.getLatestInterval() : null;
            AlarmQuery query = AlarmQuery.builder()
                    .affectedEntityId(entityId)
                    .pageLink(new TimePageLink(new PageLink(MAX_ALARMS_PER_ENTITY), startTime, null))
                    .build();
            PageData<AlarmInfo> page = ctx.getAlarmService().findAlarms(ctx.getTenantId(), query);
            long count = page.getData().stream()
                    .filter(a -> mapping.getTypesList() == null || mapping.getTypesList().isEmpty() || mapping.getTypesList().contains(a.getType()))
                    .filter(a -> mapping.getSeverityList() == null || mapping.getSeverityList().isEmpty() || mapping.getSeverityList().contains(a.getSeverity()))
                    .filter(a -> mapping.getStatusList() == null || mapping.getStatusList().isEmpty() || mapping.getStatusList().contains(a.getStatus()))
                    .count();
            obj.put(mapping.getTarget(), count);
        }
        return obj;
    }

}
