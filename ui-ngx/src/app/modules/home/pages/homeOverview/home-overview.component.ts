/**
 * 文件职责：自定义平台首页的页面控制器。
 * 该页面独立于 ThingsBoard 原生 /home，仅汇总当前用户可见的设备、资产、告警和地图点位。
 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { AttributeService } from '@core/http/attribute.service';
import { AlarmService } from '@core/http/alarm.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { DeviceInfoQuery } from '@shared/models/device.models';
import { PageLink, MAX_SAFE_PAGE_SIZE } from '@shared/models/page/page-link';
import { CustomerId } from '@shared/models/id/customer-id';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { AlarmQueryV2, AlarmSearchStatus, AlarmSeverity } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { forkJoin, of, Subject } from 'rxjs';
import { catchError, map, takeUntil } from 'rxjs/operators';
import { SmartLightningDeviceMapComponent } from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.component';
import { SmartLightningMapDevice } from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.models';
import {
  buildDeviceDistribution,
  buildDonutStyle,
  createEmptyHomeOverviewStats,
  formatCoordinate,
  HomeOverviewDevice,
  HomeOverviewDistributionItem,
  HomeOverviewStats,
  parseNumber
} from './home-overview.data';

interface HomeOverviewAttributeMap {
  [key: string]: any;
}

@Component({
  selector: 'tb-home-overview',
  standalone: false,
  templateUrl: './home-overview.component.html',
  styleUrls: ['./home-overview.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeOverviewComponent implements OnInit, OnDestroy {
  @ViewChild('deviceMap') deviceMap?: SmartLightningDeviceMapComponent;

  readonly stats: HomeOverviewStats = createEmptyHomeOverviewStats();
  devices: HomeOverviewDevice[] = [];
  distribution: HomeOverviewDistributionItem[] = [];
  loading = true;
  errorMessage = '';
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly store: Store<AppState>,
    private readonly deviceService: DeviceService,
    private readonly assetService: AssetService,
    private readonly attributeService: AttributeService,
    private readonly alarmService: AlarmService,
    private readonly entityRelationService: EntityRelationService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadOverview();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  statusDonutStyle(value: number, color: string): Record<string, string> {
    return buildDonutStyle(value, this.devices.length, color);
  }

  distributionDonutStyle(): Record<string, string> {
    if (!this.distribution.length || !this.devices.length) {
      return {background: '#d9e2ea'};
    }
    let start = 0;
    const segments: string[] = [];
    for (const item of this.distribution) {
      const end = start + item.value * 100 / this.devices.length;
      segments.push(`${item.color} ${start}% ${end}%`);
      start = end;
    }
    return {background: `conic-gradient(${segments.join(', ')})`};
  }

  formatCoordinate(value: number | undefined): string {
    return formatCoordinate(value);
  }

  private loadOverview(): void {
    const user = getCurrentAuthUser(this.store);
    const pageLink = new PageLink(MAX_SAFE_PAGE_SIZE, 0);
    const deviceFilter: any = {};
    if (user?.authority === Authority.CUSTOMER_USER && user.customerId) {
      deviceFilter.customerId = new CustomerId(user.customerId);
    }
    const devices$ = this.deviceService.getDeviceInfosByQuery(new DeviceInfoQuery(pageLink, deviceFilter));
    const assets$ = user?.authority === Authority.CUSTOMER_USER && user.customerId
      ? this.assetService.getCustomerAssetInfos(user.customerId, pageLink)
      : this.assetService.getTenantAssetInfos(pageLink);
    const alarms$ = this.alarmService.getAllAlarmsV2(
      new AlarmQueryV2(null, new TimePageLink(MAX_SAFE_PAGE_SIZE, 0), {
        statusList: [AlarmSearchStatus.ACTIVE], severityList: Object.values(AlarmSeverity)
      })
    );

    forkJoin({devices: devices$, assets: assets$, alarms: alarms$}).pipe(
      map((result) => ({
        devices: result.devices.data || [],
        assetTotal: result.assets.totalElements || 0,
        alarms: result.alarms.data || []
      })),
      catchError(() => of({devices: [], assetTotal: 0, alarms: []})),
      takeUntil(this.destroy$)
    ).subscribe((result) => {
      const alarmNames = new Map<string, string[]>();
      for (const alarm of result.alarms) {
        if (alarm.originator?.entityType !== EntityType.DEVICE) {
          continue;
        }
        const names = alarmNames.get(alarm.originator.id) || [];
        if (alarm.type && !names.includes(alarm.type)) {
          names.push(alarm.type);
        }
        alarmNames.set(alarm.originator.id, names);
      }
      this.loadDeviceAttributes(result.devices, alarmNames, result.assetTotal);
    });
  }

  private loadDeviceAttributes(deviceInfos: any[], alarmNames: Map<string, string[]>, assetTotal: number): void {
    if (!deviceInfos.length) {
      this.stats.assetTotal = assetTotal;
      this.stats.deviceTotal = 0;
      this.stats.onlineTotal = 0;
      this.stats.offlineTotal = 0;
      this.stats.alarmDeviceTotal = 0;
      this.stats.unconfiguredTotal = 0;
      this.loading = false;
      this.cd.markForCheck();
      return;
    }
    const requests = deviceInfos.map((device) => this.attributeService.getEntityAttributes(
      {entityType: EntityType.DEVICE, id: device.id.id}, AttributeScope.SERVER_SCOPE,
      ['productName', 'latitude', 'longitude', 'active']
    ).pipe(catchError(() => of([]))));
    forkJoin(requests).pipe(takeUntil(this.destroy$)).subscribe((attributeLists) => {
      this.devices = deviceInfos.map((device, index) => {
        const attrs: HomeOverviewAttributeMap = {};
        for (const item of attributeLists[index] || []) {
          attrs[item.key] = item.value;
        }
        return {
          id: device.id.id,
          name: device.name,
          productName: attrs.productName || '未配置产品名称',
          profileName: device.deviceProfileName || device.type || '未分类设备',
          active: this.toBoolean(attrs.active, device.active),
          latitude: parseNumber(attrs.latitude),
          longitude: parseNumber(attrs.longitude),
          assetPath: '--',
          alarmNames: alarmNames.get(device.id.id) || []
        };
      });
      this.stats.assetTotal = assetTotal;
      this.stats.deviceTotal = this.devices.length;
      this.stats.onlineTotal = this.devices.filter((device) => device.active).length;
      this.stats.offlineTotal = this.devices.length - this.stats.onlineTotal;
      this.stats.alarmDeviceTotal = this.devices.filter((device) => device.alarmNames.length > 0).length;
      this.stats.unconfiguredTotal = this.devices.filter((device) => device.latitude === undefined || device.longitude === undefined).length;
      this.distribution = buildDeviceDistribution(this.devices);
      this.loading = false;
      this.cd.markForCheck();
    });
  }

  private toBoolean(value: unknown, fallback: boolean): boolean {
    if (value === true || value === 1 || value === '1' || value === 'true') {
      return true;
    }
    if (value === false || value === 0 || value === '0' || value === 'false') {
      return false;
    }
    return !!fallback;
  }

  onDeviceClick(device: SmartLightningMapDevice): void {
    if (device.assetPath !== '--') {
      return;
    }
    this.entityRelationService.findInfoByTo({entityType: EntityType.DEVICE, id: device.id}).pipe(
      map((relations) => relations.find((relation) => relation.from?.entityType === EntityType.ASSET)?.fromName || '--'),
      catchError(() => of('--')),
      takeUntil(this.destroy$)
    ).subscribe((parentName) => {
      device.assetPath = parentName;
      this.deviceMap?.refresh();
      this.deviceMap?.openDevicePopup(device.id);
      this.cd.markForCheck();
    });
  }
}
