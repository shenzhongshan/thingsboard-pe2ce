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
package org.thingsboard.server.service.security.captcha;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecs;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.StringUtils;

import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Generates and verifies self-hosted (local) captcha challenges for self-registration.
 * <p>
 * Follows the {@code OtpBasedTwoFaProvider} pattern rather than the dual Caffeine/Redis cache-bean
 * pattern used elsewhere: it reads the plain {@link Cache} abstraction via {@link CacheManager} and
 * checks expiry itself off a stored timestamp. This is deliberate - {@code TBRedisCacheConfiguration}
 * does not apply the {@code cache.specs} TTL to cache names outside the
 * {@code RedisTbTransactionalCache} framework, so a bare {@code cacheManager.getCache(name)} cache
 * would never expire under Redis without this manual check.
 */
@Service
@RequiredArgsConstructor
public class LocalCaptchaService {

    private static final int CAPTCHA_ID_LENGTH = 24;
    private static final int DEFAULT_TTL_MINUTES = 2;

    private final LocalCaptchaGenerator generator;
    private final CacheManager cacheManager;
    private final CacheSpecsMap cacheSpecsMap;

    public LocalCaptchaChallenge createChallenge() {
        LocalCaptchaImage image = generator.generate();
        String captchaId = StringUtils.generateSafeToken(CAPTCHA_ID_LENGTH);
        cache().put(captchaId, new CaptchaAnswer(System.currentTimeMillis(), image.answerText()));
        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(image.imageBytes());
        return new LocalCaptchaChallenge(captchaId, dataUri);
    }

    /**
     * Verifies the answer and evicts the challenge regardless of outcome - a captcha challenge is
     * one-shot, whether the attempt succeeded or not, to block replay/brute-force of the same image.
     */
    public boolean verifyAndEvict(String captchaId, String userAnswer) {
        if (StringUtils.isEmpty(captchaId) || StringUtils.isEmpty(userAnswer)) {
            return false;
        }
        CaptchaAnswer stored = cache().get(captchaId, CaptchaAnswer.class);
        cache().evict(captchaId);
        if (stored == null) {
            return false;
        }
        if (System.currentTimeMillis() - stored.getTimestamp() > TimeUnit.MINUTES.toMillis(ttlMinutes())) {
            return false;
        }
        return stored.getAnswer().equalsIgnoreCase(userAnswer.trim());
    }

    private Cache cache() {
        return cacheManager.getCache(CacheConstants.CAPTCHA_ANSWER_CACHE);
    }

    private int ttlMinutes() {
        return Optional.ofNullable(cacheSpecsMap.getSpecs())
                .map(specs -> specs.get(CacheConstants.CAPTCHA_ANSWER_CACHE))
                .map(CacheSpecs::getTimeToLiveInMinutes)
                .orElse(DEFAULT_TTL_MINUTES);
    }

    @Data
    private static class CaptchaAnswer implements Serializable {
        private final long timestamp;
        private final String answer;
    }

}
