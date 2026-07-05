package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;

public class MobileRedirectParams {
    @Schema(description="Mobile application verification settings. Used for callback to mobile application once user is registered.")
    private String scheme;
    @Schema(description="Mobile application verification settings. Used for callback to mobile application once user is registered.")
    private String host;

    @Generated
    public MobileRedirectParams() {
    }

    @Generated
    public String getScheme() {
        return this.scheme;
    }

    @Generated
    public String getHost() {
        return this.host;
    }

    @Generated
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Generated
    public void setHost(String host) {
        this.host = host;
    }

    @Generated
    public String toString() {
        return "MobileRedirectParams(scheme=" + this.getScheme() + ", host=" + this.getHost() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MobileRedirectParams)) {
            return false;
        }
        MobileRedirectParams other = (MobileRedirectParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$scheme = this.getScheme();
        String other$scheme = other.getScheme();
        if (this$scheme == null ? other$scheme != null : !this$scheme.equals(other$scheme)) {
            return false;
        }
        String this$host = this.getHost();
        String other$host = other.getHost();
        return !(this$host == null ? other$host != null : !this$host.equals(other$host));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof MobileRedirectParams;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $scheme = this.getScheme();
        result = result * 59 + ($scheme == null ? 43 : $scheme.hashCode());
        String $host = this.getHost();
        result = result * 59 + ($host == null ? 43 : $host.hashCode());
        return result;
    }
}

