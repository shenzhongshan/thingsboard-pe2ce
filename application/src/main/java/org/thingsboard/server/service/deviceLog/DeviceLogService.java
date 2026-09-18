/**
 * 文件职责：设备日志模块的业务接口，负责提供当前用户可见设备列表。
 *
 * <p>日志详情中的属性、最新遥测和时间轴使用平台已有的遥测/审计接口读取；本接口只负责
 * 统一设备权限范围和列表展示所需的最小字段，避免把平台原生设备页面逻辑复制到自定义页面。</p>
 */
package org.thingsboard.server.service.deviceLog;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogDeviceDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelinePageDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

public interface DeviceLogService {

    /** 返回当前用户有 READ 权限的设备，供设备日志列表展示。 */
    PageData<DeviceLogDeviceDTO> findVisibleDevices(SecurityUser user, PageLink pageLink)
            throws ThingsboardException;

    /** 查询设备最近时间线，默认由控制层传入最近 30 天窗口。 */
    DeviceLogTimelinePageDTO findRecentTimeline(SecurityUser user, UUID deviceId, int days, int pageSize)
            throws ThingsboardException;

    /** 查询设备更早的历史时间线，只在用户点击“查看更多”时调用。 */
    DeviceLogTimelinePageDTO findHistoryTimeline(SecurityUser user, UUID deviceId, int pageSize,
                                                 String beforeTs) throws ThingsboardException;
}
