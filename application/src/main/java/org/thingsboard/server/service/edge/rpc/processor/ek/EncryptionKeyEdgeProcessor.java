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
package org.thingsboard.server.service.edge.rpc.processor.ek;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EncryptionKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class EncryptionKeyEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        // 事件本身不带内容（entityId 为 null），密钥在这里现查。
        if (!EdgeEventActionType.ADDED.equals(edgeEvent.getAction())) {
            return null;
        }
        EncryptionKey encryptionKey = edgeCtx.getEncryptionService().findByTenantId(edgeEvent.getTenantId());
        if (encryptionKey == null) {
            log.warn("[{}] EncryptionKey not found, edge will not be able to decrypt any secret", edgeEvent.getTenantId());
            return null;
        }
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        EncryptionKeyUpdateMsg msg = EdgeMsgConstructorUtils.constructEncryptionKeyUpdatedMsg(msgType, encryptionKey);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addEncryptionKeyUpdateMsg(msg)
                .build();
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ENCRYPTION_KEY;
    }

}
