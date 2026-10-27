/** 工作区放大边界纯计算；不依赖侧栏宽度、工具栏高度或浏览器原生全屏。 */
export interface EnergyFullscreenBounds {top: number; left: number; width: number; height: number;}
export function getEnergyFullscreenBounds(rect: Pick<DOMRect, 'top' | 'left' | 'right' | 'bottom'>,
  viewportWidth: number, viewportHeight: number): EnergyFullscreenBounds | null {
  if (![rect.top, rect.left, rect.right, rect.bottom, viewportWidth, viewportHeight].every(Number.isFinite)) { return null; }
  const top = Math.max(0, rect.top);
  const left = Math.max(0, rect.left);
  const width = Math.min(viewportWidth, rect.right) - left;
  const height = Math.min(viewportHeight, rect.bottom) - top;
  return width > 0 && height > 0 ? {top, left, width, height} : null;
}
