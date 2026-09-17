/** 文件职责：将监测服务 DTO 转换为 Controller 对外返回的 VO。 */
package org.thingsboard.server.service.monitoring.converter;

import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.monitoring.dto.AlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.DeviceStatusSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.GroundResistanceExtremaDTO;
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeAnalysisDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeArcFaultDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeCurrentDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeGroundDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeOverallDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeSpdDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMetricExtremaDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.vo.AlarmTypeTotalVO;
import org.thingsboard.server.service.monitoring.vo.DeviceStatusSummaryVO;
import org.thingsboard.server.service.monitoring.vo.GroundMonitoringSummaryVO;
import org.thingsboard.server.service.monitoring.vo.GroundResistanceExtremaVO;
import org.thingsboard.server.service.monitoring.vo.LightningCurrentMonitoringSummaryVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningDeviceContextVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningMonitoringTreeVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningRealtimeAnalysisVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningRealtimeArcFaultVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningRealtimeCurrentVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningRealtimeGroundVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningRealtimeOverallVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningRealtimeSpdVO;
import org.thingsboard.server.service.monitoring.vo.SpdMetricExtremaVO;
import org.thingsboard.server.service.monitoring.vo.SpdMonitoringSummaryVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 监测展示对象转换器。
 *
 * <p>所有方法只做 DTO 到 VO 的逐字段复制，不执行查询、统计或状态判断。</p>
 */
public final class MonitoringVOConverter {

    private MonitoringVOConverter() {
    }

    public static DeviceStatusSummaryVO toVO(DeviceStatusSummaryDTO source) {
        return new DeviceStatusSummaryVO(source.getNormal(), source.getAbnormal());
    }

    public static GroundMonitoringSummaryVO toVO(GroundMonitoringSummaryDTO source) {
        return new GroundMonitoringSummaryVO(
                source.getTotal(),
                source.getOnline(),
                source.getOffline(),
                source.getAlarmCount(),
                toAlarmTypeVOs(source.getAlarmTypeTotals()),
                source.getAlarmRate(),
                source.getMaxResistance(),
                source.getMinResistance(),
                toVO(source.getResistanceExtrema()));
    }

    public static SpdMonitoringSummaryVO toVO(SpdMonitoringSummaryDTO source) {
        return new SpdMonitoringSummaryVO(
                source.getTotalDevices(),
                source.getOnlineDevices(),
                source.getOfflineDevices(),
                source.getAlarmCount(),
                toAlarmTypeVOs(source.getAlarmTypeTotals()),
                source.getMaxLeakage(),
                source.getMaxLeakageDeviceName(),
                source.getMaxLeakagePhase(),
                source.getMaxLeakageKey(),
                source.getMaxLeakageTime(),
                source.getLeakageReportingDeviceCount(),
                source.getGroundStatusNormalCount(),
                source.getGroundStatusDeviceCount(),
                source.getSpdStatusNormalCount(),
                source.getSpdStatusDeviceCount(),
                source.getTemperatureNormalCount(),
                source.getTemperatureDeviceCount(),
                source.getDegradationNormalCount(),
                source.getDegradationDeviceCount(),
                source.getLightningCount(),
                toVO(source.getTemperatureExtrema()),
                toVO(source.getLeakageExtrema()),
                toVO(source.getVoltageExtrema()));
    }

    public static LightningCurrentMonitoringSummaryVO toVO(LightningCurrentMonitoringSummaryDTO source) {
        return new LightningCurrentMonitoringSummaryVO(
                source.getTotalDevices(),
                source.getOnlineDevices(),
                source.getOfflineDevices(),
                source.getAlarmCount(),
                toAlarmTypeVOs(source.getAlarmTypeTotals()),
                source.getEventCount(),
                source.getMaxPeakCurrent(),
                source.getPeakCurrentUnit(),
                source.getMaxPeakDeviceName(),
                source.getMaxPeakTime(),
                source.getMaxPeakPolarity(),
                source.getMinPeakCurrent(),
                source.getMinPeakDeviceName(),
                source.getMinPeakTime(),
                source.getMinPeakPolarity(),
                source.getReportingDeviceCount());
    }

    public static SmartLightningMonitoringTreeVO toVO(SmartLightningMonitoringTreeDTO source) {
        List<SmartLightningMonitoringTreeVO.TreeNode> roots = null;
        if (source.roots() != null) {
            roots = new ArrayList<>();
            for (SmartLightningMonitoringTreeDTO.TreeNode root : source.roots()) {
                roots.add(toVO(root));
            }
        }
        return new SmartLightningMonitoringTreeVO(roots);
    }

    public static SmartLightningRealtimeAnalysisVO toVO(SmartLightningRealtimeAnalysisDTO source) {
        return new SmartLightningRealtimeAnalysisVO(
                toVO(source.getOverall()),
                toVO(source.getSpd()),
                toVO(source.getGround()),
                toVO(source.getLightningCurrent()),
                toVO(source.getArcFault()));
    }

    public static PageData<SmartLightningDeviceContextVO> toDeviceContextPage(
            PageData<SmartLightningDeviceContextDTO> source) {
        List<SmartLightningDeviceContextVO> data = new ArrayList<>();
        for (SmartLightningDeviceContextDTO item : source.getData()) {
            data.add(toVO(item));
        }
        return new PageData<>(data, source.getTotalPages(), source.getTotalElements(), source.hasNext());
    }

    private static SmartLightningDeviceContextVO toVO(SmartLightningDeviceContextDTO source) {
        return SmartLightningDeviceContextVO.builder()
                .deviceId(source.getDeviceId())
                .deviceName(source.getDeviceName())
                .deviceProfileName(source.getDeviceProfileName())
                .productCategory(source.getProductCategory())
                .productName(source.getProductName())
                .functionCategory(source.getFunctionCategory())
                .deviceProtocol(source.getDeviceProtocol())
                .active(source.getActive())
                .lastActivityTime(source.getLastActivityTime())
                .gatewayIdentifier(source.getGatewayIdentifier())
                .meterAddress(source.getMeterAddress())
                .circuitName(source.getCircuitName())
                .timeSetting(source.getTimeSetting())
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .build();
    }

    private static SmartLightningMonitoringTreeVO.TreeNode toVO(SmartLightningMonitoringTreeDTO.TreeNode source) {
        List<SmartLightningMonitoringTreeVO.TreeNode> children = null;
        if (source.children() != null) {
            children = new ArrayList<>();
            for (SmartLightningMonitoringTreeDTO.TreeNode child : source.children()) {
                children.add(toVO(child));
            }
        }
        return new SmartLightningMonitoringTreeVO.TreeNode(
                source.id(), source.name(), source.entityType(), children);
    }

    private static List<AlarmTypeTotalVO> toAlarmTypeVOs(List<AlarmTypeTotalDTO> sources) {
        if (sources == null) {
            return null;
        }
        List<AlarmTypeTotalVO> result = new ArrayList<>();
        for (AlarmTypeTotalDTO source : sources) {
            result.add(new AlarmTypeTotalVO(source.getType(), source.getCount()));
        }
        return result;
    }

    private static GroundResistanceExtremaVO toVO(GroundResistanceExtremaDTO source) {
        if (source == null) {
            return null;
        }
        return new GroundResistanceExtremaVO(
                source.getMaximum(),
                source.getMaximumDeviceName(),
                source.getMaximumParameter(),
                source.getMaximumTime(),
                source.getMinimum(),
                source.getMinimumDeviceName(),
                source.getMinimumParameter(),
                source.getMinimumTime());
    }

    private static SpdMetricExtremaVO toVO(SpdMetricExtremaDTO source) {
        if (source == null) {
            return null;
        }
        return new SpdMetricExtremaVO(
                source.getMaximum(),
                source.getMaximumDeviceName(),
                source.getMaximumParameter(),
                source.getMaximumTime(),
                source.getMinimum(),
                source.getMinimumDeviceName(),
                source.getMinimumParameter(),
                source.getMinimumTime());
    }

    private static SmartLightningRealtimeOverallVO toVO(SmartLightningRealtimeOverallDTO source) {
        if (source == null) {
            return null;
        }
        return new SmartLightningRealtimeOverallVO(
                source.getTotalDevices(),
                source.getOnlineDevices(),
                source.getOfflineDevices(),
                source.getAlarmDeviceCount(),
                source.getStatus());
    }

    private static SmartLightningRealtimeSpdVO toVO(SmartLightningRealtimeSpdDTO source) {
        if (source == null) {
            return null;
        }
        return new SmartLightningRealtimeSpdVO(
                source.getTotalDevices(),
                source.getOnlineDevices(),
                source.getOfflineDevices(),
                source.getAlarmDeviceCount(),
                source.getSpdStatusNormalCount(),
                source.getSpdStatusDeviceCount(),
                source.getGroundStatusNormalCount(),
                source.getGroundStatusDeviceCount(),
                source.getTemperatureNormalCount(),
                source.getTemperatureDeviceCount(),
                source.getLifeNormalCount(),
                source.getLifeDeviceCount(),
                source.getMaximumLeakage(),
                source.getMaximumLeakageDeviceName(),
                source.getMaximumLeakageParameter(),
                source.getMaximumLeakageTime(),
                source.getStatus());
    }

    private static SmartLightningRealtimeGroundVO toVO(SmartLightningRealtimeGroundDTO source) {
        if (source == null) {
            return null;
        }
        return new SmartLightningRealtimeGroundVO(
                source.getTotalDevices(),
                source.getOnlineDevices(),
                source.getOfflineDevices(),
                source.getAlarmDeviceCount(),
                source.getReportingDeviceCount(),
                source.getMaximumResistance(),
                source.getMaximumResistanceDeviceName(),
                source.getMaximumResistanceTime(),
                source.getMinimumResistance(),
                source.getMinimumResistanceDeviceName(),
                source.getMinimumResistanceTime(),
                source.getStatus());
    }

    private static SmartLightningRealtimeCurrentVO toVO(SmartLightningRealtimeCurrentDTO source) {
        if (source == null) {
            return null;
        }
        return new SmartLightningRealtimeCurrentVO(
                source.getTotalDevices(),
                source.getOnlineDevices(),
                source.getOfflineDevices(),
                source.getAlarmDeviceCount(),
                source.getTotalSurgeCount(),
                source.getLatestPeakCurrent(),
                source.getLatestPeakDeviceName(),
                source.getLatestPeakPolarity(),
                source.getLatestPeakTime(),
                source.getPeakCurrentUnit(),
                source.getStatus());
    }

    private static SmartLightningRealtimeArcFaultVO toVO(SmartLightningRealtimeArcFaultDTO source) {
        if (source == null) {
            return null;
        }
        return new SmartLightningRealtimeArcFaultVO(
                source.getTotalDevices(),
                source.getOnlineDevices(),
                source.getOfflineDevices(),
                source.getAlarmDeviceCount(),
                source.getNormalStatusCount(),
                source.getStatusDeviceCount(),
                source.getTotalArcFaultCount(),
                source.getLatestReportTime(),
                source.getStatus());
    }
}
