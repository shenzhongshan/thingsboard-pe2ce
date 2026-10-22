// 在测试 ThingsBoard 已登录页面的 Console 执行，创建菜单后保存配置，不分配用户。
(async () => {
  const token = localStorage.getItem('jwt_token');
  if (!token) throw new Error('请先使用测试环境租户管理员登录。');

  // 文件选择器只读取用户选择的导出文件，所有 API 使用当前页面地址。
  const file = await new Promise(resolve => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json,application/json';
    input.hidden = true;
    document.body.appendChild(input);
    input.addEventListener('change', () => {
      const selected = input.files?.[0] || null;
      input.remove();
      resolve(selected);
    }, { once: true });
    input.addEventListener('cancel', () => {
      input.remove();
      resolve(null);
    }, { once: true });
    input.click();
  });
  if (!file) return;
  const payload = JSON.parse(await file.text());
  if (payload.format !== 'thingsboard-custom-menu-transfer' || payload.formatVersion !== 1 ||
      payload.name !== 'test' || payload.scope !== 'TENANT' ||
      !payload.config || !Array.isArray(payload.config.items)) {
    throw new Error('文件不是本脚本导出的 test 租户菜单。');
  }
  if (payload.sourceOrigin === location.origin) {
    throw new Error('当前地址与导出来源相同，请切换到测试环境再导入。');
  }

  async function api(path, method = 'GET', body) {
    const response = await fetch(path, {
      method,
      headers: {
        'X-Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      cache: 'no-store',
      body: body === undefined ? undefined : JSON.stringify(body)
    });
    if (!response.ok) throw new Error(`API 失败：HTTP ${response.status}，${method} ${path}`);
    return response.json();
  }

  // 遍历全部分页，目标存在同名菜单时不覆盖、不删除、不改变原分配。
  for (let page = 0; ; page++) {
    const result = await api(`/api/customMenu/infos?pageSize=100&page=${page}&scope=TENANT`);
    if (!Array.isArray(result.data) || typeof result.hasNext !== 'boolean') {
      throw new Error('目标菜单分页结构不符合当前接口，请检查服务版本。');
    }
    if (result.data.some(menu => menu.name === payload.name)) {
      throw new Error('目标已有名为 test 的菜单，请先在管理页面核对，脚本不会覆盖。');
    }
    if (!result.hasNext) break;
  }

  // 显示真实目标地址，防止在生产页面误执行。
  if (!confirm(`向 ${location.origin} 创建 test 菜单？\n创建后为“未分配”。请确认这是测试环境。`)) return;
  const created = await api('/api/customMenu?force=false', 'POST', {
    name: payload.name,
    scope: 'TENANT',
    assigneeType: 'NO_ASSIGN'
  });
  const id = created.id?.id;
  if (!id) throw new Error('创建响应缺少菜单 ID，请到管理页面核对，勿立即重复导入。');
  console.info(`已创建未分配菜单，ID：${id}；正在写入菜单内容。`);
  try {
    await api(`/api/customMenu/${encodeURIComponent(id)}/config`, 'PUT', payload.config);
    const saved = await api(`/api/customMenu/${encodeURIComponent(id)}/config`);
    // 忽略对象键顺序，验证整个菜单配置是否保持一致。
    const canonical = value => {
      if (Array.isArray(value)) return value.map(canonical);
      if (value && typeof value === 'object') {
        return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
      }
      return value;
    };
    if (JSON.stringify(canonical(saved)) !== JSON.stringify(canonical(payload.config))) {
      throw new Error('回读配置与导出文件不同，请在页面核对菜单内容。');
    }
    console.info('test 菜单已导入并回读校验通过。刷新自定义菜单列表，通过齿轮按钮分配测试用户。');
  } catch (error) {
    throw new Error(`菜单 ${id} 已创建，但配置保存或校验失败：${error.message}。请检查该菜单，勿直接重复导入。`);
  }
})().catch(error => console.error('菜单导入失败：', error.message));
