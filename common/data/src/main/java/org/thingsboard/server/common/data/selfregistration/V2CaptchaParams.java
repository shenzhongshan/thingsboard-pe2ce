package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.selfregistration.AbstractCaptchaParams;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.CaptchaVersion;

@Schema
public class V2CaptchaParams
extends AbstractCaptchaParams {
    public V2CaptchaParams(String siteKey) {
        this.siteKey = siteKey;
    }

    @Override
    public String getVersion() {
        return CaptchaVersion.V_2.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        return new V2CaptchaParams(this.siteKey);
    }

    @Generated
    public V2CaptchaParams() {
    }
}

