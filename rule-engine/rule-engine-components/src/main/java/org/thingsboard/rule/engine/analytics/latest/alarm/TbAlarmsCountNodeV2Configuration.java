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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.ArrayList;
import java.util.List;

/**
 * CE port note: PE's "count alarms for propagation entities" option is dropped - CE's
 * RuleEngineAlarmService has no propagation-entity resolver (AlarmService#getPropagationEntityIds
 * never got ported), so this node counts alarms for the message originator only.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TbAlarmsCountNodeV2Configuration implements NodeConfiguration<TbAlarmsCountNodeV2Configuration> {

    private List<AlarmsCountMapping> alarmsCountMappings;
    private String outMsgType;

    @Override
    public TbAlarmsCountNodeV2Configuration defaultConfiguration() {
        TbAlarmsCountNodeV2Configuration configuration = new TbAlarmsCountNodeV2Configuration();
        List<AlarmsCountMapping> alarmsCountMappings = new ArrayList<>();
        AlarmsCountMapping mapping = new AlarmsCountMapping();
        mapping.setTarget("alarmsCount");
        alarmsCountMappings.add(mapping);
        configuration.setAlarmsCountMappings(alarmsCountMappings);
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }

}
