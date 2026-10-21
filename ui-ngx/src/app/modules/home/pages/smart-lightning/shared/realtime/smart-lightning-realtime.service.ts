/**
 * 文件职责：智能防雷与告警页面的共享实时订阅。
 *
 * 该服务只负责维护 ThingsBoard WebSocket 和发出“数据已变化”的通知，
 * 不在这里处理设备权限、资产树过滤、告警排序或 DTO 映射；具体页面收到通知后
 * 继续调用原有 HTTP 聚合接口，保证各页面现有业务逻辑保持不变。
 */
import {Injectable, NgZone} from '@angular/core';
import {BehaviorSubject, Subject} from 'rxjs';
import {AlarmSearchStatus} from '@shared/models/alarm.models';
import {AliasFilterType} from '@shared/models/alias.models';
import {EntityType} from '@shared/models/entity-type.models';
import {EntityKeyType} from '@shared/models/query/query.models';
import {
  AlarmCountCmd,
  AlarmDataCmd,
  AlarmDataUpdate,
  EntityCountCmd,
  EntityDataCmd,
  EntityDataUpdate,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import {TelemetryWebsocketService} from '@core/ws/telemetry-websocket.service';
import {SMART_LIGHTNING_WAVEFORM_KEY} from '../telemetry/smart-lightning-measurements';

/** 防雷、能源和电气安全页面共用的型号测点订阅键；未配置型号不会展示其他设备的值。 */
export const SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS: readonly string[] = Array.from(new Set([
  'RemoteSig', 'McbStatus', 'GndStatus', 'StrikeCnt',
  // DES9／DESA 新协议诊断键接入现有共享订阅，不另建 WebSocket 连接。
  'SPD_RUL', 'SPD_Status', 'SPD_RULDay', 'SPD_Alarm', 'SPD_WorstPh', 'SPD_RULA', 'SPD_RULB', 'SPD_RULC', 'SPD_Reason', 'SPD_I1hA', 'SPD_I1hB', 'SPD_I1hC', 'SPD_I24hA', 'SPD_I24hB', 'SPD_I24hC', 'SPD_DeltaT',
  'LeakCurrA', 'LeakCurrB', 'LeakCurrC',
  'Temp1', 'Temp2', 'Temp3', 'Temp4', 'Temp5', 'Temp6', 'Temp7', 'Temp8',
  'PhAVolt', 'PhBVolt', 'PhCVolt',
  // DEM2／DEM4新版状态键接入现有订阅，其他型号继续使用各自定义的键。
  'PhAStatus', 'PhBStatus', 'PhCStatus', 'TripSW1', 'TripSW2', 'TripSW3', 'TripSW4',
  // G1A3新版地网电压、小数位与原有阻值共用现有订阅。
  'GndGridRes', 'GndVolt', 'ResDecimal', 'SampleCnt', 'SamplePrd', 'Temp',
  'SurgeCnt', 'Polarity', 'DevStatus',
  // M1AI／L2AI共用的新测点键；两种计数键已包含在共享集合中。
  SMART_LIGHTNING_WAVEFORM_KEY, 'PeakCur', 'KeepTime', 'CharImpe', 'EleCharge', 'Energy', 'PeakVolt',
  // 电气安全看板已确认的电流与电能质量指标，仍通过现有共享 WebSocket 订阅获取。
  'ResCurr1', 'ResCurr2', 'ResCurr3', 'LeakValue', 'CurrTot',
  'VoltThdA', 'VoltThdB', 'VoltThdC', 'NSeqVUnb',
  // E1P1 的数字输入输出状态和零地电压用于电气安全设备卡片及详情趋势图。
  'DO1', 'DI1', 'DI2', 'UG',
  // SFA 的以下键来自已确认的型号测点表；每个页面仍只把所属型号配置的键映射到界面。
  'Longitude', 'Latitude', 'LineVoltAB', 'LineVoltBC', 'LineVoltCA', 'Freq',
  'PhACurr', 'PhBCurr', 'PhCCurr', 'CurrVecSum', 'ResCurr',
  'ActPwrA', 'ActPwrB', 'ActPwrC', 'ActPwrTot',
  'RctPwrA', 'RctPwrB', 'RctPwrC', 'RctPwrTot',
  'AppPwrA', 'AppPwrB', 'AppPwrC', 'AppPwrTot',
  'PwrFactA', 'PwrFactB', 'PwrFactC', 'PwrFactT',
  'FwdActEA', 'FwdActEB', 'FwdActEC', 'FwdActET',
  'RevRctEA', 'RevRctEB', 'RevRctEC', 'RevRctET',
  'FwdRctEA', 'FwdRctEB', 'FwdRctEC', 'FwdRctET'
]));

/**
 * 告警实时变化事件：count 来源表示活动告警数量变化，data 来源表示告警状态、派发、确认或清除字段变化。
 * 页面只根据 source 决定是否需要重新读取，告警过滤和排序仍由原有 HTTP 接口完成。
 */
export interface SmartLightningAlarmRealtimeChange {
  readonly source: 'count' | 'data';
  readonly count?: number;
}

/** WebSocket 推送的单个设备最新遥测；详情页直接消费，不再二次查询才能刷新。 */
export interface SmartLightningDeviceTelemetryChange {
  readonly deviceId: string;
  readonly values: ReadonlyMap<string, {readonly ts: number; readonly value: string}>;
  /** 遥测键 lastActivityTime 的最新毫秒时间戳。 */
  readonly lastActivityTime?: number;
}

/**
 * 全局共享实时订阅服务。
 *
 * 多个页面可能同时处于路由缓存或短时间切换状态，因此使用引用计数：
 * 第一个页面进入时建立订阅，最后一个页面离开时才取消订阅，避免重复连接。
 */
@Injectable({providedIn: 'root'})
export class SmartLightningRealtimeService {
  private readonly deviceChangedSubject = new Subject<ReadonlySet<string>>();
  private readonly deviceTelemetryChangedSubject = new Subject<SmartLightningDeviceTelemetryChange>();
  private readonly alarmChangedSubject = new Subject<SmartLightningAlarmRealtimeChange>();
  private readonly activeChangedSubject = new Subject<void>();
  private readonly deviceContextChangedSubject = new Subject<void>();
  private readonly deviceContextIncrementalChangedSubject = new Subject<void>();
  private readonly deviceListChangedSubject = new Subject<void>();
  private readonly assetListChangedSubject = new Subject<void>();
  private readonly deviceMembershipChangedSubject = new Subject<void>();
  private readonly assetMembershipChangedSubject = new Subject<void>();
  /** 仅在完整 ENTITY_DATA 初始快照进入缓存后置为 true，避免页面用半份缓存覆盖后端汇总。 */
  private readonly entitySnapshotReadySubject = new BehaviorSubject<boolean>(false);
  /** 缓存 WebSocket 推送的设备最近活跃时间，供卡片首屏加载与实时刷新共用。 */
  private readonly latestActivityTimeByDevice = new Map<string, number>();
  private readonly devicePageSize = 10_000;
  /** 暂存当前 WebSocket 生命周期内的最新遥测，供稍后加载完成的卡片读取首帧快照。 */
  private readonly latestTelemetryByDevice = new Map<string, Map<string, {ts: number; value: string}>>();
  private subscriber?: TelemetrySubscriber;
  private consumerCount = 0;
  /** 实体和告警明细首帧是当前快照，页面已经通过 HTTP 初始化，因此跳过以免重复请求。 */
  private initialEntityFrame = true;
  private initialAlarmDataFrame = true;
  private lastDeviceCount?: number;
  private lastAssetCount?: number;

  /** 设备属性或实时遥测更新时发出的设备 ID 集合。 */
  readonly deviceChanged$ = this.deviceChangedSubject.asObservable();
  /** 设备最新遥测原始增量，保留 WebSocket 帧中的键、值与时间戳。 */
  readonly deviceTelemetryChanged$ = this.deviceTelemetryChangedSubject.asObservable();
  /** 活动告警数量或告警字段变化时发出的失效通知。 */
  readonly alarmChanged$ = this.alarmChangedSubject.asObservable();
  /** active 遥测变化的轻量通知；需要最新值时应消费 deviceTelemetryChanged$。 */
  readonly activeChanged$ = this.activeChangedSubject.asObservable();
  /** 首页使用的设备属性或实体字段变化。 */
  readonly deviceContextChanged$ = this.deviceContextChangedSubject.asObservable();
  /** 只在初始快照之后的设备属性或实体字段增量变化时发出。 */
  readonly deviceContextIncrementalChanged$ = this.deviceContextIncrementalChangedSubject.asObservable();
  /** 当前用户可见设备集合变化。 */
  readonly deviceListChanged$ = this.deviceListChangedSubject.asObservable();
  /** 当前用户可见资产集合变化。 */
  readonly assetListChanged$ = this.assetListChangedSubject.asObservable();
  /** 设备数量基线建立后的设备集合数量变化，不把初始 COUNT_DATA 当作变更。 */
  readonly deviceMembershipChanged$ = this.deviceMembershipChangedSubject.asObservable();
  /** 资产数量基线建立后的资产集合数量变化，不把初始 COUNT_DATA 当作变更。 */
  readonly assetMembershipChanged$ = this.assetMembershipChangedSubject.asObservable();
  /** 当前共享 WebSocket 生命周期内是否已经收到完整实体首帧。 */
  readonly entitySnapshotReady$ = this.entitySnapshotReadySubject.asObservable();

  constructor(
    private readonly telemetryWebsocketService: TelemetryWebsocketService,
    private readonly ngZone: NgZone
  ) {}

  /** 取得设备当前缓存的遥测快照副本，避免页面直接修改共享订阅缓存。 */
  getLatestTelemetryValues(deviceId: string): ReadonlyMap<string, {ts: number; value: string}> {
    return new Map(this.latestTelemetryByDevice.get(deviceId) || []);
  }

  /** 按设备和测点读取最新缓存值，避免地图坐标查询复制整份设备遥测快照。 */
  getLatestTelemetryValue(deviceId: string, key: string): {ts: number; value: string} | undefined {
    return this.latestTelemetryByDevice.get(deviceId)?.get(key);
  }

  /** 取得设备最近活跃时间的 WebSocket 快照；未收到有效字段时返回 undefined。 */
  getLatestActivityTime(deviceId: string): number | undefined {
    return this.latestActivityTimeByDevice.get(deviceId);
  }

  /** 页面可在初始 HTTP 响应之后同步确认共享遥测缓存是否已经完整。 */
  get isEntitySnapshotReady(): boolean {
    return this.entitySnapshotReadySubject.value;
  }

  /** 页面开始使用实时数据时获取一个订阅引用。 */
  acquire(): void {
    this.consumerCount += 1;
    if (this.consumerCount === 1) {
      this.startSubscription();
    }
  }

  /** 页面销毁或离开时释放订阅引用；最后一个引用释放后关闭 WebSocket 命令。 */
  release(): void {
    if (this.consumerCount === 0) {
      return;
    }
    this.consumerCount -= 1;
    if (this.consumerCount === 0) {
      this.stopSubscription();
    }
  }

  /** 建立一套同时包含设备实体和活动告警的订阅。 */
  private startSubscription(): void {
    if (this.subscriber) {
      return;
    }
    this.entitySnapshotReadySubject.next(false);
    this.lastDeviceCount = undefined;
    this.lastAssetCount = undefined;

    const latestValues = [
      {key: 'active', type: EntityKeyType.TIME_SERIES},
      {key: 'lastActivityTime', type: EntityKeyType.TIME_SERIES},
      ...['productCategory', 'functionCategory', 'productName', 'latitude', 'longitude']
        .map((key) => ({key, type: EntityKeyType.SERVER_ATTRIBUTE})),
      ...SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS.map((key) => ({key, type: EntityKeyType.TIME_SERIES}))
    ];

    const entityCommand = new EntityDataCmd();
    entityCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      pageLink: {pageSize: this.devicePageSize, page: 0},
      entityFields: [{key: 'name', type: EntityKeyType.ENTITY_FIELD}, {key: 'type', type: EntityKeyType.ENTITY_FIELD}],
      latestValues
    };
    entityCommand.latestCmd = {keys: latestValues};

    const alarmCommand = new AlarmCountCmd();
    alarmCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      statusList: [AlarmSearchStatus.ACTIVE]
    };

    // AlarmCountCmd 只能感知数量变化，无法感知同一条告警的确认、派发、清除和备注变化。
    // 因此增加 AlarmDataCmd，收到活动告警字段更新后通知页面复用原有 HTTP 查询刷新完整行数据。
    const alarmDataCommand = new AlarmDataCmd();
    alarmDataCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      pageLink: {
        pageSize: this.devicePageSize,
        page: 0,
        statusList: [AlarmSearchStatus.ACTIVE]
      },
      // ThingsBoard 后端查询仓库会直接遍历这三个集合；即使不请求附加字段也必须传空数组，不能省略为 null。
      alarmFields: [],
      entityFields: [],
      latestValues: []
    };

    // 数量订阅只用于发现实体集合变化；页面仍调用原有 HTTP 接口取得权限范围内的准确数据。
    const deviceCountCommand = new EntityCountCmd();
    deviceCountCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE}
    };
    const assetCountCommand = new EntityCountCmd();
    assetCountCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.ASSET}
    };

    const subscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    subscriber.subscriptionCommands.push(entityCommand, alarmCommand, alarmDataCommand, deviceCountCommand, assetCountCommand);
    subscriber.entityData$.subscribe((update) => {
      const telemetryChanges = this.extractTelemetryChanges(update);
      telemetryChanges.forEach((change) => {
        this.mergeLatestTelemetry(change);
        this.deviceTelemetryChangedSubject.next(change);
      });
      const initialEntityFrame = this.initialEntityFrame;
      this.initialEntityFrame = false;
      // 首帧也通知首页核对 HTTP 快照，覆盖请求与 WebSocket 建立之间的短暂时间差。
      if (telemetryChanges.some((change) => change.values.has('active'))) {
        this.activeChangedSubject.next();
      }
      if (this.hasDeviceContextFields(update)) {
        this.deviceContextChangedSubject.next();
        if (!initialEntityFrame) {
          this.deviceContextIncrementalChangedSubject.next();
        }
      }
      if (!initialEntityFrame) {
        this.deviceChangedSubject.next(this.extractDeviceIds(update));
      }
      // ENTITY_DATA 的 data 部分是完整首帧；缓存已经先完成合并，再允许页面本地汇总。
      if (update.data?.data != null) {
        this.entitySnapshotReadySubject.next(true);
      }
    });
    subscriber.entityCount$.subscribe((update) => {
      // 数量首帧同样用于对齐首页 HTTP 快照；后续成员变化也按原查询口径复读。
      if (update.cmdId === deviceCountCommand.cmdId) {
        if (this.lastDeviceCount != null && this.lastDeviceCount !== update.count) {
          this.deviceMembershipChangedSubject.next();
        }
        this.lastDeviceCount = update.count;
        this.deviceListChangedSubject.next();
      } else if (update.cmdId === assetCountCommand.cmdId) {
        if (this.lastAssetCount != null && this.lastAssetCount !== update.count) {
          this.assetMembershipChangedSubject.next();
        }
        this.lastAssetCount = update.count;
        this.assetListChangedSubject.next();
      }
    });
    subscriber.alarmCount$.subscribe((update) => {
      // 告警数量首帧必须向页面透传，作为后续增减判断的基线。
      // 如果在这里丢弃首帧，看板收到的第一次新增告警会被误认为基线，从而不触发列表刷新；
      // 下一次清除才会成为第二个数量变化，所以会表现为“新增慢、清除快”。
      this.alarmChangedSubject.next({source: 'count', count: update.count});
    });
    subscriber.alarmData$.subscribe((update: AlarmDataUpdate) => {
      if (this.initialAlarmDataFrame) {
        this.initialAlarmDataFrame = false;
        return;
      }
      if (update.data || (update.update && update.update.length)) {
        this.alarmChangedSubject.next({source: 'data'});
      }
    });
    this.subscriber = subscriber;
    subscriber.subscribe();
  }

  /** 从 ThingsBoard 实体更新消息中提取受影响设备 ID。 */
  private extractDeviceIds(update: EntityDataUpdate): ReadonlySet<string> {
    const ids = new Set<string>();
    const initialData = update.data?.data || [];
    const incrementalData = update.update || [];
    for (const entityData of [...initialData, ...incrementalData]) {
      const id = entityData.entityId?.id;
      if (id) {
        ids.add(id);
      }
    }
    return ids;
  }

  /** 仅首页依赖的服务器属性或设备实体字段变化才需要重取完整设备上下文。 */
  private hasDeviceContextFields(update: EntityDataUpdate): boolean {
    return [...(update.data?.data || []), ...(update.update || [])].some((entityData) =>
      Object.keys(entityData.latest?.[EntityKeyType.SERVER_ATTRIBUTE] || {}).length > 0 ||
      Object.keys(entityData.latest?.[EntityKeyType.ENTITY_FIELD] || {}).length > 0);
  }

  /**
   * 保留实体订阅返回的最新时序值。
   * 初始快照和后续增量使用同一结构；时间戳为 0 的占位值不属于真实遥测，直接忽略。
   */
  private extractTelemetryChanges(update: EntityDataUpdate): SmartLightningDeviceTelemetryChange[] {
    const entities = [...(update.data?.data || []), ...(update.update || [])];
    const changes: SmartLightningDeviceTelemetryChange[] = [];
    entities.forEach((entityData) => {
      const deviceId = entityData.entityId?.id;
      const latest = entityData.latest?.[EntityKeyType.TIME_SERIES] || {};
      const activityValue = entityData.latest?.[EntityKeyType.TIME_SERIES]?.lastActivityTime?.value;
      const activityTimestamp = activityValue == null ? NaN : Number(activityValue);
      const hasActivityTimestamp = Number.isFinite(activityTimestamp) && activityTimestamp > 0;
      const values = new Map<string, {ts: number; value: string}>();
      Object.entries(latest).forEach(([key, item]) => {
        const timestamp = Number(item?.ts);
        if (Number.isFinite(timestamp) && timestamp > 0) {
          values.set(key, {ts: timestamp, value: item.value});
        }
      });
      if (deviceId && (values.size || hasActivityTimestamp)) {
        changes.push({
          deviceId,
          values,
          ...(hasActivityTimestamp ? {lastActivityTime: activityTimestamp} : {})
        });
      }
    });
    return changes;
  }

  /** 只保留每台设备每个已订阅键的最新一条记录，供卡片初次加载时补齐 WebSocket 首帧。 */
  private mergeLatestTelemetry(change: SmartLightningDeviceTelemetryChange): void {
    let deviceValues = this.latestTelemetryByDevice.get(change.deviceId);
    if (!deviceValues) {
      deviceValues = new Map();
      this.latestTelemetryByDevice.set(change.deviceId, deviceValues);
    }
    change.values.forEach((incoming, key) => {
      const current = deviceValues.get(key);
      if (!current || incoming.ts >= current.ts) {
        deviceValues.set(key, incoming);
      }
    });
    if (change.lastActivityTime != null) {
      const currentActivityTime = this.latestActivityTimeByDevice.get(change.deviceId);
      if (currentActivityTime == null || change.lastActivityTime > currentActivityTime) {
        this.latestActivityTimeByDevice.set(change.deviceId, change.lastActivityTime);
      }
    }
  }

  /** 取消共享订阅并清理引用，防止页面销毁后继续触发刷新。 */
  private stopSubscription(): void {
    this.subscriber?.unsubscribe();
    this.subscriber = undefined;
    this.latestTelemetryByDevice.clear();
    this.latestActivityTimeByDevice.clear();
    this.initialEntityFrame = true;
    this.initialAlarmDataFrame = true;
    this.lastDeviceCount = undefined;
    this.lastAssetCount = undefined;
    this.entitySnapshotReadySubject.next(false);
  }
}
