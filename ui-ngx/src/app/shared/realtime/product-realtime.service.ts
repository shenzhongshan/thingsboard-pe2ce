/**
 * 文件职责：公共实时订阅基类，提供按产品分类独立管理的查询、缓存与生命周期。
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
import {EntityKeyType, EntityKeyValueType, FilterPredicateType, StringOperation, KeyFilter} from '@shared/models/query/query.models';
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

/**
 * 告警实时变化事件：count 来源表示活动告警数量变化，data 来源表示告警状态、派发、确认或清除字段变化。
 * 页面只根据 source 决定是否需要重新读取，告警过滤和排序仍由原有 HTTP 接口完成。
 */
export interface AlarmRealtimeChange {
  readonly source: 'count' | 'data';
  readonly count?: number;
}

/** WebSocket 推送的单个设备最新遥测；详情页直接消费，不再二次查询才能刷新。 */
export interface DeviceTelemetryChange {
  readonly deviceId: string;
  readonly values: ReadonlyMap<string, {readonly ts: number; readonly value: string}>;
  /** 遥测键 lastActivityTime 的最新毫秒时间戳。 */
  readonly lastActivityTime?: number;
}

/**
 * 按产品分类隔离状态的公共实时订阅实现。
 *
 * 多个页面可能同时处于路由缓存或短时间切换状态，因此使用引用计数：
 * 每个分类实例独立引用计数；共用平台 TelemetryWebsocketService 的同一物理连接。
 */
@Injectable({providedIn: 'root'})
export class ProductRealtimeService {
  /** 未设置分类的公共实例服务于首页/通用告警页；模块子类明确覆盖自己的服务器属性值。 */
  protected readonly productCategory: string | undefined = undefined;
  /** 公共首页只读取运行状态和坐标，完整业务测点由各模块自己的声明提供。 */
  protected readonly telemetryKeys: readonly string[] = [];

  private readonly deviceChangedSubject = new Subject<ReadonlySet<string>>();
  private readonly deviceTelemetryChangedSubject = new Subject<DeviceTelemetryChange>();
  private readonly alarmChangedSubject = new Subject<AlarmRealtimeChange>();
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
  /** 原生查询会为每个键展开多个 SQL 字段，限制每条命令的键数以避免数据库字段上限。 */
  private readonly telemetryBatchSize = 32;
  /** 暂存当前 WebSocket 生命周期内的最新遥测，供稍后加载完成的卡片读取首帧快照。 */
  private readonly latestTelemetryByDevice = new Map<string, Map<string, {ts: number; value: string}>>();
  private subscriber?: TelemetrySubscriber;
  private consumerCount = 0;
  /** 实体和告警明细首帧是当前快照，页面已经通过 HTTP 初始化，因此跳过以免重复请求。 */
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
  /** 只在本分类初始快照之后的设备属性或实体字段增量变化时发出。 */
  readonly deviceContextIncrementalChanged$ = this.deviceContextIncrementalChangedSubject.asObservable();
  /** 当前实例查询范围内可见设备集合变化。 */
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

    const contextKeys = [
      {key: 'active', type: EntityKeyType.TIME_SERIES},
      {key: 'lastActivityTime', type: EntityKeyType.TIME_SERIES},
      ...['productCategory', 'functionCategory', 'productName', 'latitude', 'longitude']
        .map((key) => ({key, type: EntityKeyType.SERVER_ATTRIBUTE}))
    ];
    const telemetryKeys = Array.from(new Set(['Longitude', 'Latitude', ...this.telemetryKeys]))
      .filter((key) => key !== 'active' && key !== 'lastActivityTime')
      .map((key) => ({key, type: EntityKeyType.TIME_SERIES}));
    const keyBatches = [contextKeys];
    for (let offset = 0; offset < telemetryKeys.length; offset += this.telemetryBatchSize) {
      keyBatches.push(telemetryKeys.slice(offset, offset + this.telemetryBatchSize));
    }
    // 每批沿用同一权限和分类查询；仅基础命令获取上下文，避免遥测首帧触发重复 HTTP 刷新。
    const entityCommands = keyBatches.map((latestValues, index) => {
      const command = new EntityDataCmd();
      command.query = {
        entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
        keyFilters: this.buildProductCategoryFilters(),
        pageLink: {pageSize: this.devicePageSize, page: 0},
        entityFields: index === 0
          ? [{key: 'name', type: EntityKeyType.ENTITY_FIELD}, {key: 'type', type: EntityKeyType.ENTITY_FIELD}] : [],
        latestValues
      };
      command.latestCmd = {keys: latestValues};
      return command;
    });
    // 命令 ID 由平台订阅时分配，因此按命令对象保存首帧状态；释放订阅后这些局部状态一并销毁。
    const receivedCommands = new Set<EntityDataCmd>();
    const completeSnapshots = new Map<EntityDataCmd, ReadonlySet<string>>();

    const alarmCommand = new AlarmCountCmd();
    alarmCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      keyFilters: this.buildProductCategoryFilters(),
      statusList: [AlarmSearchStatus.ACTIVE]
    };

    // AlarmCountCmd 只能感知数量变化，无法感知同一条告警的确认、派发、清除和备注变化。
    // 因此增加 AlarmDataCmd，收到活动告警字段更新后通知页面复用原有 HTTP 查询刷新完整行数据。
    const alarmDataCommand = new AlarmDataCmd();
    alarmDataCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      keyFilters: this.buildProductCategoryFilters(),
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
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      keyFilters: this.buildProductCategoryFilters()
    };
    const assetCountCommand = new EntityCountCmd();
    assetCountCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.ASSET}
    };

    const subscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    subscriber.subscriptionCommands.push(...entityCommands, alarmCommand, alarmDataCommand, deviceCountCommand, assetCountCommand);
    subscriber.entityData$.subscribe((update) => {
      const command = entityCommands.find((candidate) => candidate.cmdId === update.cmdId);
      if (!command) {
        return;
      }
      // 失败批次不能算作完整快照，也不能消耗其首次成功响应标记；原生订阅层仍负责显示具体错误。
      if (update.errorCode) {
        completeSnapshots.delete(command);
        this.entitySnapshotReadySubject.next(false);
        return;
      }
      const telemetryChanges = this.extractTelemetryChanges(update);
      telemetryChanges.forEach((change) => {
        this.mergeLatestTelemetry(change);
        this.deviceTelemetryChangedSubject.next(change);
      });
      const initialEntityFrame = !receivedCommands.has(command);
      receivedCommands.add(command);
      // 首帧也通知首页核对 HTTP 快照，覆盖请求与 WebSocket 建立之间的短暂时间差。
      if (telemetryChanges.some((change) => change.values.has('active'))) {
        this.activeChangedSubject.next();
      }
      if (command === entityCommands[0] && this.hasDeviceContextFields(update)) {
        this.deviceContextChangedSubject.next();
        if (!initialEntityFrame) {
          this.deviceContextIncrementalChangedSubject.next();
        }
      }
      if (!initialEntityFrame) {
        this.deviceChangedSubject.next(this.extractDeviceIds(update));
      }
      // 各批完整快照必须覆盖相同设备集合，才允许页面使用本地汇总；缺批、截断或集合不同继续保留 HTTP 统计。
      if (update.data?.data != null) {
        if (update.data.hasNext) {
          completeSnapshots.delete(command);
        } else {
          completeSnapshots.set(command, this.extractDeviceIds(update));
        }
        const contextIds = completeSnapshots.get(entityCommands[0]);
        const ready = completeSnapshots.size === entityCommands.length && contextIds != null &&
          Array.from(completeSnapshots.values()).every((ids) => ids.size === contextIds.size &&
            Array.from(ids).every((id) => contextIds.has(id)));
        if (this.entitySnapshotReadySubject.value !== ready) {
          this.entitySnapshotReadySubject.next(ready);
        }
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

  /** 使用平台原生SERVER_ATTRIBUTE等值过滤，保留平台原有租户、客户与共享实体权限。 */
  private buildProductCategoryFilters(): KeyFilter[] {
    if (!this.productCategory) {
      return [];
    }
    return [{key: {key: 'productCategory', type: EntityKeyType.SERVER_ATTRIBUTE},
      valueType: EntityKeyValueType.STRING,
      predicate: {type: FilterPredicateType.STRING, operation: StringOperation.EQUAL,
        ignoreCase: false, value: {defaultValue: this.productCategory}}}];
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

  /** 仅页面依赖的服务器属性或设备实体字段变化才需要重取完整设备上下文。 */
  private hasDeviceContextFields(update: EntityDataUpdate): boolean {
    return [...(update.data?.data || []), ...(update.update || [])].some((entityData) =>
      Object.keys(entityData.latest?.[EntityKeyType.SERVER_ATTRIBUTE] || {}).length > 0 ||
      Object.keys(entityData.latest?.[EntityKeyType.ENTITY_FIELD] || {}).length > 0);
  }

  /**
   * 保留实体订阅返回的最新时序值。
   * 初始快照和后续增量使用同一结构；时间戳为 0 的占位值不属于真实遥测，直接忽略。
   */
  private extractTelemetryChanges(update: EntityDataUpdate): DeviceTelemetryChange[] {
    const entities = [...(update.data?.data || []), ...(update.update || [])];
    const changes: DeviceTelemetryChange[] = [];
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
  private mergeLatestTelemetry(change: DeviceTelemetryChange): void {
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

  /** 只取消本分类订阅并清理本实例缓存，防止页面销毁后继续触发刷新。 */
  private stopSubscription(): void {
    this.subscriber?.unsubscribe();
    this.subscriber = undefined;
    this.latestTelemetryByDevice.clear();
    this.latestActivityTimeByDevice.clear();
    this.initialAlarmDataFrame = true;
    this.lastDeviceCount = undefined;
    this.lastAssetCount = undefined;
    this.entitySnapshotReadySubject.next(false);
  }
}
