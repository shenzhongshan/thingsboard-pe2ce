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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import java.util.ArrayList;
import java.util.List;

@Data
public class TbAggLatestTelemetryNodeV2Configuration implements NodeConfiguration<TbAggLatestTelemetryNodeV2Configuration> {

    private static final int MIN_DEDUPLICATION_IN_SEC = 10;

    private String outMsgType;
    private EntitySearchDirection direction;
    private String relationType;
    private long deduplicationInSec;
    private List<AggLatestMapping> aggMappings;

    @Override
    public TbAggLatestTelemetryNodeV2Configuration defaultConfiguration() {
        TbAggLatestTelemetryNodeV2Configuration configuration = new TbAggLatestTelemetryNodeV2Configuration();
        List<AggLatestMapping> aggMappings = new ArrayList<>();
        AggLatestMapping aggMapping = new AggLatestMapping();
        aggMapping.setSource("temperature");
        aggMapping.setSourceScope("LATEST_TELEMETRY");
        aggMapping.setAggFunction(MathFunction.AVG);
        aggMapping.setDefaultValue(0.0);
        aggMapping.setTarget("latestAvgTemperature");
        aggMappings.add(aggMapping);
        configuration.setAggMappings(aggMappings);
        configuration.setDirection(EntitySearchDirection.FROM);
        configuration.setRelationType("Contains");
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }

    public long getDeduplicationInSec() {
        return Math.max(MIN_DEDUPLICATION_IN_SEC, deduplicationInSec);
    }

}
