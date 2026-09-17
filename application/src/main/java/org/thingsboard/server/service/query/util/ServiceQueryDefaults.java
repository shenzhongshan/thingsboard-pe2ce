/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.query.util;

/**
 * 查询服务共用的分页默认值。
 *
 * <p>设备查询和告警聚合的内存特征不同，因此分别定义页大小，
 * 避免告警模块依赖监测模块的工具类。</p>
 */
public final class ServiceQueryDefaults {

    /** 设备、资产树等实体查询的页大小。 */
    public static final int ENTITY_PAGE_SIZE = 10_000;

    /** 告警聚合查询的页大小，限制单次返回对象数量。 */
    public static final int ALARM_PAGE_SIZE = 500;

    private ServiceQueryDefaults() {
    }
}
