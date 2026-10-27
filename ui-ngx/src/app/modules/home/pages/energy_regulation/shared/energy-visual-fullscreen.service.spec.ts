import assert from 'node:assert/strict';
import {EnergyVisualFullscreenService} from './energy-visual-fullscreen.service';

/** 最小DOM替身仅验证尺寸与样式恢复，不启动浏览器、Angular应用或真实地图。 */
class FakeStyle {
  values = new Map<string, {value: string; priority: string}>();
  getPropertyValue(key: string): string { return this.values.get(key)?.value || ''; }
  getPropertyPriority(key: string): string { return this.values.get(key)?.priority || ''; }
  setProperty(key: string, value: string, priority = ''): void { this.values.set(key, {value, priority}); }
  removeProperty(key: string): void { this.values.delete(key); }
}
class FakeEvents {
  listeners = new Map<string, Set<Function>>();
  addEventListener(type: string, listener: Function): void {
    if (!this.listeners.has(type)) { this.listeners.set(type, new Set()); }
    this.listeners.get(type)!.add(listener);
  }
  removeEventListener(type: string, listener: Function): void { this.listeners.get(type)?.delete(listener); }
  dispatchEvent(event: {type: string}): void { this.listeners.get(event.type)?.forEach((listener) => listener(event)); }
}
// 保存全局环境，测试结束后还原，避免影响同一进程里的Sass或模板解析。
const globalKeys = ['window', 'document', 'requestAnimationFrame', 'cancelAnimationFrame', 'ResizeObserver'];
const originals = new Map(globalKeys.map((key) => [key, Object.getOwnPropertyDescriptor(globalThis, key)]));
let observed: unknown;
let disconnected = 0;
class FakeObserver {
  observe(target: unknown): void { observed = target; }
  disconnect(): void { disconnected++; }
}
const frames = new Map<number, FrameRequestCallback>();
let frame = 0;
let rect = {top: 80, left: 210, right: 1200, bottom: 800};
let workspaceAvailable = true;
const win = Object.assign(new FakeEvents(), {innerWidth: 1200, innerHeight: 800});
const doc = Object.assign(new FakeEvents(), {querySelector: () => workspaceAvailable ? {getBoundingClientRect: () => rect} : null});
Object.assign(globalThis, {window: win, document: doc, ResizeObserver: FakeObserver,
  requestAnimationFrame: (callback: FrameRequestCallback) => { frames.set(++frame, callback); return frame; },
  cancelAnimationFrame: (id: number) => frames.delete(id)});
const card = () => ({style: new FakeStyle(), classList: {values: new Set<string>(),
  add(value: string) { this.values.add(value); }, remove(value: string) { this.values.delete(value); }}});
const a = card(); const b = card();
a.style.setProperty('height', '99px', 'important');
const control = new EnergyVisualFullscreenService();
control.toggle(a as unknown as HTMLElement);
assert.equal(control.activeTarget, a);
assert(observed);
assert.equal(a.style.getPropertyValue('left'), '210px');
assert.equal(a.style.getPropertyValue('width'), '990px');
assert(a.classList.values.has('energy-visual-expanded'));
rect = {top: 80, left: 300, right: 1000, bottom: 700};
win.dispatchEvent({type: 'resize'});
assert.equal(a.style.getPropertyValue('width'), '700px');
control.toggle(b as unknown as HTMLElement);
assert.equal(a.style.getPropertyValue('height'), '99px');
assert.equal(a.style.getPropertyPriority('height'), 'important');
assert.equal(a.style.getPropertyValue('position'), '');
assert(!a.classList.values.has('energy-visual-expanded'));
assert.equal(control.activeTarget, b);
assert.equal(doc.listeners.get('keydown')?.size, 1);
assert.equal(win.listeners.get('resize')?.size, 1);
let prevented = false; let stopped = false;
doc.dispatchEvent({type: 'keydown', key: 'Escape', preventDefault: () => { prevented = true; },
  stopImmediatePropagation: () => { stopped = true; }} as any);
assert(prevented && stopped);
assert.equal(control.activeTarget, null);
assert.equal(b.style.getPropertyValue('position'), '');
assert.equal(doc.listeners.get('keydown')?.size, 0);
assert.equal(win.listeners.get('resize')?.size, 0);
workspaceAvailable = false;
control.toggle(a as unknown as HTMLElement);
assert.equal(control.activeTarget, null);
assert.equal(a.style.getPropertyValue('height'), '99px');
workspaceAvailable = true;
control.toggle(a as unknown as HTMLElement);
control.ngOnDestroy();
assert.equal(a.style.getPropertyValue('height'), '99px');
assert.equal(frames.size, 0);
assert(disconnected >= 3);
for (const [key, descriptor] of originals) {
  if (descriptor) { Object.defineProperty(globalThis, key, descriptor); }
  else { delete (globalThis as any)[key]; }
}
console.log('进入/切换/缩放/退出、工作区观察与监听清理断言通过');
