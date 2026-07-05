package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.PaletteSettings;

@Schema
@JsonIgnoreProperties(ignoreUnknown=true)
public class WhiteLabelingParams {
    @Schema(description="Logo image URL", example="https://company.com/images/logo.png")
    protected String logoImageUrl;
    @Schema(description="The height of a logo container. Logo image will be automatically scaled.")
    protected Integer logoImageHeight;
    @Schema(description="White-labeled name of the platform", example="My Company IoT Platform")
    protected String appTitle;
    @Schema(description="JSON object that contains website icon url and type")
    protected Favicon favicon;
    @Schema(description="Complex JSON that describes structure of the Angular Material Palette. See [theming](https://material.angular.io/guide/theming) for more details")
    protected PaletteSettings paletteSettings;
    @Schema(description="Base URL for help link")
    protected String helpLinkBaseUrl;
    @Schema(description="Base URL for the repository with the UI help components (markdown)")
    protected String uiHelpBaseUrl;
    @Schema(description="Enable or Disable help links")
    protected Boolean enableHelpLinks;
    @Schema(description="Enable white-labeling", accessMode=Schema.AccessMode.READ_ONLY)
    protected boolean whiteLabelingEnabled = true;
    @Schema(description="Show platform name and version on UI and login screen")
    protected Boolean showNameVersion;
    @Schema(description="White-labeled platform name")
    protected String platformName;
    @Schema(description="White-labeled platform version")
    protected String platformVersion;
    @Schema(description="Custom CSS content")
    protected String customCss;
    @Schema(description="Hide device connectivity dialog")
    protected Boolean hideConnectivityDialog;
    @Schema(description="Override Trendz Add-on name")
    protected Boolean overrideTrendzName;

    public WhiteLabelingParams merge(WhiteLabelingParams otherWlParams) {
        if (StringUtils.isEmpty(this.logoImageUrl)) {
            this.logoImageUrl = otherWlParams.logoImageUrl;
        }
        if (this.logoImageHeight == null) {
            this.logoImageHeight = otherWlParams.logoImageHeight;
        }
        if (StringUtils.isEmpty(this.appTitle)) {
            this.appTitle = otherWlParams.appTitle;
        }
        if (this.favicon == null || StringUtils.isEmpty(this.favicon.getUrl())) {
            this.favicon = otherWlParams.favicon;
        }
        if (this.paletteSettings == null) {
            this.paletteSettings = otherWlParams.paletteSettings;
        } else if (otherWlParams.paletteSettings != null) {
            this.paletteSettings.merge(otherWlParams.paletteSettings);
        }
        if (otherWlParams.helpLinkBaseUrl != null) {
            this.helpLinkBaseUrl = otherWlParams.helpLinkBaseUrl;
        }
        if (otherWlParams.uiHelpBaseUrl != null) {
            this.uiHelpBaseUrl = otherWlParams.uiHelpBaseUrl;
        }
        if (otherWlParams.enableHelpLinks != null) {
            this.enableHelpLinks = otherWlParams.enableHelpLinks;
        }
        if (this.showNameVersion == null) {
            this.showNameVersion = otherWlParams.showNameVersion;
            this.platformName = otherWlParams.platformName;
            this.platformVersion = otherWlParams.platformVersion;
        }
        if (!StringUtils.isEmpty(otherWlParams.customCss)) {
            this.customCss = StringUtils.isEmpty(this.customCss) ? otherWlParams.customCss : otherWlParams.customCss + "\n" + this.customCss;
        }
        if (this.hideConnectivityDialog == null) {
            this.hideConnectivityDialog = false;
        }
        if (this.overrideTrendzName == null) {
            this.overrideTrendzName = otherWlParams.overrideTrendzName;
        }
        return this;
    }

    @Generated
    public WhiteLabelingParams() {
    }

    @Generated
    public String getLogoImageUrl() {
        return this.logoImageUrl;
    }

    @Generated
    public Integer getLogoImageHeight() {
        return this.logoImageHeight;
    }

    @Generated
    public String getAppTitle() {
        return this.appTitle;
    }

    @Generated
    public Favicon getFavicon() {
        return this.favicon;
    }

    @Generated
    public PaletteSettings getPaletteSettings() {
        return this.paletteSettings;
    }

    @Generated
    public String getHelpLinkBaseUrl() {
        return this.helpLinkBaseUrl;
    }

    @Generated
    public String getUiHelpBaseUrl() {
        return this.uiHelpBaseUrl;
    }

    @Generated
    public Boolean getEnableHelpLinks() {
        return this.enableHelpLinks;
    }

    @Generated
    public boolean isWhiteLabelingEnabled() {
        return this.whiteLabelingEnabled;
    }

    @Generated
    public Boolean getShowNameVersion() {
        return this.showNameVersion;
    }

    @Generated
    public String getPlatformName() {
        return this.platformName;
    }

    @Generated
    public String getPlatformVersion() {
        return this.platformVersion;
    }

    @Generated
    public String getCustomCss() {
        return this.customCss;
    }

    @Generated
    public Boolean getHideConnectivityDialog() {
        return this.hideConnectivityDialog;
    }

    @Generated
    public Boolean getOverrideTrendzName() {
        return this.overrideTrendzName;
    }

    @Generated
    public void setLogoImageUrl(String logoImageUrl) {
        this.logoImageUrl = logoImageUrl;
    }

    @Generated
    public void setLogoImageHeight(Integer logoImageHeight) {
        this.logoImageHeight = logoImageHeight;
    }

    @Generated
    public void setAppTitle(String appTitle) {
        this.appTitle = appTitle;
    }

    @Generated
    public void setFavicon(Favicon favicon) {
        this.favicon = favicon;
    }

    @Generated
    public void setPaletteSettings(PaletteSettings paletteSettings) {
        this.paletteSettings = paletteSettings;
    }

    @Generated
    public void setHelpLinkBaseUrl(String helpLinkBaseUrl) {
        this.helpLinkBaseUrl = helpLinkBaseUrl;
    }

    @Generated
    public void setUiHelpBaseUrl(String uiHelpBaseUrl) {
        this.uiHelpBaseUrl = uiHelpBaseUrl;
    }

    @Generated
    public void setEnableHelpLinks(Boolean enableHelpLinks) {
        this.enableHelpLinks = enableHelpLinks;
    }

    @Generated
    public void setWhiteLabelingEnabled(boolean whiteLabelingEnabled) {
        this.whiteLabelingEnabled = whiteLabelingEnabled;
    }

    @Generated
    public void setShowNameVersion(Boolean showNameVersion) {
        this.showNameVersion = showNameVersion;
    }

    @Generated
    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    @Generated
    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    @Generated
    public void setCustomCss(String customCss) {
        this.customCss = customCss;
    }

    @Generated
    public void setHideConnectivityDialog(Boolean hideConnectivityDialog) {
        this.hideConnectivityDialog = hideConnectivityDialog;
    }

    @Generated
    public void setOverrideTrendzName(Boolean overrideTrendzName) {
        this.overrideTrendzName = overrideTrendzName;
    }

    @Generated
    public String toString() {
        return "WhiteLabelingParams(logoImageUrl=" + this.getLogoImageUrl() + ", logoImageHeight=" + this.getLogoImageHeight() + ", appTitle=" + this.getAppTitle() + ", favicon=" + String.valueOf(this.getFavicon()) + ", paletteSettings=" + String.valueOf(this.getPaletteSettings()) + ", helpLinkBaseUrl=" + this.getHelpLinkBaseUrl() + ", uiHelpBaseUrl=" + this.getUiHelpBaseUrl() + ", enableHelpLinks=" + this.getEnableHelpLinks() + ", whiteLabelingEnabled=" + this.isWhiteLabelingEnabled() + ", showNameVersion=" + this.getShowNameVersion() + ", platformName=" + this.getPlatformName() + ", platformVersion=" + this.getPlatformVersion() + ", customCss=" + this.getCustomCss() + ", hideConnectivityDialog=" + this.getHideConnectivityDialog() + ", overrideTrendzName=" + this.getOverrideTrendzName() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WhiteLabelingParams)) {
            return false;
        }
        WhiteLabelingParams other = (WhiteLabelingParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isWhiteLabelingEnabled() != other.isWhiteLabelingEnabled()) {
            return false;
        }
        Integer this$logoImageHeight = this.getLogoImageHeight();
        Integer other$logoImageHeight = other.getLogoImageHeight();
        if (this$logoImageHeight == null ? other$logoImageHeight != null : !((Object)this$logoImageHeight).equals(other$logoImageHeight)) {
            return false;
        }
        Boolean this$enableHelpLinks = this.getEnableHelpLinks();
        Boolean other$enableHelpLinks = other.getEnableHelpLinks();
        if (this$enableHelpLinks == null ? other$enableHelpLinks != null : !((Object)this$enableHelpLinks).equals(other$enableHelpLinks)) {
            return false;
        }
        Boolean this$showNameVersion = this.getShowNameVersion();
        Boolean other$showNameVersion = other.getShowNameVersion();
        if (this$showNameVersion == null ? other$showNameVersion != null : !((Object)this$showNameVersion).equals(other$showNameVersion)) {
            return false;
        }
        Boolean this$hideConnectivityDialog = this.getHideConnectivityDialog();
        Boolean other$hideConnectivityDialog = other.getHideConnectivityDialog();
        if (this$hideConnectivityDialog == null ? other$hideConnectivityDialog != null : !((Object)this$hideConnectivityDialog).equals(other$hideConnectivityDialog)) {
            return false;
        }
        Boolean this$overrideTrendzName = this.getOverrideTrendzName();
        Boolean other$overrideTrendzName = other.getOverrideTrendzName();
        if (this$overrideTrendzName == null ? other$overrideTrendzName != null : !((Object)this$overrideTrendzName).equals(other$overrideTrendzName)) {
            return false;
        }
        String this$logoImageUrl = this.getLogoImageUrl();
        String other$logoImageUrl = other.getLogoImageUrl();
        if (this$logoImageUrl == null ? other$logoImageUrl != null : !this$logoImageUrl.equals(other$logoImageUrl)) {
            return false;
        }
        String this$appTitle = this.getAppTitle();
        String other$appTitle = other.getAppTitle();
        if (this$appTitle == null ? other$appTitle != null : !this$appTitle.equals(other$appTitle)) {
            return false;
        }
        Favicon this$favicon = this.getFavicon();
        Favicon other$favicon = other.getFavicon();
        if (this$favicon == null ? other$favicon != null : !((Object)this$favicon).equals(other$favicon)) {
            return false;
        }
        PaletteSettings this$paletteSettings = this.getPaletteSettings();
        PaletteSettings other$paletteSettings = other.getPaletteSettings();
        if (this$paletteSettings == null ? other$paletteSettings != null : !((Object)this$paletteSettings).equals(other$paletteSettings)) {
            return false;
        }
        String this$helpLinkBaseUrl = this.getHelpLinkBaseUrl();
        String other$helpLinkBaseUrl = other.getHelpLinkBaseUrl();
        if (this$helpLinkBaseUrl == null ? other$helpLinkBaseUrl != null : !this$helpLinkBaseUrl.equals(other$helpLinkBaseUrl)) {
            return false;
        }
        String this$uiHelpBaseUrl = this.getUiHelpBaseUrl();
        String other$uiHelpBaseUrl = other.getUiHelpBaseUrl();
        if (this$uiHelpBaseUrl == null ? other$uiHelpBaseUrl != null : !this$uiHelpBaseUrl.equals(other$uiHelpBaseUrl)) {
            return false;
        }
        String this$platformName = this.getPlatformName();
        String other$platformName = other.getPlatformName();
        if (this$platformName == null ? other$platformName != null : !this$platformName.equals(other$platformName)) {
            return false;
        }
        String this$platformVersion = this.getPlatformVersion();
        String other$platformVersion = other.getPlatformVersion();
        if (this$platformVersion == null ? other$platformVersion != null : !this$platformVersion.equals(other$platformVersion)) {
            return false;
        }
        String this$customCss = this.getCustomCss();
        String other$customCss = other.getCustomCss();
        return !(this$customCss == null ? other$customCss != null : !this$customCss.equals(other$customCss));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof WhiteLabelingParams;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isWhiteLabelingEnabled() ? 79 : 97);
        Integer $logoImageHeight = this.getLogoImageHeight();
        result = result * 59 + ($logoImageHeight == null ? 43 : ((Object)$logoImageHeight).hashCode());
        Boolean $enableHelpLinks = this.getEnableHelpLinks();
        result = result * 59 + ($enableHelpLinks == null ? 43 : ((Object)$enableHelpLinks).hashCode());
        Boolean $showNameVersion = this.getShowNameVersion();
        result = result * 59 + ($showNameVersion == null ? 43 : ((Object)$showNameVersion).hashCode());
        Boolean $hideConnectivityDialog = this.getHideConnectivityDialog();
        result = result * 59 + ($hideConnectivityDialog == null ? 43 : ((Object)$hideConnectivityDialog).hashCode());
        Boolean $overrideTrendzName = this.getOverrideTrendzName();
        result = result * 59 + ($overrideTrendzName == null ? 43 : ((Object)$overrideTrendzName).hashCode());
        String $logoImageUrl = this.getLogoImageUrl();
        result = result * 59 + ($logoImageUrl == null ? 43 : $logoImageUrl.hashCode());
        String $appTitle = this.getAppTitle();
        result = result * 59 + ($appTitle == null ? 43 : $appTitle.hashCode());
        Favicon $favicon = this.getFavicon();
        result = result * 59 + ($favicon == null ? 43 : ((Object)$favicon).hashCode());
        PaletteSettings $paletteSettings = this.getPaletteSettings();
        result = result * 59 + ($paletteSettings == null ? 43 : ((Object)$paletteSettings).hashCode());
        String $helpLinkBaseUrl = this.getHelpLinkBaseUrl();
        result = result * 59 + ($helpLinkBaseUrl == null ? 43 : $helpLinkBaseUrl.hashCode());
        String $uiHelpBaseUrl = this.getUiHelpBaseUrl();
        result = result * 59 + ($uiHelpBaseUrl == null ? 43 : $uiHelpBaseUrl.hashCode());
        String $platformName = this.getPlatformName();
        result = result * 59 + ($platformName == null ? 43 : $platformName.hashCode());
        String $platformVersion = this.getPlatformVersion();
        result = result * 59 + ($platformVersion == null ? 43 : $platformVersion.hashCode());
        String $customCss = this.getCustomCss();
        result = result * 59 + ($customCss == null ? 43 : $customCss.hashCode());
        return result;
    }
}

