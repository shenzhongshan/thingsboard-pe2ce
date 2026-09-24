package org.thingsboard.server.common.data.signup;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Generated;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.selfregistration.SignUpFieldId;

@Schema
public class SignUpRequest {
    @Schema(description="List of sign-up form fields")
    protected Map<SignUpFieldId, String> fields;
    @Schema(description="Response from reCAPTCHA validation")
    private String recaptchaResponse;
    @Schema(description="Local captcha challenge id, returned by GET /api/noauth/selfRegistration/localCaptcha. Only used when the self-registration captcha version is 'local'.")
    private String captchaId;
    @Schema(description="User-typed answer for the local captcha challenge. Only used when the self-registration captcha version is 'local'.")
    private String captchaAnswer;
    @Schema(description="For mobile apps only. Mobile app package name")
    private String pkgName;
    @Schema(description="For mobile apps only. Mobile app package platform")
    private PlatformType platform;
    @Schema(description="For mobile apps only. Mobile app secret")
    private String appSecret;

    @Generated
    public Map<SignUpFieldId, String> getFields() {
        return this.fields;
    }

    @Generated
    public String getRecaptchaResponse() {
        return this.recaptchaResponse;
    }

    @Generated
    public String getCaptchaId() {
        return this.captchaId;
    }

    @Generated
    public String getCaptchaAnswer() {
        return this.captchaAnswer;
    }

    @Generated
    public String getPkgName() {
        return this.pkgName;
    }

    @Generated
    public PlatformType getPlatform() {
        return this.platform;
    }

    @Generated
    public String getAppSecret() {
        return this.appSecret;
    }

    @Generated
    public void setFields(Map<SignUpFieldId, String> fields) {
        this.fields = fields;
    }

    @Generated
    public void setRecaptchaResponse(String recaptchaResponse) {
        this.recaptchaResponse = recaptchaResponse;
    }

    @Generated
    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    @Generated
    public void setCaptchaAnswer(String captchaAnswer) {
        this.captchaAnswer = captchaAnswer;
    }

    @Generated
    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    @Generated
    public void setPlatform(PlatformType platform) {
        this.platform = platform;
    }

    @Generated
    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SignUpRequest)) {
            return false;
        }
        SignUpRequest other = (SignUpRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Map<SignUpFieldId, String> this$fields = this.getFields();
        Map<SignUpFieldId, String> other$fields = other.getFields();
        if (this$fields == null ? other$fields != null : !((Object)this$fields).equals(other$fields)) {
            return false;
        }
        String this$recaptchaResponse = this.getRecaptchaResponse();
        String other$recaptchaResponse = other.getRecaptchaResponse();
        if (this$recaptchaResponse == null ? other$recaptchaResponse != null : !this$recaptchaResponse.equals(other$recaptchaResponse)) {
            return false;
        }
        String this$pkgName = this.getPkgName();
        String other$pkgName = other.getPkgName();
        if (this$pkgName == null ? other$pkgName != null : !this$pkgName.equals(other$pkgName)) {
            return false;
        }
        PlatformType this$platform = this.getPlatform();
        PlatformType other$platform = other.getPlatform();
        if (this$platform == null ? other$platform != null : !((Object)((Object)this$platform)).equals((Object)other$platform)) {
            return false;
        }
        String this$appSecret = this.getAppSecret();
        String other$appSecret = other.getAppSecret();
        return !(this$appSecret == null ? other$appSecret != null : !this$appSecret.equals(other$appSecret));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SignUpRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Map<SignUpFieldId, String> $fields = this.getFields();
        result = result * 59 + ($fields == null ? 43 : ((Object)$fields).hashCode());
        String $recaptchaResponse = this.getRecaptchaResponse();
        result = result * 59 + ($recaptchaResponse == null ? 43 : $recaptchaResponse.hashCode());
        String $pkgName = this.getPkgName();
        result = result * 59 + ($pkgName == null ? 43 : $pkgName.hashCode());
        PlatformType $platform = this.getPlatform();
        result = result * 59 + ($platform == null ? 43 : ((Object)((Object)$platform)).hashCode());
        String $appSecret = this.getAppSecret();
        result = result * 59 + ($appSecret == null ? 43 : $appSecret.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SignUpRequest(fields=" + String.valueOf(this.getFields()) + ", recaptchaResponse=" + this.getRecaptchaResponse() + ", pkgName=" + this.getPkgName() + ", platform=" + String.valueOf((Object)this.getPlatform()) + ", appSecret=" + this.getAppSecret() + ")";
    }
}

