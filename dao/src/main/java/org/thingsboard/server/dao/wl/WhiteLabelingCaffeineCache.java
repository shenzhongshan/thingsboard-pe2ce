package org.thingsboard.server.dao.wl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.dao.wl.WhiteLabelingCacheKey;

@ConditionalOnProperty(prefix="cache", value={"type"}, havingValue="caffeine", matchIfMissing=true)
@Service(value="WhiteLabelingCache")
public class WhiteLabelingCaffeineCache
extends CaffeineTbTransactionalCache<WhiteLabelingCacheKey, WhiteLabeling> {
    public WhiteLabelingCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.WHITE_LABELING_CACHE);
    }
}

