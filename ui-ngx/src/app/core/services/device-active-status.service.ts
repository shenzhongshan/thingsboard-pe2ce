/**
 * 文件职责：共享当前用户可见设备的 active 最新遥测订阅与状态缓存。
 *
 * <p>顶部设备状态汇总和设备日志列表共用一条 ENTITY_DATA WebSocket 订阅，避免同一批设备
 * 的 active 键被多个页面重复订阅。缺失或无法识别的 active 值按离线处理。</p>
 */
import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityKeyType } from '@shared/models/query/query.models';
import { EntityDataCmd, EntityDataUpdate, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';

/** active 时序数据在查询和推送中使用的标准键。 */
const ACTIVE_TELEMETRY_KEY = {key: 'active', type: EntityKeyType.TIME_SERIES};

/** 一帧设备状态遥测变更，deviceIds 是本帧中可用状态发生更新的设备集合。 */
export interface DeviceActiveStatusChange {
  deviceIds: ReadonlySet<string>;
  initialSnapshot: boolean;
}

/** 保留 active 最新值及遥测时间戳，防止较旧快照覆盖较新的推送。 */
interface CachedDeviceActiveStatus {
  active: boolean;
  timestamp?: number;
}

@Injectable({providedIn: 'root'})
export class DeviceActiveStatusService {

  /** 与现有顶部状态汇总相同，一次读取当前用户可见设备的首屏遥测。 */
  private readonly entityPageSize = 10_000;
  private readonly activeByDeviceId = new Map<string, CachedDeviceActiveStatus>();
  private readonly activeStatusChangedSubject = new Subject<DeviceActiveStatusChange>();
  private subscriber?: TelemetrySubscriber;
  private consumerCount = 0;

  /** 页面可观察共享 WebSocket 中的 active 初始快照和增量更新。 */
  readonly activeStatusChanged$ = this.activeStatusChangedSubject.asObservable();

  constructor(private readonly telemetryWebsocketService: TelemetryWebsocketService,
              private readonly ngZone: NgZone) {}

  /** 获取一份共享订阅引用；第一个调用方进入时才建立设备 active WebSocket 订阅。 */
  acquire(): void {
    this.consumerCount++;
    if (this.consumerCount === 1) {
      this.startSubscription();
    }
  }

  /** 释放调用方持有的引用；最后一个调用方离开时取消订阅并清理旧状态。 */
  release(): void {
    if (this.consumerCount === 0) {
      return;
    }
    this.consumerCount--;
    if (this.consumerCount === 0) {
      this.stopSubscription();
    }
  }

  /** 返回缓存的设备在线状态；当前订阅尚未收到该设备快照时返回 undefined。 */
  getLatestActiveStatus(deviceId: string): boolean | undefined {
    return this.activeByDeviceId.get(deviceId)?.active;
  }

  /** 接收日志抽屉已有的 active 最新遥测，不额外建立 WebSocket 订阅。 */
  setLatestActiveStatus(deviceId: string, value: unknown, timestamp?: number): void {
    const active = this.parseActiveValue(value);
    if (!deviceId || active === undefined || !this.storeActiveStatus(deviceId, active, timestamp)) {
      return;
    }
    this.activeStatusChangedSubject.next({deviceIds: new Set([deviceId]), initialSnapshot: false});
  }

  /** 创建项目原有范围的设备 active 订阅，并把初始快照及增量转成共享状态。 */
  private startSubscription(): void {
    this.activeByDeviceId.clear();
    const command = new EntityDataCmd();
    command.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      pageLink: {pageSize: this.entityPageSize, page: 0},
      latestValues: [ACTIVE_TELEMETRY_KEY]
    };
    command.latestCmd = {keys: [ACTIVE_TELEMETRY_KEY]};

    this.subscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    this.subscriber.subscriptionCommands.push(command);
    this.subscriber.entityData$.subscribe((update) => this.applyUpdate(update));
    this.subscriber.subscribe();
  }

  /** 取消底层 WebSocket 命令并清空缓存，避免释放后返回过期状态。 */
  private stopSubscription(): void {
    this.subscriber?.unsubscribe();
    this.subscriber = undefined;
    this.activeByDeviceId.clear();
  }

  /** 将实体首帧作为当前状态基线，将后续 active 键更新作为单设备增量。 */
  private applyUpdate(update: EntityDataUpdate): void {
    const initialData = update.data?.data;
    const isInitialSnapshot = initialData !== undefined;
    const changedDeviceIds = new Set<string>();

    if (isInitialSnapshot) {
      for (const entityData of initialData) {
        const deviceId = entityData.entityId?.id;
        if (deviceId) {
          const activeTelemetry = entityData.latest?.[EntityKeyType.TIME_SERIES]?.active;
          const active = this.parseActiveValue(activeTelemetry?.value);
          if (active !== undefined) {
            this.storeActiveStatus(deviceId, active, activeTelemetry?.ts);
          } else if (!this.activeByDeviceId.has(deviceId)) {
            this.storeActiveStatus(deviceId, false);
          }
          changedDeviceIds.add(deviceId);
        }
      }
    }

    for (const entityData of update.update || []) {
      const deviceId = entityData.entityId?.id;
      const activeTelemetry = entityData.latest?.[EntityKeyType.TIME_SERIES]?.active;
      const active = this.parseActiveValue(activeTelemetry?.value);
      if (deviceId && active !== undefined && this.storeActiveStatus(deviceId, active, activeTelemetry?.ts)) {
        changedDeviceIds.add(deviceId);
      }
    }

    if (isInitialSnapshot || changedDeviceIds.size > 0) {
      this.activeStatusChangedSubject.next({deviceIds: changedDeviceIds, initialSnapshot: isInitialSnapshot});
    }
  }

  /** 按遥测时间戳更新单台设备缓存；新状态较旧时忽略，避免回退在线状态。 */
  private storeActiveStatus(deviceId: string, active: boolean, timestamp?: number): boolean {
    const normalizedTimestamp = typeof timestamp === 'number' && Number.isFinite(timestamp) && timestamp > 0 ? timestamp : undefined;
    const currentStatus = this.activeByDeviceId.get(deviceId);
    if (currentStatus?.timestamp !== undefined && normalizedTimestamp !== undefined &&
      normalizedTimestamp < currentStatus.timestamp) {
      return false;
    }
    const effectiveTimestamp = normalizedTimestamp ?? currentStatus?.timestamp;
    if (currentStatus?.active === active && currentStatus.timestamp === effectiveTimestamp) {
      return false;
    }
    this.activeByDeviceId.set(deviceId, {
      active,
      ...(effectiveTimestamp !== undefined ? {timestamp: effectiveTimestamp} : {})
    });
    return true;
  }

  /** 将 active 遥测的布尔、0/1 数字和字符串格式统一转换为在线状态。 */
  private parseActiveValue(value: unknown): boolean | undefined {
    if (typeof value === 'boolean') {
      return value;
    }
    if (typeof value === 'number') {
      if (value === 1) {
        return true;
      }
      if (value === 0) {
        return false;
      }
    }
    if (typeof value === 'string') {
      const normalizedValue = value.trim().toLowerCase();
      if (normalizedValue === 'true' || normalizedValue === '1' || normalizedValue === '1.0') {
        return true;
      }
      if (normalizedValue === 'false' || normalizedValue === '0' || normalizedValue === '0.0') {
        return false;
      }
    }
    return undefined;
  }
}
