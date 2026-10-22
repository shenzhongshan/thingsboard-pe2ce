# 报告模块对客户用户开放的改造方案

- 日期：2026-09-11
- 关联：报表引擎（CE 移植版 PE Reports）、智能防雷定制页面体系

## 1. 背景与结论

仓库已内置完整报表引擎（模板 + 同步生成 + 下载 + 定时），但当前**只对租户管理员开放**：

- `ReportController` 全部接口 `@PreAuthorize("hasAuthority('TENANT_ADMIN')")`；
- 前端 `/reporting/*` 路由全部 `auth: [TENANT_ADMIN]`；
- 客户用户权限表（`CustomerUserPermissions`）未注册 `REPORT` / `REPORT_TEMPLATE` 资源（未注册 = 默认全拒绝）。

**结论：放开给客户用户（CUSTOMER_USER）。** 定位为纯权限层改造——引擎、数据权限过滤、下载链路均已就绪，无需动表结构、无需新增接口。

两个已核实的前提（方案成立的基础）：

1. **数据权限天然隔离**：交互式生成时引擎从 `SecurityContextHolder` 取当前用户，所有实体/告警/遥测查询按该用户权限重放（`DefaultTbReportRenderService.java:89-92` + `InProcessReportDataService.java:191-201`）。客户用户生成的报告只含其可见设备，零额外开发。
2. **权限粒度可控**：`CustomerUserPermissions` 是显式注册制，注册 `READ` 就只读，注册 `WRITE` 才能生成。

### 权限姿态

| 能力 | 租户管理员 | 客户用户 |
| --- | --- | --- |
| 报告模板：查看/选择 | ✅ | ✅（只 READ） |
| 报告模板：创建/修改/删除 | ✅ | ❌ |
| 生成报告 | ✅ | ✅（数据按其权限过滤） |
| 报告列表 | 全租户 | 仅自己生成的 |
| 下载报告 | ✅ | 仅自己的 |
| 删除报告 | ✅ | 仅自己的 |
| 定时计划（report-schedule） | ✅ | ❌（本期不做） |

## 2. 后端改造

### 2.1 注册客户用户权限检查器

文件：`application/src/main/java/org/thingsboard/server/service/security/permission/CustomerUserPermissions.java`

在构造器中（`put(Resource.PROFILE, ...)` 之前）追加两个注册：

```java
// 报告模板：客户用户只读（选模板生成报告，不能增删改模板）
put(Resource.REPORT_TEMPLATE, new PermissionChecker.GenericPermissionChecker(Operation.READ));

// 报告：本客户的报告可读；写/删仅限自己生成的
put(Resource.REPORT, customerReportPermissionChecker);
```

新增检查器（参照同文件 `customerAlarmPermissionChecker` 的模式）：

```java
private final PermissionChecker customerReportPermissionChecker = new PermissionChecker() {
    @Override
    public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
        if (!user.getTenantId().equals(entity.getTenantId())) {
            return false;
        }
        if (entity instanceof Report report) {
            if (user.getCustomerId().equals(report.getCustomerId())) {
                return operation == Operation.READ || user.getId().equals(report.getUserId());
            }
        }
        return false;
    }
};
```

依据：`Report` 实现 `HasCustomerId`，生成时写入 `customerId` 与 `userId`（当前登录用户），两个字段足以完成"本客户可读、本人可写删"的判定，不动表。

### 2.2 ReportController 放开注解并补客户用户语义

文件：`application/src/main/java/org/thingsboard/server/controller/ReportController.java`

**2.2.1 五个接口注解放开**（`getReportById` / `downloadReport` / `getReportInfos` / `generateReport` / `deleteReport`）：

```java
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
```

`ReportTemplateController` 只放开两个只读接口（`getReportTemplateById` / `getReportTemplateInfos`），`saveReportTemplate` / `deleteReportTemplate` 保持 `TENANT_ADMIN`。

**2.2.2 `generateReport` 修两处**：

```java
// ① customerId：客户用户生成时记为其所属客户（模板的 customerId 通常为空）
if (getCurrentUser().isCustomerUser()) {
    report.setCustomerId(getCurrentUser().getCustomerId());
} else {
    report.setCustomerId(template.getCustomerId());
}

// ② userId 已有：report.setUserId(getCurrentUser().getId()) 保持不变
```

**2.2.3 `getReportInfos` 强制客户用户只见自己的报告**：

```java
// 客户用户忽略前端传入的 userId 参数，强制按本人过滤
UserId userIdFilter;
if (getCurrentUser().isCustomerUser()) {
    userIdFilter = getCurrentUser().getId();
} else {
    userIdFilter = userId != null ? new UserId(toUUID(userId)) : null;
}
```

**2.2.4 `checkAccess` 两个私有方法补客户范围校验**（防横向越权，`Report` / `ReportTemplate` 各一份）：

```java
private Report checkAccess(Report report) throws ThingsboardException {
    if (!getCurrentUser().getTenantId().equals(report.getTenantId())) {
        throw new ThingsboardException(...PERMISSION_DENIED);
    }
    // 新增：客户用户只能触达本客户的报告
    if (getCurrentUser().isCustomerUser()
            && !getCurrentUser().getCustomerId().equals(report.getCustomerId())) {
        throw new ThingsboardException(...PERMISSION_DENIED);
    }
    return report;
}
```

### 2.3 报告存储与下载（无需改动）

- 报告二进制随 `reportService.createReport(tenantId, report, data)` 持久化，下载走 `getReportData`，均为租户内寻址，配合 2.2.4 的范围校验即闭环。

## 3. 前端改造

目录：`ui-ngx/src/app/modules/home/pages/reporting/`

### 3.1 路由放开（reporting-routing.module.ts）

| 路由 | 现状 | 改为 |
| --- | --- | --- |
| `/reporting/reports` | `TENANT_ADMIN` | `[TENANT_ADMIN, CUSTOMER_USER]` |
| `/reporting/templates` | `TENANT_ADMIN` | 保持不变 |
| `/reporting/schedule` | `TENANT_ADMIN` | 保持不变 |
| `/reporting`（默认重定向） | 固定跳 `/reporting/templates` | 按角色：客户用户跳 `/reporting/reports` |

### 3.2 菜单入口

- 检查菜单注册处（`MenuId.reports` 在 `menu.models.ts` / 菜单 section 的 authority 数组）加入 `CUSTOMER_USER`；
- 仅暴露"报告"菜单项，"模板""定时"不进客户用户菜单；
- 若走后端自定义菜单系统下发（防雷页面同款机制），在菜单配置侧同步加项。

### 3.3 页面微调（reports.component）

- 客户用户视角隐藏纯管理元素（如有"按用户筛选"下拉，客户用户场景下隐藏——后端已强制本人过滤，展示无意义）；
- 模板选择来自 `generate-report-dialog`，无需改动（接口已放开 READ）。

### 3.4 国际化

- `report.reports` 等文案键已存在（页面已内置），确认 `zh_CN` locale 有对应翻译即可。

## 4. 报告模板准备

模板由租户管理员用现有编辑器（`/reporting/templates`）创建，无需开发。注意事项：

1. **数据源查询条件要"宽"，权限来收窄**：模板内的 `EntityDataQuery` / `AlarmDataQuery` 会按生成用户权限重放，因此查询条件写业务维度（如设备属性 `productCategory = lightning`），不要写死客户，让权限层自动裁剪客户范围；
2. **模板命名面向用户**：客户用户会直接看到模板名（如「月度告警统计报告」「设备运行周报」）；
3. **PDF 格式**：图表 + 汇总表组合；需要纯数据清单时用 CSV 表格组件。

## 5. 实施步骤清单

1. `CustomerUserPermissions` 注册 `REPORT_TEMPLATE`（READ）与 `REPORT`（本客户可读/本人可写删）
2. `ReportController` 五接口注解放开 + `generateReport` customerId 修正 + `getReportInfos` 强制本人过滤 + `checkAccess` 客户范围校验
3. `ReportTemplateController` 两个只读接口放开
4. 前端路由/菜单放开（仅 reports 页）
5. 管理员配置 2-3 套防雷报告模板
6. 浏览器验证 + 权限回归测试

预计工作量：0.5～1 天（含测试）。

## 6. 验证清单

**功能验证（客户用户登录）**

- [ ] 侧边菜单出现"报告"入口，无"模板/定时"入口
- [ ] 报告页可选模板、生成成功、列表出现记录
- [ ] 下载的 PDF/CSV 内容正确、中文列名正常
- [ ] 只能看到自己生成的报告（管理员同租户生成的报告不可见）
- [ ] 可删除自己的报告

**越权验证（重点）**

- [ ] 客户用户 A 直接调 `/api/report/{B的reportId}/download` → 403
- [ ] 客户用户 A 尝试删除客户用户 B 的报告 → 403
- [ ] 客户用户 A 调 `POST /api/reportTemplate`（建模板）→ 403
- [ ] `getReportInfos` 传入他人 userId 参数 → 仍只返回本人报告
- [ ] 客户用户生成的报告只含其客户名下设备（对比管理员生成的同模板报告）

**管理员回归**

- [ ] 模板 CRUD 正常
- [ ] 定时计划不受影响（调度器生成的报告 user_id 正确落库）
- [ ] 报告列表可按 userId 筛选查看全部用户的报告

## 7. 边界与后续

- **本期不做**：客户用户的定时报告（report-schedule 仍管理员专属）；引擎侧已支持（`InProcessReportDataService` 注释表明定时任务按用户权限过滤已就绪），后续放开只需同样三步（注解/权限表/菜单）。
- **报告可见范围的可选演进**：当前"仅本人"最简单；若之后要"同客户共享报告"，把 `getReportInfos` 的客户用户过滤从 `userId` 放宽为 `customerId` 即可（权限检查器已按 customerId 判 READ）。
- **管理入口**：若不想让客户用户看到原生 reporting 页面，也可在防雷自有页面（如数据监测）加"生成报告"按钮直调 `POST /api/report/{templateId}/generate`，后端改动完全一致。
