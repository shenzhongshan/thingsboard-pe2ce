/**
 * 文件职责：智能防雷与告警页面的共享实时订阅。
 *
 * 该服务只负责维护 ThingsBoard WebSocket 和发出“数据已变化”的通知，
 * 不在这里处理设备权限、资产树过滤、告警排序或 DTO 映射；具体页面收到通知后
 * 继续调用原有 HTTP 聚合接口，保证各页面现有业务逻辑保持不变。
 */
import {Injectable, NgZone} from '@angular/core';
import {Subject} from 'rxjs';
import {AlarmSearchStatus} from '@shared/models/alarm.models';
import {AliasFilterType} from '@shared/models/alias.models';
import {EntityType} from '@shared/models/entity-type.models';
import {EntityKeyType} from '@shared/models/query/query.models';
import {
  AlarmCountCmd,
  AlarmDataCmd,
  AlarmDataUpdate,
  EntityDataCmd,
  EntityDataUpdate,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import {TelemetryWebsocketService} from '@core/ws/telemetry-websocket.service';

/** 页面实时卡片使用的规范遥测键；active 属于服务器属性，不放在此数组中。 */
export const SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS: readonly string[] = [
  'spdStatus',
  'groundStatus',
  'temperatureA',
  'temperatureB',
  'temperatureC',
  'temperatureD',
  'lifePercent',
  'leakageA',
  'leakageB',
  'leakageC',
  'groundResistance',
  'surgeCount',
  'surgePeak',
  'surgePolarity',
  'deviceStatus',
  'arcFaultCount'
];

/**
 * 告警实时变化事件：count 来源表示活动告警数量变化，data 来源表示告警状态、派发、确认或清除字段变化。
 * 页面只根据 source 决定是否需要重新读取，告警过滤和排序仍由原有 HTTP 接口完成。
 */
export interface SmartLightningAlarmRealtimeChange {
  readonly source: 'count' | 'data';
  readonly count?: number;
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
  private readonly alarmChangedSubject = new Subject<SmartLightningAlarmRealtimeChange>();
  private readonly devicePageSize = 10_000;
  private subscriber?: TelemetrySubscriber;
  private consumerCount = 0;
  /** 实体和告警明细首帧是当前快照，页面已经通过 HTTP 初始化，因此跳过以免重复请求。 */
  private initialEntityFrame = true;
  private initialAlarmDataFrame = true;

  /** 设备属性或实时遥测更新时发出的设备 ID 集合。 */
  readonly deviceChanged$ = this.deviceChangedSubject.asObservable();
  /** 活动告警数量或告警字段变化时发出的失效通知。 */
  readonly alarmChanged$ = this.alarmChangedSubject.asObservable();

  constructor(
    private readonly telemetryWebsocketService: TelemetryWebsocketService,
    private readonly ngZone: NgZone
  ) {}

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

    const latestValues = [
      {key: 'active', type: EntityKeyType.SERVER_ATTRIBUTE},
      ...SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS.map((key) => ({key, type: EntityKeyType.TIME_SERIES}))
    ];

    const entityCommand = new EntityDataCmd();
    entityCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      pageLink: {pageSize: this.devicePageSize, page: 0},
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

    const subscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    subscriber.subscriptionCommands.push(entityCommand, alarmCommand, alarmDataCommand);
    subscriber.entityData$.subscribe((update) => {
      if (this.initialEntityFrame) {
        this.initialEntityFrame = false;
        return;
      }
      this.deviceChangedSubject.next(this.extractDeviceIds(update));
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

  /** 取消共享订阅并清理引用，防止页面销毁后继续触发刷新。 */
  private stopSubscription(): void {
    this.subscriber?.unsubscribe();
    this.subscriber = undefined;
    this.initialEntityFrame = true;
    this.initialAlarmDataFrame = true;
  }
}
