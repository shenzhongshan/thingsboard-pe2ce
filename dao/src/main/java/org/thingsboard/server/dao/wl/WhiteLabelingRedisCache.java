package org.thingsboard.server.dao.wl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.cache.TbRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.dao.wl.WhiteLabelingCacheKey;

@ConditionalOnProperty(prefix="cache", value={"type"}, havingValue="redis")
@Service(value="WhiteLabelingCache")
public class WhiteLabelingRedisCache
extends RedisTbTransactionalCache<WhiteLabelingCacheKey, WhiteLabeling> {
    public WhiteLabelingRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.WHITE_LABELING_CACHE, cacheSpecsMap, connectionFactory, configuration, (TbRedisSerializer)new TbJsonRedisSerializer(WhiteLabeling.class));
    }
}

