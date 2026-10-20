/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.cache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link RedisTbTransactionalCache#evictByPrefix(String)} 的真实执行验证 —— 实际在
 * Redis 上跑那段 Lua（SCAN + DEL）。
 *
 * <p><b>需要外部 Redis，默认跳过。</b>用法：
 * <pre>mvn test -pl common/cache -Dtest=RedisEvictByPrefixTest \
 *     -Dtest.redis.host=localhost -Dtest.redis.port=6379</pre>
 *
 * <p>为什么不用进程内 Redis：现成可离线获得的 {@code it.ozimov:embedded-redis} 自带的是
 * Redis 2.8.19，而这段脚本在 Redis 5 之前会被拒绝执行 —— 那时脚本按原文复制到从库，
 * 禁止在非确定性命令（SCAN）之后执行写命令（{@code DEL}），报
 * "Write commands not allowed after non deterministic commands"；2.8 连
 * {@code redis.replicate_commands()} 这个变通手段都还没有（3.2 才引入）。
 * 本项目的缓存后端是 Valkey 8（见 docker/docker-compose.valkey-*.yml），不受此限，
 * 所以脚本保持与 PE 逐字一致，未为不支持的版本改动它。
 *
 * <p>测试不经过 put/get，而是用原始命令直接写 raw key：被测的就是
 * "raw key 前缀拼接 + 连接路由 + Lua 前缀匹配删除"这三件事，值序列化与之无关。
 */
class RedisEvictByPrefixTest {

    private static final String CACHE_NAME = "relatedEdges";

    private static JedisConnectionFactory connectionFactory;

    private TestRedisCache cache;

    static class TestRedisCache extends RedisTbTransactionalCache<TestKey, String> {
        TestRedisCache(CacheSpecsMap specs, JedisConnectionFactory cf, TBRedisCacheConfiguration cfg) {
            super(CACHE_NAME, specs, cf, cfg, new TbRedisSerializer<>() {
                @Override
                public byte[] serialize(String value) {
                    return value.getBytes();
                }

                @Override
                public String deserialize(TestKey key, byte[] bytes) {
                    return new String(bytes);
                }
            });
        }
    }

    /** 与 RelatedEdgesCacheKey 同构：{} 是 Redis hash tag，使同租户键同 slot。 */
    record TestKey(String tenant, String entity) implements Serializable {
        @Override
        public String toString() {
            return "{" + tenant + "}" + entity;
        }
    }

    @BeforeAll
    static void connect() {
        String host = System.getProperty("test.redis.host");
        assumeTrue(host != null && !host.isBlank(),
                "未提供 -Dtest.redis.host，跳过 Redis 实跑验证");
        int port = Integer.parseInt(System.getProperty("test.redis.port", "6379"));
        connectionFactory = new JedisConnectionFactory(new RedisStandaloneConfiguration(host, port));
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        CacheSpecsMap specs = new CacheSpecsMap();
        CacheSpecs spec = new CacheSpecs();
        spec.setTimeToLiveInMinutes(60);
        // maxSize 必须 > 0：否则 cacheEnabled=false，evictByPrefix 会直接 return，
        // 用例会表现为"键没被删"而失败（不会假通过）。
        spec.setMaxSize(10_000);
        specs.setSpecs(Map.of(CACHE_NAME, spec));

        TBRedisCacheConfiguration cfg = new TBRedisStandaloneConfiguration();
        cfg.setEvictTtlInMs(60_000);

        cache = new TestRedisCache(specs, connectionFactory, cfg);
        clearTestKeys();
    }

    /** 只清本测试写入的键，不 flushAll —— 外部 Redis 可能还有别的数据。 */
    private void clearTestKeys() {
        for (String t : new String[]{"t1", "t2", "tbig", "tkeep", "nosuchtenant"}) {
            cache.evictByPrefix("{" + t + "}");
        }
    }

    private void seed(TestKey key, String value) {
        try (RedisConnection c = connectionFactory.getConnection()) {
            c.stringCommands().set(StringRedisSerializer.UTF_8.serialize(CACHE_NAME + key),
                    value.getBytes());
        }
    }

    private boolean exists(TestKey key) {
        try (RedisConnection c = connectionFactory.getConnection()) {
            return Boolean.TRUE.equals(c.keyCommands()
                    .exists(StringRedisSerializer.UTF_8.serialize(CACHE_NAME + key)));
        }
    }

    @Test
    void evictByPrefix_removesOnlyMatchingTenant() {
        TestKey t1a = new TestKey("t1", "a");
        TestKey t1b = new TestKey("t1", "b");
        TestKey t2a = new TestKey("t2", "a");
        seed(t1a, "v1");
        seed(t1b, "v2");
        seed(t2a, "v3");

        cache.evictByPrefix("{t1}");

        assertThat(exists(t1a)).as("t1 的键应被删除").isFalse();
        assertThat(exists(t1b)).as("t1 的键应被删除").isFalse();
        assertThat(exists(t2a)).as("其它租户的键不应被删除（清过头的对照）").isTrue();
    }

    @Test
    void evictByPrefix_nonMatchingPrefixDeletesNothing() {
        TestKey t1a = new TestKey("t1", "a");
        seed(t1a, "v1");

        cache.evictByPrefix("{nosuchtenant}");

        assertThat(exists(t1a)).as("前缀不命中时不应删除任何键").isTrue();
    }

    /**
     * 区分前缀与子串：{@code {t2}{t1}x} 的 raw key 含 "{t1}" 但不以 "cacheName{t1}" 开头。
     * Lua 用的是 {@code MATCH prefix.."*"}，即前缀语义。
     */
    @Test
    void evictByPrefix_matchesPrefixNotSubstring() {
        TestKey target = new TestKey("t1", "a");
        TestKey lookalike = new TestKey("t2", "{t1}x");
        seed(target, "v1");
        seed(lookalike, "v2");

        cache.evictByPrefix("{t1}");

        assertThat(exists(target)).as("以前缀开头的键应被删除").isFalse();
        assertThat(exists(lookalike)).as("仅在中间包含前缀的键不应被删除").isTrue();
    }

    /**
     * 跨越多个 SCAN 批次（COUNT=1000）仍要删全 —— 脚本里的 repeat/until cursor=="0"
     * 若写成只扫一轮，这条会留下残余。
     */
    @Test
    void evictByPrefix_deletesAcrossScanBatches() {
        int n = 2500;
        for (int i = 0; i < n; i++) {
            seed(new TestKey("tbig", "e" + i), "v");
        }
        seed(new TestKey("tkeep", "x"), "v");

        cache.evictByPrefix("{tbig}");

        assertThat(exists(new TestKey("tbig", "e0"))).isFalse();
        assertThat(exists(new TestKey("tbig", "e" + (n - 1)))).isFalse();
        assertThat(exists(new TestKey("tkeep", "x"))).as("其它租户不受影响").isTrue();
    }
}
