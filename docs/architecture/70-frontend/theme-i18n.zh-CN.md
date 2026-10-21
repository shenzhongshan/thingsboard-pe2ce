# 主题与 i18n

**本文是权威版。** 英文版 [theme-i18n.md](theme-i18n.md) 为对照上游 CE 的参照副本，改动请先落在本文。

ThingsBoard 前端经一套完整的国际化（i18n）体系支持 28 种语言，并提供建立在 Angular Material 之上、
集成了 Tailwind CSS 的主题架构。

## 国际化（i18n）

### 支持的语言（28 种）

所有语言文件位于 `src/assets/locale/`，命名为 `locale.constant-<locale>.json`：

| 语言 | 代码 | 文件 |
|--------|------|------|
| 阿拉伯语（阿联酋） | ar_AE | `locale.constant-ar_AE.json` |
| 加泰罗尼亚语（西班牙） | ca_ES | `locale.constant-ca_ES.json` |
| 捷克语 | cs_CZ | `locale.constant-cs_CZ.json` |
| 丹麦语 | da_DK | `locale.constant-da_DK.json` |
| 德语 | de_DE | `locale.constant-de_DE.json` |
| 希腊语 | el_GR | `locale.constant-el_GR.json` |
| 英语（美国） | en_US | `locale.constant-en_US.json` |
| 西班牙语 | es_ES | `locale.constant-es_ES.json` |
| 波斯语（伊朗） | fa_IR | `locale.constant-fa_IR.json` |
| 法语 | fr_FR | `locale.constant-fr_FR.json` |
| 印地语 | hi_IN | `locale.constant-hi_IN.json` |
| 意大利语 | it_IT | `locale.constant-it_IT.json` |
| 日语 | ja_JP | `locale.constant-ja_JP.json` |
| 格鲁吉亚语 | ka_GE | `locale.constant-ka_GE.json` |
| 韩语 | ko_KR | `locale.constant-ko_KR.json` |
| 立陶宛语 | lt_LT | `locale.constant-lt_LT.json` |
| 拉脱维亚语 | lv_LV | `locale.constant-lv_LV.json` |
| 荷兰语（比利时） | nl_BE | `locale.constant-nl_BE.json` |
| 荷兰语（荷兰） | nl_NL | `locale.constant-nl_NL.json` |
| 挪威语 | no_NO | `locale.constant-no_NO.json` |
| 波兰语 | pl_PL | `locale.constant-pl_PL.json` |
| 葡萄牙语（巴西） | pt_BR | `locale.constant-pt_BR.json` |
| 罗马尼亚语 | ro_RO | `locale.constant-ro_RO.json` |
| 斯洛文尼亚语 | sl_SI | `locale.constant-sl_SI.json` |
| 土耳其语 | tr_TR | `locale.constant-tr_TR.json` |
| 乌克兰语 | uk_UA | `locale.constant-uk_UA.json` |
| 中文（简体） | zh_CN | `locale.constant-zh_CN.json` |
| 中文（繁体） | zh_TW | `locale.constant-zh_TW.json` |

### 翻译架构

i18n 体系使用以下关键库：

- **`@ngx-translate/core`**（~17.0.0）—— Angular 翻译框架。提供贯穿各模板使用的
  `TranslateService`、`TranslatePipe` 和 `TranslateDirective`。
- **`ngx-translate-messageformat-compiler`**（~7.2.0）—— 为复杂翻译启用 ICU MessageFormat
  （复数、性别、选择分支）。
- **`@messageformat/core`**（~3.4.0）—— ICU 消息格式化的核心库。

### Core 的翻译 service

位于 `core/translate/`：

| 文件 | 用途 |
|------|---------|
| `translate-default-loader.ts` | HTTP loader，从 `/assets/locale/locale.constant-<lang>.json` 取语言 JSON。支持缓存与回退。 |
| `translate-default-compiler.ts` | 编译 ICU MessageFormat 串以便插值。支持复数形式、性别变格和嵌套选择。 |
| `translate-default-parser.ts` | 解析带参数插值的翻译串（如 `Hello {{name}}`）。 |
| `missing-translate-handler.ts` | 处理缺失的翻译 key：记一条警告，并在开发期把 key 本身作为展示文本返回。 |

### ICU 消息格式

翻译使用 ICU 消息语法做复杂格式化：

```json
{
  "item-count": "{count, plural, =0 {No items} one {# item} other {# items}}",
  "welcome": "{gender, select, male {Welcome, Mr. {name}} female {Welcome, Ms. {name}} other {Welcome, {name}}}"
}
```

> **花括号只能用于合法的 ICU 语法，不能当普通字符写。** 上面例子里的 `{...}` 都是 ICU 结构
> （`plural`、`select`）。如果写进一对不构成合法 ICU 的字面花括号（例如想展示一段 JSON 示例），
> 编译器会在**每次渲染**该串时抛异常，页面表现为该处空白或整块不渲染。需要插入变量时用
> `{{param}}` 插值形式。改完语言文件后要读一遍浏览器控制台。

> **编辑语言 JSON 要做文本级的精准插入。** 这些文件各有上万行，用 `json.dump` 之类重写会把
> 全部行的格式都改掉。插完 key 后用 `git diff --stat` 确认改动行数是不是你预期的那几行。

### 与 NgRx 的集成

当前语言由 NgRx 的 settings 状态（`core/settings/`）管理：

- **`settings.actions.ts`** —— `ActionSettingsChangeLanguage` action 派发语言变更
- **`settings.reducer.ts`** —— 在状态中保存 `userLang` 与 `locale`
- **`settings.effects.ts`** —— 副作用：把语言持久化到 localStorage、重新加载翻译
- **`settings.selectors.ts`** —— 取当前语言的 `selectUserLang` selector

语言切换时：
1. 派发 `ActionSettingsChangeLanguage({ userLang })`
2. Effect 调用 `TranslateService.use(userLang)`
3. `TranslateDefaultLoader` 取回新的语言文件
4. 所有使用 `translate` pipe 或 directive 的模板自动更新

### 模板用法

```html
<!-- Simple translation -->
<h2 translate>device.devices</h2>
<p>{{ 'device.name' | translate }}</p>

<!-- With parameters -->
<span>{{ 'device.created-by' | translate:{user: creatorName} }}</span>

<!-- Using directive with inner HTML -->
<div [translate]="'dashboard.help-text'"></div>
```

## 主题架构

### Angular Material 主题

应用使用 Angular Material 20 的主题体系，配一套定制的 ThingsBoard 主题：

- **Material Design 3（M3）主题** —— 用 CSS 自定义属性表达颜色、排版和密度。
- **定制 TB 主题** —— 基于 Angular Material 的 `define-theme()` API，配 ThingsBoard 专属的调色板。
- **组件级主题** —— 每个组件经 CSS 自定义属性继承主题变量。

### SCSS 架构

SCSS 文件与其组件同目录放置（`.component.scss`）。全局样式含：

- **主题变量** —— 颜色 token、间距、排版层级
- **Material 覆盖** —— 对密度、形状和颜色的定制微调
- **工具类** —— 间距、flexbox、display 辅助类
- **布局** —— 侧边栏、工具栏、内容区的结构

### Tailwind CSS 集成

Tailwind CSS 3.4.x 与 Angular Material 并行集成：

- **配置** —— 项目根下的 `tailwind.config.js`
- **指令** —— 使用 `@tailwind base`、`@tailwind components`、`@tailwind utilities`
- **关闭 preflight** —— Tailwind 的 CSS reset 被关掉（`preflight: false`），以避免与 Angular
  Material 冲突
- **PostCSS** —— 用 `postcss` 与 `autoprefixer` 做 CSS 处理

任何组件模板里都能直接用 Tailwind 的主要类做快速布局与样式：

```html
<div class="flex items-center gap-2 p-4">
  <mat-icon class="text-primary">info</mat-icon>
  <span class="text-sm text-gray-600">{{ description }}</span>
</div>
```

### MDI 图标

应用经 `@mdi/svg` 使用 Material Design Icons（MDI）作为图标体系。`tb-icon` 组件渲染 MDI 图标，
支持定制颜色、尺寸和 hover 效果。Font Awesome（`font-awesome`）也仍可用以兼容旧代码。

### SCADA 符号主题

SCADA 符号用 `@svgdotjs/svg.js` 做 SVG 操作。符号可被主题化，并能引用仪表板的颜色常量实现动态改色。

### 排版

- **主字体** —— Roboto（经 `typeface-roboto`）
- **等宽字体** —— 用于代码编辑器（ACE 编辑器、JSON 编辑器）
- **字号缩放** —— 经 Angular Material 的排版层级实现响应式排版

## 代码检查

### ESLint 配置

位于 `ui-ngx/eslint.config.mjs`（ESLint 9 的 flat config 格式）：

**插件：**
- `angular-eslint`（~20.7.0）—— Angular 专属的检查规则（模板、组件 selector、生命周期钩子）
- `typescript-eslint`（~8.54.0）—— TypeScript 检查规则
- `eslint-plugin-import` —— 导入顺序与校验
- `eslint-plugin-jsdoc` —— JSDoc 注释校验
- `eslint-plugin-prefer-arrow` —— 偏好箭头函数
- `eslint-plugin-tailwindcss` —— Tailwind CSS 类名校验与排序

**关键规则：**
- Angular 模板规则：无障碍（alt 文本、label）、禁用 autofocus、禁用正数 tabindex
- TypeScript 规则：偏好 `const`、偏好可选链、禁用 `any`
- 导入规则：导入排序（先 Angular，再第三方，最后本地）
- Tailwind 规则：禁止重复类名、禁止不必要的任意值

执行：
```bash
cd ui-ngx && yarn lint
```

## 构建系统细节

### 基于 esbuild 的 Angular CLI

应用使用 Angular 基于 esbuild 的构建体系（`@angular/build`）以加快构建：

- **开发** —— `ng serve` 配 HMR（模块热替换）
- **生产** —— `ng build --configuration production`，带优化（压缩、tree-shaking、死代码消除）
- **类型生成** —— `yarn build:types` 从 protobuf 定义生成 TypeScript 接口
- **图标元数据** —— `yarn build:icon-metadata` 生成图标查找数据

### 环境配置

环境相关设置位于 `src/environments/`：

- `environment.ts` —— 开发配置
- `environment.prod.ts` —— 生产配置

## 关联

- [前端总览](README.zh-CN.md) —— 前端整体架构
- [Core 模块](core.zh-CN.md) —— Core 的翻译 service 与 NgRx settings 状态
- [Shared 模块](shared.zh-CN.md) —— 用主题体系设置样式的共享组件
- [页面模块](pages.zh-CN.md) —— 页面使用翻译与主题变量
