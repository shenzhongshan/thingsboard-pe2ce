/**
 * 文件职责：复用天地图、设备水滴标记、状态颜色和设备详情弹窗。
 * 页面只提供设备数据与筛选函数；组件不决定哪些设备属于防雷分类。
 */
import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import L from 'leaflet';
import { smartLightningTianDiTuMapConfig } from '../../dashboard/smart-lightning-map.config';
import { SmartLightningMapDevice, SmartLightningMapDraftPoint } from './smart-lightning-device-map.models';

@Component({
  selector: 'tb-smart-lightning-device-map',
  standalone: false,
  template: '<div #mapHost class="smart-lightning-device-map" aria-label="设备天地图"></div>',
  styleUrls: ['./smart-lightning-device-map.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SmartLightningDeviceMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapHost') private mapHost!: ElementRef<HTMLDivElement>;
  @Input() devices: SmartLightningMapDevice[] = [];
  @Input() includeDevice: (device: SmartLightningMapDevice) => boolean = () => true;
  /** 可选的设备路径索引，供智能防雷看板复用已有资产树结果。 */
  @Input() assetPaths?: Map<string, string>;
  /** 可选的告警名称索引，供实时告警刷新后立即更新点位颜色。 */
  @Input() activeAlarmNames?: Map<string, string[]>;
  @Input() addingPoint = false;
  @Input() draftPoint: SmartLightningMapDraftPoint | null = null;
  @Output() mapClick = new EventEmitter<L.LatLng>();
  @Output() deviceClick = new EventEmitter<SmartLightningMapDevice>();

  private map?: L.Map;
  private mapReady = false;
  private draftMarker?: L.CircleMarker;
  private readonly markers = new Map<string, L.Marker>();

  ngAfterViewInit(): void {
    this.initializeMap();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.mapReady && (changes.devices || changes.includeDevice || changes.assetPaths || changes.activeAlarmNames || changes.draftPoint || changes.addingPoint)) {
      this.renderMarkers();
      this.updateMapClickMode();
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  /** 让父页面在设备属性保存成功后立即刷新地图。 */
  refresh(): void {
    this.renderMarkers();
  }

  /** 保存点位后打开对应设备弹窗。 */
  openDevicePopup(deviceId: string): void {
    this.markers.get(deviceId)?.openPopup();
  }

  private initializeMap(): void {
    const config = smartLightningTianDiTuMapConfig;
    this.map = L.map(this.mapHost.nativeElement, {attributionControl: true, zoomControl: true}).setView(config.center, config.zoom);
    const tileOptions: L.TileLayerOptions = {subdomains: '01234567', maxNativeZoom: 18, maxZoom: 21, attribution: '&copy; 天地图 Tianditu'};
    L.tileLayer('https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + config.token, tileOptions).addTo(this.map);
    L.tileLayer('https://t{s}.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + config.token, tileOptions).addTo(this.map);
    this.mapReady = true;
    this.renderMarkers();
    this.updateMapClickMode();
  }

  private renderMarkers(): void {
    if (!this.map) {
      return;
    }
    this.markers.forEach((marker) => marker.remove());
    this.markers.clear();
    for (const device of this.devices) {
      if (!this.includeDevice(device) || device.latitude === undefined || device.longitude === undefined) {
        continue;
      }
      const status = this.getStatus(device);
      const marker = L.marker([device.latitude, device.longitude], {icon: this.buildMarkerIcon(status)})
        .bindPopup(this.buildPopup(device, status), {className: `smart-lightning-map-popup-container smart-lightning-map-popup-${status}`})
        .addTo(this.map);
      marker.on('popupopen', () => this.applyPopupBackground(marker, status));
      marker.on('click', () => this.deviceClick.emit(device));
      marker.bindTooltip(device.name, {direction: 'top', offset: [0, -30]});
      this.markers.set(device.id, marker);
    }
    this.renderDraftPoint();
  }

  private renderDraftPoint(): void {
    this.draftMarker?.remove();
    if (!this.map || !this.draftPoint) {
      return;
    }
    this.draftMarker = L.circleMarker([this.draftPoint.latitude, this.draftPoint.longitude], {
      radius: 8, color: '#ffffff', weight: 3, fillColor: '#195ce8', fillOpacity: 1
    }).bindPopup('请选择设备并确认绑定').addTo(this.map);
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

  private getStatus(device: SmartLightningMapDevice): 'alarm' | 'offline' | 'online' {
    const alarms = device.alarmNames || this.activeAlarmNames?.get(device.id) || [];
    if (alarms.length > 0) {
      return 'alarm';
    }
    return device.active ? 'online' : 'offline';
  }

  private buildMarkerIcon(status: 'alarm' | 'offline' | 'online'): L.DivIcon {
    const color = status === 'alarm' ? '#e53935' : status === 'offline' ? '#8b95a5' : '#16a34a';
    const svg = `<svg width="30" height="38" viewBox="0 0 30 38" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">` +
      `<path d="M15 37C13.6 35.1 3 22.4 3 14.3C3 7.9 8.4 2.5 15 2.5S27 7.9 27 14.3C27 22.4 16.4 35.1 15 37Z" fill="${color}" stroke="#ffffff" stroke-width="2"/>` +
      `<circle cx="15" cy="14" r="5" fill="#ffffff" fill-opacity=".9"/></svg>`;
    return L.divIcon({className: 'smart-lightning-map-pin', html: svg, iconSize: [30, 38], iconAnchor: [15, 37], popupAnchor: [0, -34]});
  }

  private buildPopup(device: SmartLightningMapDevice, status: 'alarm' | 'offline' | 'online'): string {
    const statusText = status === 'online' ? '在线' : status === 'offline' ? '离线' : '告警';
    const alarmNames = device.alarmNames || this.activeAlarmNames?.get(device.id) || [];
    const alarmText = alarmNames.length ? alarmNames.join('/') : '无告警';
    const background = status === 'alarm' ? '#fee2e2' : status === 'offline' ? '#e5e7eb' : '#dcfce7';
    return `<div class="smart-lightning-map-popup" style="min-width:220px;padding:10px 12px;line-height:1.8;background:${background}">` +
      `<strong>设备名称：${this.escape(device.name)}</strong><br>` +
      `产品名称：${this.escape(device.productName || '未配置')}<br>` +
      `设备类型：${this.escape(device.profileName || '未配置')}<br>` +
      `绝对路径：${this.escape(this.assetPaths?.get(device.id) || device.assetPath || '--')}<br>` +
      `<span class="map-status-${status}">在线状态：${statusText}</span><br>` +
      `告警情况：${this.escape(alarmText)}<br>` +
      `纬度：${device.latitude?.toFixed(6)}<br>经度：${device.longitude?.toFixed(6)}</div>`;
  }

  private applyPopupBackground(marker: L.Marker, status: 'alarm' | 'offline' | 'online'): void {
    const popup = marker.getPopup()?.getElement();
    if (!popup) {
      return;
    }
    const background = status === 'alarm' ? '#fee2e2' : status === 'offline' ? '#e5e7eb' : '#dcfce7';
    for (const selector of ['.leaflet-popup-content-wrapper', '.leaflet-popup-content', '.leaflet-popup-tip']) {
      popup.querySelector<HTMLElement>(selector)?.style.setProperty('background-color', background, 'important');
    }
  }

  private escape(value: string): string {
    return String(value).replace(/[&<>"']/g, (character) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[character] || character));
  }
}
