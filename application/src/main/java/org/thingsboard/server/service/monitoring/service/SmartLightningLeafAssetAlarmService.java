/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.SmartLightningLeafAssetAlarmCountDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/** 末级设备父资产告警统计业务接口。 */
public interface SmartLightningLeafAssetAlarmService {

    /** 按告警 startTs 统计指定周期内每个末级设备父资产直接挂载设备的告警数量和处理率。 */
    List<SmartLightningLeafAssetAlarmCountDTO> getCounts(SecurityUser user, String period)
            throws ThingsboardException;
}
