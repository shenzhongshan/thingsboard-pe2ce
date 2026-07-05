package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.selfregistration.AbstractCaptchaParams;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.CaptchaVersion;

@Schema
public class V3CaptchaParams
extends AbstractCaptchaParams {
    public V3CaptchaParams(String siteKey, String logActionName) {
        this.siteKey = siteKey;
        this.logActionName = logActionName;
    }

    @Override
    public String getVersion() {
        return CaptchaVersion.V_3.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        return new V3CaptchaParams(this.siteKey, this.logActionName);
    }

    @Generated
    public V3CaptchaParams() {
    }
}

