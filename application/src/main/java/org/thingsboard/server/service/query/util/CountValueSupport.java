/** 文件职责：跨业务模块的 long 到 int 计数边界转换。 */
package org.thingsboard.server.service.query.util;

/** 数据库 COUNT 返回 long，而页面 DTO 仍使用 int 时的统一转换工具。 */
public final class CountValueSupport {

    private CountValueSupport() {
    }

    /** 超过 int 上限时饱和，避免页面统计发生负数溢出。 */
    public static int toIntCount(long count) {
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }
}
