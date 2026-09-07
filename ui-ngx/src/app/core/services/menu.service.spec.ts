import assert from 'node:assert/strict';
import '@angular/compiler';
import { NEVER } from 'rxjs';
import { MenuService } from './menu.service';
import { MenuItemType } from '@shared/models/custom-menu.models';

// This file is executable with ts-node until the project restores an Angular test target.
// It protects the customer-menu contract: a configured customer menu is authoritative.
const service = new MenuService(
  { pipe: () => NEVER } as any,
  {} as any,
  { events: NEVER, url: '/' } as any
);

const base = [
  { id: 'home', name: 'Home', type: 'link', path: '/home' },
  { id: 'alarms', name: 'Alarms', type: 'link', path: '/alarms' },
  { id: 'edge_instances', name: 'Edge instances', type: 'link', path: '/edge' }
] as any;

const configuredItems = [{ type: MenuItemType.DEFAULT, id: 'home', visible: true }] as any;
const sections = (service as any).overlaySections(base, configuredItems, false);

assert.deepEqual(sections.map((section: { id: string }) => section.id), ['home']);
