package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.CaptchaVersion;

@Schema
public class EnterpriseCaptchaParams
implements CaptchaParams {
    @Schema(description="Your Google Cloud project ID")
    protected String projectId;
    @Schema(description="Service account credentials")
    private String serviceAccountCredentials;
    @Schema(description="Service account credentials file name")
    private String serviceAccountCredentialsFileName;
    @Schema(description="The reCAPTCHA key associated with android app.")
    protected String androidKey;
    @Schema(description="The reCAPTCHA key associated with iOS app.")
    protected String iosKey;
    @Schema(description="Optional action name used for logging")
    protected String logActionName;

    public EnterpriseCaptchaParams(String androidKey, String iOSKey, String logActionName) {
        this.androidKey = androidKey;
        this.iosKey = iOSKey;
        this.logActionName = logActionName;
    }

    @Override
    public String getVersion() {
        return CaptchaVersion.ENTERPRISE.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        if (platformType == PlatformType.ANDROID) {
            return new EnterpriseCaptchaParams(this.androidKey, null, this.logActionName);
        }
        if (platformType == PlatformType.IOS) {
            return new EnterpriseCaptchaParams(null, this.iosKey, this.logActionName);
        }
        return null;
    }

    @Generated
    public String getProjectId() {
        return this.projectId;
    }

    @Generated
    public String getServiceAccountCredentials() {
        return this.serviceAccountCredentials;
    }

    @Generated
    public String getServiceAccountCredentialsFileName() {
        return this.serviceAccountCredentialsFileName;
    }

    @Generated
    public String getAndroidKey() {
        return this.androidKey;
    }

    @Generated
    public String getIosKey() {
        return this.iosKey;
    }

    @Generated
    public String getLogActionName() {
        return this.logActionName;
    }

    @Generated
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Generated
    public void setServiceAccountCredentials(String serviceAccountCredentials) {
        this.serviceAccountCredentials = serviceAccountCredentials;
    }

    @Generated
    public void setServiceAccountCredentialsFileName(String serviceAccountCredentialsFileName) {
        this.serviceAccountCredentialsFileName = serviceAccountCredentialsFileName;
    }

    @Generated
    public void setAndroidKey(String androidKey) {
        this.androidKey = androidKey;
    }

    @Generated
    public void setIosKey(String iosKey) {
        this.iosKey = iosKey;
    }

    @Generated
    public void setLogActionName(String logActionName) {
        this.logActionName = logActionName;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EnterpriseCaptchaParams)) {
            return false;
        }
        EnterpriseCaptchaParams other = (EnterpriseCaptchaParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$projectId = this.getProjectId();
        String other$projectId = other.getProjectId();
        if (this$projectId == null ? other$projectId != null : !this$projectId.equals(other$projectId)) {
            return false;
        }
        String this$serviceAccountCredentials = this.getServiceAccountCredentials();
        String other$serviceAccountCredentials = other.getServiceAccountCredentials();
        if (this$serviceAccountCredentials == null ? other$serviceAccountCredentials != null : !this$serviceAccountCredentials.equals(other$serviceAccountCredentials)) {
            return false;
        }
        String this$serviceAccountCredentialsFileName = this.getServiceAccountCredentialsFileName();
        String other$serviceAccountCredentialsFileName = other.getServiceAccountCredentialsFileName();
        if (this$serviceAccountCredentialsFileName == null ? other$serviceAccountCredentialsFileName != null : !this$serviceAccountCredentialsFileName.equals(other$serviceAccountCredentialsFileName)) {
            return false;
        }
        String this$androidKey = this.getAndroidKey();
        String other$androidKey = other.getAndroidKey();
        if (this$androidKey == null ? other$androidKey != null : !this$androidKey.equals(other$androidKey)) {
            return false;
        }
        String this$iosKey = this.getIosKey();
        String other$iosKey = other.getIosKey();
        if (this$iosKey == null ? other$iosKey != null : !this$iosKey.equals(other$iosKey)) {
            return false;
        }
        String this$logActionName = this.getLogActionName();
        String other$logActionName = other.getLogActionName();
        return !(this$logActionName == null ? other$logActionName != null : !this$logActionName.equals(other$logActionName));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof EnterpriseCaptchaParams;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $projectId = this.getProjectId();
        result = result * 59 + ($projectId == null ? 43 : $projectId.hashCode());
        String $serviceAccountCredentials = this.getServiceAccountCredentials();
        result = result * 59 + ($serviceAccountCredentials == null ? 43 : $serviceAccountCredentials.hashCode());
        String $serviceAccountCredentialsFileName = this.getServiceAccountCredentialsFileName();
        result = result * 59 + ($serviceAccountCredentialsFileName == null ? 43 : $serviceAccountCredentialsFileName.hashCode());
        String $androidKey = this.getAndroidKey();
        result = result * 59 + ($androidKey == null ? 43 : $androidKey.hashCode());
        String $iosKey = this.getIosKey();
        result = result * 59 + ($iosKey == null ? 43 : $iosKey.hashCode());
        String $logActionName = this.getLogActionName();
        result = result * 59 + ($logActionName == null ? 43 : $logActionName.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "EnterpriseCaptchaParams(projectId=" + this.getProjectId() + ", serviceAccountCredentials=" + this.getServiceAccountCredentials() + ", serviceAccountCredentialsFileName=" + this.getServiceAccountCredentialsFileName() + ", androidKey=" + this.getAndroidKey() + ", iosKey=" + this.getIosKey() + ", logActionName=" + this.getLogActionName() + ")";
    }

    @Generated
    public EnterpriseCaptchaParams() {
    }
}

