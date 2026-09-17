/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SPD 周期数值最大值和最小值的服务传输片段。
 *
 * <p>最大值、最小值分别保留来源设备、规范参数键和发生时间，
 * VO 转换器可直接使用这些字段构造展示详情，不需要再次查询设备。</p>
 */
@Getter
@RequiredArgsConstructor
public class SpdMetricExtremaDTO {

    /** 周期最大值；没有有效数据时为 null。 */
    private final Double maximum;
    /** 产生周期最大值的设备名称。 */
    private final String maximumDeviceName;
    /** 产生周期最大值的规范遥测键。 */
    private final String maximumParameter;
    /** 周期最大值发生时间。 */
    private final Long maximumTime;
    /** 周期最小值；没有有效数据时为 null。 */
    private final Double minimum;
    /** 产生周期最小值的设备名称。 */
    private final String minimumDeviceName;
    /** 产生周期最小值的规范遥测键。 */
    private final String minimumParameter;
    /** 周期最小值发生时间。 */
    private final Long minimumTime;
}
