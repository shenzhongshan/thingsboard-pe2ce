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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class TbAbstractDuplicateMsgNode<C> extends TbAbstractTransformNode<C> {

    protected ListenableFuture<List<TbMsg>> duplicate(TbContext ctx, TbMsg msg) {
        ListenableFuture<List<EntityId>> newOriginatorsFuture = getNewOriginators(ctx, msg);
        return Futures.transform(newOriginatorsFuture, newOriginators -> {
            if (newOriginators == null || newOriginators.isEmpty()) {
                return Collections.<TbMsg>emptyList();
            }
            if (newOriginators.size() == 1) {
                return Collections.singletonList(ctx.transformMsgOriginator(msg, newOriginators.get(0)));
            }
            return newOriginators.stream().map(newOriginator -> duplicateMsgForOriginator(msg, newOriginator)).toList();
        }, ctx.getDbCallbackExecutor());
    }

    private static TbMsg duplicateMsgForOriginator(TbMsg originalMsg, EntityId newOriginator) {
        return originalMsg.transform().id(UUID.randomUUID()).originator(newOriginator).callback(TbMsgCallback.EMPTY).build();
    }

    protected abstract ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg);

}
