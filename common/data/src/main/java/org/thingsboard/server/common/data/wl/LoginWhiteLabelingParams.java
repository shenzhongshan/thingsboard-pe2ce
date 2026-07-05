package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LoginWhiteLabelingParams
extends WhiteLabelingParams {
    @Schema(description="Login page background color", example="#d90f0f")
    private String pageBackgroundColor;
    @Schema(description="Enable/Disable dark foreground")
    private boolean darkForeground;
    @Schema(description="Domain id")
    private DomainId domainId;
    @Schema(description="Base URL for the activation link, etc", example="https://iot.mycompany.com")
    private String baseUrl;
    @Schema(description="Prohibit use of other URLs. It is recommended to enable this setting", example="true")
    private boolean prohibitDifferentUrl;
    @Schema(description="Id of the settings object that store this parameters")
    private String adminSettingsId;
    @Schema(description="Show platform name and version on login page")
    private Boolean showNameBottom;

    public LoginWhiteLabelingParams merge(LoginWhiteLabelingParams otherWlParams) {
        Integer prevLogoImageHeight = this.logoImageHeight;
        super.merge(otherWlParams);
        if (prevLogoImageHeight == null) {
            this.logoImageHeight = null;
        }
        if (this.showNameBottom == null) {
            this.showNameBottom = otherWlParams.showNameBottom;
        }
        if (this.pageBackgroundColor == null) {
            this.pageBackgroundColor = otherWlParams.pageBackgroundColor;
        }
        return this;
    }

    @Generated
    public LoginWhiteLabelingParams() {
    }

    @Generated
    public String getPageBackgroundColor() {
        return this.pageBackgroundColor;
    }

    @Generated
    public boolean isDarkForeground() {
        return this.darkForeground;
    }

    @Generated
    public DomainId getDomainId() {
        return this.domainId;
    }

    @Generated
    public String getBaseUrl() {
        return this.baseUrl;
    }

    @Generated
    public boolean isProhibitDifferentUrl() {
        return this.prohibitDifferentUrl;
    }

    @Generated
    public String getAdminSettingsId() {
        return this.adminSettingsId;
    }

    @Generated
    public Boolean getShowNameBottom() {
        return this.showNameBottom;
    }

    @Generated
    public void setPageBackgroundColor(String pageBackgroundColor) {
        this.pageBackgroundColor = pageBackgroundColor;
    }

    @Generated
    public void setDarkForeground(boolean darkForeground) {
        this.darkForeground = darkForeground;
    }

    @Generated
    public void setDomainId(DomainId domainId) {
        this.domainId = domainId;
    }

    @Generated
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Generated
    public void setProhibitDifferentUrl(boolean prohibitDifferentUrl) {
        this.prohibitDifferentUrl = prohibitDifferentUrl;
    }

    @Generated
    public void setAdminSettingsId(String adminSettingsId) {
        this.adminSettingsId = adminSettingsId;
    }

    @Generated
    public void setShowNameBottom(Boolean showNameBottom) {
        this.showNameBottom = showNameBottom;
    }

    @Override
    @Generated
    public String toString() {
        return "LoginWhiteLabelingParams(pageBackgroundColor=" + this.getPageBackgroundColor() + ", darkForeground=" + this.isDarkForeground() + ", domainId=" + String.valueOf(this.getDomainId()) + ", baseUrl=" + this.getBaseUrl() + ", prohibitDifferentUrl=" + this.isProhibitDifferentUrl() + ", adminSettingsId=" + this.getAdminSettingsId() + ", showNameBottom=" + this.getShowNameBottom() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LoginWhiteLabelingParams)) {
            return false;
        }
        LoginWhiteLabelingParams other = (LoginWhiteLabelingParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (this.isDarkForeground() != other.isDarkForeground()) {
            return false;
        }
        if (this.isProhibitDifferentUrl() != other.isProhibitDifferentUrl()) {
            return false;
        }
        Boolean this$showNameBottom = this.getShowNameBottom();
        Boolean other$showNameBottom = other.getShowNameBottom();
        if (this$showNameBottom == null ? other$showNameBottom != null : !((Object)this$showNameBottom).equals(other$showNameBottom)) {
            return false;
        }
        String this$pageBackgroundColor = this.getPageBackgroundColor();
        String other$pageBackgroundColor = other.getPageBackgroundColor();
        if (this$pageBackgroundColor == null ? other$pageBackgroundColor != null : !this$pageBackgroundColor.equals(other$pageBackgroundColor)) {
            return false;
        }
        DomainId this$domainId = this.getDomainId();
        DomainId other$domainId = other.getDomainId();
        if (this$domainId == null ? other$domainId != null : !((Object)this$domainId).equals(other$domainId)) {
            return false;
        }
        String this$baseUrl = this.getBaseUrl();
        String other$baseUrl = other.getBaseUrl();
        if (this$baseUrl == null ? other$baseUrl != null : !this$baseUrl.equals(other$baseUrl)) {
            return false;
        }
        String this$adminSettingsId = this.getAdminSettingsId();
        String other$adminSettingsId = other.getAdminSettingsId();
        return !(this$adminSettingsId == null ? other$adminSettingsId != null : !this$adminSettingsId.equals(other$adminSettingsId));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LoginWhiteLabelingParams;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        result = result * 59 + (this.isDarkForeground() ? 79 : 97);
        result = result * 59 + (this.isProhibitDifferentUrl() ? 79 : 97);
        Boolean $showNameBottom = this.getShowNameBottom();
        result = result * 59 + ($showNameBottom == null ? 43 : ((Object)$showNameBottom).hashCode());
        String $pageBackgroundColor = this.getPageBackgroundColor();
        result = result * 59 + ($pageBackgroundColor == null ? 43 : $pageBackgroundColor.hashCode());
        DomainId $domainId = this.getDomainId();
        result = result * 59 + ($domainId == null ? 43 : ((Object)$domainId).hashCode());
        String $baseUrl = this.getBaseUrl();
        result = result * 59 + ($baseUrl == null ? 43 : $baseUrl.hashCode());
        String $adminSettingsId = this.getAdminSettingsId();
        result = result * 59 + ($adminSettingsId == null ? 43 : $adminSettingsId.hashCode());
        return result;
    }
}

