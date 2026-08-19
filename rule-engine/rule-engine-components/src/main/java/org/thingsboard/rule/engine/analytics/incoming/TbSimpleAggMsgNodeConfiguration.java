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
package org.thingsboard.rule.engine.analytics.incoming;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.rule.engine.analytics.incoming.state.StatePersistPolicy;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * CE port of PE's aggregate-stream configuration. PE extends TbAbstractLatestNodeConfiguration to
 * inherit a {@code parentEntitiesQuery}, which drives its "interval entities" lookup; CE has no such
 * query hierarchy, so this configuration is standalone and {@code autoCreateIntervals} only fills
 * gaps for entities the node has already seen (see TbSimpleAggMsgNode's class javadoc).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbSimpleAggMsgNodeConfiguration implements NodeConfiguration<TbSimpleAggMsgNodeConfiguration> {

    private String mathFunction;
    private AggIntervalType aggIntervalType;
    private String timeZoneId;
    private String aggIntervalTimeUnit;
    private int aggIntervalValue;
    private boolean autoCreateIntervals;
    private String intervalPersistencePolicy;
    private String intervalCheckTimeUnit;
    private int intervalCheckValue;
    private String inputValueKey;
    private String outputValueKey;
    private String statePersistencePolicy;
    private String statePersistenceTimeUnit;
    private int statePersistenceValue;
    private String outMsgType;

    @Override
    public TbSimpleAggMsgNodeConfiguration defaultConfiguration() {
        TbSimpleAggMsgNodeConfiguration configuration = new TbSimpleAggMsgNodeConfiguration();
        configuration.setMathFunction(MathFunction.AVG.name());
        configuration.setAggIntervalType(AggIntervalType.HOUR);
        configuration.setTimeZoneId(ZoneId.systemDefault().getId());
        configuration.setAggIntervalTimeUnit(TimeUnit.HOURS.name());
        configuration.setAggIntervalValue(1);
        configuration.setAutoCreateIntervals(false);
        configuration.setIntervalPersistencePolicy(IntervalPersistPolicy.ON_EACH_CHECK_AFTER_INTERVAL_END.name());
        configuration.setIntervalCheckTimeUnit(TimeUnit.MINUTES.name());
        configuration.setIntervalCheckValue(1);
        configuration.setInputValueKey("temperature");
        configuration.setOutputValueKey("avgHourlyTemperature");
        configuration.setStatePersistencePolicy(StatePersistPolicy.ON_EACH_CHANGE.name());
        configuration.setStatePersistenceTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStatePersistenceValue(1);
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }

}
