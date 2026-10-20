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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Caffeine 侧 {@link TbTransactionalCache#evictByPrefix(String)} 的行为约定。
 *
 * <p>重点在"只清前缀命中的键"：如果实现里误把 cacheName 也拼进比较串（Redis 侧才需要
 * 拼），匹配会全部落空；反之若前缀比较写成 contains 而非 startsWith，又会误清。
 * 两个方向都要有用例，否则"一条也没清"和"清过头"都能蒙过单向断言。
 */
class CaffeineEvictByPrefixTest {

    private static final String CACHE_NAME = "testCache";

    /** 形如 {tenantId}entityId —— 与 RelatedEdgesCacheKey 同构：前缀即 Redis hash tag。 */
    record Key(String tenant, String entity) implements Serializable {
        @Override
        public String toString() {
            return "{" + tenant + "}" + entity;
        }
    }

    private TestCache cache;

    static class TestCache extends CaffeineTbTransactionalCache<Key, String> {
        TestCache(SimpleCacheManager mgr) {
            super(mgr, CACHE_NAME);
        }
    }

    @BeforeEach
    void setUp() {
        SimpleCacheManager mgr = new SimpleCacheManager();
        mgr.setCaches(List.of(new CaffeineCache(CACHE_NAME, Caffeine.newBuilder().build())));
        mgr.initializeCaches();
        cache = new TestCache(mgr);
    }

    @Test
    void evictByPrefix_removesOnlyMatchingTenant() {
        Key t1a = new Key("t1", "a");
        Key t1b = new Key("t1", "b");
        Key t2a = new Key("t2", "a");
        cache.put(t1a, "v1");
        cache.put(t1b, "v2");
        cache.put(t2a, "v3");

        cache.evictByPrefix("{t1}");

        assertThat(cache.get(t1a)).as("t1 的键应被清除").isNull();
        assertThat(cache.get(t1b)).as("t1 的键应被清除").isNull();
        // 这条是"清过头"的对照：只传了 {t1} 前缀，t2 必须不受影响
        assertThat(cache.get(t2a)).as("其它租户的键不应受影响").isNotNull();
        assertThat(cache.get(t2a).get()).isEqualTo("v3");
    }

    /**
     * 区分 startsWith 与 contains 的关键用例：{@code {t2}{t1}x} **包含** "{t1}" 但不以它开头。
     * 没有这条，把实现误写成 contains 也照样全绿 —— 第一版就缺它，用注入破坏法才发现
     * 那组用例对这个区别不敏感。
     */
    @Test
    void evictByPrefix_matchesPrefixNotSubstring() {
        Key target = new Key("t1", "a");
        Key lookalike = new Key("t2", "{t1}x");   // toString = {t2}{t1}x
        cache.put(target, "v1");
        cache.put(lookalike, "v2");

        cache.evictByPrefix("{t1}");

        assertThat(cache.get(target)).as("以前缀开头的键应被清除").isNull();
        assertThat(cache.get(lookalike))
                .as("仅在中间包含前缀的键不应被清除（证明是 startsWith 而非 contains）")
                .isNotNull();
    }

    @Test
    void evictByPrefix_nonMatchingPrefixKeepsEverything() {
        Key t1a = new Key("t1", "a");
        cache.put(t1a, "v1");

        cache.evictByPrefix("{nosuchtenant}");

        assertThat(cache.get(t1a)).as("前缀不命中时不应清除任何键").isNotNull();
    }

    /**
     * 前缀**不含 cacheName** —— Caffeine 的键是 K 对象，cacheName 只出现在 Redis 的
     * raw key 里。误拼 cacheName 会一条都匹配不上，而且不报错，所以用一条用例钉住。
     */
    @Test
    void evictByPrefix_prefixMustNotIncludeCacheName() {
        Key t1a = new Key("t1", "a");
        cache.put(t1a, "v1");

        cache.evictByPrefix(CACHE_NAME + "{t1}");

        assertThat(cache.get(t1a))
                .as("带 cacheName 的前缀不该命中 Caffeine 的键（证明比较的是 key.toString()）")
                .isNotNull();
    }
}
