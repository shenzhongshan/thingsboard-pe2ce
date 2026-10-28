/**
 * 文件职责：注册告警模块自定义 JPA Repository。
 *
 * <p>ThingsBoard 原生 JPA 配置只扫描 dao 模块，本配置仅增加 application 模块的
 * 告警统计 Repository，不修改原生 JpaDaoConfig 或原生 Repository。</p>
 */
package org.thingsboard.server.service.alarms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;

@Configuration
@EnableJpaRepositories(
        value = "org.thingsboard.server.service.alarms.repository",
        bootstrapMode = BootstrapMode.LAZY)
public class AlarmJpaRepositoryConfig {
}
