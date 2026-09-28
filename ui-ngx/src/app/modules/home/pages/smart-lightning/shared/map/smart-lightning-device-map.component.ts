/**
 * 文件职责：公共设备地图组件，统一负责天地图底图切换、点位筛选、状态颜色、详情模态框和选点事件。
 * 业务页面只提供设备数据与筛选函数，不再自行拼接地图弹窗或处理点位点击展示。
 */
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import L from 'leaflet';
import { smartLightningTianDiTuMapConfig } from './smart-lightning-device-map.config';
import {
  isValidSmartLightningMapCoordinate,
  toSmartLightningMapCoordinate
} from './smart-lightning-device-map.data';
import { SmartLightningMapDevice, SmartLightningMapDraftPoint } from './smart-lightning-device-map.models';

type SmartLightningMapStatus = 'alarm' | 'offline' | 'online';

@Component({
  selector: 'tb-smart-lightning-device-map',
  standalone: false,
  templateUrl: './smart-lightning-device-map.component.html',
  styleUrls: ['./smart-lightning-device-map.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SmartLightningDeviceMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapHost') private mapHost!: ElementRef<HTMLDivElement>;

  /** 地图接收的候选设备；页面可以通过 includeDevice 继续限制业务分类。 */
  @Input() devices: SmartLightningMapDevice[] = [];
  /** 页面级设备筛选函数，地图组件不内置防雷、能源等产品分类规则。 */
  @Input() includeDevice: (device: SmartLightningMapDevice) => boolean = () => true;
  /** 设备 ID 到绝对资产路径的索引，供详情模态框展示。 */
  @Input() assetPaths?: Map<string, string>;
  /** 设备 ID 到未清除告警名称的索引，告警优先于在线/离线状态。 */
  @Input() activeAlarmNames?: Map<string, string[]>;
  /** 是否进入地图选点模式。 */
  @Input() addingPoint = false;
  /** 当前尚未绑定设备的临时坐标。 */
  @Input() draftPoint: SmartLightningMapDraftPoint | null = null;

  /** 地图被点击时仅把坐标交给页面，设备属性保存仍由页面业务流程完成。 */
  @Output() mapClick = new EventEmitter<L.LatLng>();
  /** 保留点位点击事件，供需要联动详情页的调用方使用；详情模态框由本组件直接打开。 */
  @Output() deviceClick = new EventEmitter<SmartLightningMapDevice>();

  /** 当前点位详情，使用 Angular 模板渲染，避免拼接 HTML 后详情层丢失。 */
  selectedDevice: SmartLightningMapDevice | null = null;
  /** 按钮显示单击后将切换到的底图名称。 */
  nextBaseLayerLabel = '卫星影像';
  /** 图标表示按钮单击后将切换到的底图类型。 */
  nextBaseLayerIcon = 'mdi:satellite-variant';
  private map?: L.Map;
  private mapReady = false;
  private draftMarker?: L.CircleMarker;
  private vectorBaseLayer?: L.LayerGroup;
  private satelliteBaseLayer?: L.LayerGroup;
  private readonly markers = new Map<string, L.Marker>();

  constructor(
    private readonly zone: NgZone,
    private readonly changeDetector: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {
    this.initializeMap();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.mapReady && (changes.devices || changes.includeDevice || changes.assetPaths || changes.activeAlarmNames || changes.draftPoint || changes.addingPoint)) {
      this.renderMarkers();
      this.updateMapClickMode();
      if (this.selectedDevice) {
        const updatedDevice = this.findDevice(this.selectedDevice.id);
        if (updatedDevice) {
          // 首页实时刷新点位时，同步更新已经打开的详情，避免颜色已变化而弹层仍显示旧状态。
          this.selectedDevice = updatedDevice;
          this.changeDetector.markForCheck();
        } else {
          this.closeDeviceDetails();
        }
      }
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.markers.clear();
    this.vectorBaseLayer = undefined;
    this.satelliteBaseLayer = undefined;
  }

  /** 让页面在保存设备坐标后立即重建地图图层。 */
  refresh(): void {
    this.renderMarkers();
  }

  /**
   * 保留旧调用方法名，保存点位后打开共享详情模态框。
   * 如果设备尚未完成坐标刷新，则只等待下一次输入变化，不抛出异常。
   */
  openDevicePopup(deviceId: string): void {
    const device = this.findDevice(deviceId);
    const marker = this.markers.get(deviceId);
    if (!device || !marker) {
      return;
    }
    this.openDeviceDetails(device);
    marker.openTooltip();
    this.map?.panTo(marker.getLatLng(), {animate: true, duration: 0.25});
  }

  /** 关闭设备详情模态框。 */
  closeDeviceDetails(): void {
    this.selectedDevice = null;
    this.changeDetector.markForCheck();
  }

  /** 支持键盘 Esc 关闭详情，避免弹层打开后只能点击关闭按钮。 */
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.selectedDevice) {
      this.closeDeviceDetails();
    }
  }

  /** 当前详情设备的状态文案。 */
  selectedDeviceStatus(): string {
    return this.selectedDevice ? this.getStatusLabel(this.getStatus(this.selectedDevice)) : '--';
  }

  /** 当前详情设备的状态样式类。 */
  selectedDeviceStatusClass(): string {
    return this.selectedDevice ? `status-${this.getStatus(this.selectedDevice)}` : '';
  }

  /** 当前详情设备的告警名称，按设备去重后使用“/”分隔。 */
  selectedDeviceAlarms(): string[] {
    return this.selectedDevice ? this.getAlarmNames(this.selectedDevice) : [];
  }

  /** 单击在矢量地图和卫星影像间切换，设备点位图层保持不变。 */
  toggleBaseLayer(): void {
    if (!this.map || !this.vectorBaseLayer || !this.satelliteBaseLayer) {
      return;
    }
    const satelliteLayerIsActive = this.map.hasLayer(this.satelliteBaseLayer);
    if (satelliteLayerIsActive) {
      this.satelliteBaseLayer.removeFrom(this.map);
      this.vectorBaseLayer.addTo(this.map);
      this.nextBaseLayerLabel = '卫星影像';
      this.nextBaseLayerIcon = 'mdi:satellite-variant';
    } else {
      this.vectorBaseLayer.removeFrom(this.map);
      this.satelliteBaseLayer.addTo(this.map);
      this.nextBaseLayerLabel = '矢量地图';
      this.nextBaseLayerIcon = 'mdi:map-outline';
    }
  }

  /** 当前详情设备的绝对资产路径。 */
  selectedDeviceAssetPath(): string {
    if (!this.selectedDevice) {
      return '--';
    }
    return this.assetPaths?.get(this.selectedDevice.id) || this.selectedDevice.assetPath || '--';
  }

  private initializeMap(): void {
    const config = smartLightningTianDiTuMapConfig;
    this.map = L.map(this.mapHost.nativeElement, {attributionControl: true, zoomControl: true})
      .setView(config.center, config.zoom);
    const tileOptions: L.TileLayerOptions = {
      subdomains: '01234567',
      maxNativeZoom: 18,
      maxZoom: 21,
      attribution: '&copy; 天地图 Tianditu'
    };
    const createTileLayer = (layer: 'vec' | 'cva' | 'img' | 'cia'): L.TileLayer => L.tileLayer(
      `https://t{s}.tianditu.gov.cn/${layer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${config.token}`,
      {...tileOptions}
    );
    // 底图和对应注记放在同一组中，按钮切换时保持矢量标注与卫星标注匹配。
    this.vectorBaseLayer = L.layerGroup([createTileLayer('vec'), createTileLayer('cva')]);
    this.satelliteBaseLayer = L.layerGroup([createTileLayer('img'), createTileLayer('cia')]);
    this.vectorBaseLayer.addTo(this.map);
    this.mapReady = true;
    this.renderMarkers();
    this.updateMapClickMode();
  }

  /** 重新创建所有设备点位，保证告警状态、路径和详情数据来自同一份模型。 */
  private renderMarkers(): void {
    if (!this.map) {
      return;
    }
    this.markers.forEach((marker) => marker.remove());
    this.markers.clear();

    for (const sourceDevice of this.devices) {
      if (!this.includeDevice(sourceDevice) || !isValidSmartLightningMapCoordinate(sourceDevice.latitude, sourceDevice.longitude)) {
        continue;
      }
      const device = this.mergeDeviceDetails(sourceDevice);
      const latitude = toSmartLightningMapCoordinate(device.latitude)!;
      const longitude = toSmartLightningMapCoordinate(device.longitude)!;
      const status = this.getStatus(device);
      const marker = L.marker([latitude, longitude], {icon: this.buildMarkerIcon(status)}).addTo(this.map);
      marker.on('click', () => this.zone.run(() => {
        this.openDeviceDetails(device);
        this.deviceClick.emit(device);
      }));
      marker.bindTooltip(device.name, {direction: 'top', offset: [0, -30]});
      this.markers.set(device.id, marker);
    }
    this.renderDraftPoint();
  }

  /** 把页面传入的路径和告警索引合并到设备模型，地图内部统一使用完整详情对象。 */
  private mergeDeviceDetails(device: SmartLightningMapDevice): SmartLightningMapDevice {
    return {
      ...device,
      assetPath: this.assetPaths?.get(device.id) || device.assetPath || '--',
      alarmNames: this.getAlarmNames(device)
    };
  }

  private renderDraftPoint(): void {
    this.draftMarker?.remove();
    if (!this.map || !this.draftPoint) {
      return;
    }
    this.draftMarker = L.circleMarker([this.draftPoint.latitude, this.draftPoint.longitude], {
      radius: 8,
      color: '#ffffff',
      weight: 3,
      fillColor: '#195ce8',
      fillOpacity: 1
    }).bindTooltip('请选择设备并确认绑定', {direction: 'top'}).addTo(this.map);
  }

  private updateMapClickMode(): void {
    if (!this.map) {
      return;
    }
    this.map.off('click');
    if (this.addingPoint) {
      this.map.getContainer().style.cursor = 'crosshair';
      this.map.on('click', (event: L.LeafletMouseEvent) => this.mapClick.emit(event.latlng));
    } else {
      this.map.getContainer().style.cursor = '';
    }
  }

  private openDeviceDetails(device: SmartLightningMapDevice): void {
    this.selectedDevice = this.mergeDeviceDetails(device);
    this.changeDetector.markForCheck();
  }

  private findDevice(deviceId: string): SmartLightningMapDevice | undefined {
    const device = this.devices.find((item) => item.id === deviceId);
    return device ? this.mergeDeviceDetails(device) : undefined;
  }

  private getStatus(device: SmartLightningMapDevice): SmartLightningMapStatus {
    if (this.getAlarmNames(device).length > 0) {
      return 'alarm';
    }
    return device.active ? 'online' : 'offline';
  }

  private getStatusLabel(status: SmartLightningMapStatus): string {
    return status === 'alarm' ? '告警' : status === 'online' ? '在线' : '离线';
  }

  private getAlarmNames(device: SmartLightningMapDevice): string[] {
    const names = device.alarmNames?.length ? device.alarmNames : this.activeAlarmNames?.get(device.id) || [];
    return [...new Set(names.map((name) => name?.trim()).filter(Boolean))];
  }

  private buildMarkerIcon(status: SmartLightningMapStatus): L.DivIcon {
    const color = status === 'alarm' ? '#e53935' : status === 'offline' ? '#8b95a5' : '#16a34a';
    const svg = `<svg width="30" height="38" viewBox="0 0 30 38" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">` +
      `<path d="M15 37C13.6 35.1 3 22.4 3 14.3C3 7.9 8.4 2.5 15 2.5S27 7.9 27 14.3C27 22.4 16.4 35.1 15 37Z" fill="${color}" stroke="#ffffff" stroke-width="2"/>` +
      `<circle cx="15" cy="14" r="5" fill="#ffffff" fill-opacity=".9"/></svg>`;
    return L.divIcon({className: 'smart-lightning-map-pin', html: svg, iconSize: [30, 38], iconAnchor: [15, 37]});
  }
}
