# Smart Lightning Shared Asset Tree Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the repeated smart-lightning asset tree into one reusable Angular component without changing page-specific data behavior.

**Architecture:** Add a module-based `tb-smart-lightning-asset-tree` component under the smart-lightning feature's shared directory. It owns tree rendering, search, expand/collapse, keyboard interaction, and visual state classes; parent pages retain API loading, root filtering, selected IDs, alarm IDs, and detail-page locking.

**Tech Stack:** Angular 20 module components, TypeScript, SCSS, RxJS, existing ThingsBoard smart-lightning tree models.

**Spec:** `docs/superpowers/specs/2026-09-08-smart-lightning-shared-asset-tree.md`

## Global Constraints

- Do not change Maven files or run Maven commands.
- Reuse `SmartLightningMonitoringAssetTree` and existing tree helper functions.
- Keep the existing root-asset filter and page-specific selection semantics.
- Unselected nodes have no visible marker; selected normal nodes use a 12px green marker; selected alarm devices use a red marker.

---

### Task 1: Create the shared asset-tree component

**Files:**
- Create: `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/asset-tree/smart-lightning-asset-tree.component.ts`
- Create: `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/asset-tree/smart-lightning-asset-tree.component.html`
- Create: `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/asset-tree/smart-lightning-asset-tree.component.scss`
- Modify: `ui-ngx/src/app/modules/home/home-pages.module.ts`

**Interfaces:**
- Consumes the tree model and inputs defined in the spec.
- Produces `searchChange`, `selectNode`, and `toggleNode` events for page containers.

- [ ] **Step 1: Add the component class with typed inputs and outputs.**
- [ ] **Step 2: Render loading, error, empty, search, and tree states.**
- [ ] **Step 3: Add keyboard-safe expand and select interactions.**
- [ ] **Step 4: Add shared styles for marker-only selection and alarm markers.**
- [ ] **Step 5: Declare the component in `HomePagesModule`.**

### Task 2: Migrate monitoring page

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring.component.html`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring.component.scss`

- [ ] **Step 1: Replace the inline asset tree markup with the shared component.**
- [ ] **Step 2: Bind root ID, selected IDs, expanded IDs, alarm IDs, loading, and search.**
- [ ] **Step 3: Forward component events to existing page methods.**
- [ ] **Step 4: Remove page-local tree-only markup and obsolete tree styles.**

### Task 3: Migrate query, alarm list, and detail pages

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/query/smart-lightning-data-query.component.html`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/query/smart-lightning-data-query.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/deviceAlarms/list/smart-lightning-alarm-list.component.html`
- Modify: `ui-ngx/src/app/modules/home/pages/deviceAlarms/list/smart-lightning-alarm-list.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-device-detail.component.html`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-device-detail.component.ts`

- [ ] **Step 1: Replace each inline tree with the shared component.**
- [ ] **Step 2: Bind each page's existing root, selection, expansion, search, alarm, and lock state.**
- [ ] **Step 3: Forward events without changing existing query or alarm filtering.**
- [ ] **Step 4: Remove duplicated tree styles from the three page stylesheets.**

### Task 4: Verify the refactor

**Files:**
- Test: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data.spec.ts`

- [ ] **Step 1: Run the existing smart-lightning data test with `ts-node` and path mapping.**
- [ ] **Step 2: Run TypeScript static checking and isolate pre-existing dependency errors from changed-file errors.**
- [ ] **Step 3: Run `git diff --check` for all changed files.**
- [ ] **Step 4: Confirm no Maven command was run.**
