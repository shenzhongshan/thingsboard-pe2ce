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

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Alternate local captcha backend, using cn.hutool:hutool-captcha
 * ("Happy-Captcha" was requested by name but its exact Maven coordinates could not be confirmed and
 * no matching jar existed locally; Hutool is a well-known, actively maintained, Maven-Central-hosted
 * equivalent - a distorted-line-noise image captcha with the same shape). Select via
 * {@code security.captcha.local.provider=hutool}.
 */
@ConditionalOnProperty(prefix = "security.captcha.local", value = "provider", havingValue = "hutool")
@Service("LocalCaptchaGenerator")
public class HutoolLocalCaptchaGenerator implements LocalCaptchaGenerator {

    private final int width;
    private final int height;
    private final int length;

    public HutoolLocalCaptchaGenerator(@Value("${security.captcha.local.width:160}") int width,
                                       @Value("${security.captcha.local.height:60}") int height,
                                       @Value("${security.captcha.local.length:5}") int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    @Override
    public LocalCaptchaImage generate() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height, length, 20);
        return new LocalCaptchaImage(captcha.getCode(), captcha.getImageBytes());
    }

}
