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
package org.thingsboard.rule.engine.integration;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ACTION,
        name = "integration downlink",
        configClazz = TbIntegrationDownlinkConfiguration.class,
        nodeDescription = "Pushes downlink message to selected integration",
        nodeDetails = "Will push downlink message to the selected integration. CE port: only MQTT integrations support " +
                "downlink in this build (publishes the message payload to the integration's configured downlink topic).",
        icon = "input"
)
public class TbIntegrationDownlinkNode implements TbNode {

    private IntegrationId integrationId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbIntegrationDownlinkConfiguration config = TbNodeUtils.convert(configuration, TbIntegrationDownlinkConfiguration.class);
        if (config.getIntegrationId() == null) {
            throw new TbNodeException("Integration id is not set in the rule node configuration!");
        }
        this.integrationId = new IntegrationId(config.getIntegrationId());
        Integration integration = ctx.findIntegrationById(this.integrationId);
        if (integration == null) {
            throw new TbNodeException("Integration with ID [" + this.integrationId + "] not found!");
        }
        if (!integration.getTenantId().equals(ctx.getTenantId())) {
            throw new TbNodeException("Integration with ID [" + this.integrationId + "] belongs to different tenant!");
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (integrationId != null) {
            ctx.pushToIntegrationDownlink(integrationId, msg,
                    () -> ctx.tellNext(msg, "Success"),
                    t -> ctx.tellFailure(msg, t));
        } else {
            ctx.tellNext(msg, "Failure");
        }
    }

    @Override
    public void destroy() {
        this.integrationId = null;
    }

}
