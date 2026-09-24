/**
 * 文件职责：设备日志页面的剪贴板适配工具。
 *
 * <p>优先使用浏览器异步剪贴板 API；在非安全上下文或旧浏览器中，回退到临时
 * textarea，保证自定义日志页的复制操作与原生页面一样可以使用。</p>
 */
export function copyTextToClipboard(value: string): Promise<boolean> {
  const text = value ?? '';
  if (!text) {
    return Promise.resolve(false);
  }
  if (navigator.clipboard?.writeText) {
    return navigator.clipboard.writeText(text)
      .then(() => true)
      .catch(() => Promise.resolve(copyWithLegacyApi(text)));
  }
  return Promise.resolve(copyWithLegacyApi(text));
}

/** 使用 execCommand 作为剪贴板 API 不可用时的兼容回退。 */
function copyWithLegacyApi(text: string): boolean {
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand('copy');
  document.body.removeChild(textarea);
  return copied;
}
