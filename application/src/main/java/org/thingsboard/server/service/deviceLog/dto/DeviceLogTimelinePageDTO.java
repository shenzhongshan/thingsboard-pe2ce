/** 文件职责：设备日志时间线分页结果，支持“查看更多”游标加载。 */
package org.thingsboard.server.service.deviceLog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 不返回全量 total，避免历史日志统计引起额外数据库开销。 */
@Getter
@AllArgsConstructor
public class DeviceLogTimelinePageDTO {

    private List<DeviceLogTimelineItemDTO> data;
    private boolean hasNext;
    private String nextCursor;
}
