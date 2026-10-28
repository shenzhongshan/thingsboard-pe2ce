/** 文件职责：将告警服务 DTO 转换为 Controller 对外返回的 VO。 */
package org.thingsboard.server.service.alarms.converter;

import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisOverviewDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmDeviceCoverageDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmFocusDeviceDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmLeafAssetCountDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmPeriodSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSeverityTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmStatusTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTrendPointDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmAnalysisOverviewVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmAnalysisSummaryVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmDeviceCoverageVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmFocusDeviceVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmLeafAssetCountVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmPeriodSummaryVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmSeverityTotalVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmStatusTotalVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmSummaryVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmTrendPointVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmTypeTotalVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 告警展示对象转换器。
 *
 * <p>这里只进行逐字段复制，不包含统计、排序或权限判断，保证 Controller 不承担业务逻辑。</p>
 */
public final class DeviceAlarmVOConverter {

    private DeviceAlarmVOConverter() {
    }

    public static DeviceAlarmSummaryVO toVO(DeviceAlarmSummaryDTO source) {
        return new DeviceAlarmSummaryVO(source.getActiveCount(), toTypeVOs(source.getTypeTotals()));
    }

    public static DeviceAlarmAnalysisSummaryVO toVO(DeviceAlarmAnalysisSummaryDTO source) {
        return new DeviceAlarmAnalysisSummaryVO(
                toVO(source.getDay()),
                toVO(source.getMonth()),
                toVO(source.getYear()),
                toTrendVOs(source.getDayTrend()),
                toTrendVOs(source.getMonthTrend()),
                toTrendVOs(source.getYearTrend()),
                toFocusDeviceVOs(source.getFocusDevices()));
    }

    public static DeviceAlarmAnalysisOverviewVO toVO(DeviceAlarmAnalysisOverviewDTO source) {
        return new DeviceAlarmAnalysisOverviewVO(
                toStatusVOs(source.getStatusDistribution()),
                toVO(source.getDeviceCoverage()));
    }

    public static List<DeviceAlarmLeafAssetCountVO> toLeafAssetVOs(List<DeviceAlarmLeafAssetCountDTO> sources) {
        if (sources == null) {
            return null;
        }
        List<DeviceAlarmLeafAssetCountVO> result = new ArrayList<>();
        for (DeviceAlarmLeafAssetCountDTO source : sources) {
            result.add(new DeviceAlarmLeafAssetCountVO(
                    source.getAssetId(),
                    source.getAssetName(),
                    source.getAssetPath(),
                    source.getAlarmCount(),
                    source.getHandledCount(),
                    source.getHandledRate()));
        }
        return result;
    }

    public static List<DeviceAlarmTypeTotalVO> toTypeVOs(List<DeviceAlarmTypeTotalDTO> sources) {
        if (sources == null) {
            return null;
        }
        List<DeviceAlarmTypeTotalVO> result = new ArrayList<>();
        for (DeviceAlarmTypeTotalDTO source : sources) {
            result.add(new DeviceAlarmTypeTotalVO(source.getType(), source.getCount()));
        }
        return result;
    }

    public static List<DeviceAlarmSeverityTotalVO> toSeverityVOs(List<DeviceAlarmSeverityTotalDTO> sources) {
        if (sources == null) {
            return null;
        }
        List<DeviceAlarmSeverityTotalVO> result = new ArrayList<>();
        for (DeviceAlarmSeverityTotalDTO source : sources) {
            result.add(new DeviceAlarmSeverityTotalVO(source.getSeverity(), source.getCount()));
        }
        return result;
    }

    private static DeviceAlarmPeriodSummaryVO toVO(DeviceAlarmPeriodSummaryDTO source) {
        if (source == null) {
            return null;
        }
        return new DeviceAlarmPeriodSummaryVO(
                source.getCurrentCount(), source.getPreviousCount(), source.getChangePercent());
    }

    private static DeviceAlarmDeviceCoverageVO toVO(DeviceAlarmDeviceCoverageDTO source) {
        if (source == null) {
            return null;
        }
        return new DeviceAlarmDeviceCoverageVO(
                source.getTotalDevices(), source.getAlarmedDevices(), source.getActiveAlarmDevices());
    }

    private static List<DeviceAlarmTrendPointVO> toTrendVOs(List<DeviceAlarmTrendPointDTO> sources) {
        if (sources == null) {
            return null;
        }
        List<DeviceAlarmTrendPointVO> result = new ArrayList<>();
        for (DeviceAlarmTrendPointDTO source : sources) {
            result.add(new DeviceAlarmTrendPointVO(source.getLabel(), source.getCount()));
        }
        return result;
    }

    private static List<DeviceAlarmFocusDeviceVO> toFocusDeviceVOs(List<DeviceAlarmFocusDeviceDTO> sources) {
        if (sources == null) {
            return null;
        }
        List<DeviceAlarmFocusDeviceVO> result = new ArrayList<>();
        for (DeviceAlarmFocusDeviceDTO source : sources) {
            result.add(new DeviceAlarmFocusDeviceVO(
                    source.getDeviceName(),
                    source.getParentNodeName(),
                    source.getAlarmType(),
                    source.getDurationMs()));
        }
        return result;
    }

    private static List<DeviceAlarmStatusTotalVO> toStatusVOs(List<DeviceAlarmStatusTotalDTO> sources) {
        if (sources == null) {
            return null;
        }
        List<DeviceAlarmStatusTotalVO> result = new ArrayList<>();
        for (DeviceAlarmStatusTotalDTO source : sources) {
            result.add(new DeviceAlarmStatusTotalVO(source.getStatus(), source.getCount()));
        }
        return result;
    }
}
