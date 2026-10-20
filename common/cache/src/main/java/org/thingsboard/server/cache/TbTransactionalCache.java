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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TbTransactionalCache<K extends Serializable, V extends Serializable> {

    String getCacheName();

    TbCacheValueWrapper<V> get(K key);

    void put(K key, V value);

    void putIfAbsent(K key, V value);

    void evict(K key);

    void evict(Collection<K> keys);

    void evictOrPut(K key, V value);

    /**
     * 按键前缀批量失效。用于"受影响的键集合未知"的场景 —— 例如一个实体分组被分配给
     * edge 后，该组全部成员的 related-edges 结果都变了，但成员列表此刻可能已不可枚举。
     *
     * <p><b>前缀不含 cacheName</b>：Redis 实现会自己拼上 cacheName（它的 raw key 是
     * {@code cacheName + key.toString()}），而 Caffeine 实现比较的是 {@code key.toString()}
     * 本身。调用方传的是纯 key 前缀，两种实现各自适配。传错了不会报错，只是一条都匹配不上。
     *
     * <p>Redis 下请让前缀以 {@code {hashTag}} 开头（如 {@code "{tenantId}"}），这样
     * 同前缀的键必然同 slot，cluster 模式只需扫描一个节点。参见 CLUSTER KEYSLOT。
     */
    void evictByPrefix(String prefix);

    TbCacheTransaction<K, V> newTransactionForKey(K key);

    /**
     * Note that all keys should be in the same cache slot for redis. You may control the cache slot using '{}' bracers.
     * See CLUSTER KEYSLOT command for more details.
     * @param keys - list of keys to use
     * @return transaction object
     */
    TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys);

    default V getOrFetchFromDB(K key, Supplier<V> dbCall, boolean cacheNullValue, boolean putToCache) {
        if (putToCache) {
            return getAndPutInTransaction(key, dbCall, cacheNullValue);
        } else {
            TbCacheValueWrapper<V> cacheValueWrapper = get(key);
            if (cacheValueWrapper != null) {
                return cacheValueWrapper.get();
            }
            return dbCall.get();
        }
    }

    default V getAndPutInTransaction(K key, Supplier<V> dbCall, boolean cacheNullValue) {
        return getAndPutInTransaction(key, dbCall, Function.identity(), Function.identity(), cacheNullValue);
    }

    default <R> R getAndPutInTransaction(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue) {
        TbCacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            V cacheValue = cacheValueWrapper.get();
            return cacheValue != null ? cacheValueToResult.apply(cacheValue) : null;
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            R dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.put(key, dbValueToCacheValue.apply(dbValue));
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

    default <R> R getOrFetchFromDB(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue, boolean putToCache) {
        if (putToCache) {
            return getAndPutInTransaction(key, dbCall, cacheValueToResult, dbValueToCacheValue, cacheNullValue);
        } else {
            TbCacheValueWrapper<V> cacheValueWrapper = get(key);
            if (cacheValueWrapper != null) {
                var cacheValue = cacheValueWrapper.get();
                return cacheValue == null ? null : cacheValueToResult.apply(cacheValue);
            }
            return dbCall.get();
        }
    }

}
