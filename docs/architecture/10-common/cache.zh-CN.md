# Common Cache（`common/cache`）

**本文是权威版。** 英文版 [cache.md](cache.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:cache`

`common/cache` 模块提供双后端的缓存抽象层：Caffeine（进程内）与 Redis/Valkey（分布式）。它包含
按实体的缓存实现、事务型缓存语义、用于保证一致性的带版本缓存条目，以及限流服务。

## 架构

### 双后端设计

模块支持两种缓存后端，由配置选择：

| 后端 | 适用场景 | 实现 |
|---------|----------|---------------|
| Caffeine | 单节点、低延迟 | `CaffeineTbTransactionalCache`、`VersionedCaffeineTbCache` |
| Redis | 多节点集群、共享缓存 | `RedisTbTransactionalCache`、`VersionedRedisTbCache` |

Caffeine 是默认。应用配置里 `cache.type=redis` 时启用 Redis。Redis 操作被包在事务里
（`RedisTbCacheTransaction` / `CaffeineTbCacheTransaction`）。

### 核心缓存接口

| 接口/类 | 用途 |
|----------------|---------|
| `TbTransactionalCache<K,V>` | 带事务语义的键值存储（get、put、evict） |
| `VersionedTbCache<K,V>` | 带版本跟踪的缓存，用于检测陈旧条目 |
| `TbCacheTransaction<K,V>` | 可提交/可回滚的缓存事务 |
| `TbCacheValueWrapper<V>` | 类 Optional 的缓存值包装（用于区分"值为 null"与"不存在"） |
| `CacheSpecs` | 按实体的缓存规格（TTL、最大条数等） |
| `CacheSpecsMap` | 以缓存名为键的缓存规格注册表 |

### Redis 配置

Redis 连接模式通过专门的配置类支持：

| 类 | 模式 |
|-------|------|
| `TBRedisStandaloneConfiguration` | 单 Redis 实例 |
| `TBRedisClusterConfiguration` | Redis Cluster |
| `TBRedisSentinelConfiguration` | Redis Sentinel（高可用） |
| `TBRedisCacheConfiguration` | Redis 基础配置（SSL、连接池设置） |

Redis 的 SSL/TLS 经 `RedisSslCredentials` 支持。

### Redis 序列化

两类序列化策略在 Java 对象与 Redis 字节数组之间转换：

| 序列化器 | 格式 | 适用场景 |
|-----------|--------|----------|
| `TbJavaRedisSerializer` | Java 序列化 | 内部 Java 对象 |
| `TbJsonRedisSerializer` | JSON（Jackson） | POJO、DTO |
| `TbTypedJsonRedisSerializer<T>` | 带类型的 JSON | 携带 Class<T> 的类型安全 JSON 反序列化 |

### 带版本的缓存

`VersionedTbCache` 在基础缓存之上加了基于版本的一致性。每个缓存条目都有对应的版本。当从缓存取到的
版本比期望版本旧时，该条目被视为陈旧，调用方重新从数据库取。这避免了分布式场景下缓存失效的竞态。

```java
public interface VersionedTbCache<K, V> {
    CacheValueWrapper<V> get(K key);
    void put(K key, V value);
    void evict(K key);
    // Version tracking handled internally
}
```

## 按实体的缓存实现

每个主要实体类型都有专属的缓存实现，提供类型安全的缓存键，以及用于集群广播的失效事件：

| 实体 | Caffeine 实现 | Redis 实现 | 键类 |
|--------|-------------|------------|-----------|
| Device | `DeviceCaffeineCache` | `DeviceRedisCache` | `DeviceCacheKey` |
| Customer | `CustomerCaffeineCache` | `CustomerRedisCache` | `CustomerCacheKey` |
| User | `UserCaffeineCache` | `UserRedisCache` | `UserCacheKey` |
| Edge | `EdgeCaffeineCache` | `EdgeRedisCache` | `EdgeCacheKey` |
| Related Edges | `RelatedEdgesCaffeineCache` | `RelatedEdgesRedisCache` | `RelatedEdgesCacheKey` |
| OTA Package | `CaffeineOtaPackageCache` | `RedisOtaPackageDataCache` | （内置） |
| Resource Info | `ResourceInfoCaffeineCache` | `ResourceInfoRedisCache` | `ResourceInfoCacheKey` |
| Users Update Time | `UsersSessionInvalidationCaffeineCache` | `UsersSessionInvalidationRedisCache` | （内部） |

每个实体缓存都有对应的 `EvictEvent`，在实体被更新或删除时经队列系统在集群范围广播。

### 缓存失效事件

| 事件 | 触发条件 |
|-------|---------|
| `DeviceCacheEvictEvent` | 设备更新/删除 |
| `CustomerCacheEvictEvent` | 客户更新/删除 |
| `UserCacheEvictEvent` | 用户更新/删除 |
| `EdgeCacheEvictEvent` | Edge 更新/删除 |
| `RelatedEdgesEvictEvent` | Edge 分配关系变化 |
| `ResourceInfoEvictEvent` | 资源更新/删除 |

## 限流

`limits/` 子包提供限流支持：

| 类 | 用途 |
|-------|---------|
| `RateLimitService` | 限流检查接口 |
| `DefaultRateLimitService` | 基于租户配置文件限额的实现 |
| `TenantProfileProvider` | 为限额查询提供租户配置文件 |

## 缓存键模式

缓存键是复合对象（如 `DeviceCacheKey` 包装 `DeviceId`）。键实现 `equals()`/`hashCode()` 以保证
缓存查找正确。带版本的缓存键在此基础上加了版本分量（`VersionedCacheKey`）。

## 依赖关系

- **依赖：** `common/data`（实体 ID、模型）、Caffeine、Spring Data Redis
- **被谁依赖：** `dao`、`application`（需要缓存的 service）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `cache`
