import assert from 'node:assert/strict';
import {getEnergyFullscreenBounds} from './energy-visual-fullscreen.data';

// 放大尺寸必须来自应用内容区，不能写死侧栏和顶部高度。
assert.deepEqual(getEnergyFullscreenBounds({top: 90, left: 210, right: 1920, bottom: 1080}, 1920, 1080),
  {top: 90, left: 210, width: 1710, height: 990});
assert.deepEqual(getEnergyFullscreenBounds({top: -20, left: -10, right: 1500, bottom: 1000}, 1200, 800),
  {top: 0, left: 0, width: 1200, height: 800});
assert.equal(getEnergyFullscreenBounds({top: 900, left: 100, right: 1200, bottom: 1000}, 1200, 800), null);
assert.equal(getEnergyFullscreenBounds({top: 50, left: 100, right: 100, bottom: 800}, 1200, 800), null);
console.log('4项工作区放大边界用例通过');
