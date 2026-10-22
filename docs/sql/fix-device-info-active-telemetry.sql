-- 用途：修复state.persistToTelemetry=true，但原生设备列表仍读取属性active的已有部署库。
-- 前提：在目标环境确认该配置为true，且数据库已有public.device_info_active_ts_view。
-- 该脚本需在目标数据库执行一次；仅更新源码、替换程序包或重启不会自动执行本脚本。
-- 不重跑schema-views.sql，不DROP/CASCADE，不修改设备、属性或遥测数据，可重复执行。
BEGIN;

CREATE OR REPLACE VIEW public.device_info_view AS
SELECT * FROM public.device_info_active_ts_view;

-- 返回修复后的定义，来源应为device_info_active_ts_view。
SELECT pg_get_viewdef('public.device_info_view'::regclass, true) AS device_info_view_definition;

COMMIT;
