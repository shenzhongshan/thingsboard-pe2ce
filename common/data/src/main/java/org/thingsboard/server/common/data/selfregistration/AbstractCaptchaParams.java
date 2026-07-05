package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;

@Schema
public abstract class AbstractCaptchaParams
implements CaptchaParams {
    @Schema(description="Captcha site key for 'I'm not a robot' validation")
    protected String siteKey;
    @Schema(description="Optional action name used for logging (for captcha version 'v3' and 'enterprise')")
    protected String logActionName;
    @Schema(description="Secret key to validate the Captcha. Should match the Captcha Site Key.")
    private String secretKey;

    @Generated
    public String getSiteKey() {
        return this.siteKey;
    }

    @Generated
    public String getLogActionName() {
        return this.logActionName;
    }

    @Generated
    public String getSecretKey() {
        return this.secretKey;
    }

    @Generated
    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }

    @Generated
    public void setLogActionName(String logActionName) {
        this.logActionName = logActionName;
    }

    @Generated
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractCaptchaParams)) {
            return false;
        }
        AbstractCaptchaParams other = (AbstractCaptchaParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$siteKey = this.getSiteKey();
        String other$siteKey = other.getSiteKey();
        if (this$siteKey == null ? other$siteKey != null : !this$siteKey.equals(other$siteKey)) {
            return false;
        }
        String this$logActionName = this.getLogActionName();
        String other$logActionName = other.getLogActionName();
        if (this$logActionName == null ? other$logActionName != null : !this$logActionName.equals(other$logActionName)) {
            return false;
        }
        String this$secretKey = this.getSecretKey();
        String other$secretKey = other.getSecretKey();
        return !(this$secretKey == null ? other$secretKey != null : !this$secretKey.equals(other$secretKey));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractCaptchaParams;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $siteKey = this.getSiteKey();
        result = result * 59 + ($siteKey == null ? 43 : $siteKey.hashCode());
        String $logActionName = this.getLogActionName();
        result = result * 59 + ($logActionName == null ? 43 : $logActionName.hashCode());
        String $secretKey = this.getSecretKey();
        result = result * 59 + ($secretKey == null ? 43 : $secretKey.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "AbstractCaptchaParams(siteKey=" + this.getSiteKey() + ", logActionName=" + this.getLogActionName() + ", secretKey=" + this.getSecretKey() + ")";
    }

    @Generated
    public AbstractCaptchaParams() {
    }
}

