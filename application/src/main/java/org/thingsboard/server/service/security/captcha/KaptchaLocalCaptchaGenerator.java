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

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Default local captcha backend, using the classic com.github.penggle:kaptcha library.
 */
@ConditionalOnProperty(prefix = "security.captcha.local", value = "provider", havingValue = "kaptcha", matchIfMissing = true)
@Service("LocalCaptchaGenerator")
public class KaptchaLocalCaptchaGenerator implements LocalCaptchaGenerator {

    // excludes 0/O/1/I/l - already hard to tell apart even before Kaptcha's own distortion/noise
    private static final String CHAR_STRING = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Producer producer;

    public KaptchaLocalCaptchaGenerator(@Value("${security.captcha.local.width:160}") int width,
                                        @Value("${security.captcha.local.height:60}") int height,
                                        @Value("${security.captcha.local.length:5}") int length) {
        Properties props = new Properties();
        props.setProperty("kaptcha.image.width", String.valueOf(width));
        props.setProperty("kaptcha.image.height", String.valueOf(height));
        props.setProperty("kaptcha.textproducer.char.length", String.valueOf(length));
        props.setProperty("kaptcha.textproducer.char.string", CHAR_STRING);
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        kaptcha.setConfig(new Config(props));
        this.producer = kaptcha;
    }

    @Override
    public LocalCaptchaImage generate() {
        String answer = producer.createText();
        BufferedImage image = producer.createImage(answer);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return new LocalCaptchaImage(answer, out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode local captcha image", e);
        }
    }

}
