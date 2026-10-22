// 在本地 ThingsBoard 已登录页面的 Console 执行，只读取菜单并下载 JSON。
(async () => {
  const menuName = 'test';
  const token = localStorage.getItem('jwt_token');
  if (!token) throw new Error('请先使用租户管理员登录本地 ThingsBoard。');

  // 使用当前页面的同源 API，不输出令牌或复制用户分配 ID。
  async function get(path) {
    const response = await fetch(path, {
      headers: { 'X-Authorization': `Bearer ${token}` },
      cache: 'no-store'
    });
    if (!response.ok) throw new Error(`读取失败：HTTP ${response.status}，${path}`);
    return response.json();
  }

  const matches = [];
  for (let page = 0; ; page++) {
    const result = await get(`/api/customMenu/infos?pageSize=100&page=${page}&scope=TENANT`);
    if (!Array.isArray(result.data) || typeof result.hasNext !== 'boolean') {
      throw new Error('菜单分页返回结构不符合当前接口，请检查服务版本。');
    }
    matches.push(...result.data.filter(menu => menu.name === menuName));
    if (!result.hasNext) break;
  }
  if (matches.length !== 1) {
    throw new Error(`名称精确等于 ${menuName} 的菜单有 ${matches.length} 条，必须唯一才能导出。`);
  }
  const menu = matches[0];
  if (!menu.id?.id) throw new Error('菜单缺少 ID，停止导出。');
  const config = await get(`/api/customMenu/${encodeURIComponent(menu.id.id)}/config`);
  if (!Array.isArray(config.items)) throw new Error('菜单配置缺少 items 数组。');
  const payload = {
    format: 'thingsboard-custom-menu-transfer',
    formatVersion: 1,
    exportedAt: new Date().toISOString(),
    sourceOrigin: location.origin,
    name: menuName,
    scope: 'TENANT',
    config
  };
  const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], {
    type: 'application/json;charset=utf-8'
  }));
  const link = document.createElement('a');
  link.href = url;
  link.download = 'thingsboard-custom-menu-test.json';
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 60000);
  console.info(`已导出 ${menuName}，顶层菜单项 ${config.items.length} 个。未导出登录凭据和用户分配。`);
})().catch(error => console.error('菜单导出失败：', error.message));
